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

package me.lucko.luckperms.common.webeditor.socket;

import com.google.gson.JsonObject;
import me.lucko.luckperms.common.http.BytesocksClient;
import me.lucko.luckperms.common.http.UnsuccessfulRequestException;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerTask;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.gson.GsonProvider;
import me.lucko.luckperms.common.util.gson.JObject;
import me.lucko.luckperms.common.webeditor.WebEditorRequest;
import me.lucko.luckperms.common.webeditor.WebEditorSession;
import me.lucko.luckperms.common.webeditor.socket.listener.WebEditorSocketListener;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class WebEditorSocket {

    /** The plugin */
    private final LuckPermsPlugin plugin;
    /** The sender who created the editor session */
    private final Sender sender;
    /** The web editor session */
    private final WebEditorSession session;
    /** The socket listener that handles incoming messages */
    private final WebEditorSocketListener listener;
    /** The public and private keys used to sign messages sent by the plugin */
    private final KeyPair pluginKeyPair;

    /** The websocket backing the connection */
    private BytesocksClient.Socket socket;
    /** A task to check if the socket is still active */
    private SchedulerTask keepaliveTask;
    /** The public key used by the editor to sign messages */
    private PublicKey remotePublicKey;
    /** If the connection is closed */
    private boolean closed = false;

    public WebEditorSocket(LuckPermsPlugin plugin, Sender sender, WebEditorSession session) {
        this.plugin = plugin;
        this.sender = sender;
        this.session = session;
        this.listener = new WebEditorSocketListener(this);
        this.pluginKeyPair = plugin.getWebEditorStore().keyPair();
    }

    /**
     * Initializes the socket connection.
     *
     * @param client the bytesocks client to connect to
     * @throws UnsuccessfulRequestException if the request fails
     * @throws IOException if an i/o error occurs
     */
    public void initialize(BytesocksClient client) throws UnsuccessfulRequestException, IOException {
        this.socket = client.createSocket(this.listener);
    }

    /**
     * Waits the specified amount of time for the socket to connect,
     * before throwing an exception if a timeout occurs.
     *
     * @param timeout the timeout
     * @param unit the timeout unit
     */
    public void waitForConnect(long timeout, TimeUnit unit) {
        try {
            this.listener.connectFuture().get(timeout, unit);
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            throw new RuntimeException("Timed out waiting to socket to connect", e);
        }
    }

    /**
     * Adds detail about the socket channel and the plugin public key to
     * the editor request payload that gets sent via bytebin to the viewer.
     *
     * @param request the request
     */
    public void appendDetailToRequest(WebEditorRequest request) {
        String channelId = this.socket.channelId();
        String publicKey = Base64.getEncoder().encodeToString(this.pluginKeyPair.getPublic().getEncoded());

        JsonObject socket = new JsonObject();
        socket.addProperty("protocolVersion", SignatureAlgorithm.INSTANCE.protocolVersion());
        socket.addProperty("channelId", channelId);
        socket.addProperty("publicKey", publicKey);

        JsonObject payload = request.getPayload();
        payload.add("socket", socket);
    }

    /**
     * Send a message to the socket.
     *
     * <p>The message will be encoded as JSON and
     * signed using the public public key.</p>
     *
     * @param msg the message
     */
    public void send(JsonObject msg) {
        String encoded = GsonProvider.normal().toJson(msg);
        String signature = SignatureAlgorithm.INSTANCE.sign(this.pluginKeyPair.getPrivate(), encoded);

        JsonObject frame = new JObject()
                .add("msg", encoded)
                .add("signature", signature)
                .toJson();

        this.socket.socket().send(GsonProvider.normal().toJson(frame));
    }

    public boolean trustConnection(String nonce) {
        if (this.listener.shouldIgnoreMessages()) {
            return false;
        }

        if (this.remotePublicKey != null) {
            return false;
        }

        PublicKey publicKey = this.listener.helloHandler().getAttemptedConnection(nonce);
        if (publicKey == null) {
            return false;
        }

        this.remotePublicKey = publicKey;

        // save the key in the keystore
        this.plugin.getWebEditorStore().keystore().trust(this.sender, this.remotePublicKey.getEncoded());

        // send a reply back to the editor to say that it is now trusted
        send(SocketMessageType.HELLO_REPLY.builder()
                .add("nonce", nonce)
                .add("state", "trusted")
                .toJson()
        );
        return true;
    }

    public void scheduleCleanupIfUnused() {
        this.plugin.getBootstrap().getScheduler().asyncLater(this::afterOpenFor1Minute, 1, TimeUnit.MINUTES);
    }

    private void afterOpenFor1Minute() {
        if (this.closed) {
            return;
        }

        if (this.remotePublicKey == null && !this.listener.helloHandler().hasAttemptedConnection()) {
            // If the editor hasn't made an initial connection after 1 minute,
            // then close + stop listening to the socket.
            closeSocket();
        } else {
            // Otherwise, setup a keepalive monitoring task
            this.keepaliveTask = this.plugin.getBootstrap().getScheduler().asyncRepeating(this::keepalive, 10, TimeUnit.SECONDS);
        }
    }

    /**
     * The keepalive tasks checks to see when the last ping from the editor was. If the editor
     * hasn't sent anything for 1 minute, then close the connection
     */
    private void keepalive() {
        if (System.currentTimeMillis() - this.listener.pingHandler().getLastPing() > TimeUnit.MINUTES.toMillis(1)) {
            cancelKeepalive();
            closeSocket();
        }
    }

    public void close() {
        try {
            send(SocketMessageType.PONG.builder()
                    .add("ok", false)
                    .toJson()
            );
        } catch (Exception e) {
            // ignore
        }

        cancelKeepalive();
        closeSocket();
    }

    private void closeSocket() {
        this.socket.socket().close(1000, "Normal");
        this.plugin.getWebEditorStore().sockets().removeSocket(this);
        this.closed = true;
    }

    private void cancelKeepalive() {
        if (this.keepaliveTask != null) {
            this.keepaliveTask.cancel();
            this.keepaliveTask = null;
        }
    }

    public LuckPermsPlugin getPlugin() {
        return this.plugin;
    }

    public Sender getSender() {
        return this.sender;
    }

    public WebEditorSession getSession() {
        return this.session;
    }

    public BytesocksClient.Socket getSocket() {
        return this.socket;
    }

    public PublicKey getRemotePublicKey() {
        return this.remotePublicKey;
    }

    public void setRemotePublicKey(PublicKey remotePublicKey) {
        this.remotePublicKey = remotePublicKey;
    }

    public boolean isClosed() {
        return this.closed;
    }

}
