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

package me.lucko.luckperms.common.minecraft.context;

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.context.ImmutableContextSetImpl;
import me.lucko.luckperms.common.minecraft.MinecraftLuckPermsPlugin;
import net.luckperms.api.context.Context;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.ImmutableContextSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Set;

public class MinecraftPlayerCalculator implements ContextCalculator<ServerPlayer> {
    protected final MinecraftLuckPermsPlugin<?, ?> plugin;

    protected final boolean gamemode;
    protected final boolean world;
    protected final boolean dimensionType;

    public MinecraftPlayerCalculator(MinecraftLuckPermsPlugin<?, ?> plugin, Set<String> disabled) {
        this.plugin = plugin;
        this.gamemode = !disabled.contains(DefaultContextKeys.GAMEMODE_KEY);
        this.world = !disabled.contains(DefaultContextKeys.WORLD_KEY);
        this.dimensionType = !disabled.contains(DefaultContextKeys.DIMENSION_TYPE_KEY);
    }

    @Override
    public void calculate(@NonNull ServerPlayer target, @NonNull ContextConsumer consumer) {
        if (this.gamemode) {
            GameType gameMode = target.gameMode.getGameModeForPlayer();
            consumer.accept(DefaultContextKeys.GAMEMODE_KEY, gameMode.getName());
        }

        ServerLevel level = target.level();
        if (this.dimensionType) {
            consumer.accept(DefaultContextKeys.DIMENSION_TYPE_KEY, getContextKey(level.dimensionTypeRegistration().unwrapKey().orElse(BuiltinDimensionTypes.OVERWORLD).identifier()));
        }

        if (this.world) {
            this.plugin.getConfiguration().get(ConfigKeys.WORLD_REWRITES).rewriteAndSubmit(getContextKey(level.dimension().identifier()), consumer);
        }
    }

    @Override
    public @NonNull ContextSet estimatePotentialContexts() {
        ImmutableContextSet.Builder builder = new ImmutableContextSetImpl.BuilderImpl();

        if (this.gamemode) {
            for (GameType gameType : GameType.values()) {
                builder.add(DefaultContextKeys.GAMEMODE_KEY, gameType.getName());
            }
        }

        MinecraftServer server = this.plugin.getBootstrap().getServer().orElse(null);
        if (this.dimensionType && server != null) {
            server.registryAccess().lookup(Registries.DIMENSION_TYPE).ifPresent(registry -> {
                for (Identifier id : registry.keySet()) {
                    builder.add(DefaultContextKeys.DIMENSION_TYPE_KEY, getContextKey(id));
                }
            });
        }

        if (this.world && server != null) {
            for (ServerLevel level : server.getAllLevels()) {
                if (Context.isValidValue(level.dimension().identifier().toString())) {
                    builder.add(DefaultContextKeys.WORLD_KEY, level.dimension().identifier().toString());
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
}
