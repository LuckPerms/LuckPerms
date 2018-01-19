/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
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

package me.lucko.luckperms.bukkit.processors;

import me.lucko.luckperms.bukkit.LPBukkitPlugin;

import java.util.HashSet;
import java.util.Set;

/**
 * Performs the initial setup for Bukkit permission processors
 */
public class BukkitProcessorsSetupTask implements Runnable {
    private final LPBukkitPlugin plugin;

    public BukkitProcessorsSetupTask(LPBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        this.plugin.getDefaultsProvider().refresh();
        this.plugin.getChildPermissionProvider().setup();

        Set<String> perms = new HashSet<>();
        this.plugin.getServer().getPluginManager().getPermissions().forEach(p -> {
            perms.add(p.getName());
            perms.addAll(p.getChildren().keySet());
        });

        perms.forEach(p -> this.plugin.getPermissionVault().offer(p));
    }
}
