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

package me.lucko.luckperms.nukkit.context;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityLevelChangeEvent;
import cn.nukkit.event.player.PlayerGameModeChangeEvent;
import cn.nukkit.level.Level;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.context.ImmutableContextSetImpl;
import me.lucko.luckperms.nukkit.LPNukkitPlugin;
import net.luckperms.api.context.Context;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.ImmutableContextSet;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Locale;
import java.util.Set;

public class NukkitPlayerCalculator implements ContextCalculator<Player>, Listener {
    private static final int[] KNOWN_GAMEMODES = {Player.SURVIVAL, Player.CREATIVE, Player.ADVENTURE, Player.SPECTATOR};
    private static final int[] KNOWN_DIMENSION_TYPES = {Level.DIMENSION_OVERWORLD, Level.DIMENSION_NETHER};

    private final LPNukkitPlugin plugin;

    private final boolean gamemode;
    private final boolean world;
    private final boolean dimensionType;

    public NukkitPlayerCalculator(LPNukkitPlugin plugin, Set<String> disabled) {
        this.plugin = plugin;
        this.gamemode = !disabled.contains(DefaultContextKeys.GAMEMODE_KEY);
        this.world = !disabled.contains(DefaultContextKeys.WORLD_KEY);
        this.dimensionType = !disabled.contains(DefaultContextKeys.DIMENSION_TYPE_KEY);
    }

    @Override
    public void calculate(@NonNull Player subject, @NonNull ContextConsumer consumer) {
        if (this.gamemode) {
            consumer.accept(DefaultContextKeys.GAMEMODE_KEY, getGamemodeName(subject.getGamemode()));
        }

        Level level = subject.getLevel();
        if (this.dimensionType) {
            consumer.accept(DefaultContextKeys.DIMENSION_TYPE_KEY, getDimensionName(level.getDimension()));
        }
        if (this.world) {
            this.plugin.getConfiguration().get(ConfigKeys.WORLD_REWRITES).rewriteAndSubmit(level.getName(), consumer);
        }
    }

    @Override
    public @NonNull ContextSet estimatePotentialContexts() {
        ImmutableContextSet.Builder builder = new ImmutableContextSetImpl.BuilderImpl();
        if (this.gamemode) {
            for (int mode : KNOWN_GAMEMODES) {
                builder.add(DefaultContextKeys.GAMEMODE_KEY, getGamemodeName(mode));
            }
        }
        if (this.dimensionType) {
            for (int dim : KNOWN_DIMENSION_TYPES) {
                builder.add(DefaultContextKeys.DIMENSION_TYPE_KEY, getDimensionName(dim));
            }
        }
        if (this.world) {
            for (Level world : this.plugin.getBootstrap().getServer().getLevels().values()) {
                String worldName = world.getName();
                if (Context.isValidValue(worldName)) {
                    builder.add(DefaultContextKeys.WORLD_KEY, worldName);
                }
            }
        }
        return builder.build();
    }

    private static String getGamemodeName(int mode) {
        switch (mode) {
            case Player.SURVIVAL: return "survival";
            case Player.CREATIVE: return "creative";
            case Player.ADVENTURE: return "adventure";
            case Player.SPECTATOR: return "spectator";
            default: return Server.getGamemodeString(mode, true).toLowerCase(Locale.ROOT);
        }
    }

    private static String getDimensionName(int dim) {
        switch (dim) {
            // use the namespaced keys used by the game
            case Level.DIMENSION_OVERWORLD: return "overworld";
            case Level.DIMENSION_NETHER: return "the_nether";
            default: return "unknown" + dim;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onWorldChange(EntityLevelChangeEvent e) {
        if ((this.world || this.dimensionType) && e.getEntity() instanceof Player) {
            Player player = (Player) e.getEntity();
            this.plugin.getContextManager().signalContextUpdate(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent e) {
        if (this.gamemode) {
            this.plugin.getContextManager().signalContextUpdate(e.getPlayer());
        }
    }
}
