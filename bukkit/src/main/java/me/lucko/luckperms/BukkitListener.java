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

package me.lucko.luckperms;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.inject.Injector;
import me.lucko.luckperms.inject.LPPermissible;
import me.lucko.luckperms.users.BukkitUser;
import me.lucko.luckperms.users.User;
import me.lucko.luckperms.utils.AbstractListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

class BukkitListener extends AbstractListener implements Listener {
    private final LPBukkitPlugin plugin;

    BukkitListener(LPBukkitPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent e) {
        if (!plugin.getDatastore().isAcceptingLogins()) {
            // The datastore is disabled, prevent players from joining the server
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Message.LOADING_ERROR.toString());
            return;
        }

        // Process login
        onAsyncLogin(e.getUniqueId(), e.getName());

        // Pre-process the user's permissions, so they're ready for PLE.
        BukkitUser user = (BukkitUser) plugin.getUserManager().get(plugin.getUuidCache().getUUID(e.getUniqueId()));
        Map<String, Boolean> toApply = user.exportNodes(
                new Contexts(
                        Collections.singletonMap("server", plugin.getConfiguration().getServer()),
                        plugin.getConfiguration().isIncludingGlobalPerms(),
                        plugin.getConfiguration().isIncludingGlobalWorldPerms(),
                        true,
                        plugin.getConfiguration().isApplyingGlobalGroups(),
                        plugin.getConfiguration().isApplyingGlobalWorldGroups()
                ),
                Collections.emptyList(),
                true
        );
        user.setLoginPreProcess(toApply);

        // Hook with Vault early
        if (plugin.getVaultHook() != null && plugin.getVaultHook().isHooked()) {
            plugin.getVaultHook().getPermissionHook().getVaultUserManager().setupUser(user);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerPreLoginMonitor(AsyncPlayerPreLoginEvent e) {
        if (plugin.getDatastore().isAcceptingLogins() && e.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            // Login event was cancelled by another plugin
            final UUID internal = plugin.getUuidCache().getUUID(e.getUniqueId());
            onLeave(e.getUniqueId());
            if (plugin.getVaultHook() != null && plugin.getVaultHook().isHooked()) {
                plugin.getVaultHook().getPermissionHook().getVaultUserManager().clearUser(internal);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent e) {
        final Player player = e.getPlayer();
        final User user = plugin.getUserManager().get(plugin.getUuidCache().getUUID(player.getUniqueId()));

        if (user == null) {
            e.disallow(PlayerLoginEvent.Result.KICK_OTHER, Message.LOADING_ERROR.toString());
            return;
        }

        BukkitUser u = (BukkitUser) user;
        try {
            // Make a new permissible for the user
            LPPermissible lpPermissible = new LPPermissible(player, plugin, plugin.getDefaultsProvider());

            // Insert the pre-processed permissions into the permissible
            lpPermissible.getLuckPermsPermissions().putAll(u.getLoginPreProcess());
            u.setLoginPreProcess(null);

            // Inject into the player
            Injector.inject(player, lpPermissible);
            u.setLpPermissible(lpPermissible);

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLoginMonitor(PlayerLoginEvent e) {
        if (e.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            // The player got denied on sync login.
            final UUID internal = plugin.getUuidCache().getUUID(e.getPlayer().getUniqueId());
            onLeave(e.getPlayer().getUniqueId());
            if (plugin.getVaultHook() != null && plugin.getVaultHook().isHooked()) {
                plugin.getVaultHook().getPermissionHook().getVaultUserManager().clearUser(internal);
            }
        } else {
            User user = plugin.getUserManager().get(plugin.getUuidCache().getUUID(e.getPlayer().getUniqueId()));

            // Call another update to calculate full context. (incl. per world permissions)
            plugin.doAsync(user::refreshPermissions);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent e) {
        // Refresh permissions again
        UUID internal = plugin.getUuidCache().getUUID(e.getPlayer().getUniqueId());
        plugin.getWorldCalculator().getWorldCache().put(internal, e.getPlayer().getWorld().getName());
        plugin.doAsync(() -> refreshPlayer(internal));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent e) {
        final UUID internal = plugin.getUuidCache().getUUID(e.getPlayer().getUniqueId());
        plugin.getWorldCalculator().getWorldCache().remove(internal);
        onLeave(e.getPlayer().getUniqueId());
        if (plugin.getVaultHook() != null && plugin.getVaultHook().isHooked()) {
            plugin.getVaultHook().getPermissionHook().getVaultUserManager().clearUser(internal);
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
        if (plugin.getConfiguration().isOpsEnabled()) {
            return;
        }

        String s = e.getMessage()
                .replace("/", "")
                .replace("bukkit:", "")
                .replace("spigot:", "")
                .replace("minecraft:", "");

        if (s.startsWith("op") || s.startsWith("deop")) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(Message.OP_DISABLED.toString());
        }
    }
}
