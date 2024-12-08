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

import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.implementation.StorageImplementation;
import me.lucko.luckperms.common.storage.implementation.rest.RestStorage;
import org.junit.jupiter.api.Tag;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.utility.DockerImageName;

@Tag("docker")
public class RestStorageTest extends AbstractStorageTest {

    private final GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("ghcr.io/luckperms/rest-api"))
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(RestStorageTest.class)))
            .withExposedPorts(8080)
            .waitingFor(new WaitAllStrategy()
                    .withStrategy(Wait.forListeningPort())
                    .withStrategy(Wait.forLogMessage(".*Successfully enabled.*", 1))
            );

    @Override
    protected StorageImplementation makeStorage(LuckPermsPlugin plugin) throws Exception {
        this.container.start();
        String host = this.container.getHost();
        Integer port = this.container.getFirstMappedPort();

        return new RestStorage(plugin, "http://" + host + ":" + port + "/", null);
    }

    @Override
    protected void cleanupResources() {
        this.container.stop();
    }
}
