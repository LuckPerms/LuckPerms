package me.lucko.luckperms.common.messaging;

import io.nats.client.Connection;
import io.nats.client.Options;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class NatsMessagingService extends AbstractMessagingService {
    private Connection connection;

    public NatsMessagingService(LuckPermsPlugin plugin) {
        super(plugin, "Nats");
    }
    public void init(Properties natsProps) throws IOException {
        this.connection = new Options.Builder(natsProps).build().connect();
    }


    @Override
    protected void sendMessage(String message) {
        try {
            connection.publish(CHANNEL, message.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        connection.close();
    }
}
