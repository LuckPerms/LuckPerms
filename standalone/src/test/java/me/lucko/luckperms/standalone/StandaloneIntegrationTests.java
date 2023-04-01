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

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.node.types.Permission;
import me.lucko.luckperms.standalone.app.LuckPermsApplication;
import me.lucko.luckperms.standalone.app.integration.CommandExecutor;
import me.lucko.luckperms.standalone.app.integration.HealthReporter;

import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.NodeEqualityPredicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A set of 'integration tests' for the standalone LuckPerms app.
 */
public class StandaloneIntegrationTests {

    private @TempDir Path tempDir;

    @Test
    public void testLoadEnableDisable() {
        useTestPlugin((app, bootstrap, plugin) -> {
            HealthReporter.Health health = app.getHealthReporter().poll();
            assertNotNull(health);
            assertTrue(health.isUp());
        });
    }

    @Test
    public void testRunCommand() {
        useTestPlugin((app, bootstrap, plugin) -> {
            CommandExecutor commandExecutor = app.getCommandExecutor();
            commandExecutor.execute("group default permission set test").join();

            Group group = bootstrap.getPlugin().getStorage().loadGroup("default").join().orElse(null);
            assertNotNull(group);
            assertTrue(group.hasNode(DataType.NORMAL, Permission.builder().permission("test").build(), NodeEqualityPredicate.EXACT).asBoolean());
        });
    }

    @Test
    public void testReloadConfig() throws IOException {
        useTestPlugin((app, bootstrap, plugin) -> {
            String server = plugin.getConfiguration().get(ConfigKeys.SERVER);
            assertEquals("global", server);

            Integer syncTime = plugin.getConfiguration().get(ConfigKeys.SYNC_TIME);
            assertEquals(-1, syncTime);

            Path config = this.tempDir.resolve("config.yml");
            assertTrue(Files.exists(config));

            String configString = Files.readString(config)
                    .replace("server: global", "server: test")
                    .replace("sync-minutes: -1", "sync-minutes: 10");
            Files.writeString(config, configString);

            plugin.getConfiguration().reload();

            server = plugin.getConfiguration().get(ConfigKeys.SERVER);
            assertEquals("test", server); // changed

            syncTime = plugin.getConfiguration().get(ConfigKeys.SYNC_TIME);
            assertEquals(-1, syncTime); // unchanged
        });
    }

    private <E extends Throwable> void useTestPlugin(TestPluginConsumer<E> consumer) throws E {
        LuckPermsApplication app = new LuckPermsApplication(() -> {});
        LPStandaloneTestBootstrap bootstrap = new LPStandaloneTestBootstrap(app, this.tempDir);

        bootstrap.onLoad();
        bootstrap.onEnable();

        try {
            consumer.accept(app, bootstrap, bootstrap.getPlugin());
        } finally {
            bootstrap.onDisable();
        }
    }

    interface TestPluginConsumer<E extends Throwable> {
        void accept(LuckPermsApplication app, LPStandaloneTestBootstrap bootstrap, LPStandalonePlugin plugin) throws E;
    }

}
