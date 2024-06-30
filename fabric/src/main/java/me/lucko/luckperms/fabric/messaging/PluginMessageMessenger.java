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
import me.lucko.luckperms.common.messaging.pluginmsg.AbstractPluginMessageMessenger;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerTask;
import me.lucko.luckperms.fabric.LPFabricPlugin;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class PluginMessageMessenger extends AbstractPluginMessageMessenger implements ServerPlayNetworking.PlayPayloadHandler<PluginMessageMessenger.PluginMessagePayload> {
    private static final Identifier CHANNEL = Identifier.of(AbstractPluginMessageMessenger.CHANNEL);

    private final LPFabricPlugin plugin;

    public PluginMessageMessenger(LPFabricPlugin plugin, IncomingMessageConsumer consumer) {
        super(consumer);
        this.plugin = plugin;
    }

    public void init() {
        PayloadTypeRegistry.playS2C().register(PluginMessagePayload.ID, PluginMessagePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PluginMessagePayload.ID, PluginMessagePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PluginMessagePayload.ID, this);
    }

    @Override
    public void close() {
        ServerPlayNetworking.unregisterGlobalReceiver(CHANNEL);
    }

    @Override
    protected void sendOutgoingMessage(byte[] buf) {
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

            ServerPlayNetworking.send(p, new PluginMessagePayload(buf));

            SchedulerTask t = taskRef.getAndSet(null);
            if (t != null) {
                t.cancel();
            }
        }, 10, TimeUnit.SECONDS);
        taskRef.set(task);
    }

    @Override
    public void receive(PluginMessagePayload payload, ServerPlayNetworking.Context context) {
        handleIncomingMessage(payload.data);
    }

    public static class PluginMessagePayload implements CustomPayload {
        public static final CustomPayload.Id<PluginMessagePayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, PluginMessagePayload> CODEC = PacketCodec.of(PluginMessagePayload::write, PluginMessagePayload::new).cast();

        private final byte[] data;

        private PluginMessagePayload(byte[] data) {
            this.data = data;
        }

        private PluginMessagePayload(PacketByteBuf buf) {
            this.data = new byte[buf.readableBytes()];
            buf.readBytes(this.data);
        }

        private void write(PacketByteBuf buf) {
            buf.writeBytes(this.data);
        }

        @Override
        public Id<PluginMessagePayload> getId() {
            return ID;
        }
    }
}
