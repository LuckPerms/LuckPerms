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

package me.lucko.luckperms.common.storage;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.implementation.StorageImplementation;
import me.lucko.luckperms.common.storage.implementation.mongodb.MongoStorage;
import me.lucko.luckperms.common.storage.misc.StorageCredentials;
import org.bson.Document;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("docker")
public class MongoStorageTest extends AbstractStorageTest {

    private static final String DATABASE = "minecraft";
    private static final String PREFIX = "luckperms_";
    private final GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("mongo"))
            .withExposedPorts(27017);
    private String host;
    private int port;

    @Override
    protected StorageImplementation makeStorage(LuckPermsPlugin plugin) throws Exception {
        this.container.start();
        this.host = this.container.getHost();
        this.port = this.container.getFirstMappedPort();

        StorageCredentials credentials = new StorageCredentials(
                this.host + ":" + this.port,
                DATABASE,
                "",
                ""
        );
        return new MongoStorage(plugin, credentials, PREFIX, "");
    }

    @Override
    protected void cleanupResources() {
        this.container.stop();
    }

    @Test
    public void testIndexesAreCreatedAndInitIsIdempotent() throws Exception {
        this.storage.shutdown();
        this.storage.init();

        try (MongoClient client = new MongoClient(new ServerAddress(this.host, this.port))) {
            MongoDatabase database = client.getDatabase(DATABASE);

            assertIndexCreatedOnce(
                    database,
                    PREFIX + "uuid",
                    "_id_",
                    new Document("_id", 1)
            );

            assertIndexCreatedOnce(
                    database,
                    PREFIX + "uuid",
                    "name_1",
                    new Document("name", 1)
            );

            assertIndexCreatedOnce(
                    database,
                    PREFIX + "users",
                    "permissions.key_1",
                    new Document("permissions.key", 1)
            );

            assertIndexCreatedOnce(
                    database,
                    PREFIX + "groups",
                    "permissions.key_1",
                    new Document("permissions.key", 1)
            );
        }
    }

    private static void assertIndexCreatedOnce(
            MongoDatabase database,
            String collectionName,
            String indexName,
            Document expectedKey) {
        List<Document> collectionIndexes = database.getCollection(collectionName).listIndexes().into(new ArrayList<>());
        Document collectionIndex = collectionIndexes.stream()
                .filter(index -> indexName.equals(index.getString("name")))
                .findFirst().orElse(null);

        assertNotNull(collectionIndex);

        assertEquals(
                expectedKey,
                collectionIndex.get("key", Document.class)
        );

        // Ensure index has not been duplicated.
        assertEquals(
                1L,
                collectionIndexes.stream()
                        .filter(index -> indexName.equals(index.getString("name")))
                        .count()
        );
    }
}
