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
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.webeditor.socket.SignatureAlgorithm;
import me.lucko.luckperms.common.webeditor.socket.SocketMessageType;
import me.lucko.luckperms.common.webeditor.socket.WebEditorSocket;
import me.lucko.luckperms.common.webeditor.store.RemoteSession;

import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler for {@link SocketMessageType#HELLO}
 */
public class HandlerHello implements Handler {

    /** The session is accepted, the editor public key is already known so no further action is needed */
    private static final String STATE_ACCEPTED = "accepted";
    /** The session is accepted, but the user needs to confirm in-game before any changes will be accepted. */
    private static final String STATE_UNTRUSTED = "untrusted";
    /** A session has already been established with a different identity */
    private static final String STATE_REJECTED = "rejected";
    /** The remote editor session is based off session data which has already been applied */
    private static final String STATE_INVALID = "invalid";

    /** The socket */
    private final WebEditorSocket socket;

    /** A list of attempted connections (connections that have been attempted with an untrusted public key) */
    private final Map<String, PublicKey> attemptedConnections = new HashMap<>();

    public HandlerHello(WebEditorSocket socket) {
        this.socket = socket;
    }

    public PublicKey getAttemptedConnection(String nonce) {
        return this.attemptedConnections.get(nonce);
    }

    public boolean hasAttemptedConnection() {
        return !this.attemptedConnections.isEmpty();
    }

    @Override
    public void handle(JsonObject msg) {
        String nonce = getStringOrThrow(msg, "nonce");
        String sessionId = getStringOrThrow(msg, "sessionId");
        String browser = msg.get("browser").getAsString();
        PublicKey remotePublicKey = SignatureAlgorithm.INSTANCE.parsePublicKey(msg.get("publicKey").getAsString());

        // check if the public keys are the same
        // (this allows the same editor to re-connect, but prevents new connections)
        if (this.socket.getRemotePublicKey() != null && !this.socket.getRemotePublicKey().equals(remotePublicKey)) {
            sendReply(nonce, STATE_REJECTED);
            return;
        }

        // check if session is valid
        RemoteSession session = this.socket.getPlugin().getWebEditorStore().sessions().getSession(sessionId);
        if (session == null || session.isCompleted()) {
            sendReply(nonce, STATE_INVALID);
            return;
        }

        // check if the public key is trusted
        if (!this.socket.getPlugin().getWebEditorStore().keystore().isTrusted(this.socket.getSender(), remotePublicKey.getEncoded())) {
            sendReply(nonce, STATE_UNTRUSTED);

            // ask the user if they want to trust the connection
            Message.EDITOR_SOCKET_UNTRUSTED.send(this.socket.getSender(), nonce, browser, this.socket.getSession().getCommandLabel(), this.socket.getSender().isConsole());
            this.attemptedConnections.put(nonce, remotePublicKey);
            return;
        }

        boolean reconnected = this.socket.getRemotePublicKey() != null;
        this.socket.setRemotePublicKey(remotePublicKey);

        sendReply(nonce, STATE_ACCEPTED);

        if (reconnected) {
            Message.EDITOR_SOCKET_RECONNECTED.send(this.socket.getSender());
        } else {
            Message.EDITOR_SOCKET_CONNECTED.send(this.socket.getSender());
        }
    }

    private void sendReply(String nonce, String state) {
        this.socket.send(SocketMessageType.HELLO_REPLY.builder()
                .add("nonce", nonce)
                .add("state", state)
                .toJson()
        );
    }

    private static String getStringOrThrow(JsonObject msg, String key) {
        String val = msg.get(key).getAsString();
        if (val == null || val.isEmpty()) {
            throw new IllegalStateException("missing " + key);
        }
        return val;
    }
}
