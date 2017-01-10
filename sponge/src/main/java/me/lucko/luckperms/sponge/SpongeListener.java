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

package me.lucko.luckperms.sponge;

import me.lucko.luckperms.api.caching.UserData;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.core.UuidCache;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.utils.AbstractListener;
import me.lucko.luckperms.sponge.timings.LPTiming;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.World;

import co.aikar.timings.Timing;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public class SpongeListener extends AbstractListener {
    private final LPSpongePlugin plugin;

    SpongeListener(LPSpongePlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Listener(order = Order.EARLY)
    public void onClientAuth(ClientConnectionEvent.Auth e) {
        if (!plugin.getStorage().isAcceptingLogins()) {
            /* Datastore is disabled, prevent players from joining the server
               Just don't load their data, they will be kicked at login */
            return;
        }

        final GameProfile p = e.getProfile();
        onAsyncLogin(p.getUniqueId(), p.getName().get()); // Load the user into LuckPerms
    }

    @SuppressWarnings("deprecation")
    @Listener(order = Order.EARLY)
    public void onClientLogin(ClientConnectionEvent.Login e) {
        try (Timing ignored = plugin.getTimings().time(LPTiming.ON_CLIENT_LOGIN)) {
            final GameProfile player = e.getProfile();
            final User user = plugin.getUserManager().get(plugin.getUuidCache().getUUID(player.getUniqueId()));

            // Check if the user was loaded successfully.
            if (user == null) {
                e.setCancelled(true);
                e.setMessage(TextSerializers.LEGACY_FORMATTING_CODE.deserialize(Message.LOADING_ERROR.toString()));
                return;
            }

            // Attempt to pre-process some permissions for the user to save time later. Might not work, but it's better than nothing.
            Optional<Player> p = e.getCause().first(Player.class);
            if (p.isPresent()) {
                MutableContextSet context = MutableContextSet.fromSet(plugin.getContextManager().getApplicableContext(p.get()));

                List<String> worlds = plugin.getGame().getServer().getWorlds().stream()
                        .map(World::getName)
                        .collect(Collectors.toList());

                plugin.doAsync(() -> {
                    UserData data = user.getUserData();
                    data.preCalculate(plugin.getService().calculateContexts(context));

                    for (String world : worlds) {
                        MutableContextSet modified = MutableContextSet.fromSet(context);
                        modified.removeAll("world");
                        modified.add("world", world);
                        data.preCalculate(plugin.getService().calculateContexts(modified));
                    }
                });
            }
        }
    }

    @Listener(order = Order.EARLY)
    public void onClientJoin(ClientConnectionEvent.Join e) {
        // Refresh permissions again
        plugin.doAsync(() -> refreshPlayer(e.getTargetEntity().getUniqueId()));
    }

    @Listener(order = Order.LAST)
    public void onClientLeave(ClientConnectionEvent.Disconnect e) {
        try (Timing ignored = plugin.getTimings().time(LPTiming.ON_CLIENT_LEAVE)) {
            final UuidCache cache = plugin.getUuidCache();

            final User user = plugin.getUserManager().get(cache.getUUID(e.getTargetEntity().getUniqueId()));
            if (user != null) {
                user.unregisterData();
            }

            // Unload the user from memory when they disconnect;
            cache.clearCache(e.getTargetEntity().getUniqueId());
        }
    }

    @Listener
    public void onSendCommand(SendCommandEvent e) {
        CommandSource source = e.getCause().first(CommandSource.class).orElse(null);
        if (source == null) return;

        final String name = e.getCommand().toLowerCase();
        if (name.equals("op") || name.equals("deop")) {
            Message.OP_DISABLED_SPONGE.send(plugin.getSenderFactory().wrap(source));
        }
    }
}
