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
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.function.Consumer;

/**
 * Simple/dumb socket that listens for connections on a given port,
 * reads the input to a string, then executes it as a command.
 *
 * Combined with a small sh/nc program, this makes it easy to execute
 * commands against a standalone instance of LP in a Docker container.
 */
public class DockerCommandSocket extends ServerSocket implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger(DockerCommandSocket.class);

    public static DockerCommandSocket createAndStart(int port, TerminalInterface terminal) {
        DockerCommandSocket socket = null;

        try {
            socket = new DockerCommandSocket(port, terminal::runCommand);

            Thread thread = new Thread(socket, "docker-command-socket");
            thread.setDaemon(true);
            thread.start();

            LOGGER.info("Created Docker command socket on port " + port);
        } catch (Exception e) {
            LOGGER.error("Error starting docker command socket", e);
        }

        return socket;
    }

    private final Consumer<String> callback;

    public DockerCommandSocket(int port, Consumer<String> callback) throws IOException {
        super(port);
        this.callback = callback;
    }

    @Override
    public void run() {
        while (!isClosed()) {
            try (Socket socket = accept()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    String cmd;
                    while ((cmd = reader.readLine()) != null) {
                        LOGGER.info("Executing command from Docker: " + cmd);
                        this.callback.accept(cmd);
                    }
                }
            } catch (IOException e) {
                if (e instanceof SocketException && e.getMessage().equals("Socket closed")) {
                    return;
                }
                LOGGER.error("Error processing input from the Docker socket", e);
            }
        }
    }
}
