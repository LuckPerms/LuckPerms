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

package me.lucko.luckperms.fabric.messaging;

import com.google.common.collect.Iterables;

import me.lucko.luckperms.common.plugin.scheduler.SchedulerTask;
import me.lucko.luckperms.fabric.LPFabricPlugin;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;
import net.luckperms.api.messenger.message.OutgoingMessage;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class PluginMessageMessenger implements Messenger, ServerPlayNetworking.PlayChannelHandler {
    private static final Identifier CHANNEL = new Identifier("luckperms", "update");

    private final LPFabricPlugin plugin;
    private final IncomingMessageConsumer consumer;

    public PluginMessageMessenger(LPFabricPlugin plugin, IncomingMessageConsumer consumer) {
        this.plugin = plugin;
        this.consumer = consumer;
    }

    public void init() {
        ServerPlayNetworking.registerGlobalReceiver(CHANNEL, this);
    }

    @Override
    public void close() {
        ServerPlayNetworking.unregisterGlobalReceiver(CHANNEL);
    }

    @Override
    public void sendOutgoingMessage(@NonNull OutgoingMessage outgoingMessage) {
        AtomicReference<SchedulerTask> taskRef = new AtomicReference<>();
        SchedulerTask task = this.plugin.getBootstrap().getScheduler().asyncRepeating(() -> {
            MinecraftServer server = this.plugin.getBootstrap().getServer().orElse(null);
            if (server == null) {
                return;
            }

            Collection<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
            ServerPlayerEntity p = Iterables.getFirst(players, null);
            if (p == null) {
                return;
            }

            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString(outgoingMessage.asEncodedString());
            ServerPlayNetworking.send(p, CHANNEL, buf);

            SchedulerTask t = taskRef.getAndSet(null);
            if (t != null) {
                t.cancel();
            }
        }, 10, TimeUnit.SECONDS);
        taskRef.set(task);
    }

    @Override
    public void receive(MinecraftServer server, ServerPlayerEntity entity, ServerPlayNetworkHandler netHandler, PacketByteBuf buf, PacketSender packetSender) {
        String msg = buf.readString();
        this.consumer.consumeIncomingMessageAsString(msg);
    }
}
