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
import me.lucko.luckperms.api.sponge.LuckPermsUserSubject;
import me.lucko.luckperms.api.sponge.collections.UserCollection;

import java.util.Collections;
import java.util.Map;
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
        UserCollection uc = plugin.getService().getUserSubjects();
        if (!uc.getUsers().containsKey(getUuid())) {
            return;
        }

        // Calculate the permissions that should be applied. This is done async, who cares about how long it takes or how often it's done.
        Map<String, Boolean> toApply = exportNodes(
                getPlugin().getConfiguration().getServer(),
                null, // TODO per world perms
                null,
                plugin.getConfiguration().getIncludeGlobalPerms(),
                true,
                Collections.emptyList()
        );

        try {
            LuckPermsUserSubject us = uc.getUsers().get(getUuid());
            Map<String, Boolean> existing = us.getPermissionCache();

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
            us.invalidateCache();
            existing.putAll(toApply);

            plugin.getApiProvider().fireEventAsync(new UserPermissionRefreshEvent(new UserLink(this)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
