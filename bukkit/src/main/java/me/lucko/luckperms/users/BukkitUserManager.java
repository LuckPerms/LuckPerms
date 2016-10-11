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

import me.lucko.luckperms.LPBukkitPlugin;
import me.lucko.luckperms.api.context.ContextListener;
import me.lucko.luckperms.inject.Injector;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class BukkitUserManager extends UserManager implements ContextListener<Player> {
    private final LPBukkitPlugin plugin;

    public BukkitUserManager(LPBukkitPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    public void preUnload(User user) {
        if (user instanceof BukkitUser) {
            BukkitUser u = (BukkitUser) user;
            Player player = plugin.getServer().getPlayer(plugin.getUuidCache().getExternalUUID(u.getUuid()));
            if (player != null) {
                if (u.getPermissible() != null) {
                    Injector.unInject(player);
                    u.setPermissible(null);
                }

                if (plugin.getConfiguration().isAutoOp()) {
                    player.setOp(false);
                }
            }
        }
    }

    @Override
    public void cleanup(User user) {
        if (plugin.getServer().getPlayer(plugin.getUuidCache().getExternalUUID(user.getUuid())) == null) {
            unload(user);
        }
    }

    @Override
    public User apply(UserIdentifier id) {
        BukkitUser user = id.getUsername() == null ?
                new BukkitUser(id.getUuid(), plugin) :
                new BukkitUser(id.getUuid(), id.getUsername(), plugin);
        return user;
    }

    @Override
    public void updateAllUsers() {
        // Sometimes called async, as we need to get the players on the Bukkit thread.
        plugin.doSync(() -> {
            Set<UUID> players = plugin.getServer().getOnlinePlayers().stream()
                    .map(p -> plugin.getUuidCache().getUUID(p.getUniqueId()))
                    .collect(Collectors.toSet());
            plugin.doAsync(() -> players.forEach(u -> plugin.getDatastore().loadUser(u, "null")));
        });
    }

    @Override
    public void onContextChange(Player subject, Map.Entry<String, String> before, Map.Entry<String, String> current) throws Exception {
        UUID internal = plugin.getUuidCache().getUUID(subject.getUniqueId());

        User user = get(internal);
        if (user != null) {
            plugin.doAsync(user::refreshPermissions);
        }
    }
}
