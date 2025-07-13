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

import me.lucko.luckperms.common.plugin.logging.Log4jPluginLogger;
import me.lucko.luckperms.library.LuckPermsLibrary;
import me.lucko.luckperms.library.LuckPermsLibraryDependencies;
import me.lucko.luckperms.standalone.utils.DockerCommandSocket;
import me.lucko.luckperms.standalone.utils.HeartbeatHttpServer;
import me.lucko.luckperms.standalone.utils.TerminalInterface;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The LuckPerms standalone application.
 */
public class LuckPermsApplication implements AutoCloseable {

    /** A logger instance */
    public static final Logger LOGGER = LogManager.getLogger(LuckPermsApplication.class);

    /** All of the LuckPerms stuff */
    private LuckPermsLibrary library;

    /** If the application is running */
    private final AtomicBoolean running = new AtomicBoolean(true);

    /** The docker command socket */
    private DockerCommandSocket dockerCommandSocket;
    /** The heartbeat http server */
    private HeartbeatHttpServer heartbeatHttpServer;

    /**
     * Start the app
     */
    public LuckPermsApplication(String[] args) {
    	library = new LuckPermsLibrary(
    			LuckPermsLibraryDependencies.loadAll(), new Log4jPluginLogger(LOGGER), StandaloneLibraryManager::new);
    	library.start();

        TerminalInterface terminal = new TerminalInterface(this);

        List<String> arguments = Arrays.asList(args);
        if (arguments.contains("--docker")) {
            this.dockerCommandSocket = DockerCommandSocket.createAndStart("/opt/luckperms/luckperms.sock", terminal);
            this.heartbeatHttpServer = HeartbeatHttpServer.createAndStart(3001, () -> library.getLuckPerms().runHealthCheck());
        }

        terminal.start(); // blocking
    }

    public void requestShutdown() {
        close();
        LogManager.shutdown(true);
    }

    @Override
    public void close() {
        this.running.set(false);

        if (this.dockerCommandSocket != null) {
            try {
                this.dockerCommandSocket.close();
            } catch (Exception e) {
                LOGGER.warn(e);
            }
        }

        if (this.heartbeatHttpServer != null) {
            try {
                this.heartbeatHttpServer.close();
            } catch (Exception e) {
                LOGGER.warn(e);
            }
        }

        library.close();
    }

    public AtomicBoolean runningState() {
        return this.running;
    }

    public LuckPermsLibrary getLibrary() {
        return library;
    }

}
