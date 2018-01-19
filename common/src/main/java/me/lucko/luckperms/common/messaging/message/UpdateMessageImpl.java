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

import me.lucko.luckperms.api.messenger.message.type.UpdateMessage;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

import javax.annotation.Nonnull;

public class UpdateMessageImpl extends AbstractMessage implements UpdateMessage {
    private static final String UPDATE_HEADER = "update:";

    public static UpdateMessageImpl decode(String msg) {
        if (msg.startsWith(UPDATE_HEADER) && msg.length() > UPDATE_HEADER.length()) {
            String content = msg.substring(UPDATE_HEADER.length());
            return decodeContent(content);
        }

        return null;
    }

    public UpdateMessageImpl(UUID id) {
        super(id);
    }

    @Nonnull
    @Override
    public String asEncodedString() {
        return UPDATE_HEADER + encodeContent(getId());
    }

    private static String encodeContent(UUID uuid) {
        ByteBuffer buf = ByteBuffer.allocate(Long.BYTES * 2);
        buf.putLong(uuid.getMostSignificantBits());
        buf.putLong(uuid.getLeastSignificantBits());
        return Base64.getEncoder().encodeToString(buf.array());
    }

    private static UpdateMessageImpl decodeContent(String s) {
        try {
            byte[] bytes = Base64.getDecoder().decode(s);
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            return new UpdateMessageImpl(new UUID(buf.getLong(), buf.getLong()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
