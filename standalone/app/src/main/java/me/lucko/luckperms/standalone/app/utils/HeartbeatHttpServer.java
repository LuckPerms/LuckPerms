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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.luckperms.api.platform.Health;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Provides a tiny http server indicating the current status of the app
 */
public class HeartbeatHttpServer implements HttpHandler, AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger(HeartbeatHttpServer.class);

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("heartbeat-http-server-%d")
            .build()
    );

    public static HeartbeatHttpServer createAndStart(int port, Supplier<Health> healthReporter) {
        HeartbeatHttpServer socket = null;

        try {
            socket = new HeartbeatHttpServer(healthReporter, port);
            LOGGER.info("Started healthcheck HTTP server on :" + port);
        } catch (Exception e) {
            LOGGER.error("Error starting Heartbeat HTTP server", e);
        }

        return socket;
    }

    private final Supplier<Health> healthReporter;
    private final HttpServer server;

    public HeartbeatHttpServer(Supplier<Health> healthReporter, int port) throws IOException {
        this.healthReporter = healthReporter;
        this.server = HttpServer.create(new InetSocketAddress(port), 50);
        this.server.createContext("/health", this);
        this.server.setExecutor(EXECUTOR);
        this.server.start();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Health health = this.healthReporter.get();
        byte[] response = health.toString().getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(health.isHealthy() ? 200 : 503, response.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(response);
        }
    }

    @Override
    public void close() {
        this.server.stop(0);
    }
}
