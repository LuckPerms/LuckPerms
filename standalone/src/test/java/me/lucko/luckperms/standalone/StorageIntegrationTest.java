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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import me.lucko.luckperms.common.actionlog.LogPage;
import me.lucko.luckperms.common.actionlog.LoggedAction;
import me.lucko.luckperms.common.filter.FilterList;
import me.lucko.luckperms.common.filter.PageParameters;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.matcher.StandardNodeMatchers;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.node.types.Meta;
import me.lucko.luckperms.common.node.types.Permission;
import me.lucko.luckperms.common.node.types.Prefix;
import me.lucko.luckperms.common.storage.misc.NodeEntry;
import me.lucko.luckperms.standalone.app.LuckPermsApplication;
import me.lucko.luckperms.standalone.utils.TestPluginBootstrap;
import me.lucko.luckperms.standalone.utils.TestPluginBootstrap.TestPlugin;
import me.lucko.luckperms.standalone.utils.TestPluginProvider;
import net.luckperms.api.actionlog.Action;
import net.luckperms.api.event.cause.CreationCause;
import net.luckperms.api.model.PlayerSaveResult;
import net.luckperms.api.model.PlayerSaveResult.Outcome;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.platform.Health;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class StorageIntegrationTest {

    private static final Node TEST_PERMISSION_1 = Permission.builder()
            .permission("example.permission")
            .build();

    private static final Node TEST_PERMISSION_2 = Permission.builder()
            .permission("test")
            .value(false)
            .expiry(LocalDate.of(2050, Month.APRIL, 1).atStartOfDay().toInstant(ZoneOffset.UTC))
            .withContext("server", "foo")
            .withContext("world", "bar")
            .withContext("test", "test")
            .build();

    private static final Node TEST_GROUP = Inheritance.builder()
            .group("default")
            .value(false)
            .expiry(LocalDate.of(2050, Month.APRIL, 1).atStartOfDay().toInstant(ZoneOffset.UTC))
            .withContext("server", "foo")
            .withContext("world", "bar")
            .withContext("test", "test")
            .build();

    private static final Node TEST_PREFIX = Prefix.builder()
            .priority(100)
            .prefix("TEST")
            .withContext("server", "foo")
            .withContext("world", "bar")
            .build();

    private static final Node TEST_META = Meta.builder()
            .key("foo")
            .value("bar")
            .build();


    private static void testStorage(LuckPermsApplication app, TestPluginBootstrap bootstrap, TestPlugin plugin) {
        // check the plugin is healthy
        Health health = plugin.runHealthCheck();
        assertNotNull(health);
        assertTrue(health.isHealthy());

        // try to create / save a group
        Group group = plugin.getStorage().createAndLoadGroup("test", CreationCause.INTERNAL).join();
        group.setNode(DataType.NORMAL, TEST_PERMISSION_1, true);
        group.setNode(DataType.NORMAL, TEST_PERMISSION_2, true);
        group.setNode(DataType.NORMAL, TEST_GROUP, true);
        group.setNode(DataType.NORMAL, TEST_PREFIX, true);
        group.setNode(DataType.NORMAL, TEST_META, true);
        plugin.getStorage().saveGroup(group).join();

        // try to create / save a track
        Track track = plugin.getStorage().createAndLoadTrack("example", CreationCause.INTERNAL).join();
        track.setGroups(ImmutableList.of("default", "test"));
        plugin.getStorage().saveTrack(track).join();

        // try to create / save a user
        UUID exampleUniqueId = UUID.fromString("c1d60c50-70b5-4722-8057-87767557e50d");
        String exampleUsername = "Luck";

        PlayerSaveResult saveResult = plugin.getStorage().savePlayerData(exampleUniqueId, exampleUsername).join();
        assertEquals(ImmutableSet.of(Outcome.CLEAN_INSERT), saveResult.getOutcomes());
        assertNull(saveResult.getOtherUniqueIds());
        assertNull(saveResult.getPreviousUsername());

        User user = plugin.getStorage().loadUser(exampleUniqueId, exampleUsername).join();
        user.setNode(DataType.NORMAL, TEST_PERMISSION_1, true);
        user.setNode(DataType.NORMAL, TEST_PERMISSION_2, true);
        user.setNode(DataType.NORMAL, TEST_GROUP, true);
        user.setNode(DataType.NORMAL, TEST_PREFIX, true);
        user.setNode(DataType.NORMAL, TEST_META, true);
        plugin.getStorage().saveUser(user).join();

        // add something to the action log
        LoggedAction exampleLogEntry = LoggedAction.build()
                .source(UUID.randomUUID())
                .sourceName("Test Source")
                .targetType(Action.Target.Type.USER)
                .target(UUID.randomUUID())
                .targetName("Test Target")
                .description("hello 123 hello 123")
                .build();
        plugin.getStorage().logAction(exampleLogEntry).join();

        // read back the data we just saved to ensure it is as expected
        plugin.getSyncTaskBuffer().requestDirectly();

        Group testGroup = plugin.getGroupManager().getIfLoaded("test");
        assertNotNull(testGroup);
        assertEquals(ImmutableSet.of(TEST_PERMISSION_1, TEST_PERMISSION_2, TEST_GROUP, TEST_PREFIX, TEST_META), testGroup.normalData().asSet());

        User testUser = plugin.getStorage().loadUser(exampleUniqueId, null).join();
        assertNotNull(testUser);
        assertEquals(ImmutableSet.of(Inheritance.builder("default").build(), TEST_PERMISSION_1, TEST_PERMISSION_2, TEST_GROUP, TEST_PREFIX, TEST_META), testUser.normalData().asSet());
        assertTrue(exampleUsername.equalsIgnoreCase(testUser.getUsername().orElse("unknown")));

        Track testTrack = plugin.getTrackManager().getIfLoaded("example");
        assertNotNull(testTrack);
        assertEquals(ImmutableList.of("default", "test"), track.getGroups());

        LogPage actionLog = plugin.getStorage().getLogPage(FilterList.empty(), new PageParameters(1000, 1)).join();
        assertTrue(actionLog.getContent().contains(exampleLogEntry));

        List<NodeEntry<String, Node>> groupSearchResult = plugin.getStorage().searchGroupNodes(StandardNodeMatchers.key(TEST_PERMISSION_1)).join();
        assertEquals(1, groupSearchResult.size());
        assertTrue(groupSearchResult.contains(NodeEntry.of("test", TEST_PERMISSION_1)));

        List<NodeEntry<UUID, Node>> userSearchResult = plugin.getStorage().searchUserNodes(StandardNodeMatchers.key(TEST_PERMISSION_1)).join();
        assertEquals(1, userSearchResult.size());
        assertTrue(userSearchResult.contains(NodeEntry.of(exampleUniqueId, TEST_PERMISSION_1)));

        List<NodeEntry<UUID, PrefixNode>> userWildcardSearchResult = plugin.getStorage().searchUserNodes(StandardNodeMatchers.type(NodeType.PREFIX)).join();
        assertEquals(1, userWildcardSearchResult.size());
        assertTrue(userWildcardSearchResult.contains(NodeEntry.of(exampleUniqueId, TEST_PREFIX)));


        // create another user and test getUniqueUsers method
        UUID otherExampleUniqueId = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
        String otherExampleUsername = "Notch";

        plugin.getStorage().savePlayerData(otherExampleUniqueId, otherExampleUsername).join();
        assertFalse(plugin.getStorage().getUniqueUsers().join().contains(otherExampleUniqueId));

        User otherUser = plugin.getStorage().loadUser(otherExampleUniqueId, otherExampleUsername).join();
        otherUser.setNode(DataType.NORMAL, TEST_PERMISSION_1, true);
        plugin.getStorage().saveUser(otherUser).join();
        assertTrue(plugin.getStorage().getUniqueUsers().join().contains(otherExampleUniqueId));

        otherUser.clearNodes(DataType.NORMAL, null, true);
        plugin.getStorage().saveUser(otherUser).join();
        assertFalse(plugin.getStorage().getUniqueUsers().join().contains(otherExampleUniqueId));


        // test uuid/username lookup
        assertEquals(otherExampleUniqueId, plugin.getStorage().getPlayerUniqueId(otherExampleUsername).join());
        assertTrue(otherExampleUsername.equalsIgnoreCase(plugin.getStorage().getPlayerName(otherExampleUniqueId).join()));

        plugin.getStorage().deletePlayerData(otherExampleUniqueId).join();
        assertNull(plugin.getStorage().getPlayerUniqueId(otherExampleUsername).join());
        assertNull(plugin.getStorage().getPlayerName(otherExampleUniqueId).join());
        assertNull(plugin.getStorage().getPlayerUniqueId("example").join());
        assertNull(plugin.getStorage().getPlayerName(UUID.randomUUID()).join());


        // test savePlayerData
        saveResult = plugin.getStorage().savePlayerData(exampleUniqueId, exampleUsername).join();
        assertEquals(ImmutableSet.of(Outcome.NO_CHANGE), saveResult.getOutcomes());
        assertNull(saveResult.getOtherUniqueIds());
        assertNull(saveResult.getPreviousUsername());

        saveResult = plugin.getStorage().savePlayerData(exampleUniqueId, "test").join();
        assertEquals(ImmutableSet.of(Outcome.USERNAME_UPDATED), saveResult.getOutcomes());
        assertNull(saveResult.getOtherUniqueIds());
        assertTrue(exampleUsername.equalsIgnoreCase(saveResult.getPreviousUsername()));
        assertNull(plugin.getStorage().getPlayerUniqueId(exampleUsername).join());
        assertTrue("test".equalsIgnoreCase(plugin.getStorage().getPlayerName(exampleUniqueId).join()));

        saveResult = plugin.getStorage().savePlayerData(otherExampleUniqueId, "test").join();
        assertEquals(ImmutableSet.of(Outcome.CLEAN_INSERT, Outcome.OTHER_UNIQUE_IDS_PRESENT_FOR_USERNAME), saveResult.getOutcomes());
        assertEquals(ImmutableSet.of(exampleUniqueId), saveResult.getOtherUniqueIds());
        assertNull(saveResult.getPreviousUsername());
        assertEquals(otherExampleUniqueId, plugin.getStorage().getPlayerUniqueId("test").join());
        assertNull(plugin.getStorage().getPlayerName(exampleUniqueId).join());
    }

    @Nested
    @Tag("docker")
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
    @Tag("docker")
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
    @Tag("docker")
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
    @Tag("docker")
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
    @Tag("docker")
    class Rest {

        @Container
        private final GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("ghcr.io/luckperms/rest-api"))
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(StorageIntegrationTest.class)))
                .withExposedPorts(8080)
                .waitingFor(new WaitAllStrategy()
                        .withStrategy(Wait.forListeningPort())
                        .withStrategy(Wait.forLogMessage(".*Successfully enabled.*", 1))
                );

        @Test
        public void testRest(@TempDir Path tempDir) {
            assertTrue(this.container.isRunning());

            String host = this.container.getHost();
            Integer port = this.container.getFirstMappedPort();

            Map<String, String> config = ImmutableMap.<String, String>builder()
                    .put("storage-method", "rest")
                    .put("data.rest-url", "http://" + host + ":" + port + "/")
                    .build();

            TestPluginProvider.use(tempDir, config, StorageIntegrationTest::testStorage);
        }
    }

    @Nested
    class FlatFileDatabase {

        @Test
        public void testH2(@TempDir Path tempDir) {
            TestPluginProvider.use(tempDir, ImmutableMap.of("storage-method", "h2"), StorageIntegrationTest::testStorage);
        }

        @Test
        public void testSqlite(@TempDir Path tempDir) {
            TestPluginProvider.use(tempDir, ImmutableMap.of("storage-method", "sqlite"), StorageIntegrationTest::testStorage);
        }

    }

    @Nested
    class FlatFile {

        @Test
        public void testYaml(@TempDir Path tempDir) throws IOException {
            TestPluginProvider.use(tempDir, ImmutableMap.of("storage-method", "yaml"), StorageIntegrationTest::testStorage);

            Path storageDir = tempDir.resolve("yaml-storage");
            compareFiles(storageDir, "example/yaml", "groups/default.yml");
            compareFiles(storageDir, "example/yaml", "groups/test.yml");
            compareFiles(storageDir, "example/yaml", "tracks/example.yml");
            compareFiles(storageDir, "example/yaml", "users/c1d60c50-70b5-4722-8057-87767557e50d.yml");
        }

        @Test
        public void testJson(@TempDir Path tempDir) throws IOException {
            TestPluginProvider.use(tempDir, ImmutableMap.of("storage-method", "json"), StorageIntegrationTest::testStorage);

            Path storageDir = tempDir.resolve("json-storage");
            compareFiles(storageDir, "example/json", "groups/default.json");
            compareFiles(storageDir, "example/json", "groups/test.json");
            compareFiles(storageDir, "example/json", "tracks/example.json");
            compareFiles(storageDir, "example/json", "users/c1d60c50-70b5-4722-8057-87767557e50d.json");
        }

        @Test
        public void testHocon(@TempDir Path tempDir) throws IOException {
            TestPluginProvider.use(tempDir, ImmutableMap.of("storage-method", "hocon"), StorageIntegrationTest::testStorage);

            Path storageDir = tempDir.resolve("hocon-storage");
            compareFiles(storageDir, "example/hocon", "groups/default.conf");
            compareFiles(storageDir, "example/hocon", "groups/test.conf");
            compareFiles(storageDir, "example/hocon", "tracks/example.conf");
            compareFiles(storageDir, "example/hocon", "users/c1d60c50-70b5-4722-8057-87767557e50d.conf");
        }

        @Test
        public void testToml(@TempDir Path tempDir) throws IOException {
            TestPluginProvider.use(tempDir, ImmutableMap.of("storage-method", "toml"), StorageIntegrationTest::testStorage);
        }

        @Test
        public void testYamlCombined(@TempDir Path tempDir) throws IOException {
            TestPluginProvider.use(tempDir, ImmutableMap.of("storage-method", "yaml-combined"), StorageIntegrationTest::testStorage);

            Path storageDir = tempDir.resolve("yaml-storage");
            compareFiles(storageDir, "example/yaml-combined", "groups.yml");
            compareFiles(storageDir, "example/yaml-combined", "tracks.yml");
            compareFiles(storageDir, "example/yaml-combined", "users.yml");
        }

        @Test
        public void testJsonCombined(@TempDir Path tempDir) throws IOException {
            TestPluginProvider.use(tempDir, ImmutableMap.of("storage-method", "json-combined"), StorageIntegrationTest::testStorage);

            Path storageDir = tempDir.resolve("json-storage");
            compareFiles(storageDir, "example/json-combined", "groups.json");
            compareFiles(storageDir, "example/json-combined", "tracks.json");
            compareFiles(storageDir, "example/json-combined", "users.json");
        }

        @Test
        public void testHoconCombined(@TempDir Path tempDir) throws IOException {
            TestPluginProvider.use(tempDir, ImmutableMap.of("storage-method", "hocon-combined"), StorageIntegrationTest::testStorage);

            Path storageDir = tempDir.resolve("hocon-storage");
            compareFiles(storageDir, "example/hocon-combined", "groups.conf");
            compareFiles(storageDir, "example/hocon-combined", "tracks.conf");
            compareFiles(storageDir, "example/hocon-combined", "users.conf");
        }

        @Test
        public void testTomlCombined(@TempDir Path tempDir) throws IOException {
            TestPluginProvider.use(tempDir, ImmutableMap.of("storage-method", "toml-combined"), StorageIntegrationTest::testStorage);
        }

        private static void compareFiles(Path dir, String examplePath, String file) throws IOException {
            String exampleFile = examplePath + "/" + file;

            String expected;
            try (InputStream in = StorageIntegrationTest.class.getClassLoader().getResourceAsStream(exampleFile)) {
                if (in == null) {
                    throw new IOException("File does not exist: " + exampleFile);
                }
                expected = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }

            String actual = Files.readString(dir.resolve(Paths.get(file)));
            assertEquals(expected.trim(), actual.trim());
        }

    }

}
