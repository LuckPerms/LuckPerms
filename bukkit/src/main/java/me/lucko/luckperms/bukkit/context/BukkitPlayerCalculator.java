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

package me.lucko.luckperms.bukkit.context;

import com.google.common.collect.ImmutableMap;
import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.context.ImmutableContextSetImpl;
import me.lucko.luckperms.common.util.EnumNamer;
import net.luckperms.api.context.Context;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.ImmutableContextSet;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Set;

public class BukkitPlayerCalculator implements ContextCalculator<Player>, Listener {
    private static final EnumNamer<GameMode> GAMEMODE_NAMER = new EnumNamer<>(
            GameMode.class,
            EnumNamer.LOWER_CASE_NAME
    );
    private static final EnumNamer<Environment> DIMENSION_TYPE_NAMER = new EnumNamer<>(
            Environment.class,
            // use the namespaced keys used by the game
            ImmutableMap.<Environment, String>builder()
                    .put(Environment.NORMAL, "overworld")
                    .put(Environment.NETHER, "the_nether")
                    .put(Environment.THE_END, "the_end")
                    .build(),
            EnumNamer.LOWER_CASE_NAME
    );

    private final LPBukkitPlugin plugin;

    private final boolean gamemode;
    private final boolean world;
    private final boolean dimensionType;

    public BukkitPlayerCalculator(LPBukkitPlugin plugin, Set<String> disabled) {
        this.plugin = plugin;
        this.gamemode = !disabled.contains(DefaultContextKeys.GAMEMODE_KEY);
        this.world = !disabled.contains(DefaultContextKeys.WORLD_KEY);
        this.dimensionType = !disabled.contains(DefaultContextKeys.DIMENSION_TYPE_KEY);
    }

    @SuppressWarnings("ConstantConditions") // bukkit lies
    @Override
    public void calculate(@NonNull Player subject, @NonNull ContextConsumer consumer) {
        if (this.gamemode) {
            GameMode mode = subject.getGameMode();
            if (mode != null) {
                consumer.accept(DefaultContextKeys.GAMEMODE_KEY, GAMEMODE_NAMER.name(mode));
            }
        }

        World world = subject.getWorld();
        if (world != null) {
            Environment environment = world.getEnvironment();
            if (this.dimensionType && environment != null) {
                consumer.accept(DefaultContextKeys.DIMENSION_TYPE_KEY, DIMENSION_TYPE_NAMER.name(environment));
            }
            if (this.world) {
                this.plugin.getConfiguration().get(ConfigKeys.WORLD_REWRITES).rewriteAndSubmit(world.getName(), consumer);
            }
        }
    }

    @Override
    public @NonNull ContextSet estimatePotentialContexts() {
        ImmutableContextSet.Builder builder = new ImmutableContextSetImpl.BuilderImpl();
        if (this.gamemode) {
            for (GameMode mode : GameMode.values()) {
                builder.add(DefaultContextKeys.GAMEMODE_KEY, GAMEMODE_NAMER.name(mode));
            }
        }
        if (this.dimensionType) {
            for (Environment env : Environment.values()) {
                builder.add(DefaultContextKeys.DIMENSION_TYPE_KEY, DIMENSION_TYPE_NAMER.name(env));
            }
        }
        if (this.world) {
            for (World world : this.plugin.getBootstrap().getServer().getWorlds()) {
                String worldName = world.getName();
                if (Context.isValidValue(worldName)) {
                    builder.add(DefaultContextKeys.WORLD_KEY, worldName);
                }
            }
        }
        return builder.build();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onWorldChange(PlayerChangedWorldEvent e) {
        if (this.world || this.dimensionType) {
            this.plugin.getContextManager().signalContextUpdate(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoinWorld(PlayerJoinEvent e) {
        if (this.world || this.dimensionType) {
            this.plugin.getContextManager().signalContextUpdate(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent e) {
        if (this.gamemode) {
            this.plugin.getContextManager().signalContextUpdate(e.getPlayer());
        }
    }
}
