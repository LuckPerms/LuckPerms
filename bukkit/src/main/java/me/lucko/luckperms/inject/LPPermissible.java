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

package me.lucko.luckperms.inject;

import com.google.common.base.Splitter;
import lombok.Getter;
import lombok.NonNull;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.Tristate;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.*;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Modified PermissibleBase for LuckPerms
 */
public class LPPermissible extends PermissibleBase {

    @Getter
    private final CommandSender parent;
    private final LuckPermsPlugin plugin;

    private final List<PermissionAttachment> attachments = new LinkedList<>();
    private final Map<String, PermissionAttachmentInfo> attachmentPermissions = new HashMap<>();

    @Getter
    private final Map<String, Boolean> luckPermsPermissions = new ConcurrentHashMap<>();

    public LPPermissible(@NonNull CommandSender sender, LuckPermsPlugin plugin) {
        super(sender);
        this.parent = sender;
        this.plugin = plugin;
    }

    @Override
    public boolean isOp() {
        return parent.isOp();
    }

    @Override
    public void setOp(boolean value) {
        parent.setOp(value);
    }

    @Override
    public boolean isPermissionSet(@NonNull String name) {
        return luckPermsPermissions.containsKey(name.toLowerCase()) || attachmentPermissions.containsKey(name.toLowerCase());
    }

    @Override
    public boolean isPermissionSet(@NonNull Permission perm) {
        return isPermissionSet(perm.getName());
    }

    private Tristate getPermissionValue(String permission) {
        if (plugin.getConfiguration().getDebugPermissionChecks()) {
            plugin.getLog().info("Checking if " + parent.getName() + " has permission: " + permission);
        }

        permission = permission.toLowerCase();

        if (luckPermsPermissions.containsKey(permission)) {
            return Tristate.fromBoolean(luckPermsPermissions.get(permission));
        }

        if (attachmentPermissions.containsKey(permission)) {
            return Tristate.fromBoolean(attachmentPermissions.get(permission).getValue());
        }

        if (plugin.getConfiguration().getApplyWildcards()) {
            if (luckPermsPermissions.containsKey("*") || luckPermsPermissions.containsKey("'*'")) {
                return Tristate.TRUE;
            }

            String node = "";
            Iterable<String> permParts = Splitter.on('.').split(permission);
            for (String s : permParts) {
                if (node.equals("")) {
                    node = s;
                } else {
                    node = node + "." + s;
                }

                if (luckPermsPermissions.containsKey(node + ".*")) {
                    return Tristate.fromBoolean(luckPermsPermissions.get(node + ".*"));
                }
            }
        }

        Permission defPerm = Bukkit.getServer().getPluginManager().getPermission(permission);
        if (defPerm != null) {
            return Tristate.fromBoolean(defPerm.getDefault().getValue(isOp()));
        }

        return Tristate.UNDEFINED;
    }

    @Override
    public boolean hasPermission(@NonNull String name) {
        Tristate ts = getPermissionValue(name);
        if (ts != Tristate.UNDEFINED) {
            return ts.asBoolean();
        }

        return Permission.DEFAULT_PERMISSION.getValue(isOp());
    }

    @Override
    public boolean hasPermission(@NonNull Permission perm) {
        Tristate ts = getPermissionValue(perm.getName());
        if (ts != Tristate.UNDEFINED) {
            return ts.asBoolean();
        }

        return perm.getDefault().getValue(isOp());
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
        if (Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new RemoveAttachmentRunnable(result), ticks) == -1) {
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
        if (attachmentPermissions == null) {
            return;
        }

        attachmentPermissions.clear();
        Set<Permission> defaults = Bukkit.getServer().getPluginManager().getDefaultPermissions(isOp());
        Bukkit.getServer().getPluginManager().subscribeToDefaultPerms(isOp(), parent);

        for (Permission perm : defaults) {
            String name = perm.getName().toLowerCase();

            attachmentPermissions.put(name, new PermissionAttachmentInfo(parent, name, null, true));
            Bukkit.getServer().getPluginManager().subscribeToPermission(name, parent);
            calculateChildPermissions(perm.getChildren(), false, null);
        }

        for (PermissionAttachment attachment : attachments) {
            calculateChildPermissions(attachment.getPermissions(), false, attachment);
        }
    }

    @Override
    public synchronized void clearPermissions() {
        Set<String> perms = attachmentPermissions.keySet();

        for (String name : perms) {
            Bukkit.getServer().getPluginManager().unsubscribeFromPermission(name, parent);
        }

        Bukkit.getServer().getPluginManager().unsubscribeFromDefaultPerms(false, parent);
        Bukkit.getServer().getPluginManager().unsubscribeFromDefaultPerms(true, parent);

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

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        Set<PermissionAttachmentInfo> perms = new HashSet<>();
        perms.addAll(attachmentPermissions.values());

        perms.addAll(luckPermsPermissions.entrySet().stream()
                .map(e -> new PermissionAttachmentInfo(parent, e.getKey(), null, e.getValue()))
                .collect(Collectors.toList()));

        return perms;
    }

    private class RemoveAttachmentRunnable implements Runnable {
        private PermissionAttachment attachment;

        private RemoveAttachmentRunnable(PermissionAttachment attachment) {
            this.attachment = attachment;
        }

        public void run() {
            attachment.remove();
        }
    }
}
