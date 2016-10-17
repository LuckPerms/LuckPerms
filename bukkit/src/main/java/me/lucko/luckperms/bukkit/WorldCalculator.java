/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

package me.lucko.luckperms.bukkit;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.lucko.luckperms.api.context.ContextCalculator;
import me.lucko.luckperms.common.LuckPermsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class WorldCalculator extends ContextCalculator<Player> implements Listener {
    private static final String WORLD_KEY = "world";

    private final LuckPermsPlugin plugin;

    @Getter
    private final Map<UUID, String> worldCache = new ConcurrentHashMap<>();

    @Override
    public Map<String, String> giveApplicableContext(Player subject, Map<String, String> accumulator) {
        String world = getWorld(subject);

        if (world != null) {
            accumulator.put(WORLD_KEY, world);
        }

        return accumulator;
    }

    @Override
    public boolean isContextApplicable(Player subject, Map.Entry<String, String> context) {
        if (!context.getKey().equals(WORLD_KEY)) {
            return false;
        }

        String world = getWorld(subject);
        return world != null && world.equals(context.getValue());
    }

    private String getWorld(Player player) {
        UUID internal = plugin.getUuidCache().getUUID(player.getUniqueId());
        if (!worldCache.containsKey(internal)) {
            return null;
        }

        String world = worldCache.get(internal);
        return plugin.getConfiguration().getWorldRewrites().getOrDefault(world, world);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerLoginEvent e) {
        UUID internal = plugin.getUuidCache().getUUID(e.getPlayer().getUniqueId());
        worldCache.put(internal, e.getPlayer().getWorld().getName());
        pushUpdate(
                e.getPlayer(),
                Maps.immutableEntry(WORLD_KEY, null),
                Maps.immutableEntry(WORLD_KEY, e.getPlayer().getWorld().getName())
        );
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent e) {
        UUID internal = plugin.getUuidCache().getUUID(e.getPlayer().getUniqueId());
        worldCache.put(internal, e.getPlayer().getWorld().getName());
        pushUpdate(
                e.getPlayer(),
                Maps.immutableEntry(WORLD_KEY, e.getFrom().getName()),
                Maps.immutableEntry(WORLD_KEY, e.getPlayer().getWorld().getName())
        );
    }
}
