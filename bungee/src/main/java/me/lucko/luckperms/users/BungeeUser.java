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

import me.lucko.luckperms.BungeePlayerCache;
import me.lucko.luckperms.LPBungeePlugin;
import me.lucko.luckperms.api.event.events.UserPermissionRefreshEvent;
import me.lucko.luckperms.api.implementation.internal.UserLink;
import me.lucko.luckperms.contexts.Contexts;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Collections;
import java.util.HashMap;
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
    public synchronized void refreshPermissions() {
        ProxiedPlayer player = plugin.getProxy().getPlayer(plugin.getUuidCache().getExternalUUID(getUuid()));
        if (player == null) {
            return;
        }

        BungeePlayerCache playerCache = plugin.getPlayerCache().get(getUuid());
        if (playerCache == null) {
            return;
        }

        // Calculate the permissions that should be applied. This is done async.
        Map<String, Boolean> toApply = exportNodes(
                new Contexts(
                        plugin.getContextManager().giveApplicableContext(player, new HashMap<>()),
                        plugin.getConfiguration().isIncludingGlobalPerms(),
                        plugin.getConfiguration().isIncludingGlobalWorldPerms(),
                        true,
                        plugin.getConfiguration().isApplyingGlobalGroups(),
                        plugin.getConfiguration().isApplyingGlobalWorldGroups()
                ),
                Collections.emptyList(),
                true
        );

        Map<String, Boolean> existing = playerCache.getPermissions();

        boolean different = false;
        if (toApply.size() != existing.size()) {
            different = true;
        } else {
            for (Map.Entry<String, Boolean> e : existing.entrySet()) {
                if (toApply.containsKey(e.getKey()) && toApply.get(e.getKey()) == e.getValue()) {
                    continue;
                }
                different = true;
                break;
            }
        }

        if (!different) return;

        existing.clear();
        playerCache.invalidateCache();
        existing.putAll(toApply);

        plugin.getApiProvider().fireEventAsync(new UserPermissionRefreshEvent(new UserLink(this)));
    }
}
