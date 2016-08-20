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

import me.lucko.luckperms.LPSpongePlugin;
import me.lucko.luckperms.api.event.events.UserPermissionRefreshEvent;
import me.lucko.luckperms.api.implementation.internal.UserLink;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.Tristate;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class SpongeUser extends User {
    private final LPSpongePlugin plugin;

    SpongeUser(UUID uuid, LPSpongePlugin plugin) {
        super(uuid, plugin);
        this.plugin = plugin;
    }

    SpongeUser(UUID uuid, String username, LPSpongePlugin plugin) {
        super(uuid, username, plugin);
        this.plugin = plugin;
    }

    @Override
    public void refreshPermissions() {
        plugin.doSync(() -> {
            Optional<Player> p = plugin.getGame().getServer().getPlayer(plugin.getUuidCache().getExternalUUID(getUuid()));
            if (!p.isPresent()) return;

            final Player player = p.get();

            // Clear existing permissions
            player.getSubjectData().clearParents();
            player.getSubjectData().clearPermissions();

            // Re-add all defined permissions for the user
            final String world = player.getWorld().getName();
            Map<String, Boolean> local = getLocalPermissions(getPlugin().getConfiguration().getServer(), world, null);
            local.entrySet().forEach(e -> player.getSubjectData().setPermission(Collections.emptySet(), e.getKey(), Tristate.fromBoolean(e.getValue())));
            plugin.getApiProvider().fireEventAsync(new UserPermissionRefreshEvent(new UserLink(this)));
        });
    }
}
