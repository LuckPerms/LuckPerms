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
import me.lucko.luckperms.common.plugin.scheduler.SchedulerTask;
import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;
import net.luckperms.api.messenger.message.OutgoingMessage;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.concurrent.TimeUnit;

/**
 * An implementation of {@link Messenger} using RabbitMQ.
 */
public class RabbitMQMessenger implements Messenger {
    private static final int DEFAULT_PORT = 5672;
    private static final String EXCHANGE = "luckperms";
    private static final String ROUTING_KEY = "luckperms:update";
    private static final boolean CHANNEL_PROP_DURABLE = false;
    private static final boolean CHANNEL_PROP_EXCLUSIVE = true;
    private static final boolean CHANNEL_PROP_AUTO_DELETE = true;

    private final LuckPermsPlugin plugin;
    private final IncomingMessageConsumer consumer;

    private ConnectionFactory connectionFactory;
    private Connection connection;
    private Channel channel;
    private Subscription sub;
    private SchedulerTask checkConnectionTask;

    public RabbitMQMessenger(LuckPermsPlugin plugin, IncomingMessageConsumer consumer) {
        this.plugin = plugin;
        this.consumer = consumer;
    }

    public void init(String address, String virtualHost, String username, String password) {
        String[] addressSplit = address.split(":");
        String host = addressSplit[0];
        int port = addressSplit.length > 1 ? Integer.parseInt(addressSplit[1]) : DEFAULT_PORT;

        this.connectionFactory = new ConnectionFactory();
        this.connectionFactory.setHost(host);
        this.connectionFactory.setPort(port);
        this.connectionFactory.setVirtualHost(virtualHost);
        this.connectionFactory.setUsername(username);
        this.connectionFactory.setPassword(password);

        this.sub = new Subscription();
        checkAndReopenConnection(true);
        this.checkConnectionTask = this.plugin.getBootstrap().getScheduler().asyncRepeating(() -> checkAndReopenConnection(false), 5, TimeUnit.SECONDS);
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
            this.checkConnectionTask.cancel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks the connection, and re-opens it if necessary.
     *
     * @return true if the connection is now alive, false otherwise
     */
    private boolean checkAndReopenConnection(boolean firstStartup) {
        boolean connectionAlive = this.connection != null && this.connection.isOpen();
        boolean channelAlive = this.channel != null && this.channel.isOpen();

        if (connectionAlive && channelAlive) {
            return true;
        }

        // cleanup existing
        if (this.channel != null && this.channel.isOpen()) {
            try {
                this.channel.close();
            } catch (Exception e) {
                // ignore
            }
        }
        if (this.connection != null && this.connection.isOpen()) {
            try {
                this.connection.close();
            } catch (Exception e) {
                // ignore
            }
        }

        // (re)create

        if (!firstStartup) {
            this.plugin.getLogger().warn("RabbitMQ pubsub connection dropped, trying to re-open the connection");
        }

        try {
            this.connection = this.connectionFactory.newConnection();
            this.channel = this.connection.createChannel();

            String queue = this.channel.queueDeclare("", CHANNEL_PROP_DURABLE, CHANNEL_PROP_EXCLUSIVE, CHANNEL_PROP_AUTO_DELETE, null).getQueue();
            this.channel.exchangeDeclare(EXCHANGE, BuiltinExchangeType.TOPIC, CHANNEL_PROP_DURABLE, CHANNEL_PROP_AUTO_DELETE, null);
            this.channel.queueBind(queue, EXCHANGE, ROUTING_KEY);
            this.channel.basicConsume(queue, true, this.sub, tag -> {});

            if (!firstStartup) {
                this.plugin.getLogger().info("RabbitMQ pubsub connection re-established");
            }
            return true;
        } catch (Exception e) {
            if (firstStartup) {
                this.plugin.getLogger().warn("Unable to connect to RabbitMQ, waiting for 5 seconds then retrying...", e);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                return checkAndReopenConnection(false);
            } else {
                this.plugin.getLogger().severe("Unable to connect to RabbitMQ", e);
                return false;
            }
        }
    }

    private class Subscription implements DeliverCallback {
        @Override
        public void handle(String consumerTag, Delivery message) {
            try {
                byte[] data = message.getBody();
                ByteArrayDataInput input = ByteStreams.newDataInput(data);
                String msg = input.readUTF();
                RabbitMQMessenger.this.consumer.consumeIncomingMessageAsString(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
