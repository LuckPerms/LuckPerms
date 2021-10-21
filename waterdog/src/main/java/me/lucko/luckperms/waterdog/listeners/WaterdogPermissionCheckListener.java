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

package me.lucko.luckperms.waterdog.listeners;

import dev.waterdog.waterdogpe.event.EventPriority;
import dev.waterdog.waterdogpe.event.defaults.PlayerPermissionCheckEvent;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import me.lucko.luckperms.common.calculator.result.TristateResult;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.query.QueryOptionsImpl;
import me.lucko.luckperms.common.verbose.VerboseCheckTarget;
import me.lucko.luckperms.common.verbose.event.PermissionCheckEvent.Origin;
import me.lucko.luckperms.waterdog.LPWaterdogBootstrap;
import me.lucko.luckperms.waterdog.LPWaterdogPlugin;
import me.lucko.luckperms.waterdog.event.TristateCheckEvent;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;

import java.util.Objects;

public class WaterdogPermissionCheckListener {
    private final LPWaterdogPlugin plugin;

    public WaterdogPermissionCheckListener(LPWaterdogPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(LPWaterdogBootstrap boostrap) {
        boostrap.getProxy().getEventManager().subscribe(
                PlayerPermissionCheckEvent.class,
                this::onPlayerPermissionCheck,
                EventPriority.HIGH
        );
        boostrap.getProxy().getEventManager().subscribe(
                PlayerPermissionCheckEvent.class,
                this::onOtherPermissionCheck,
                EventPriority.HIGHEST
        );
        boostrap.getProxy().getEventManager().subscribe(
                TristateCheckEvent.class,
                this::onPlayerTristateCheck,
                EventPriority.HIGH
        );
        boostrap.getProxy().getEventManager().subscribe(
                TristateCheckEvent.class,
                this::onOtherTristateCheck,
                EventPriority.HIGHEST
        );
    }

    public void onPlayerPermissionCheck(PlayerPermissionCheckEvent e) {
        Objects.requireNonNull(e.getPermission(), "permission");
        Objects.requireNonNull(e.getPlayer(), "sender");

        ProxiedPlayer player = e.getPlayer();

        User user = this.plugin.getUserManager().getIfLoaded(player.getUniqueId());
        if (user == null) {
            this.plugin.getLogger().warn("A permission check was made for player " + player.getName() + " - " + player.getUniqueId() + ", " +
                    "but LuckPerms does not have any permissions data loaded for them. Perhaps their UUID has been altered since login?", new Exception());

            e.setHasPermission(false);
            return;
        }

        QueryOptions queryOptions = this.plugin.getContextManager().getQueryOptions(player);
        Tristate result = user.getCachedData().getPermissionData(queryOptions).checkPermission(e.getPermission(), Origin.PLATFORM_PERMISSION_CHECK).result();
        if (result == Tristate.UNDEFINED && this.plugin.getConfiguration().get(ConfigKeys.APPLY_WATERDOG_CONFIG_PERMISSIONS)) {
            return; // just use the result provided by the proxy when the event was created
        }

        e.setHasPermission(result.asBoolean());
    }

    public void onPlayerTristateCheck(TristateCheckEvent e) {
        if (!(e.getSender() instanceof ProxiedPlayer)) {
            return;
        }

        Objects.requireNonNull(e.getPermission(), "permission");
        Objects.requireNonNull(e.getSender(), "sender");

        ProxiedPlayer player = (ProxiedPlayer) e.getSender();

        User user = this.plugin.getUserManager().getIfLoaded(player.getUniqueId());
        if (user == null) {
            this.plugin.getLogger().warn("A permission check was made for player " + player.getName() + " - " + player.getUniqueId() + ", " +
                    "but LuckPerms does not have any permissions data loaded for them. Perhaps their UUID has been altered since login?", new Exception());

            e.setResult(Tristate.UNDEFINED);
            return;
        }

        QueryOptions queryOptions = this.plugin.getContextManager().getQueryOptions(player);
        Tristate result = user.getCachedData().getPermissionData(queryOptions).checkPermission(e.getPermission(), Origin.PLATFORM_LOOKUP_CHECK).result();
        if (result == Tristate.UNDEFINED && this.plugin.getConfiguration().get(ConfigKeys.APPLY_WATERDOG_CONFIG_PERMISSIONS)) {
            return; // just use the result provided by the proxy when the event was created
        }

        e.setResult(result);
    }

    public void onOtherPermissionCheck(PlayerPermissionCheckEvent e) {
        Objects.requireNonNull(e.getPermission(), "permission");
        Objects.requireNonNull(e.getPlayer(), "sender");

        String permission = e.getPermission();
        Tristate result = Tristate.of(e.hasPermission());
        VerboseCheckTarget target = VerboseCheckTarget.internal(e.getPlayer().getName());

        this.plugin.getVerboseHandler().offerPermissionCheckEvent(Origin.PLATFORM_PERMISSION_CHECK, target, QueryOptionsImpl.DEFAULT_CONTEXTUAL, permission, TristateResult.of(result));
        this.plugin.getPermissionRegistry().offer(permission);
    }

    public void onOtherTristateCheck(TristateCheckEvent e) {
        if (e.getSender() instanceof ProxiedPlayer) {
            return;
        }

        Objects.requireNonNull(e.getPermission(), "permission");
        Objects.requireNonNull(e.getSender(), "sender");

        String permission = e.getPermission();
        Tristate result = e.getResult();
        VerboseCheckTarget target = VerboseCheckTarget.internal(e.getSender().getName());

        this.plugin.getVerboseHandler().offerPermissionCheckEvent(Origin.PLATFORM_LOOKUP_CHECK, target, QueryOptionsImpl.DEFAULT_CONTEXTUAL, permission, TristateResult.of(result));
        this.plugin.getPermissionRegistry().offer(permission);
    }
}
