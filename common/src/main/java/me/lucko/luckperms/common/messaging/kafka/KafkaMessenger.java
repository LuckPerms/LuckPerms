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

package me.lucko.luckperms.common.messaging.kafka;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerTask;
import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;
import net.luckperms.api.messenger.message.OutgoingMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class KafkaMessenger implements Messenger {

    private static final String TOPIC = "luckperms";
    private static final String MESSAGE_KEY = "luckperms:update";

    private final LuckPermsPlugin plugin;
    private final IncomingMessageConsumer consumer;

    private KafkaProducer<String, byte[]> producer;
    private KafkaConsumer<String, byte[]> kafkaConsumer;

    private SchedulerTask reconnectTask;
    private Thread consumerThread;

    private String bootstrapServers;
    private volatile boolean running;


    public KafkaMessenger(
            LuckPermsPlugin plugin,
            IncomingMessageConsumer consumer
    ) {
        this.plugin = plugin;
        this.consumer = consumer;
    }


    public void init(String address) {
        this.bootstrapServers = address;

        reconnect();

        this.reconnectTask = plugin.getBootstrap()
                .getScheduler()
                .asyncRepeating(
                        this::checkConnection,
                        5,
                        TimeUnit.SECONDS
                );
    }


    @Override
    public void sendOutgoingMessage(
            @NonNull OutgoingMessage outgoingMessage
    ) {
        try {
            producer.send(
                    new ProducerRecord<>(
                            TOPIC,
                            MESSAGE_KEY,
                            encodeMessage(outgoingMessage)
                    )
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private byte[] encodeMessage(
            OutgoingMessage outgoingMessage
    ) {
        var output = ByteStreams.newDataOutput();
        output.writeUTF(outgoingMessage.asEncodedString());

        return output.toByteArray();
    }


    private void reconnect() {
        closeConnections();

        try {
            producer = createProducer();
            kafkaConsumer = createConsumer();

            startConsumer();

            plugin.getLogger()
                    .info("Kafka pubsub connection established");

        } catch (Exception e) {
            plugin.getLogger()
                    .warn("Unable to connect to Kafka", e);
        }
    }


    private KafkaProducer<String, byte[]> createProducer() {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);

        return new KafkaProducer<>(properties, new StringSerializer(), new ByteArraySerializer());
    }


    private KafkaConsumer<String, byte[]> createConsumer() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, createConsumerGroup());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        KafkaConsumer<String, byte[]> consumer =
                new KafkaConsumer<>(properties, new StringDeserializer(), new ByteArrayDeserializer());

        consumer.subscribe(Collections.singletonList(TOPIC));
        return consumer;
    }


    private String createConsumerGroup() {
        return "luckperms-" + UUID.randomUUID();
    }


    private void startConsumer() {

        running = true;

        consumerThread = new Thread(
                this::consumeLoop,
                "LuckPerms-Kafka-Consumer"
        );

        consumerThread.setDaemon(true);
        consumerThread.start();
    }


    private void consumeLoop() {

        while (running) {

            try {
                pollMessages();

            } catch (Exception e) {

                plugin.getLogger()
                        .warn("Kafka consumer stopped", e);

                return;
            }
        }
    }


    private void pollMessages() {

        var records = kafkaConsumer.poll(
                Duration.ofSeconds(1)
        );

        for (ConsumerRecord<String, byte[]> record : records) {
            consume(record);
        }
    }


    private void consume(
            ConsumerRecord<String, byte[]> record
    ) {

        try {

            ByteArrayDataInput input =
                    ByteStreams.newDataInput(record.value());

            consumer.consumeIncomingMessageAsString(
                    input.readUTF()
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void checkConnection() {

        if (isConnected()) {
            return;
        }

        plugin.getLogger()
                .warn("Kafka connection lost, reconnecting");

        reconnect();
    }


    private boolean isConnected() {

        return producer != null
                && kafkaConsumer != null;
    }


    private void closeConnections() {

        running = false;

        stopConsumer();

        closeProducer();

        closeConsumer();
    }


    private void stopConsumer() {

        if (consumerThread == null) {
            return;
        }

        consumerThread.interrupt();
        consumerThread = null;
    }


    private void closeProducer() {

        if (producer == null) {
            return;
        }

        producer.close();
        producer = null;
    }


    private void closeConsumer() {

        if (kafkaConsumer == null) {
            return;
        }

        kafkaConsumer.close();
        kafkaConsumer = null;
    }


    @Override
    public void close() {

        if (reconnectTask != null) {
            reconnectTask.cancel();
        }

        closeConnections();
    }
}
