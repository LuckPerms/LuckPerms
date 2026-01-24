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

package me.lucko.luckperms.hytale.context;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.ChangeGameModeEvent;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.context.ImmutableContextSetImpl;
import me.lucko.luckperms.common.util.EnumNamer;
import me.lucko.luckperms.hytale.LPHytalePlugin;
import net.luckperms.api.context.Context;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.ImmutableContextSet;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HytalePlayerCalculator implements ContextCalculator<PlayerRef> {
    private static final EnumNamer<GameMode> GAMEMODE_NAMER = new EnumNamer<>(
            GameMode.class,
            EnumNamer.LOWER_CASE_NAME
    );

    protected final LPHytalePlugin plugin;

    protected final boolean gamemode;
    protected final boolean world;

    private final Map<UUID, GameMode> playerGameModes = new ConcurrentHashMap<>();

    public HytalePlayerCalculator(LPHytalePlugin plugin, Set<String> disabled) {
        this.plugin = plugin;
        this.gamemode = !disabled.contains(DefaultContextKeys.GAMEMODE_KEY);
        this.world = !disabled.contains(DefaultContextKeys.WORLD_KEY);
    }

    public void registerEvents(EventRegistry registry) {
        registry.registerGlobal(AddPlayerToWorldEvent.class, this::onAddPlayerToWorld);
    }

    public void registerSystems(ComponentRegistryProxy<EntityStore> registry) {
        registry.registerSystem(new ChangeGameModeSystem());
    }

    @Override
    public void calculate(@NonNull PlayerRef target, @NonNull ContextConsumer consumer) {
        Ref<EntityStore> ref = target.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }

        Store<EntityStore> store = ref.getStore();
        Player player = store.isInThread() ? store.getComponent(ref, Player.getComponentType()) : null;

        if (this.gamemode) {
            GameMode mode;
            if (player != null) {
                mode = player.getGameMode();
            } else {
                mode = this.playerGameModes.get(target.getUuid());
            }

            if (mode != null) {
                consumer.accept(DefaultContextKeys.GAMEMODE_KEY, GAMEMODE_NAMER.name(mode));
            }
        }

        World world = store.getExternalData().getWorld();
        if (this.world) {
            this.plugin.getConfiguration().get(ConfigKeys.WORLD_REWRITES).rewriteAndSubmit(world.getName(), consumer);
        }
    }

    @Override
    public @NonNull ContextSet estimatePotentialContexts() {
        ImmutableContextSet.Builder builder = new ImmutableContextSetImpl.BuilderImpl();

        if (this.gamemode) {
            for (GameMode value : GameMode.values()) {
                builder.add(DefaultContextKeys.GAMEMODE_KEY, GAMEMODE_NAMER.name(value));
            }
        }

        if (this.world) {
            for (World world : Universe.get().getWorlds().values()) {
                String name = world.getName();
                if (Context.isValidValue(name)) {
                    builder.add(DefaultContextKeys.WORLD_KEY, name);
                }
            }
        }

        return builder.build();
    }

    private void onAddPlayerToWorld(AddPlayerToWorldEvent event) {
        Holder<EntityStore> holder = event.getHolder();

        PlayerRef playerRef = holder.getComponent(PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        this.plugin.getContextManager().signalContextUpdate(playerRef);

        Player player = holder.getComponent(Player.getComponentType());
        if (player != null) {
            GameMode gameMode = player.getGameMode();
            if (gameMode != null) {
                this.playerGameModes.put(playerRef.getUuid(), gameMode);
            }
        }
    }

    private void onGameModeEvent(PlayerRef playerRef, ChangeGameModeEvent e) {
        this.playerGameModes.put(playerRef.getUuid(), e.getGameMode());
    }

    private final class ChangeGameModeSystem extends EntityEventSystem<EntityStore, ChangeGameModeEvent> {
        ChangeGameModeSystem() {
            super(ChangeGameModeEvent.class);
        }

        @Override
        public void handle(int index, ArchetypeChunk<EntityStore> archetypeChunk, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer, ChangeGameModeEvent event) {
            Ref<EntityStore> entity = archetypeChunk.getReferenceTo(index);
            PlayerRef playerRef = entity.getStore().getComponent(entity, PlayerRef.getComponentType());
            if (playerRef == null) {
                return;
            }
            onGameModeEvent(playerRef, event);
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Archetype.empty();
        }
    }
}
