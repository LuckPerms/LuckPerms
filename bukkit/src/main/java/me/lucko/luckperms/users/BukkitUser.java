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

package me.lucko.luckperms.users;

import lombok.Getter;
import lombok.Setter;
import me.lucko.luckperms.LPBukkitPlugin;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.util.Map;
import java.util.UUID;

public class BukkitUser extends User {

    private final LPBukkitPlugin plugin;

    @Getter
    @Setter
    private PermissionAttachment attachment = null;

    BukkitUser(UUID uuid, LPBukkitPlugin plugin) {
        super(uuid, plugin);
        this.plugin = plugin;
    }

    BukkitUser(UUID uuid, String username, LPBukkitPlugin plugin) {
        super(uuid, username, plugin);
        this.plugin = plugin;
    }

    @Override
    public void refreshPermissions() {
        plugin.doSync(() -> {
            final Player player = plugin.getServer().getPlayer(plugin.getUuidCache().getExternalUUID(getUuid()));
            if (player == null) return;

            if (attachment == null) {
                getPlugin().getLog().warn("User " + getName() + " does not have a permissions attachment defined.");
                setAttachment(player.addAttachment(plugin));
            }

            // Clear existing permissions
            attachment.getPermissions().keySet().forEach(p -> attachment.setPermission(p, false));

            // Re-add all defined permissions for the user
            Map<String, Boolean> local = getLocalPermissions(getPlugin().getConfiguration().getServer(), player.getWorld().getName(), null);
            local.entrySet().forEach(e -> attachment.setPermission(e.getKey(), e.getValue()));
        });
    }
}
