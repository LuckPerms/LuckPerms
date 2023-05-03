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

package me.lucko.luckperms.sponge.context;

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.context.ImmutableContextSetImpl;
import me.lucko.luckperms.sponge.LPSpongePlugin;
import net.luckperms.api.context.Context;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.ImmutableContextSet;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.Game;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Server;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.ChangeEntityWorldEvent;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.server.ServerWorld;

public class SpongePlayerCalculator implements ContextCalculator<Subject> {
    private final LPSpongePlugin plugin;

    public SpongePlayerCalculator(LPSpongePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void calculate(@NonNull Subject subject, @NonNull ContextConsumer consumer) {
        if (subject instanceof Locatable) {
            World<?, ?> world = ((Locatable) subject).world();
            consumer.accept(DefaultContextKeys.DIMENSION_TYPE_KEY, getContextKey(world.worldType().key(RegistryTypes.WORLD_TYPE)));
            if (world instanceof ServerWorld) {
                this.plugin.getConfiguration().get(ConfigKeys.WORLD_REWRITES).rewriteAndSubmit(getContextKey(((ServerWorld) world).key()), consumer);
            }
        }

        if (subject instanceof ValueContainer) {
            ValueContainer valueContainer = (ValueContainer) subject;
            valueContainer.get(Keys.GAME_MODE).ifPresent(mode -> consumer.accept(DefaultContextKeys.GAMEMODE_KEY, getContextKey(mode.key(RegistryTypes.GAME_MODE))));
        }
    }

    @Override
    public ContextSet estimatePotentialContexts() {
        ImmutableContextSet.Builder builder = new ImmutableContextSetImpl.BuilderImpl();
        Game game = this.plugin.getBootstrap().getGame();

        game.registry(RegistryTypes.GAME_MODE).stream().forEach(mode -> {
            builder.add(DefaultContextKeys.GAMEMODE_KEY, getContextKey(mode.key(RegistryTypes.GAME_MODE)));
        });

        if (game.isServerAvailable()) {
            Server server = game.server();

            server.registry(RegistryTypes.WORLD_TYPE).stream().forEach(dim -> {
                builder.add(DefaultContextKeys.DIMENSION_TYPE_KEY, getContextKey(dim.key(RegistryTypes.WORLD_TYPE)));
            });

            for (ServerWorld world : server.worldManager().worlds()) {
                String worldName = getContextKey(world.key());
                if (Context.isValidValue(worldName)) {
                    builder.add(DefaultContextKeys.WORLD_KEY, worldName);
                }
            }
        }

        return builder.build();
    }

    private static String getContextKey(ResourceKey key) {
        if (key.namespace().equals("minecraft")) {
            return key.value();
        }
        return key.formatted();
    }

    @Listener(order = Order.LAST)
    public void onWorldChange(ChangeEntityWorldEvent.Post e) {
        Entity targetEntity = e.entity();
        if (!(targetEntity instanceof Subject)) {
            return;
        }

        this.plugin.getContextManager().signalContextUpdate((Subject) targetEntity);
    }

    // TODO: find replacement
    //@Listener(order = Order.LAST)
    //public void onGameModeChange(ChangeGameModeEvent e) {
    //    Humanoid targetEntity = e.getHumanoid();
    //    if (targetEntity instanceof Subject) {
    //        this.plugin.getContextManager().signalContextUpdate((Subject) targetEntity);
    //    }
    //}
}
