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

package me.lucko.luckperms.common.messaging.rabbitmq;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;

import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;
import net.luckperms.api.messenger.message.OutgoingMessage;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * An implementation of {@link Messenger} using RabbitMQ.
 */
public class RabbitMQMessenger implements Messenger {
    private static final int DEFAULT_PORT = 5672;
    private static final String EXCHANGE = "luckperms";
    private static final String ROUTING_KEY = "luckperms:update";

    private final LuckPermsPlugin plugin;
    private final IncomingMessageConsumer consumer;

    private ConnectionFactory connectionFactory;
    private Connection connection;
    private Channel channel;
    private Subscription sub;

    public RabbitMQMessenger(LuckPermsPlugin plugin, IncomingMessageConsumer consumer) {
        this.plugin = plugin;
        this.consumer = consumer;
    }

    public void init(String address, String username, String password) {
        String[] addressSplit = address.split(":");
        String host = addressSplit[0];
        int port = addressSplit.length > 1 ? Integer.parseInt(addressSplit[1]) : DEFAULT_PORT;

        this.connectionFactory = new ConnectionFactory();
        this.connectionFactory.setHost(host);
        this.connectionFactory.setPort(port);
        this.connectionFactory.setUsername(username);
        this.connectionFactory.setPassword(password);

        this.sub = new Subscription(this);
        this.plugin.getBootstrap().getScheduler().executeAsync(this.sub);
    }

    @Override
    public void sendOutgoingMessage(@NonNull OutgoingMessage outgoingMessage) {
        try {
            ByteArrayDataOutput output = ByteStreams.newDataOutput();
            output.writeUTF(outgoingMessage.asEncodedString());
            this.channel.basicPublish(EXCHANGE, ROUTING_KEY, new AMQP.BasicProperties.Builder().build(), output.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        try {
            this.channel.close();
            this.connection.close();
            this.sub.isClosed = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class Subscription implements Runnable {
        private final RabbitMQMessenger parent;
        private boolean isClosed = false;
        private boolean firstStartup = true;

        private Subscription(RabbitMQMessenger parent) {
            this.parent = parent;
        }

        @Override
        public void run() {
            while (!Thread.interrupted() && !this.isClosed) {
                try {
                    if (!checkAndReopenConnection()) {
                        // Sleep for 5 seconds to prevent massive spam in console
                        Thread.sleep(5000);
                        continue;
                    }

                    // Check connection life every every 30 seconds
                    Thread.sleep(30_000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                } finally {
                    this.firstStartup = false;
                }
            }
        }

        private boolean checkAndReopenConnection() {
            boolean channelIsDead = this.parent.channel == null || !this.parent.channel.isOpen();
            if (channelIsDead) {
                boolean connectionIsDead = this.parent.connection == null || !this.parent.connection.isOpen();
                if (connectionIsDead) {
                    if (!this.firstStartup) {
                        this.parent.plugin.getLogger().warn("RabbitMQ pubsub connection dropped, trying to re-open the connection");
                    }
                    try {
                        this.parent.connection = this.parent.connectionFactory.newConnection();
                        this.parent.channel = this.parent.connection.createChannel();

                        String queue = this.parent.channel.queueDeclare("", false, true, true, null).getQueue();
                        this.parent.channel.exchangeDeclare(EXCHANGE, BuiltinExchangeType.TOPIC, false, true, null);
                        this.parent.channel.queueBind(queue, EXCHANGE, ROUTING_KEY);
                        this.parent.channel.basicConsume(queue, true, new ChannelListener(), (consumerTag) -> { });

                        if (!this.firstStartup) {
                            this.parent.plugin.getLogger().info("RabbitMQ pubsub connection re-established");
                        }
                        return true;
                    } catch (Exception ignored) {
                        return false;
                    }
                } else {
                    try {
                        this.parent.channel = this.parent.connection.createChannel();
                        return true;
                    } catch (Exception ignored) {
                        return false;
                    }
                }
            }
            return true;
        }

        private class ChannelListener implements DeliverCallback {
            @Override
            public void handle(String consumerTag, Delivery message) {
                try {
                    byte[] data = message.getBody();
                    ByteArrayDataInput input = ByteStreams.newDataInput(data);
                    String msg = input.readUTF();
                    Subscription.this.parent.consumer.consumeIncomingMessageAsString(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
