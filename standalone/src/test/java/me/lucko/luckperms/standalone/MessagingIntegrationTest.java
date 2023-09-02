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

package me.lucko.luckperms.standalone;

import com.google.common.collect.ImmutableMap;
import me.lucko.luckperms.common.actionlog.LoggedAction;
import me.lucko.luckperms.common.messaging.InternalMessagingService;
import me.lucko.luckperms.standalone.app.integration.HealthReporter;
import me.lucko.luckperms.standalone.utils.TestPluginProvider;
import net.luckperms.api.actionlog.Action;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.log.LogReceiveEvent;
import net.luckperms.api.event.sync.PreNetworkSyncEvent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@Tag("docker")
public class MessagingIntegrationTest {

    private static void testMessaging(Map<String, String> config, Path tempDirA, Path tempDirB) throws InterruptedException {
        try (TestPluginProvider.Plugin pluginA = TestPluginProvider.create(tempDirA, config);
             TestPluginProvider.Plugin pluginB = TestPluginProvider.create(tempDirB, config)) {

            // check the plugins are healthy
            HealthReporter.Health healthA = pluginA.app().getHealthReporter().poll();
            assertNotNull(healthA);
            assertTrue(healthA.isUp());

            HealthReporter.Health healthB = pluginB.app().getHealthReporter().poll();
            assertNotNull(healthB);
            assertTrue(healthB.isUp());

            InternalMessagingService messagingServiceA = pluginA.plugin().getMessagingService().orElse(null);
            InternalMessagingService messagingServiceB = pluginB.plugin().getMessagingService().orElse(null);
            assertNotNull(messagingServiceA);
            assertNotNull(messagingServiceB);

            LoggedAction exampleLogEntry = LoggedAction.build()
                    .source(UUID.randomUUID())
                    .sourceName("Test Source")
                    .targetType(Action.Target.Type.USER)
                    .target(UUID.randomUUID())
                    .targetName("Test Target")
                    .description("hello 123 hello 123")
                    .build();

            // register 2 listeners on plugin B
            CountDownLatch latch = new CountDownLatch(2);
            EventBus eventBus = pluginB.app().getApi().getEventBus();
            eventBus.subscribe(PreNetworkSyncEvent.class, e -> {
                latch.countDown();
                e.setCancelled(true);
            });
            eventBus.subscribe(LogReceiveEvent.class, e -> {
                if (e.getEntry().equals(exampleLogEntry)) {
                    latch.countDown();
                }
            });

            // send some messages from plugin A to plugin B
            messagingServiceA.pushUpdate();
            messagingServiceA.pushLog(exampleLogEntry);

            // wait for the messages to be sent/received
            assertTrue(latch.await(30, TimeUnit.SECONDS));
        }
    }

    @Nested
    class MySql {

        @Container
        private final GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("mysql:8"))
                .withEnv("MYSQL_DATABASE", "minecraft")
                .withEnv("MYSQL_ROOT_PASSWORD", "passw0rd")
                .withExposedPorts(3306);

        @Test
        public void testMySql(@TempDir Path tempDirA, @TempDir Path tempDirB) throws InterruptedException {
            assertTrue(this.container.isRunning());

            String host = this.container.getHost();
            Integer port = this.container.getFirstMappedPort();

            Map<String, String> config = ImmutableMap.<String, String>builder()
                    .put("storage-method", "mysql")
                    .put("data.address", host + ":" + port)
                    .put("data.database", "minecraft")
                    .put("data.username", "root")
                    .put("data.password", "passw0rd")
                    .build();

            testMessaging(config, tempDirA, tempDirB);
        }
    }

    @Nested
    class MariaDb {

        @Container
        private final GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("mariadb"))
                .withEnv("MARIADB_USER", "minecraft")
                .withEnv("MARIADB_PASSWORD", "passw0rd")
                .withEnv("MARIADB_ROOT_PASSWORD", "rootpassw0rd")
                .withEnv("MARIADB_DATABASE", "minecraft")
                .withExposedPorts(3306);

        @Test
        public void testMySql(@TempDir Path tempDirA, @TempDir Path tempDirB) throws InterruptedException {
            assertTrue(this.container.isRunning());

            String host = this.container.getHost();
            Integer port = this.container.getFirstMappedPort();

            Map<String, String> config = ImmutableMap.<String, String>builder()
                    .put("storage-method", "mariadb")
                    .put("data.address", host + ":" + port)
                    .put("data.database", "minecraft")
                    .put("data.username", "minecraft")
                    .put("data.password", "passw0rd")
                    .build();

            testMessaging(config, tempDirA, tempDirB);
        }
    }

    @Nested
    class Postgres {

        @Container
        private final GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("postgres"))
                .withEnv("POSTGRES_PASSWORD", "passw0rd")
                .withExposedPorts(5432);

        @Test
        public void testPostgres(@TempDir Path tempDirA, @TempDir Path tempDirB) throws InterruptedException {
            assertTrue(this.container.isRunning());

            String host = this.container.getHost();
            Integer port = this.container.getFirstMappedPort();

            Map<String, String> config = ImmutableMap.<String, String>builder()
                    .put("storage-method", "postgresql")
                    .put("data.address", host + ":" + port)
                    .put("data.database", "postgres")
                    .put("data.username", "postgres")
                    .put("data.password", "passw0rd")
                    .build();

            testMessaging(config, tempDirA, tempDirB);
        }
    }

    @Nested
    class Redis {

        @Container
        private final GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("redis"))
                .withExposedPorts(6379);

        @Test
        public void testRedis(@TempDir Path tempDirA, @TempDir Path tempDirB) throws InterruptedException {
            assertTrue(this.container.isRunning());

            String host = this.container.getHost();
            Integer port = this.container.getFirstMappedPort();

            Map<String, String> config = ImmutableMap.<String, String>builder()
                    .put("messaging-service", "redis")
                    .put("redis.enabled", "true")
                    .put("redis.address", host + ":" + port)
                    .build();

            testMessaging(config, tempDirA, tempDirB);
        }
    }

    @Nested
    class RabbitMq {

        @Container
        private final GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("rabbitmq"))
                .withExposedPorts(5672);

        @Test
        public void testRabbitMq(@TempDir Path tempDirA, @TempDir Path tempDirB) throws InterruptedException {
            assertTrue(this.container.isRunning());

            String host = this.container.getHost();
            Integer port = this.container.getFirstMappedPort();

            Map<String, String> config = ImmutableMap.<String, String>builder()
                    .put("messaging-service", "rabbitmq")
                    .put("rabbitmq.enabled", "true")
                    .put("rabbitmq.address", host + ":" + port)
                    .build();

            testMessaging(config, tempDirA, tempDirB);
        }
    }

    @Nested
    class Nats {

        @Container
        private final GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("nats"))
                .withExposedPorts(4222);

        @Test
        public void testNats(@TempDir Path tempDirA, @TempDir Path tempDirB) throws InterruptedException {
            assertTrue(this.container.isRunning());

            String host = this.container.getHost();
            Integer port = this.container.getFirstMappedPort();

            Map<String, String> config = ImmutableMap.<String, String>builder()
                    .put("messaging-service", "nats")
                    .put("nats.enabled", "true")
                    .put("nats.address", host + ":" + port)
                    .build();

            testMessaging(config, tempDirA, tempDirB);
        }
    }

}
