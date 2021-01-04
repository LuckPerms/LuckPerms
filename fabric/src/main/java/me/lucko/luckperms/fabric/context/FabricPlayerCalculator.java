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

package me.lucko.luckperms.fabric.context;

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.context.contextset.ImmutableContextSetImpl;
import me.lucko.luckperms.common.util.EnumNamer;
import me.lucko.luckperms.fabric.LPFabricPlugin;
import me.lucko.luckperms.fabric.event.PlayerChangeWorldCallback;
import me.lucko.luckperms.fabric.event.RespawnPlayerCallback;

import net.luckperms.api.context.Context;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.ImmutableContextSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Optional;

public class FabricPlayerCalculator implements ContextCalculator<ServerPlayerEntity> {
    private static final EnumNamer<GameMode> GAMEMODE_NAMER = new EnumNamer<>(
            GameMode.class,
            EnumNamer.LOWER_CASE_NAME
    );

    private final LPFabricPlugin plugin;

    public FabricPlayerCalculator(LPFabricPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerListeners() {
        PlayerChangeWorldCallback.EVENT.register(this::onWorldChange);
        RespawnPlayerCallback.EVENT.register(this::onPlayerRespawn);
    }

    @Override
    public void calculate(@NonNull ServerPlayerEntity target, @NonNull ContextConsumer consumer) {
        GameMode mode = target.interactionManager.getGameMode();
        if (mode != null && mode != GameMode.NOT_SET) {
            consumer.accept(DefaultContextKeys.GAMEMODE_KEY, GAMEMODE_NAMER.name(mode));
        }

        // TODO: figure out dimension type context too
        ServerWorld world = target.getServerWorld();
        this.plugin.getConfiguration().get(ConfigKeys.WORLD_REWRITES).rewriteAndSubmit(getContextKey(world.getRegistryKey().getValue()), consumer);
    }

    @Override
    public ContextSet estimatePotentialContexts() {
        ImmutableContextSet.Builder builder = new ImmutableContextSetImpl.BuilderImpl();

        for (GameMode mode : GameMode.values()) {
            builder.add(DefaultContextKeys.GAMEMODE_KEY, GAMEMODE_NAMER.name(mode));
        }

        // TODO: dimension type

        Optional<MinecraftServer> server = this.plugin.getBootstrap().getServer();
        if (server.isPresent()) {
            Iterable<ServerWorld> worlds = server.get().getWorlds();
            for (ServerWorld world : worlds) {
                String worldName = getContextKey(world.getRegistryKey().getValue());
                if (Context.isValidValue(worldName)) {
                    builder.add(DefaultContextKeys.WORLD_KEY, worldName);
                }
            }
        }

        return builder.build();
    }

    private static String getContextKey(Identifier key) {
        if (key.getNamespace().equals("minecraft")) {
            return key.getPath();
        }
        return key.toString();
    }

    private void onWorldChange(ServerWorld origin, ServerWorld destination, ServerPlayerEntity player) {
        this.plugin.getContextManager().invalidateCache(player);
    }

    private void onPlayerRespawn(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
        this.plugin.getContextManager().invalidateCache(oldPlayer);
        this.plugin.getContextManager().invalidateCache(newPlayer);
    }
}
