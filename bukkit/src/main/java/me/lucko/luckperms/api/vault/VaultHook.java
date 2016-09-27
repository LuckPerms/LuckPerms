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

package me.lucko.luckperms.api.vault;

import lombok.Getter;
import me.lucko.luckperms.LPBukkitPlugin;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;

@Getter
public class VaultHook {
    private VaultChatHook chatHook = null;
    private VaultPermissionHook permissionHook = null;

    public void hook(LPBukkitPlugin plugin) {
        try {
            if (permissionHook == null) {
                permissionHook = new VaultPermissionHook();
            }
            permissionHook.setPlugin(plugin);
            permissionHook.setServer(plugin.getConfiguration().getVaultServer());
            permissionHook.setIncludeGlobal(plugin.getConfiguration().isVaultIncludingGlobal());
            permissionHook.setup();

            if (chatHook == null) {
                chatHook = new VaultChatHook(permissionHook);
            }

            final ServicesManager sm = plugin.getServer().getServicesManager();
            sm.register(Permission.class, permissionHook, plugin, ServicePriority.High);
            sm.register(Chat.class, chatHook, plugin, ServicePriority.High);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unhook(LPBukkitPlugin plugin) {
        final ServicesManager sm = plugin.getServer().getServicesManager();
        if (permissionHook != null) {
            sm.unregister(Permission.class, permissionHook);
        }
        if (chatHook != null) {
            sm.unregister(Chat.class, chatHook);
        }
    }

    public boolean isHooked() {
        return permissionHook != null && chatHook != null;
    }

}
