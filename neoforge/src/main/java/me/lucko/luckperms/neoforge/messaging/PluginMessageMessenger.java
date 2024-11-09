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

package me.lucko.luckperms.neoforge.messaging;

import com.google.common.collect.Iterables;
import me.lucko.luckperms.common.messaging.pluginmsg.AbstractPluginMessageMessenger;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerTask;
import me.lucko.luckperms.neoforge.LPNeoForgePlugin;
import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class PluginMessageMessenger extends AbstractPluginMessageMessenger implements Messenger {
    private static final ResourceLocation CHANNEL_ID = ResourceLocation.parse(AbstractPluginMessageMessenger.CHANNEL);
    private static final CustomPacketPayload.Type<MessageWrapper> PAYLOAD_TYPE = new CustomPacketPayload.Type<>(CHANNEL_ID);

    private final LPNeoForgePlugin plugin;

    public PluginMessageMessenger(LPNeoForgePlugin plugin, IncomingMessageConsumer consumer) {
        super(consumer);
        this.plugin = plugin;
    }

    @SubscribeEvent
    private void register(final RegisterPayloadHandlersEvent event) {
        event.registrar("1").executesOn(HandlerThread.NETWORK).commonBidirectional(
                PAYLOAD_TYPE,
                StreamCodec.of(
                        (bytebuf, wrapper) -> bytebuf.writeBytes(wrapper.bytes),
                        buf -> {
                            byte[] bytes = new byte[buf.readableBytes()];
                            return new MessageWrapper(bytes);
                        }
                ),
                (payload, context) -> handleIncomingMessage(payload.bytes())
        );
    }

    public void init() {
        this.plugin.getBootstrap().registerListeners(this);
    }

    @Override
    protected void sendOutgoingMessage(byte[] buf) {
        AtomicReference<SchedulerTask> taskRef = new AtomicReference<>();
        SchedulerTask task = this.plugin.getBootstrap().getScheduler().asyncRepeating(() -> {
            ServerPlayer player = this.plugin.getBootstrap().getServer()
                    .map(MinecraftServer::getPlayerList)
                    .map(PlayerList::getPlayers)
                    .map(players -> Iterables.getFirst(players, null))
                    .orElse(null);

            if (player == null) {
                return;
            }

            PacketDistributor.sendToPlayer(player, new MessageWrapper(buf));

            SchedulerTask t = taskRef.getAndSet(null);
            if (t != null) {
                t.cancel();
            }
        }, 10, TimeUnit.SECONDS);
        taskRef.set(task);
    }

    public static void registerChannel() {
        // do nothing - the channels are registered in the static initializer, we just
        // need to make sure that is called (which it will be if this method runs)
    }

    public record MessageWrapper(byte[] bytes) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PAYLOAD_TYPE;
        }
    }

}
