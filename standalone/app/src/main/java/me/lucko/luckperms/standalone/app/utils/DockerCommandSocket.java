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

package me.lucko.luckperms.standalone.app.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

/**
 * Simple/dumb unix domain socket that listens for connections,
 * reads the input to a string, then executes it as a command.
 *
 * <p>Combined with a small sh/nc program, this makes it easy to execute
 * commands against a standalone instance of LP in a Docker container.</p>
 */
public class DockerCommandSocket implements Runnable, AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger(DockerCommandSocket.class);

    public static DockerCommandSocket createAndStart(String socketPath, TerminalInterface terminal) {
        DockerCommandSocket socket = null;

        try {
            Path path = Paths.get(socketPath);
            Files.deleteIfExists(path);

            ServerSocketChannel channel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
            channel.bind(UnixDomainSocketAddress.of(path));

            socket = new DockerCommandSocket(channel, terminal::runCommand);

            Thread thread = new Thread(socket, "docker-command-socket");
            thread.setDaemon(true);
            thread.start();
        } catch (Exception e) {
            LOGGER.error("Error starting docker command socket", e);
        }

        return socket;
    }

    private final ServerSocketChannel channel;
    private final Consumer<String> callback;

    public DockerCommandSocket(ServerSocketChannel channel, Consumer<String> callback) throws IOException {
        this.channel = channel;
        this.callback = callback;
    }

    @Override
    public void run() {
        while (this.channel.isOpen()) {
            try (SocketChannel socket = this.channel.accept()) {
                try (BufferedReader reader = new BufferedReader(Channels.newReader(socket, StandardCharsets.UTF_8))) {
                    String cmd;
                    while ((cmd = reader.readLine()) != null) {
                        LOGGER.info("Executing command from Docker: " + cmd);
                        this.callback.accept(cmd);
                    }
                }
            } catch (ClosedChannelException e) {
                // ignore
            } catch (IOException e) {
                LOGGER.error("Error processing input from the Docker socket", e);
            }
        }
    }

    @Override
    public void close() throws Exception {
        this.channel.close();
    }
}
