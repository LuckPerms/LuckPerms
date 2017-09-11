/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.bukkit.model;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import me.lucko.luckperms.common.caching.UserCache;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.verbose.CheckOrigin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.permissions.PermissionRemovedExecutor;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * PermissibleBase for LuckPerms.
 *
 * This class overrides all methods defined in PermissibleBase, and provides custom handling
 * from LuckPerms.
 *
 * This means that all permission checks made for a player are handled directly by the plugin.
 * Method behaviour is retained, but alternate implementation is used.
 *
 * "Hot" method calls, (namely #hasPermission) are significantly faster than the base implementation.
 *
 * This class is **thread safe**. This means that when LuckPerms is installed on the server,
 * is is safe to call Player#hasPermission asynchronously.
 */
@Getter
public class LPPermissible extends PermissibleBase {

    // the LuckPerms user this permissible references.
    private final User user;

    // the player this permissible is injected into.
    private final Player parent;

    // the luckperms plugin instance
    private final LPBukkitPlugin plugin;

    // the subscription manager, handling the players permission subscriptions.
    private final SubscriptionManager subscriptions;

    // the players previous permissible. (the one they had before this one was injected)
    @Setter
    private PermissibleBase oldPermissible = null;

    // if the permissible is currently active.
    private final AtomicBoolean active = new AtomicBoolean(false);

    // the permissions registered by PermissionAttachments.
    // stored in this format, as that's what is used by #getEffectivePermissions
    private final Map<String, PermissionAttachmentInfo> attachmentPermissions = new ConcurrentHashMap<>();

    // the attachments hooked onto the permissible.
    private final List<PermissionAttachment> attachments = Collections.synchronizedList(new ArrayList<>());

    public LPPermissible(@NonNull Player parent, @NonNull User user, @NonNull LPBukkitPlugin plugin) {
        super(parent);
        this.user = user;
        this.parent = parent;
        this.plugin = plugin;
        this.subscriptions = new SubscriptionManager(this);
    }

    @Override
    public boolean isPermissionSet(@NonNull String permission) {
        Tristate ts = user.getUserData().getPermissionData(calculateContexts()).getPermissionValue(permission, CheckOrigin.PLATFORM_LOOKUP_CHECK);
        return ts != Tristate.UNDEFINED || Permission.DEFAULT_PERMISSION.getValue(isOp());
    }

    @Override
    public boolean isPermissionSet(@NonNull Permission permission) {
        Tristate ts = user.getUserData().getPermissionData(calculateContexts()).getPermissionValue(permission.getName(), CheckOrigin.PLATFORM_LOOKUP_CHECK);
        if (ts != Tristate.UNDEFINED) {
            return true;
        }

        if (!plugin.getConfiguration().get(ConfigKeys.APPLY_BUKKIT_DEFAULT_PERMISSIONS)) {
            return Permission.DEFAULT_PERMISSION.getValue(isOp());
        } else {
            return permission.getDefault().getValue(isOp());
        }
    }

    @Override
    public boolean hasPermission(@NonNull String permission) {
        Tristate ts = user.getUserData().getPermissionData(calculateContexts()).getPermissionValue(permission, CheckOrigin.PLATFORM_PERMISSION_CHECK);
        return ts != Tristate.UNDEFINED ? ts.asBoolean() : Permission.DEFAULT_PERMISSION.getValue(isOp());
    }

    @Override
    public boolean hasPermission(@NonNull Permission permission) {
        Tristate ts = user.getUserData().getPermissionData(calculateContexts()).getPermissionValue(permission.getName(), CheckOrigin.PLATFORM_PERMISSION_CHECK);
        if (ts != Tristate.UNDEFINED) {
            return ts.asBoolean();
        }

        if (!plugin.getConfiguration().get(ConfigKeys.APPLY_BUKKIT_DEFAULT_PERMISSIONS)) {
            return Permission.DEFAULT_PERMISSION.getValue(isOp());
        } else {
            return permission.getDefault().getValue(isOp());
        }
    }

    /**
     * Updates the players subscriptions asynchronously
     */
    public void updateSubscriptionsAsync() {
        if (!active.get()) {
            return;
        }

        plugin.doAsync(this::updateSubscriptions);
    }

    /**
     * Updates the players subscriptions
     */
    public void updateSubscriptions() {
        if (!active.get()) {
            return;
        }

        UserCache cache = user.getUserData();

        // calculate their "active" permissions
        Set<String> ent = new HashSet<>(cache.getPermissionData(calculateContexts()).getImmutableBacking().keySet());

        // include defaults, if enabled.
        if (plugin.getConfiguration().get(ConfigKeys.APPLY_BUKKIT_DEFAULT_PERMISSIONS)) {
            if (parent.isOp()) {
                ent.addAll(plugin.getDefaultsProvider().getOpDefaults().keySet());
            } else {
                ent.addAll(plugin.getDefaultsProvider().getNonOpDefaults().keySet());
            }
        }

        subscriptions.subscribe(ent);
    }

    /**
     * Unsubscribes from all permissions asynchronously
     */
    public void unsubscribeFromAllAsync() {
        plugin.doAsync(this::unsubscribeFromAll);
    }

    /**
     * Unsubscribes from all permissions
     */
    public void unsubscribeFromAll() {
        subscriptions.subscribe(Collections.emptySet());
    }

    /**
     * Adds attachments to this permissible.
     *
     * @param attachments the attachments to add
     */
    public void addAttachments(Collection<PermissionAttachment> attachments) {
        this.attachments.addAll(attachments);
    }

    /**
     * Obtains a {@link Contexts} instance for the player.
     * Values are determined using the plugins ContextManager.
     *
     * @return the calculated contexts for the player.
     */
    public Contexts calculateContexts() {
        return plugin.getContextManager().getApplicableContexts(parent);
    }

    @Override
    public void setOp(boolean value) {
        parent.setOp(value);
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        Set<PermissionAttachmentInfo> perms = new HashSet<>();
        perms.addAll(attachmentPermissions.values());

        perms.addAll(
                user.getUserData().getPermissionData(calculateContexts()).getImmutableBacking().entrySet().stream()
                        .map(e -> new PermissionAttachmentInfo(parent, e.getKey(), null, e.getValue()))
                        .collect(Collectors.toList())
        );

        return perms;
    }

    @Override
    public PermissionAttachment addAttachment(@NonNull Plugin plugin, @NonNull String name, boolean value) {
        if (!plugin.isEnabled()) {
            throw new IllegalArgumentException("Plugin " + plugin.getDescription().getFullName() + " is not enabled");
        }

        PermissionAttachment result = addAttachment(plugin);
        result.setPermission(name, value);

        recalculatePermissions();

        return result;
    }

    @Override
    public PermissionAttachment addAttachment(@NonNull Plugin plugin) {
        if (!plugin.isEnabled()) {
            throw new IllegalArgumentException("Plugin " + plugin.getDescription().getFullName() + " is not enabled");
        }

        PermissionAttachment result = new PermissionAttachment(plugin, parent);

        attachments.add(result);
        recalculatePermissions();

        return result;
    }

    @Override
    public PermissionAttachment addAttachment(@NonNull Plugin plugin, @NonNull String name, boolean value, int ticks) {
        if (!plugin.isEnabled()) {
            throw new IllegalArgumentException("Plugin " + plugin.getDescription().getFullName() + " is not enabled");
        }

        PermissionAttachment result = addAttachment(plugin, ticks);
        if (result != null) {
            result.setPermission(name, value);
        }

        return result;
    }

    @Override
    public PermissionAttachment addAttachment(@NonNull Plugin plugin, int ticks) {
        if (!plugin.isEnabled()) {
            throw new IllegalArgumentException("Plugin " + plugin.getDescription().getFullName() + " is not enabled");
        }

        PermissionAttachment result = addAttachment(plugin);
        if (Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, result::remove, ticks) == -1) {
            Bukkit.getServer().getLogger().log(Level.WARNING, "Could not add PermissionAttachment to " + parent + " for plugin " + plugin.getDescription().getFullName() + ": Scheduler returned -1");
            result.remove();
            return null;
        } else {
            return result;
        }
    }

    @Override
    public void removeAttachment(@NonNull PermissionAttachment attachment) {
        if (attachments.contains(attachment)) {
            attachments.remove(attachment);
            PermissionRemovedExecutor ex = attachment.getRemovalCallback();

            if (ex != null) {
                ex.attachmentRemoved(attachment);
            }

            recalculatePermissions();
        } else {
            throw new IllegalArgumentException("Given attachment is not part of Permissible object " + parent);
        }
    }

    @Override
    public void recalculatePermissions() {
        recalculatePermissions(true);
    }

    public void recalculatePermissions(boolean invalidate) {
        if (attachmentPermissions == null) {
            return;
        }

        attachmentPermissions.clear();

        for (PermissionAttachment attachment : attachments) {
            calculateChildPermissions(attachment.getPermissions(), false, attachment);
        }

        if (invalidate) {
            user.getUserData().invalidatePermissionCalculators();
        }
    }

    @Override
    public synchronized void clearPermissions() {
        Set<String> perms = attachmentPermissions.keySet();

        for (String name : perms) {
            Bukkit.getServer().getPluginManager().unsubscribeFromPermission(name, parent);
        }

        attachmentPermissions.clear();
    }

    private void calculateChildPermissions(Map<String, Boolean> children, boolean invert, PermissionAttachment attachment) {
        for (Map.Entry<String, Boolean> e : children.entrySet()) {
            Permission perm = Bukkit.getServer().getPluginManager().getPermission(e.getKey());
            boolean value = e.getValue() ^ invert;
            String name = e.getKey().toLowerCase();

            attachmentPermissions.put(name, new PermissionAttachmentInfo(parent, name, attachment, value));
            Bukkit.getServer().getPluginManager().subscribeToPermission(name, parent);

            if (perm != null) {
                calculateChildPermissions(perm.getChildren(), !value, attachment);
            }
        }
    }
}
