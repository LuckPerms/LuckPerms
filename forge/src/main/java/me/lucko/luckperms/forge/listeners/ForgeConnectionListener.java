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

package me.lucko.luckperms.forge.listeners;

import com.mojang.authlib.GameProfile;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.context.manager.QueryOptionsCache;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener;
import me.lucko.luckperms.forge.ForgeSenderFactory;
import me.lucko.luckperms.forge.LPForgePlugin;
import me.lucko.luckperms.forge.bridge.server.level.ServerPlayerBridge;
import me.lucko.luckperms.forge.event.ConnectionEvent;
import net.kyori.adventure.text.Component;
import net.minecraft.Util;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ForgeConnectionListener extends AbstractConnectionListener {
    private final LPForgePlugin plugin;
    private final Map<UUID, CompletableFuture<Boolean>> pendingConnections;

    public ForgeConnectionListener(LPForgePlugin plugin) {
        super(plugin);
        this.plugin = plugin;
        this.pendingConnections = new ConcurrentHashMap<>();
    }

    @SubscribeEvent
    public void onAuth(ConnectionEvent.Auth event) {
        GameProfile profile = event.getProfile();

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing pre-login for " + profile.getId() + " - " + profile.getName());
        }

        if (this.pendingConnections.containsKey(profile.getId())) {
            return;
        }

        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
            /* Actually process the login for the connection.
               We do this here to delay the login until the data is ready.
               If the login gets cancelled later on, then this will be cleaned up.

               This includes:
               - loading uuid data
               - loading permissions
               - creating a user instance in the UserManager for this connection.
               - setting up cached data. */
            try {
                User user = loadUser(profile.getId(), profile.getName());
                recordConnection(profile.getId());
                this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(profile.getId(), profile.getName(), user);
                return true;
            } catch (Exception ex) {
                this.plugin.getLogger().severe("Exception occurred whilst loading data for " + profile.getId() + " - " + profile.getName(), ex);
                this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(profile.getId(), profile.getName(), null);
                return !this.plugin.getConfiguration().get(ConfigKeys.CANCEL_FAILED_LOGINS);
            }
        }, this.plugin.getBootstrap().getScheduler().async());

        this.pendingConnections.put(profile.getId(), future);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getPlayer();
        GameProfile profile = player.getGameProfile();

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing post-login for " + profile.getId() + " - " + profile.getName());
        }

        try {
            CompletableFuture<Boolean> future = this.pendingConnections.get(player.getGameProfile().getId());
            if (future.get() != Boolean.TRUE) {
                Component component = TranslationManager.render(Message.LOADING_DATABASE_ERROR.build(), player.getLanguage());
                player.connection.disconnect(ForgeSenderFactory.toNativeText(component));
                return;
            }
        } catch (Exception ex) {
            Component component = TranslationManager.render(Message.LOADING_STATE_ERROR.build(), player.getLanguage());
            player.connection.disconnect(ForgeSenderFactory.toNativeText(component));
            return;
        } finally {
            this.pendingConnections.remove(profile.getId());
        }

        User user = this.plugin.getUserManager().getIfLoaded(profile.getId());

        if (user == null) {
            if (!getUniqueConnections().contains(profile.getId())) {
                this.plugin.getLogger().warn("User " + profile.getId() + " - " + profile.getName() +
                        " doesn't have data pre-loaded, they have never been processed during pre-login in this session.");
            } else {
                this.plugin.getLogger().warn("User " + profile.getId() + " - " + profile.getName() +
                        " doesn't currently have data pre-loaded, but they have been processed before in this session.");
            }

            Component component = TranslationManager.render(Message.LOADING_STATE_ERROR.build(), player.getLanguage());
            if (this.plugin.getConfiguration().get(ConfigKeys.CANCEL_FAILED_LOGINS)) {
                player.connection.disconnect(ForgeSenderFactory.toNativeText(component));
            } else {
                player.sendMessage(ForgeSenderFactory.toNativeText(component), Util.NIL_UUID);
            }
        }

        ((ServerPlayerBridge) player).bridge$setUser(user);
        ((ServerPlayerBridge) player).bridge$setQueryOptionsCache(new QueryOptionsCache<>(player, this.plugin.getContextManager()));

        this.plugin.getContextManager().signalContextUpdate(player);
    }

    @SubscribeEvent
    public void onDisconnect(ConnectionEvent.Disconnect event) {
        GameProfile profile = event.getProfile();
        CompletableFuture<Boolean> future = this.pendingConnections.remove(profile.getId());
        if (future != null) {
            future.cancel(true);
        }

        handleDisconnect(profile.getId());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        handleDisconnect(event.getPlayer().getGameProfile().getId());
    }

}
