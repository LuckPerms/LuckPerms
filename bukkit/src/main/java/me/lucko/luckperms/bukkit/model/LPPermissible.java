/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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
import me.lucko.luckperms.common.core.model.User;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.permissions.PermissionRemovedExecutor;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Modified PermissibleBase for LuckPerms
 */
@Getter
public class LPPermissible extends PermissibleBase {

    private final User user;
    private final Player parent;
    private final LPBukkitPlugin plugin;
    private final SubscriptionManager subscriptions;

    @Setter
    private PermissibleBase oldPermissible = null;

    private final AtomicBoolean active = new AtomicBoolean(false);

    // Attachment stuff.
    private final Map<String, PermissionAttachmentInfo> attachmentPermissions = new ConcurrentHashMap<>();
    private final List<PermissionAttachment> attachments = Collections.synchronizedList(new LinkedList<>());

    public LPPermissible(@NonNull Player parent, User user, LPBukkitPlugin plugin) {
        super(parent);
        this.user = user;
        this.parent = parent;
        this.plugin = plugin;
        this.subscriptions = new SubscriptionManager(this);

        // recalculatePermissions();
    }

    public void updateSubscriptionsAsync() {
        if (!active.get()) {
            return;
        }

        plugin.doAsync(this::updateSubscriptions);
    }

    public void updateSubscriptions() {
        if (!active.get()) {
            return;
        }

        UserCache cache = user.getUserData();
        if (cache == null) {
            return;
        }

        Set<String> ent = new HashSet<>(cache.getPermissionData(calculateContexts()).getImmutableBacking().keySet());

        if (parent.isOp()) {
            ent.addAll(plugin.getDefaultsProvider().getOpDefaults().keySet());
        } else {
            ent.addAll(plugin.getDefaultsProvider().getNonOpDefaults().keySet());
        }

        subscriptions.subscribe(ent);
    }

    public void unsubscribeFromAllAsync() {
        plugin.doAsync(this::unsubscribeFromAll);
    }

    public void unsubscribeFromAll() {
        subscriptions.subscribe(Collections.emptySet());
    }

    public void addAttachments(List<PermissionAttachment> attachments) {
        this.attachments.addAll(attachments);
    }

    public Contexts calculateContexts() {
        return new Contexts(
                plugin.getContextManager().getApplicableContext(parent),
                plugin.getConfiguration().get(ConfigKeys.INCLUDING_GLOBAL_PERMS),
                plugin.getConfiguration().get(ConfigKeys.INCLUDING_GLOBAL_WORLD_PERMS),
                true,
                plugin.getConfiguration().get(ConfigKeys.APPLYING_GLOBAL_GROUPS),
                plugin.getConfiguration().get(ConfigKeys.APPLYING_GLOBAL_WORLD_GROUPS),
                parent.isOp()
        );
    }

    private boolean hasData() {
        return user != null && user.getUserData() != null;
    }

    @Override
    public void setOp(boolean value) {
        parent.setOp(value);
    }

    @Override
    public boolean isPermissionSet(@NonNull String name) {
        return hasData() && user.getUserData().getPermissionData(calculateContexts()).getPermissionValue(name) != Tristate.UNDEFINED;
    }

    @Override
    public boolean isPermissionSet(@NonNull Permission perm) {
        return isPermissionSet(perm.getName());
    }

    @Override
    public boolean hasPermission(@NonNull String name) {
        if (hasData()) {
            Tristate ts = user.getUserData().getPermissionData(calculateContexts()).getPermissionValue(name);
            if (ts != Tristate.UNDEFINED) {
                return ts.asBoolean();
            }
        }

        return Permission.DEFAULT_PERMISSION.getValue(isOp());
    }

    @Override
    public boolean hasPermission(@NonNull Permission perm) {
        if (hasData()) {
            Tristate ts = user.getUserData().getPermissionData(calculateContexts()).getPermissionValue(perm.getName());
            if (ts != Tristate.UNDEFINED) {
                return ts.asBoolean();
            }
        }

        return perm.getDefault().getValue(isOp());
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        Set<PermissionAttachmentInfo> perms = new HashSet<>();
        perms.addAll(attachmentPermissions.values());

        if (hasData()) {
            perms.addAll(
                    user.getUserData().getPermissionData(calculateContexts()).getImmutableBacking().entrySet().stream()
                            .map(e -> new PermissionAttachmentInfo(parent, e.getKey(), null, e.getValue()))
                            .collect(Collectors.toList())
            );
        }

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

        if (hasData() && invalidate) {
            user.getUserData().invalidatePermissionCalculators();
        }
    }

    @Override
    public synchronized void clearPermissions() {
        Set<String> perms = attachmentPermissions.keySet();

        for (String name : perms) {
            Bukkit.getServer().getPluginManager().unsubscribeFromPermission(name, parent);
        }

        // Bukkit.getServer().getPluginManager().unsubscribeFromDefaultPerms(false, parent);
        // Bukkit.getServer().getPluginManager().unsubscribeFromDefaultPerms(true, parent);

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
