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
import me.lucko.luckperms.common.webeditor.socket.SocketMessageType;
import me.lucko.luckperms.common.webeditor.socket.WebEditorSocket;

/**
 * Handler for {@link SocketMessageType#PING}
 */
public class HandlerPing implements Handler {

    /** The socket */
    private final WebEditorSocket socket;

    /** The time a ping was last received */
    private long lastPing = 0;

    public HandlerPing(WebEditorSocket socket) {
        this.socket = socket;
    }

    public long getLastPing() {
        return this.lastPing;
    }

    @Override
    public void handle(JsonObject msg) {
        this.lastPing = System.currentTimeMillis();
        this.socket.send(SocketMessageType.PONG.builder()
                .add("ok", true)
                .toJson()
        );
    }
}
