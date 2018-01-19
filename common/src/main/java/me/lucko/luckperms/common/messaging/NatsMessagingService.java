package me.lucko.luckperms.common.messaging;

import io.nats.client.Connection;
import io.nats.client.Options;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class NatsMessagingService extends AbstractMessagingService {
    private Connection connection;
    private String channel;

    NatsMessagingService(LuckPermsPlugin plugin) {
        super(plugin, "Nats");
    }
    public void init(Properties natsProps, String channel) throws IOException {
        this.channel = channel;
        this.connection = new Options.Builder(natsProps).build().connect();
        connection.subscribe(channel, msg -> {
            onMessage(new String(msg.getData(), StandardCharsets.UTF_8), null);
        });
    }


    @Override
    protected void sendMessage(String message) {
        try {
            connection.publish(channel, message.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        connection.close();
    }
}
