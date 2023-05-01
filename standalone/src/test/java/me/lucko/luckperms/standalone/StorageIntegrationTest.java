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
import com.google.common.collect.ImmutableSet;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.node.types.Permission;
import me.lucko.luckperms.standalone.app.LuckPermsApplication;
import me.lucko.luckperms.standalone.app.integration.HealthReporter;
import me.lucko.luckperms.standalone.utils.TestPluginBootstrap;
import me.lucko.luckperms.standalone.utils.TestPluginBootstrap.TestPlugin;
import me.lucko.luckperms.standalone.utils.TestPluginProvider;
import net.luckperms.api.event.cause.CreationCause;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.Node;
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
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@Tag("docker")
public class StorageIntegrationTest {

    private static final Node TEST_PERMISSION = Permission.builder()
            .permission("test")
            .value(false)
            .expiry(1, TimeUnit.HOURS)
            .withContext("server", "foo")
            .withContext("world", "bar")
            .withContext("test", "test")
            .build();

    private static final Node TEST_GROUP = Inheritance.builder()
            .group("default")
            .value(false)
            .expiry(1, TimeUnit.HOURS)
            .withContext("server", "foo")
            .withContext("world", "bar")
            .withContext("test", "test")
            .build();


    private static void testStorage(LuckPermsApplication app, TestPluginBootstrap bootstrap, TestPlugin plugin) {
        // check the plugin is healthy
        HealthReporter.Health health = app.getHealthReporter().poll();
        assertNotNull(health);
        assertTrue(health.isUp());

        // try to create / save a group
        Group group = plugin.getStorage().createAndLoadGroup("test", CreationCause.INTERNAL).join();
        group.setNode(DataType.NORMAL, TEST_PERMISSION, true);
        group.setNode(DataType.NORMAL, TEST_GROUP, true);
        plugin.getStorage().saveGroup(group).join();

        plugin.getStorage().loadAllGroups().join();

        Group testGroup = plugin.getGroupManager().getIfLoaded("test");
        assertNotNull(testGroup);

        assertEquals(ImmutableSet.of(TEST_PERMISSION, TEST_GROUP), testGroup.normalData().asSet());
    }

    @Nested
    class MySql {

        @Container
        private final GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("mysql:8"))
                .withEnv("MYSQL_DATABASE", "minecraft")
                .withEnv("MYSQL_ROOT_PASSWORD", "passw0rd")
                .withExposedPorts(3306);

        @Test
        public void testMySql(@TempDir Path tempDir) {
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

            TestPluginProvider.use(tempDir, config, StorageIntegrationTest::testStorage);
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
        public void testMariaDb(@TempDir Path tempDir) {
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

            TestPluginProvider.use(tempDir, config, StorageIntegrationTest::testStorage);
        }
    }

    @Nested
    class Postgres {

        @Container
        private final GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("postgres"))
                .withEnv("POSTGRES_PASSWORD", "passw0rd")
                .withExposedPorts(5432);

        @Test
        public void testPostgres(@TempDir Path tempDir) {
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

            TestPluginProvider.use(tempDir, config, StorageIntegrationTest::testStorage);
        }
    }

    @Nested
    class MongoDb {

        @Container
        private final GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("mongo"))
                .withExposedPorts(27017);

        @Test
        public void testMongo(@TempDir Path tempDir) {
            assertTrue(this.container.isRunning());

            String host = this.container.getHost();
            Integer port = this.container.getFirstMappedPort();

            Map<String, String> config = ImmutableMap.<String, String>builder()
                    .put("storage-method", "mongodb")
                    .put("data.address", host + ":" + port)
                    .put("data.database", "minecraft")
                    .put("data.username", "")
                    .put("data.password", "")
                    .build();

            TestPluginProvider.use(tempDir, config, StorageIntegrationTest::testStorage);
        }
    }

    @Nested
    class FlatFile {

        @Test
        public void testYaml(@TempDir Path tempDir) {
            TestPluginProvider.use(tempDir, ImmutableMap.of("storage-method", "yaml"), StorageIntegrationTest::testStorage);
        }

        @Test
        public void testJson(@TempDir Path tempDir) {
            TestPluginProvider.use(tempDir, ImmutableMap.of("storage-method", "json"), StorageIntegrationTest::testStorage);
        }

        @Test
        public void testHocon(@TempDir Path tempDir) {
            TestPluginProvider.use(tempDir, ImmutableMap.of("storage-method", "hocon"), StorageIntegrationTest::testStorage);
        }

        @Test
        public void testToml(@TempDir Path tempDir) {
            TestPluginProvider.use(tempDir, ImmutableMap.of("storage-method", "toml"), StorageIntegrationTest::testStorage);
        }

        @Test
        public void testYamlCombined(@TempDir Path tempDir) {
            TestPluginProvider.use(tempDir, ImmutableMap.of("storage-method", "yaml-combined"), StorageIntegrationTest::testStorage);
        }

        @Test
        public void testJsonCombined(@TempDir Path tempDir) {
            TestPluginProvider.use(tempDir, ImmutableMap.of("storage-method", "json-combined"), StorageIntegrationTest::testStorage);
        }

        @Test
        public void testHoconCombined(@TempDir Path tempDir) {
            TestPluginProvider.use(tempDir, ImmutableMap.of("storage-method", "hocon-combined"), StorageIntegrationTest::testStorage);
        }

        @Test
        public void testTomlCombined(@TempDir Path tempDir) {
            TestPluginProvider.use(tempDir, ImmutableMap.of("storage-method", "toml-combined"), StorageIntegrationTest::testStorage);
        }

    }

}
