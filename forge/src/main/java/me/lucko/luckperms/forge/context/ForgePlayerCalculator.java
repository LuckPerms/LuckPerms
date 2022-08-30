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

package me.lucko.luckperms.forge.context;

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.context.ImmutableContextSetImpl;
import me.lucko.luckperms.forge.LPForgePlugin;

import net.luckperms.api.context.Context;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.ImmutableContextSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Set;

public class ForgePlayerCalculator implements ContextCalculator<ServerPlayer> {
    /**
     * GameType.NOT_SET(-1, "") was removed in 1.17
     */
    private static final int GAME_MODE_NOT_SET = -1;

    private final LPForgePlugin plugin;

    private final boolean gamemode;
    private final boolean world;
    private final boolean dimensionType;

    public ForgePlayerCalculator(LPForgePlugin plugin, Set<String> disabled) {
        this.plugin = plugin;
        this.gamemode = !disabled.contains(DefaultContextKeys.GAMEMODE_KEY);
        this.world = !disabled.contains(DefaultContextKeys.WORLD_KEY);
        this.dimensionType = !disabled.contains(DefaultContextKeys.DIMENSION_TYPE_KEY);
    }

    @Override
    public void calculate(@NonNull ServerPlayer target, @NonNull ContextConsumer consumer) {
        ServerLevel level = target.getLevel();
        if (this.dimensionType) {
            consumer.accept(DefaultContextKeys.DIMENSION_TYPE_KEY, getContextKey(level.dimension().location()));
        }

        ServerLevelData levelData = (ServerLevelData) level.getLevelData();
        if (this.world) {
            this.plugin.getConfiguration().get(ConfigKeys.WORLD_REWRITES).rewriteAndSubmit(levelData.getLevelName(), consumer);
        }

        GameType gameMode = target.gameMode.getGameModeForPlayer();
        if (this.gamemode && gameMode.getId() != GAME_MODE_NOT_SET) {
            consumer.accept(DefaultContextKeys.GAMEMODE_KEY, gameMode.getName());
        }
    }

    @Override
    public @NonNull ContextSet estimatePotentialContexts() {
        ImmutableContextSet.Builder builder = new ImmutableContextSetImpl.BuilderImpl();

        if (this.gamemode) {
            for (GameType gameType : GameType.values()) {
                if (gameType.getId() == GAME_MODE_NOT_SET) {
                    continue;
                }

                builder.add(DefaultContextKeys.GAMEMODE_KEY, gameType.getName());
            }
        }

        MinecraftServer server = this.plugin.getBootstrap().getServer().orElse(null);
        if (this.dimensionType && server != null) {
            server.registryAccess().registry(Registry.DIMENSION_TYPE_REGISTRY).ifPresent(registry -> {
                for (ResourceLocation resourceLocation : registry.keySet()) {
                    builder.add(DefaultContextKeys.DIMENSION_TYPE_KEY, getContextKey(resourceLocation));
                }
            });
        }

        if (this.world && server != null) {
            for (ServerLevel level : server.getAllLevels()) {
                ServerLevelData levelData = (ServerLevelData) level.getLevelData();
                if (Context.isValidValue(levelData.getLevelName())) {
                    builder.add(DefaultContextKeys.WORLD_KEY, levelData.getLevelName());
                }
            }
        }

        return builder.build();
    }

    private static String getContextKey(ResourceLocation key) {
        if (key.getNamespace().equals("minecraft")) {
            return key.getPath();
        }
        return key.toString();
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(this.world || this.dimensionType)) {
            return;
        }

        this.plugin.getContextManager().signalContextUpdate((ServerPlayer) event.getEntity());
    }

    @SubscribeEvent
    public void onPlayerChangeGameMode(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (!this.gamemode || event.getNewGameMode().getId() == GAME_MODE_NOT_SET) {
            return;
        }

        this.plugin.getContextManager().signalContextUpdate((ServerPlayer) event.getEntity());
    }

}
