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

import me.lucko.luckperms.LPBungeePlugin;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class BungeeUser extends User {
    private final LPBungeePlugin plugin;

    BungeeUser(UUID uuid, LPBungeePlugin plugin) {
        super(uuid, plugin);
        this.plugin = plugin;
    }

    BungeeUser(UUID uuid, String username, LPBungeePlugin plugin) {
        super(uuid, username, plugin);
        this.plugin = plugin;
    }

    @Override
    public void refreshPermissions() {
        ProxiedPlayer player = plugin.getProxy().getPlayer(plugin.getUuidCache().getExternalUUID(getUuid()));
        if (player == null) return;

        // Clear existing permissions
        Collection<String> perms = new ArrayList<>(player.getPermissions());
        perms.forEach(p -> player.setPermission(p, false));

        // Re-add all defined permissions for the user
        final String server = player.getServer() == null ? null : (player.getServer().getInfo() == null ? null : player.getServer().getInfo().getName());
        Map<String, Boolean> local = getLocalPermissions(getPlugin().getConfiguration().getServer(), server, null);
        local.entrySet().forEach(e -> player.setPermission(e.getKey(), e.getValue()));
    }
}
