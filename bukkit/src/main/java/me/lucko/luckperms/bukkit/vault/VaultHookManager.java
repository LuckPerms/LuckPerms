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

package me.lucko.luckperms.bukkit.vault;

import me.lucko.luckperms.bukkit.LPBukkitPlugin;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;

/**
 * Handles hooking with the Vault API
 */
public class VaultHookManager {
    private VaultChatHook chatHook = null;
    private VaultPermissionHook permissionHook = null;

    /**
     * Registers the LuckPerms implementation of {@link Permission} and {@link Chat} with
     * the service manager.
     *
     * @param plugin the plugin
     */
    public void hook(LPBukkitPlugin plugin) {
        try {
            if (this.permissionHook == null) {
                this.permissionHook = new VaultPermissionHook(plugin);
            }

            if (this.chatHook == null) {
                this.chatHook = new VaultChatHook(plugin, this.permissionHook);
            }

            final ServicesManager sm = plugin.getBootstrap().getServer().getServicesManager();
            sm.register(Permission.class, this.permissionHook, plugin.getBootstrap(), ServicePriority.High);
            sm.register(Chat.class, this.chatHook, plugin.getBootstrap(), ServicePriority.High);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Unregisters the LuckPerms Vault hooks, if present.
     *
     * @param plugin the plugin
     */
    public void unhook(LPBukkitPlugin plugin) {
        final ServicesManager sm = plugin.getBootstrap().getServer().getServicesManager();

        if (this.permissionHook != null) {
            sm.unregister(Permission.class, this.permissionHook);
            this.permissionHook.getExecutor().shutdown();
            this.permissionHook = null;
        }

        if (this.chatHook != null) {
            sm.unregister(Chat.class, this.chatHook);
            this.chatHook = null;
        }
    }

}
