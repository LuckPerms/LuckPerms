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

package me.lucko.luckperms.standalone.app;

import me.lucko.luckperms.standalone.app.integration.CommandExecutor;
import me.lucko.luckperms.standalone.app.integration.ShutdownCallback;
import me.lucko.luckperms.standalone.app.utils.DockerCommandSocket;
import me.lucko.luckperms.standalone.app.utils.HeartbeatHttpServer;
import me.lucko.luckperms.standalone.app.utils.TerminalInterface;
import net.luckperms.api.LuckPerms;
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

    /** A callback to shutdown the application via the loader bootstrap. */
    private final ShutdownCallback shutdownCallback;

    /** The instance of the LuckPerms API available within the app */
    private LuckPerms luckPermsApi;
    /** A command executor interface to run LuckPerms commands */
    private CommandExecutor commandExecutor;

    /** If the application is running */
    private final AtomicBoolean running = new AtomicBoolean(true);

    /** The docker command socket */
    private DockerCommandSocket dockerCommandSocket;
    /** The heartbeat http server */
    private HeartbeatHttpServer heartbeatHttpServer;

    public LuckPermsApplication(ShutdownCallback shutdownCallback) {
        this.shutdownCallback = shutdownCallback;
    }

    /**
     * Start the app
     */
    public void start(String[] args) {
        TerminalInterface terminal = new TerminalInterface(this, this.commandExecutor);

        List<String> arguments = Arrays.asList(args);
        if (arguments.contains("--docker")) {
            this.dockerCommandSocket = DockerCommandSocket.createAndStart("/opt/luckperms/luckperms.sock", terminal);
            this.heartbeatHttpServer = HeartbeatHttpServer.createAndStart(3001, () -> this.luckPermsApi.runHealthCheck());
        }

        terminal.start(); // blocking
    }

    public void requestShutdown() {
        this.shutdownCallback.shutdown();
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
    }

    public AtomicBoolean runningState() {
        return this.running;
    }

    // called before start()
    public void setApi(LuckPerms luckPermsApi) {
        this.luckPermsApi = luckPermsApi;
    }

    // called before start()
    public void setCommandExecutor(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    public LuckPerms getApi() {
        return this.luckPermsApi;
    }

    public CommandExecutor getCommandExecutor() {
        return this.commandExecutor;
    }

    public String getVersion() {
        return "@version@";
    }

}
