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
import me.lucko.luckperms.api.sponge.LuckPermsUserSubject;
import me.lucko.luckperms.api.sponge.collections.UserCollection;

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
    public synchronized void refreshPermissions() {
        UserCollection uc = plugin.getService().getUserSubjects();
        if (!uc.getUsers().containsKey(getUuid())) {
            return;
        }

        LuckPermsUserSubject us = uc.getUsers().get(getUuid());
        us.calculateActivePermissions(true);
    }
}
