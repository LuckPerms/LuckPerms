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
import me.lucko.luckperms.common.minecraft.MinecraftSenderFactory;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener;
import me.lucko.luckperms.fabric.LPFabricPlugin;
import me.lucko.luckperms.fabric.model.MixinUser;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.kyori.adventure.text.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

public class FabricConnectionListener extends AbstractConnectionListener {
    private static volatile FabricConnectionListener instance;

    private final LPFabricPlugin plugin;
    private final Map<ServerLoginPacketListenerImpl, CompletableFuture<Boolean>> pendingLogins = new ConcurrentHashMap<>();

    public FabricConnectionListener(LPFabricPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
        instance = this;
    }

    public void registerListeners() {
        ServerLoginConnectionEvents.DISCONNECT.register(this::onLoginDisconnect);
        ServerPlayConnectionEvents.JOIN.register(this::onLogin);
        ServerPlayConnectionEvents.DISCONNECT.register(this::onDisconnect);
    }

    public static boolean shouldContinueLogin(ServerLoginPacketListenerImpl netHandler, GameProfile profile) {
        FabricConnectionListener listener = instance;
        if (listener == null) {
            throw new IllegalStateException("Fabric connection listener has not been initialized");
        }
        return listener.prepareLogin(netHandler, profile);
    }

    private boolean prepareLogin(ServerLoginPacketListenerImpl netHandler, GameProfile profile) {
        CompletableFuture<Boolean> future = this.pendingLogins.computeIfAbsent(netHandler, ignored ->
                CompletableFuture.supplyAsync(() -> loadUserForLogin(netHandler, profile), this.plugin.getBootstrap().getScheduler().async())
                        .exceptionally(ex -> {
                            Throwable cause = ex instanceof CompletionException && ex.getCause() != null ? ex.getCause() : ex;
                            denyLogin(netHandler, profile.id(), profile.name(), cause);
                            return false;
                        })
        );
        return future.getNow(false);
    }

    private boolean loadUserForLogin(ServerLoginPacketListenerImpl netHandler, GameProfile profile) {
        UUID uniqueId = profile.id();
        String username = profile.name();

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
            return true;
        } catch (Exception ex) {
            denyLogin(netHandler, uniqueId, username, ex);
            return false;
        }
    }

    private void denyLogin(ServerLoginPacketListenerImpl netHandler, UUID uniqueId, String username, Throwable cause) {
        this.plugin.getLogger().severe("Exception occurred whilst loading data for " + uniqueId + " - " + username, cause);

        Component reason = TranslationManager.render(Message.LOADING_DATABASE_ERROR.build());
        netHandler.disconnect(MinecraftSenderFactory.toNativeText(reason));
        this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(uniqueId, username, null);
    }

    private void onLoginDisconnect(ServerLoginPacketListenerImpl netHandler, MinecraftServer server) {
        this.pendingLogins.remove(netHandler);
    }

    private void onLogin(ServerGamePacketListenerImpl netHandler, PacketSender packetSender, MinecraftServer server) {
        final ServerPlayer player = netHandler.player;

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing login for " + player.getUUID() + " - " + player.getGameProfile().name());
        }

        handleLoggedIn(player.getUUID());

        final User user = this.plugin.getUserManager().getIfLoaded(player.getUUID());

        /* User instance is null for whatever reason. Could be that it was unloaded between asyncpre and now. */
        if (user == null) {
            this.plugin.getLogger().warn("User " + player.getUUID() + " - " + player.getGameProfile().id() +
                    " doesn't currently have data pre-loaded - denying login.");
            Component reason = TranslationManager.render(Message.LOADING_STATE_ERROR.build());
            netHandler.disconnect(MinecraftSenderFactory.toNativeText(reason));
            return;
        }

        // init permissions handler
        ((MixinUser) player).luckperms$initializePermissions(user);

        this.plugin.getContextManager().signalContextUpdate(player);
    }

    private void onDisconnect(ServerGamePacketListenerImpl netHandler, MinecraftServer server) {
        if (!server.isStopped()) {
            handleDisconnect(netHandler.player.getUUID());
        }
    }

}
