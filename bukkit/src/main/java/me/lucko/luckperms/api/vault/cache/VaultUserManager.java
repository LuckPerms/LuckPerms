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

package me.lucko.luckperms.api.vault.cache;

import lombok.RequiredArgsConstructor;
import me.lucko.luckperms.LPBukkitPlugin;
import me.lucko.luckperms.api.vault.VaultPermissionHook;
import me.lucko.luckperms.users.User;
import org.bukkit.World;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class VaultUserManager {
    private final LPBukkitPlugin plugin;
    private final VaultPermissionHook vault;
    private final Map<UUID, VaultUserCache> userCache = new ConcurrentHashMap<>();

    public void setupUser(User user) {
        VaultUserCache vaultUser = userCache.computeIfAbsent(user.getUuid(), uuid -> new VaultUserCache(plugin, vault, user));
        vaultUser.calculatePermissions(Collections.singletonMap("server", vault.getServer()), true);
        for (World world : plugin.getServer().getWorlds()) {
            Map<String, String> context = new HashMap<>();
            context.put("server", vault.getServer());
            context.put("world", world.getName());
            vaultUser.calculatePermissions(context, true);
        }
    }

    public void clearUser(UUID uuid) {
        userCache.remove(uuid);
    }

    public boolean containsUser(UUID uuid) {
        return userCache.containsKey(uuid);
    }

    public VaultUserCache getUser(UUID uuid) {
        return userCache.get(uuid);
    }

}
