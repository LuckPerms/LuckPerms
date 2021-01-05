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

package me.lucko.luckperms.fabric.listeners;

import com.mojang.authlib.GameProfile;

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener;
import me.lucko.luckperms.fabric.FabricSenderFactory;
import me.lucko.luckperms.fabric.LPFabricPlugin;
import me.lucko.luckperms.fabric.mixin.ServerLoginNetworkHandlerAccessor;
import me.lucko.luckperms.fabric.model.MixinUser;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking.LoginSynchronizer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.kyori.adventure.text.Component;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class FabricConnectionListener extends AbstractConnectionListener {
    private final LPFabricPlugin plugin;

    public FabricConnectionListener(LPFabricPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    public void registerListeners() {
        ServerLoginConnectionEvents.QUERY_START.register(this::onPreLogin);
        ServerPlayConnectionEvents.JOIN.register(this::onLogin);
        ServerPlayConnectionEvents.DISCONNECT.register(this::onDisconnect);
    }

    private void onPreLogin(ServerLoginNetworkHandler netHandler, MinecraftServer server, PacketSender packetSender, LoginSynchronizer sync) {
        /* Called when the player first attempts a connection with the server. */

        // Get their profile from the net handler - it should have been initialised by now.
        GameProfile profile = ((ServerLoginNetworkHandlerAccessor) netHandler).getGameProfile();
        UUID uniqueId = PlayerEntity.getUuidFromProfile(profile);
        String username = profile.getName();

        // Register with the LoginSynchronizer that we want to perform a task before the login proceeds.
        sync.waitFor(CompletableFuture.runAsync(() -> onPreLoginAsync(netHandler, uniqueId, username), this.plugin.getBootstrap().getScheduler().async()));
    }

    private void onPreLoginAsync(ServerLoginNetworkHandler netHandler, UUID uniqueId, String username) {
        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing pre-login for " + uniqueId + " - " + username);
        }

        /* Actually process the login for the connection.
           We do this here to delay the login until the data is ready.
           If the login gets cancelled later on, then this will be cleaned up.

           This includes:
           - loading uuid data
           - loading permissions
           - creating a user instance in the UserManager for this connection.
           - setting up cached data. */
        try {
            User user = loadUser(uniqueId, username);
            recordConnection(uniqueId);
            this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(uniqueId, username, user);
        } catch (Exception ex) {
            this.plugin.getLogger().severe("Exception occurred whilst loading data for " + uniqueId + " - " + username, ex);

            // deny the connection
            Component reason = TranslationManager.render(Message.LOADING_DATABASE_ERROR.build());
            netHandler.disconnect(FabricSenderFactory.toNativeText(reason));
            this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(uniqueId, username, null);
        }
    }

    private void onLogin(ServerPlayNetworkHandler netHandler, PacketSender packetSender, MinecraftServer server) {
        final ServerPlayerEntity player = netHandler.player;

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing login for " + player.getUuid() + " - " + player.getName());
        }

        final User user = this.plugin.getUserManager().getIfLoaded(player.getUuid());

        /* User instance is null for whatever reason. Could be that it was unloaded between asyncpre and now. */
        if (user == null) {
            this.plugin.getLogger().warn("User " + player.getUuid() + " - " + player.getName() +
                    " doesn't currently have data pre-loaded - denying login.");
            Component reason = TranslationManager.render(Message.LOADING_STATE_ERROR.build());
            netHandler.disconnect(FabricSenderFactory.toNativeText(reason));
            return;
        }

        // init permissions handler
        ((MixinUser) player).initializePermissions(user);

        this.plugin.getContextManager().signalContextUpdate(player);
    }

    private void onDisconnect(ServerPlayNetworkHandler netHandler, MinecraftServer server) {
        handleDisconnect(netHandler.player.getUuid());
    }

}
