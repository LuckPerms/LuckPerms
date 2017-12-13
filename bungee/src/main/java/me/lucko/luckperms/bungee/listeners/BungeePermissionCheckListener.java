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

package me.lucko.luckperms.bungee.listeners;

import lombok.RequiredArgsConstructor;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.bungee.LPBungeePlugin;
import me.lucko.luckperms.bungee.event.TristateCheckEvent;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.verbose.CheckOrigin;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PermissionCheckEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

@RequiredArgsConstructor
public class BungeePermissionCheckListener implements Listener {
    private final LPBungeePlugin plugin;

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPermissionCheck(PermissionCheckEvent e) {
        if (!(e.getSender() instanceof ProxiedPlayer)) {
            return;
        }

        ProxiedPlayer player = ((ProxiedPlayer) e.getSender());

        User user = plugin.getUserManager().getIfLoaded(plugin.getUuidCache().getUUID(player.getUniqueId()));
        if (user == null) {
            e.setHasPermission(false);
            return;
        }

        Contexts contexts = plugin.getContextManager().getApplicableContexts(player);
        Tristate result = user.getCachedData().getPermissionData(contexts).getPermissionValue(e.getPermission(), CheckOrigin.PLATFORM_PERMISSION_CHECK);
        if (result == Tristate.UNDEFINED && plugin.getConfiguration().get(ConfigKeys.APPLY_BUNGEE_CONFIG_PERMISSIONS)) {
            return; // just use the result provided by the proxy when the event was created
        }

        e.setHasPermission(result.asBoolean());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerTristateCheck(TristateCheckEvent e) {
        if (!(e.getSender() instanceof ProxiedPlayer)) {
            return;
        }

        ProxiedPlayer player = ((ProxiedPlayer) e.getSender());

        User user = plugin.getUserManager().getIfLoaded(plugin.getUuidCache().getUUID(player.getUniqueId()));
        if (user == null) {
            e.setResult(Tristate.UNDEFINED);
            return;
        }

        Contexts contexts = plugin.getContextManager().getApplicableContexts(player);
        Tristate result = user.getCachedData().getPermissionData(contexts).getPermissionValue(e.getPermission(), CheckOrigin.PLATFORM_LOOKUP_CHECK);
        if (result == Tristate.UNDEFINED && plugin.getConfiguration().get(ConfigKeys.APPLY_BUNGEE_CONFIG_PERMISSIONS)) {
            return; // just use the result provided by the proxy when the event was created
        }

        e.setResult(result);
    }
}
