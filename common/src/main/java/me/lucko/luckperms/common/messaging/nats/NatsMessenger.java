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

package me.lucko.luckperms.common.messaging.nats;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.Options.Builder;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.util.Throwing;
import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;
import net.luckperms.api.messenger.message.OutgoingMessage;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.time.Duration;

/**
 * An implementation of Messenger for Nats messaging client.
 */
public class NatsMessenger implements Messenger {

    private static final String CHANNEL = "luckperms:update";

    private final LuckPermsPlugin plugin;
    private final IncomingMessageConsumer consumer;
    private Connection connection;
    private Dispatcher messageDispatcher;

    public NatsMessenger(LuckPermsPlugin plugin, IncomingMessageConsumer consumer) {
        this.plugin = plugin;
        this.consumer = consumer;
    }

    @Override
    public void sendOutgoingMessage(@NonNull OutgoingMessage outgoingMessage) {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF(outgoingMessage.asEncodedString());
        this.connection.publish(CHANNEL, output.toByteArray());
    }

    public void init(String address, String username, String password, boolean ssl) {
        String[] addressSplit = address.split(":");
        String host = addressSplit[0];
        int port = addressSplit.length > 1 ? Integer.parseInt(addressSplit[1]) : Options.DEFAULT_PORT;

        this.connection = createConnection(builder -> {
            builder.server("nats://" + host + ":" + port)
                    .reconnectWait(Duration.ofSeconds(5))
                    .maxReconnects(Integer.MAX_VALUE)
                    .connectionName("LuckPerms");

            if (username != null && password != null) {
                builder.userInfo(username, password);
            }

            if (ssl) {
                builder.secure();
            }
        });
        this.messageDispatcher = this.connection.createDispatcher(new Handler()).subscribe(CHANNEL);
    }

    private Connection createConnection(Throwing.Consumer<Builder> config) {
        try {
            Builder builder = new Builder();
            config.accept(builder);
            return Nats.connect(builder.build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {
            this.connection.closeDispatcher(this.messageDispatcher);
            this.connection.close();
        } catch (InterruptedException e) {
            this.plugin.getLogger().warn("An error occurred during closing messenger.", e);
        }
    }

    private class Handler implements MessageHandler {

        @Override
        public void onMessage(Message message) {
            byte[] data = message.getData();
            ByteArrayDataInput input = ByteStreams.newDataInput(data);
            String messageAsString = input.readUTF();

            NatsMessenger.this.consumer.consumeIncomingMessageAsString(messageAsString);
        }
    }
}
