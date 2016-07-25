package me.lucko.luckperms.api.vault;

import me.lucko.luckperms.LPBukkitPlugin;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;

public class VaultHook {

    private static VaultChatHook chatHook = null;
    private static VaultPermissionHook permissionHook = null;

    public static void hook(LPBukkitPlugin plugin) {
        try {
            if (permissionHook == null) {
                permissionHook = new VaultPermissionHook();
            }
            permissionHook.setPlugin(plugin);

            if (chatHook == null) {
                chatHook = new VaultChatHook(permissionHook);
            }

            final ServicesManager sm = plugin.getServer().getServicesManager();
            sm.unregisterAll(plugin);
            sm.register(Permission.class, permissionHook, plugin, ServicePriority.High);
            sm.register(Chat.class, chatHook, plugin, ServicePriority.Lowest);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
