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

import me.lucko.luckperms.common.util.ImmutableCollectors;
import me.lucko.luckperms.common.util.gson.JObject;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

public enum SocketMessageType {

    /** Sent when the editor first says "hello" over the channel. (editor -> plugin) */
    HELLO("hello"),

    /** Sent when the plugin replies to the editors "hello" message. (plugin -> editor) */
    HELLO_REPLY("hello-reply"),

    /** Sent by the editor to confirm that a connection has been established. (editor -> plugin) */
    CONNECTED("connected"),

    /** Sent by the editor to request that the plugin applies a change. (editor -> plugin) */
    CHANGE_REQUEST("change-request"),

    /** Sent by the plugin to confirm that the changes sent by the editor have been accepted or applied. (plugin -> editor) */
    CHANGE_RESPONSE("change-response"),

    /** Ping message to keep the socket alive. (editor -> plugin) */
    PING("ping"),

    /** Ping response. (plugin -> editor) */
    PONG("pong");

    public final String id;

    SocketMessageType(String id) {
        this.id = id;
    }

    public JObject builder() {
        return new JObject().add("type", this.id);
    }

    private static final Map<String, SocketMessageType> LOOKUP = Arrays.stream(SocketMessageType.values())
            .collect(ImmutableCollectors.toMap(m -> m.id, Function.identity()));

    public static SocketMessageType getById(String id) {
        SocketMessageType type = LOOKUP.get(id);
        if (type == null) {
            throw new IllegalArgumentException(id);
        }
        return type;
    }
}
