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

package me.lucko.luckperms.common.webeditor.socket.listener;

import com.google.gson.JsonObject;
import me.lucko.luckperms.common.util.gson.GsonProvider;
import me.lucko.luckperms.common.webeditor.socket.SignatureAlgorithm;
import me.lucko.luckperms.common.webeditor.socket.SocketMessageType;
import me.lucko.luckperms.common.webeditor.socket.WebEditorSocket;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.EOFException;
import java.security.PublicKey;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

public class WebEditorSocketListener extends WebSocketListener {

    /** The socket */
    private final WebEditorSocket socket;

    // Individual handlers for each message type
    private final HandlerHello helloHandler;
    private final HandlerConnected connectedHandler;
    private final HandlerPing pingHandler;
    private final HandlerChangeRequest changeRequestHandler;

    /** A future that will complete when the connection is established successfully */
    private final CompletableFuture<Void> connectFuture = new CompletableFuture<>();

    /** Message receive lock */
    private final ReentrantLock lock = new ReentrantLock();

    public WebEditorSocketListener(WebEditorSocket socket) {
        this.socket = socket;
        this.helloHandler = new HandlerHello(socket);
        this.connectedHandler = new HandlerConnected(socket);
        this.pingHandler = new HandlerPing(socket);
        this.changeRequestHandler = new HandlerChangeRequest(socket);
    }

    @Override
    public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
        this.connectFuture.complete(null);
    }

    @Override
    public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable e, Response response) {
        if (e instanceof EOFException) {
            return; // ignore
        }
        this.socket.getPlugin().getLogger().warn("Exception occurred in web socket", e);
    }

    @Override
    public void onMessage(@NonNull WebSocket webSocket, @NonNull String msg) {
        this.socket.getPlugin().getBootstrap().getScheduler().async(() -> {
            this.lock.lock();
            try {
                if (shouldIgnoreMessages()) {
                    return;
                }

                handleMessageFrame(msg);
            } catch (Exception e) {
                this.socket.getPlugin().getLogger().warn("Exception occurred handling message from socket", e);
            } finally {
                this.lock.unlock();
            }
        });
    }

    /**
     * Checks if incoming messages should be ignored.
     *
     * @return true if messages should be ignored
     */
    public boolean shouldIgnoreMessages() {
        if (this.socket.isClosed()) {
            return true;
        }

        if (!this.socket.getSender().isValid()) {
            this.socket.close();
            return true;
        }

        return false;
    }

    private void handleMessageFrame(String stringMsg) {
        JsonObject frame = GsonProvider.parser().parse(stringMsg).getAsJsonObject();

        String innerMsg = frame.get("msg").getAsString();
        String signature = frame.get("signature").getAsString();

        if (innerMsg == null || innerMsg.isEmpty() || signature == null || signature.isEmpty()) {
            throw new IllegalArgumentException("Incomplete message");
        }

        // check signature to ensure the message is from the connected editor
        PublicKey remotePublicKey = this.socket.getRemotePublicKey();
        boolean verified = remotePublicKey != null && SignatureAlgorithm.INSTANCE.verify(remotePublicKey, innerMsg, signature);

        // parse the inner message
        JsonObject msg = GsonProvider.parser().parse(innerMsg).getAsJsonObject();
        SocketMessageType type = SocketMessageType.getById(msg.get("type").getAsString());

        if (type == SocketMessageType.HELLO) {
            this.helloHandler.handle(msg);
            return;
        }

        if (!verified) {
            throw new IllegalStateException("Signature not accepted");
        }

        switch (type) {
            case CHANGE_REQUEST:
                this.changeRequestHandler.handle(msg);
                break;
            case CONNECTED:
                this.connectedHandler.handle(msg);
                break;
            case PING:
                this.pingHandler.handle(msg);
                break;
            default:
                throw new IllegalStateException("Invalid message type: " + type);
        }
    }

    public CompletableFuture<Void> connectFuture() {
        return this.connectFuture;
    }

    public HandlerHello helloHandler() {
        return this.helloHandler;
    }

    public HandlerConnected connectedHandler() {
        return this.connectedHandler;
    }

    public HandlerPing pingHandler() {
        return this.pingHandler;
    }

    public HandlerChangeRequest changeRequestHandler() {
        return this.changeRequestHandler;
    }
}
