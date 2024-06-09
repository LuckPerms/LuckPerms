package me.lucko.luckperms.common.storage;

import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.implementation.StorageImplementation;
import me.lucko.luckperms.common.storage.implementation.mongodb.MongoStorage;
import me.lucko.luckperms.common.storage.misc.StorageCredentials;
import org.junit.jupiter.api.Tag;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

//@Tag("docker")
public class MongoStorageTest extends AbstractStorageTest {

    private final GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("mongo"))
            .withExposedPorts(27017);

    @Override
    protected StorageImplementation makeStorage(LuckPermsPlugin plugin) throws Exception {
        this.container.start();
        String host = this.container.getHost();
        Integer port = this.container.getFirstMappedPort();

        StorageCredentials credentials = new StorageCredentials(
                host + ":" + port,
                "minecraft",
                "",
                ""
        );
        return new MongoStorage(plugin, credentials, "", "");
    }

    @Override
    protected void cleanupResources() {
        this.container.stop();
    }
}
