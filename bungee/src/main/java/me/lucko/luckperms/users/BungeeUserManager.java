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
import me.lucko.luckperms.api.context.ContextListener;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Map;
import java.util.UUID;

public class BungeeUserManager extends UserManager implements ContextListener<ProxiedPlayer> {
    private final LPBungeePlugin plugin;

    public BungeeUserManager(LPBungeePlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    public void cleanup(User user) {
        if (plugin.getProxy().getPlayer(plugin.getUuidCache().getExternalUUID(user.getUuid())) == null) {
            unload(user);
        }
    }

    @Override
    public User apply(UserIdentifier id) {
        BungeeUser user = id.getUsername() == null ?
                new BungeeUser(id.getUuid(), plugin) :
                new BungeeUser(id.getUuid(), id.getUsername(), plugin);
        giveDefaultIfNeeded(user, false);
        return user;
    }

    @Override
    public void updateAllUsers() {
        plugin.getProxy().getPlayers().stream()
                .map(p -> plugin.getUuidCache().getUUID(p.getUniqueId()))
                .forEach(u -> plugin.getDatastore().loadUser(u, "null"));
    }

    @Override
    public void onContextChange(ProxiedPlayer subject, Map.Entry<String, String> before, Map.Entry<String, String> current) throws Exception {
        UUID internal = plugin.getUuidCache().getUUID(subject.getUniqueId());

        User user = get(internal);
        if (user != null) {
            plugin.doAsync(user::refreshPermissions);
        }
    }
}
