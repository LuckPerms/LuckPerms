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

import me.lucko.luckperms.common.http.BytesocksClient;
import me.lucko.luckperms.common.http.UnsuccessfulRequestException;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
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
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

public class WebEditorSocket extends WebSocketListener {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String AUTH_SECRET_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int AUTH_SECRET_LENGTH = 5;

    /** editor first contact with the plugin - no signatures established yet (editor -> plugin) */
    private static final String MESSAGE_FRAME_HELLO = "hello";
    /** editor message to plugin in the form {"msg":"...", "signature":"..."} (editor -> plugin) */
    private static final String MESSAGE_FRAME_MSG = "msg";

    // messages sent from the plugin to the editor are not signed
    private static final String MESSAGE_TYPE_HELLO_REPLY = "hello-reply"; // plugin -> editor
    private static final String MESSAGE_TYPE_CHANGE_ACCEPTED = "change-accepted"; // plugin -> editor
    private static final String MESSAGE_TYPE_NEW_SESSION_DATA = "new-session-data"; // plugin -> editor

    private static final String MESSAGE_TYPE_APPLY_CHANGE = "apply-change"; // editor -> plugin

    /** The plugin */
    private final LuckPermsPlugin plugin;
    /** The sender who created the editor session */
    private final Sender sender;
    /** The web editor session */
    private final WebEditorSession session;
    /** The time the socket was created */
    private final long creationTime = System.currentTimeMillis();

    /** The current state of this listener */
    private State state = State.WAITING_FOR_EDITOR_TO_CONNECT;

    /** The websocket backing the connection */
    private BytesocksClient.Socket socket;
    /** The auth secret that must be sent during initial handshake */
    private String authSecret;
    /** The public and private keys used to sign messages sent by the plugin */
    private KeyPair localKeys;
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
        this.authSecret = generateAuthSecret();
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

    public void appendDetailToRequest(WebEditorRequest request) {
        JsonObject payload = request.getPayload();

        String channelId = this.socket.channelId();
        payload.addProperty("socketChannelId", channelId);

        String publicKey = Base64.getEncoder().encodeToString(this.localKeys.getPublic().getEncoded());
        payload.addProperty("publicKey", publicKey);
    }

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
                .add("type", MESSAGE_FRAME_MSG)
                .add("msg", encoded)
                .add("signature", signature)
                .toJson();

        this.socket.socket().send(GsonProvider.normal().toJson(frame));
    }

    /**
     * Handles a message "frame" sent from the editor.
     *
     * <p>Initially, the socket waits for an initial connection from the editor.
     * When the editor makes first contact, it will share a public key that all future message
     * frames will be signed with.
     * All subsequent messages can be verified as originating from the same editor instance by
     * using the public key to check the signature is valid. Any other clients snooping on the
     * channel/connection cannot spoof messages because they do not have the private key.</p>
     *
     * @param msg the message
     * @throws Exception catch all
     */
    private void handleMessageFrame(String msg) throws Exception {
        if (this.authSecret == null) {
            throw new IllegalStateException("Auth secret is null");
        }

        JsonObject frame = GsonProvider.parser().parse(msg).getAsJsonObject();

        if (this.state == State.WAITING_FOR_EDITOR_TO_CONNECT) {
            String type = frame.get("type").getAsString();
            if (!MESSAGE_FRAME_HELLO.equals(type)) {
                throw new IllegalStateException("Unexpected message type: " + type);
            }

            String authSecret = frame.get("auth").getAsString();
            if (!MessageDigest.isEqual(authSecret.getBytes(StandardCharsets.UTF_8), this.authSecret.getBytes(StandardCharsets.UTF_8))) {
                throw new IllegalStateException("Invalid auth secret");
            }

            // parse public key + update state
            this.remotePublicKey = parsePublicKey(frame.get("publicKey").getAsString());
            this.state = State.WAITING_FOR_CHANGES;

            // send a reply back to the editor to say we accept
            String nonce = frame.get("nonce").getAsString();
            send(new JObject()
                    .add("type", MESSAGE_TYPE_HELLO_REPLY)
                    .add("nonce", nonce)
                    .add("accepted", true)
                    .toJson()
            );

            Message.EDITOR_SOCKET_CONNECTED.send(this.sender);

        } else if (this.state == State.WAITING_FOR_CHANGES) {
            String type = frame.get("type").getAsString();
            if (MESSAGE_FRAME_HELLO.equals(type)) {
                // could happen if duplicate editor windows are opened
                // send a reply back to the editor to say we don't accept
                String nonce = frame.get("nonce").getAsString();
                send(new JObject()
                        .add("type", "hello-reply")
                        .add("nonce", nonce)
                        .add("accepted", false)
                        .toJson()
                );
                return;
            }

            if (!MESSAGE_FRAME_MSG.equals(type)) {
                throw new IllegalStateException("Unexpected message type: " + type);
            }

            String innerMsg = frame.get("msg").getAsString();
            String signature = frame.get("signature").getAsString();

            if (innerMsg == null || innerMsg.isEmpty() || signature == null || signature.isEmpty()) {
                throw new IllegalArgumentException("Incomplete message");
            }

            // check signature to ensure the message is from the connected editor
            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initVerify(this.remotePublicKey);
            sign.update(innerMsg.getBytes(StandardCharsets.UTF_8));

            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            if (!sign.verify(signatureBytes)) {
                throw new IllegalStateException("Could not verify message using signature");
            }

            // parse the inner message
            JsonObject parsed = GsonProvider.parser().parse(innerMsg).getAsJsonObject();
            handleMessage(parsed);
        } else {
            throw new AssertionError(this.state);
        }
    }

    private void handleMessage(JsonObject msg) throws Exception {
        String type = msg.get("type").getAsString();
        if (!MESSAGE_TYPE_APPLY_CHANGE.equals(type)) {
            throw new IllegalStateException("Invalid message type: " + type);
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

    /**
     * Checks if incoming messages should be ignored.
     *
     * @return true if messages should be ignored
     */
    private boolean shouldIgnoreMessages() {
        if (this.closed) {
            return true;
        }

        if (!this.sender.isValid() || (System.currentTimeMillis() - this.creationTime) > TimeUnit.MINUTES.toMillis(20)) {
            this.socket.socket().close(1000, "Normal");
            this.closed = true;
            return true;
        }

        return false;
    }

    /**
     * If the editor hasn't made an initial connection after 30 seconds,
     * then close + stop listening to the socket.
     */
    public void scheduleCleanupIfUnused() {
        this.plugin.getBootstrap().getScheduler().asyncLater(() -> {
            if (!this.closed && this.state == State.WAITING_FOR_EDITOR_TO_CONNECT) {
                this.socket.socket().close(1000, "Normal");
                this.closed = true;
            }
        }, 30, TimeUnit.SECONDS);
    }

    /**
     * Gets the auth secret.
     *
     * @return the auth secret
     */
    public String getAuthSecret() {
        return this.authSecret;
    }

    @Override
    public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
        this.connectFuture.complete(null);
    }

    @Override
    public void onMessage(@NonNull WebSocket webSocket, @NonNull String msg) {
        this.plugin.getBootstrap().getScheduler().executeAsync(() -> {
            try {
                this.lock.lock();

                if (shouldIgnoreMessages()) {
                    return;
                }

                try {
                    handleMessageFrame(msg);
                } finally {
                    this.lock.unlock();
                }
            } catch (Exception e) {
                this.plugin.getLogger().warn("Exception occurred handling message from socket", e);
            }
        });
    }

    private enum State {
        WAITING_FOR_EDITOR_TO_CONNECT,
        WAITING_FOR_CHANGES
    }

    private static String generateAuthSecret() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < AUTH_SECRET_LENGTH; i++) {
            sb.append(AUTH_SECRET_CHARACTERS.charAt(RANDOM.nextInt(AUTH_SECRET_CHARACTERS.length())));
        }
        return sb.toString();
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
