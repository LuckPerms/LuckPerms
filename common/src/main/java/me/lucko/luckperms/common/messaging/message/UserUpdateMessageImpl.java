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

package me.lucko.luckperms.common.messaging.message;

import me.lucko.luckperms.api.messenger.message.type.UserUpdateMessage;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

import javax.annotation.Nonnull;

public class UserUpdateMessageImpl extends AbstractMessage implements UserUpdateMessage {
    private static final String USER_UPDATE_HEADER = "userupdate:";

    public static UserUpdateMessageImpl decode(String msg) {
        if (msg.startsWith(USER_UPDATE_HEADER) && msg.length() > USER_UPDATE_HEADER.length()) {
            String content = msg.substring(USER_UPDATE_HEADER.length());
            return decodeContent(content);
        }

        return null;
    }

    private final UUID userUuid;

    public UserUpdateMessageImpl(UUID id, UUID userUuid) {
        super(id);
        this.userUuid = userUuid;
    }

    @Nonnull
    @Override
    public UUID getUser() {
        return this.userUuid;
    }

    @Nonnull
    @Override
    public String asEncodedString() {
        return USER_UPDATE_HEADER + encodeContent(getId(), this.userUuid);
    }

    private static String encodeContent(UUID id, UUID userUuid) {
        ByteBuffer buf = ByteBuffer.allocate(Long.BYTES * 4);
        buf.putLong(id.getMostSignificantBits());
        buf.putLong(id.getLeastSignificantBits());
        buf.putLong(userUuid.getMostSignificantBits());
        buf.putLong(userUuid.getLeastSignificantBits());
        return Base64.getEncoder().encodeToString(buf.array());
    }

    private static UserUpdateMessageImpl decodeContent(String s) {
        try {
            byte[] bytes = Base64.getDecoder().decode(s);
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            UUID id = new UUID(buf.getLong(), buf.getLong());
            UUID userUuid = new UUID(buf.getLong(), buf.getLong());
            return new UserUpdateMessageImpl(id, userUuid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
