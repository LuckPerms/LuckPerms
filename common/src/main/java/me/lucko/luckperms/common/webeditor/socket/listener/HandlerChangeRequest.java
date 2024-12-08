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
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.http.UnsuccessfulRequestException;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.webeditor.WebEditorResponse;
import me.lucko.luckperms.common.webeditor.socket.SocketMessageType;
import me.lucko.luckperms.common.webeditor.socket.WebEditorSocket;

import java.io.IOException;
import java.util.Objects;

/**
 * Handler for {@link SocketMessageType#CHANGE_REQUEST}
 */
public class HandlerChangeRequest implements Handler {

    /** The change has been accepted, and the plugin will now apply it. */
    private static final String STATE_ACCEPTED = "accepted";
    /** The change has been applied. */
    private static final String STATE_APPLIED = "applied";

    /** The socket */
    private final WebEditorSocket socket;

    public HandlerChangeRequest(WebEditorSocket socket) {
        this.socket = socket;
    }

    @Override
    public void handle(JsonObject msg) {
        if (!this.socket.getSender().hasPermission(CommandPermission.APPLY_EDITS)) {
            throw new IllegalStateException("Sender does not have applyedits permission");
        }

        // get the bytebin code containing the editor data
        String code = msg.get("code").getAsString();
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Invalid code");
        }

        // send "change-accepted" response
        this.socket.getPlugin().getBootstrap().getScheduler().async(() ->
                this.socket.send(SocketMessageType.CHANGE_RESPONSE.builder()
                        .add("state", STATE_ACCEPTED)
                        .toJson()
                )
        );

        // download data from bytebin
        JsonObject data;
        try {
            data = this.socket.getPlugin().getBytebin().getJsonContent(code).getAsJsonObject();
            Objects.requireNonNull(data);
        } catch (UnsuccessfulRequestException | IOException e) {
            throw new RuntimeException("Error reading data", e);
        }

        // apply changes
        Message.EDITOR_SOCKET_CHANGES_RECEIVED.send(this.socket.getSender());
        new WebEditorResponse(code, data).apply(
                this.socket.getPlugin(),
                this.socket.getSender(),
                this.socket.getSession(),
                "lp",
                false
        );

        // create a new session
        String newSessionCode = this.socket.getSession().createFollowUpSession();
        this.socket.send(SocketMessageType.CHANGE_RESPONSE.builder()
                .add("state", STATE_APPLIED)
                .add("newSessionCode", newSessionCode)
                .toJson()
        );
    }

}
