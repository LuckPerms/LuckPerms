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

package me.lucko.luckperms.common.webeditor;

import com.google.gson.JsonObject;

import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.http.BytesocksClient;
import me.lucko.luckperms.common.http.UnsuccessfulRequestException;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerTask;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.gson.GsonProvider;
import me.lucko.luckperms.common.util.gson.JObject;

import org.checkerframework.checker.nullness.qual.NonNull;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

public class WebEditorSocket extends WebSocketListener {

    // message type ids
    private static final String MESSAGE_TYPE_HELLO_REPLY = "hello-reply"; // plugin -> editor
    private static final String MESSAGE_TYPE_CHANGE_ACCEPTED = "change-accepted"; // plugin -> editor
    private static final String MESSAGE_TYPE_NEW_SESSION_DATA = "new-session-data"; // plugin -> editor
    private static final String MESSAGE_TYPE_HELLO = "hello"; // editor -> plugin
    private static final String MESSAGE_TYPE_TRUSTED_REPLY = "trusted-reply"; // editor -> plugin
    private static final String MESSAGE_TYPE_APPLY_CHANGE = "apply-change"; // editor -> plugin
    private static final String MESSAGE_TYPE_PING = "ping"; // editor -> plugin
    private static final String MESSAGE_TYPE_PONG = "pong"; // plugin -> editor

    /** The plugin */
    private final LuckPermsPlugin plugin;
    /** The sender who created the editor session */
    private final Sender sender;
    /** The web editor session */
    private final WebEditorSession session;
    /** The time a ping was last received */
    private long lastPing = 0;
    /** A task to check if the socket is still active */
    private SchedulerTask keepaliveTask;

    /** The websocket backing the connection */
    private BytesocksClient.Socket socket;
    /** The public and private keys used to sign messages sent by the plugin */
    private KeyPair localKeys;
    /** A list of attempted connections (connections that have been attempted with an untrusted public key) */
    private final Map<String, PublicKey> attemptedConnections = new HashMap<>();
    /** The public key used by the editor to sign messages */
    private PublicKey remotePublicKey;
    /** If the connection is closed */
    private boolean closed = false;

    /** A future that will complete when the connection is established successfully */
    private final CompletableFuture<Void> connectFuture = new CompletableFuture<>();
    /** Message receive lock */
    private final ReentrantLock lock = new ReentrantLock();

    public WebEditorSocket(LuckPermsPlugin plugin, Sender sender, WebEditorSession session) {
        this.plugin = plugin;
        this.sender = sender;
        this.session = session;
    }

    /**
     * Initializes the socket connection.
     *
     * @param client the bytesocks client to connect to
     * @throws UnsuccessfulRequestException if the request fails
     * @throws IOException if an i/o error occurs
     */
    public void initialize(BytesocksClient client) throws UnsuccessfulRequestException, IOException {
        this.socket = client.createSocket(this);
        this.localKeys = generateKeyPair();
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
            this.connectFuture.get(timeout, unit);
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
        JsonObject payload = request.getPayload();

        String channelId = this.socket.channelId();
        payload.addProperty("socketChannelId", channelId);

        String publicKey = Base64.getEncoder().encodeToString(this.localKeys.getPublic().getEncoded());
        payload.addProperty("publicKey", publicKey);
    }

    /**
     * Send a message to the socket.
     *
     * <p>The message will be encoded as JSON and
     * signed using the public public key.</p>
     *
     * @param msg the message
     */
    private void send(JsonObject msg) {
        String encoded = GsonProvider.normal().toJson(msg);
        String signature;

        try {
            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initSign(this.localKeys.getPrivate());
            sign.update(encoded.getBytes(StandardCharsets.UTF_8));

            signature = Base64.getEncoder().encodeToString(sign.sign());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        JsonObject frame = new JObject()
                .add("msg", encoded)
                .add("signature", signature)
                .toJson();

        this.socket.socket().send(GsonProvider.normal().toJson(frame));
    }

    /**
     * Handles a message "frame" sent from the editor.
     *
     * <p>Initially, the plugin handler waits for an initial connection from the editor.
     * When the editor makes first contact, it will share a public key that all future message
     * frames will be signed with.
     * All subsequent messages can be verified as originating from the same editor instance by
     * using the public key to check the signature is valid. Any other clients snooping on the
     * channel/connection cannot spoof messages because they do not have the private key.</p>
     *
     * @param stringMsg the message
     * @throws Exception catch all
     */
    private void handleMessageFrame(String stringMsg) throws Exception {
        JsonObject frame = GsonProvider.parser().parse(stringMsg).getAsJsonObject();

        String innerMsg = frame.get("msg").getAsString();
        String signature = frame.get("signature").getAsString();

        if (innerMsg == null || innerMsg.isEmpty() || signature == null || signature.isEmpty()) {
            throw new IllegalArgumentException("Incomplete message");
        }

        boolean verified = true;

        if (this.remotePublicKey != null) {
            // check signature to ensure the message is from the connected editor
            try {
                Signature sign = Signature.getInstance("SHA256withRSA");
                sign.initVerify(this.remotePublicKey);
                sign.update(innerMsg.getBytes(StandardCharsets.UTF_8));

                byte[] signatureBytes = Base64.getDecoder().decode(signature);
                verified = sign.verify(signatureBytes);
            } catch (Exception e) {
                verified = false;
            }
        }

        // parse the inner message
        JsonObject msg = GsonProvider.parser().parse(innerMsg).getAsJsonObject();
        String type = msg.get("type").getAsString();

        if (type.equals(MESSAGE_TYPE_HELLO)) {
            handleHelloMessage(msg);
            return;
        }

        if (!verified) {
            throw new IllegalStateException("Signature not accepted");
        }

        switch (type) {
            case MESSAGE_TYPE_APPLY_CHANGE:
                handleApplyChangeMessage(msg);
                break;
            case MESSAGE_TYPE_TRUSTED_REPLY:
                handleTrustedReplyMessage();
                break;
            case MESSAGE_TYPE_PING:
                handlePing();
                break;
            default:
                throw new IllegalStateException("Invalid message type: " + type);
        }
    }

    private void handleHelloMessage(JsonObject msg) {
        String nonce = msg.get("nonce").getAsString();
        if (nonce == null || nonce.isEmpty()) {
            throw new IllegalStateException("Invalid nonce");
        }

        // parse public key
        PublicKey remotePublicKey = parsePublicKey(msg.get("publicKey").getAsString());
        boolean reconnected = false;

        // if the public key has already been set (if a session has already connected),
        // don't accept a new connection unless the public keys match
        // i.e. this allows the same editor to re-connect, but prevents new connections
        if (this.remotePublicKey != null) {
            if (!this.remotePublicKey.equals(remotePublicKey)) {
                send(new JObject()
                        .add("type", MESSAGE_TYPE_HELLO_REPLY)
                        .add("nonce", nonce)
                        .add("state", "rejected")
                        .toJson()
                );
                return;
            }

            reconnected = true;
        }

        // check if the public key is trusted
        if (!this.plugin.getWebEditorStore().keystore().isTrusted(this.sender, remotePublicKey.getEncoded())) {
            // prompt the user to trust the key
            send(new JObject()
                    .add("type", MESSAGE_TYPE_HELLO_REPLY)
                    .add("nonce", nonce)
                    .add("state", "untrusted")
                    .toJson()
            );

            String browser = msg.get("browser").getAsString();

            Message.EDITOR_SOCKET_UNTRUSTED.send(this.sender, nonce, browser, this.session.getCommandLabel(), this.sender.isConsole());
            this.attemptedConnections.put(nonce, remotePublicKey);
            return;
        }

        // public key is already trusted! woo
        this.remotePublicKey = remotePublicKey;

        // send a reply back to the editor to say we accept
        send(new JObject()
                .add("type", MESSAGE_TYPE_HELLO_REPLY)
                .add("nonce", nonce)
                .add("state", "accepted")
                .toJson()
        );

        if (reconnected) {
            Message.EDITOR_SOCKET_RECONNECTED.send(this.sender);
        } else {
            Message.EDITOR_SOCKET_CONNECTED.send(this.sender);
        }
    }

    public boolean trustConnection(String nonce) {
        if (shouldIgnoreMessages()) {
            return false;
        }

        if (this.remotePublicKey != null) {
            return false;
        }

        PublicKey publicKey = this.attemptedConnections.get(nonce);
        if (publicKey == null) {
            return false;
        }

        this.remotePublicKey = publicKey;

        // save the key in the keystore
        this.plugin.getWebEditorStore().keystore().trust(this.sender, this.remotePublicKey.getEncoded());

        // send a reply back to the editor to say that it is now trusted
        send(new JObject()
                .add("type", MESSAGE_TYPE_HELLO_REPLY)
                .add("nonce", nonce)
                .add("state", "trusted")
                .toJson()
        );
        return true;
    }

    private void handleApplyChangeMessage(JsonObject msg) {
        if (!this.sender.hasPermission(CommandPermission.APPLY_EDITS)) {
            throw new IllegalStateException("Sender does not have applyedits permission");
        }

        // get the bytebin code containing the editor data
        String code = msg.get("code").getAsString();
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Invalid code");
        }

        // send "change-accepted" response
        this.plugin.getBootstrap().getScheduler().executeAsync(() -> {
            send(new JObject()
                    .add("type", MESSAGE_TYPE_CHANGE_ACCEPTED)
                    .toJson()
            );
        });

        // download data from bytebin
        JsonObject data;
        try {
            data = this.plugin.getBytebin().getJsonContent(code).getAsJsonObject();
            Objects.requireNonNull(data);
        } catch (UnsuccessfulRequestException | IOException e) {
            throw new RuntimeException("Error reading data", e);
        }

        // apply changes
        new WebEditorResponse(code, data).apply(this.plugin, this.sender, "lp", false);

        // create a new session
        String newSessionCode = this.session.createFollowUpSession();
        send(new JObject()
                .add("type", MESSAGE_TYPE_NEW_SESSION_DATA)
                .add("newSessionCode", newSessionCode)
                .toJson()
        );
    }

    private void handleTrustedReplyMessage() {
        Message.EDITOR_SOCKET_CONNECTED.send(this.sender);
    }

    private void handlePing() {
        this.lastPing = System.currentTimeMillis();
        send(new JObject()
                .add("type", MESSAGE_TYPE_PONG)
                .add("ok", true)
                .toJson()
        );
    }

    private void closeSocket() {
        new Exception().printStackTrace();
        this.socket.socket().close(1000, "Normal");
        this.closed = true;
    }

    private void cancelKeepalive() {
        new Exception().printStackTrace();
        if (this.keepaliveTask != null) {
            this.keepaliveTask.cancel();
            this.keepaliveTask = null;
        }
    }

    public void scheduleCleanupIfUnused() {
        this.plugin.getBootstrap().getScheduler().asyncLater(this::afterOpenFor1Minute, 1, TimeUnit.MINUTES);
    }

    private void afterOpenFor1Minute() {
        if (!this.closed && this.remotePublicKey == null && this.attemptedConnections.isEmpty()) {
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
        if (System.currentTimeMillis() - this.lastPing > TimeUnit.MINUTES.toMillis(1)) {
            cancelKeepalive();
            closeSocket();
        }
    }

    private void close() {
        try {
            send(new JObject()
                    .add("type", MESSAGE_TYPE_PONG)
                    .add("ok", false)
                    .toJson()
            );
        } catch (Exception e) {
            // ignore
        }

        cancelKeepalive();
        closeSocket();
    }

    /**
     * Checks if incoming messages should be ignored.
     *
     * @return true if messages should be ignored
     */
    private boolean shouldIgnoreMessages() {
        if (this.closed) {
            return true;
        }

        if (!this.sender.isValid()) {
            close();
            return true;
        }

        return false;
    }

    @Override
    public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
        this.connectFuture.complete(null);
    }

    @Override
    public void onMessage(@NonNull WebSocket webSocket, @NonNull String msg) {
        this.plugin.getBootstrap().getScheduler().executeAsync(() -> {
            this.lock.lock();
            try {
                if (shouldIgnoreMessages()) {
                    return;
                }

                handleMessageFrame(msg);
            } catch (Exception e) {
                this.plugin.getLogger().warn("Exception occurred handling message from socket", e);
            } finally {
                this.lock.unlock();
            }
        });
    }

    private static PublicKey parsePublicKey(String base64String) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64String);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
            KeyFactory rsa = KeyFactory.getInstance("RSA");
            return rsa.generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalArgumentException("Exception parsing public key", e);
        }
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(4096);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Exception generating keypair", e);
        }
    }

}
