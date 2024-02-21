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
    private final LPBukkitPlugin plugin;

    private LuckPermsVaultChat chat = null;
    private LuckPermsVaultPermission permission = null;

    public VaultHookManager(LPBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers the LuckPerms implementation of {@link Permission} and {@link Chat} with
     * the service manager.
     */
    public void hook() {
        try {
            if (this.permission == null) {
                this.permission = new LuckPermsVaultPermission(this.plugin);
            }

            if (this.chat == null) {
                this.chat = new LuckPermsVaultChat(this.plugin, this.permission);
            }

            ServicesManager servicesManager = this.plugin.getBootstrap().getServer().getServicesManager();
            servicesManager.register(Permission.class, this.permission, this.plugin.getLoader(), ServicePriority.High);
            servicesManager.register(Chat.class, this.chat, this.plugin.getLoader(), ServicePriority.High);

        } catch (Exception e) {
            this.plugin.getLogger().severe("Error occurred whilst hooking into Vault.", e);
        }
    }

    /**
     * Unregisters the LuckPerms Vault hooks, if present.
     */
    public void unhook() {
        ServicesManager servicesManager = this.plugin.getBootstrap().getServer().getServicesManager();

        if (this.permission != null) {
            servicesManager.unregister(Permission.class, this.permission);
            this.permission = null;
        }

        if (this.chat != null) {
            servicesManager.unregister(Chat.class, this.chat);
            this.chat = null;
        }
    }

}
