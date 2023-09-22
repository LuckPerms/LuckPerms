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

package me.lucko.luckperms.integration;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class IntegrationTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationTests.class);

    @Nested
    class Bukkit extends Generic {

        @Container
        private final GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("itzg/minecraft-server"))
                .withEnv("EULA", "TRUE")
                .withEnv("TYPE", "PAPER")
                .withEnv("ONLINE_MODE", "FALSE")
                .withEnv("LUCKPERMS_DEBUG_LOGINS", "true")
                .withExposedPorts(25565)
                .withClasspathResourceMapping("luckperms-bukkit.testjar", "/plugins/LuckPerms.jar", BindMode.READ_ONLY)
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .waitingFor(Wait.forLogMessage(".*Done.*", 1).withStartupTimeout(Duration.ofSeconds(120)));

        @Override
        protected GenericContainer<?> container() {
            return this.container;
        }

        @Test
        public void testBukkit() throws IOException, InterruptedException {
            // ensure the server has started
            assertTrue(this.container.isRunning());

            // ensure the LuckPerms plugin enabled successfully
            assertLogsContain("[LuckPerms] Successfully enabled.");

            // create a Minecraft client and login to the server
            loginWithClient();

            // wait for the player to connect
            awaitLogsContain("lucko joined the game");

            // ensure the player login was handled by LuckPerms
            assertLogsContain(
                    "[LuckPerms] Processing pre-login for 9a06e4db-8487-39b2-8751-1d107561d787 - lucko",
                    "[LuckPerms] Processing login for 9a06e4db-8487-39b2-8751-1d107561d787 - lucko"
            );

            // give player some permissions & enable verbose mode
            executeServerCommand("lp user lucko permission set minecraft.command.ban");
            executeServerCommand("lp verbose on minecraft.command.ban");

            // wait for verbose mode to be enabled
            awaitLogsContain("[LP] Verbose logging enabled");

            // get the client to execute a command
            executeClientCommand("ban");

            // wait for the verbose results to show up
            awaitLogsContain("[LP] VB > lucko - minecraft.command.ban - true");

            // disable verbose mode and disconnect the client
            executeServerCommand("lp verbose off");
            disconnectClient();

            // wait for verbose to be disabled and the client to leave
            awaitLogsContain(
                    "[LP] Verbose logging disabled.",
                    "lucko left the game"
            );

            // stop the server
            executeServerCommand("stop");

            // ensure the server process stops gracefully
            await().atMost(30, TimeUnit.SECONDS).until(() -> !this.container.isRunning());

            // ensure the plugin disabled
            assertLogsContain("[LuckPerms] Goodbye!");

            // check for LuckPerms stack traces in the log output
            assertFalse(logsContain("at me.lucko.luckperms"), "There seems to be stack traces from LuckPerms in the logs");
        }
    }

    @Nested
    class Fabric extends Generic {

        @Container
        private final GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("itzg/minecraft-server"))
                .withEnv("EULA", "TRUE")
                .withEnv("TYPE", "FABRIC")
                .withEnv("ONLINE_MODE", "FALSE")
                .withEnv("MODRINTH_PROJECTS", "fabric-api")
                .withEnv("LUCKPERMS_DEBUG_LOGINS", "true")
                .withExposedPorts(25565)
                .withClasspathResourceMapping("luckperms-fabric.testjar", "/mods/LuckPerms.jar", BindMode.READ_ONLY)
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .waitingFor(Wait.forLogMessage(".*Done.*", 1).withStartupTimeout(Duration.ofSeconds(120)));

        @Override
        protected GenericContainer<?> container() {
            return this.container;
        }

        @Test
        public void testFabric() throws IOException, InterruptedException {
            // ensure the server has started
            assertTrue(this.container.isRunning());

            // ensure the LuckPerms plugin enabled successfully
            assertLogsContain("Successfully enabled.");// assertLogsContain("[LuckPerms] Successfully enabled.");

            // create a Minecraft client and login to the server
            loginWithClient();

            // wait for the player to connect
            //awaitLogsContain("lucko joined the game");

            // ensure the player login was handled by LuckPerms
            awaitLogsContain(
                    "Processing pre-login for 9a06e4db-8487-39b2-8751-1d107561d787 - lucko",
                    "Processing login for 9a06e4db-8487-39b2-8751-1d107561d787 - lucko"
            );

            // give player some permissions & enable verbose mode
            executeServerCommand("lp user lucko permission set minecraft.command.ban");
            executeServerCommand("lp verbose on minecraft.command.ban");

            // wait for verbose mode to be enabled
            awaitLogsContain("[LP] Verbose logging enabled");

            // get the client to execute a command
            executeClientCommand("ban");

            // wait for the verbose results to show up
            awaitLogsContain("[LP] VB > lucko - minecraft.command.ban - true");

            // disable verbose mode and disconnect the client
            executeServerCommand("lp verbose off");
            disconnectClient();

            // wait for verbose to be disabled and the client to leave
            awaitLogsContain(
                    "[LP] Verbose logging disabled.",
                    "lucko left the game"
            );

            // stop the server
            executeServerCommand("stop");

            // ensure the server process stops gracefully
            await().atMost(30, TimeUnit.SECONDS).until(() -> !this.container.isRunning());

            // ensure the plugin disabled
            assertLogsContain("[LuckPerms] Goodbye!");

            // check for LuckPerms stack traces in the log output
            assertFalse(logsContain("at me.lucko.luckperms"), "There seems to be stack traces from LuckPerms in the logs");
        }
    }

    static abstract class Generic {
        private TcpClientSession client;

        protected abstract GenericContainer<?> container();

        protected boolean logsContain(String... strings) {
            String logs = container().getLogs();
            return Arrays.stream(strings).allMatch(logs::contains);
        }

        protected void assertLogsContain(String... strings) {
            assertTrue(logsContain(strings), "container logs must contain: " + Arrays.stream(strings).collect(Collectors.joining(", ", "'", "'")));
        }

        protected void awaitLogsContain(String... strings) {
            await().atMost(10, TimeUnit.SECONDS).until(() -> logsContain(strings));
        }

        protected void loginWithClient() {
            this.client = new TcpClientSession(
                    container().getHost(),
                    container().getFirstMappedPort(),
                    new MinecraftProtocol("lucko")
            );
            this.client.connect();
        }

        protected void disconnectClient() {
            this.client.disconnect("Disconnecting");
        }

        protected void executeServerCommand(String command) throws IOException, InterruptedException {
            assertEquals(0, container().execInContainer("mc-send-to-console", command).getExitCode());
        }

        protected void executeClientCommand(String command) {
            // TODO: the other args here are a bit of a mystery but it seems to work
            this.client.send(new ServerboundChatCommandPacket(command, Instant.now().toEpochMilli(), 0, new ArrayList<>(), 0, new BitSet()));
        }

    }

}
