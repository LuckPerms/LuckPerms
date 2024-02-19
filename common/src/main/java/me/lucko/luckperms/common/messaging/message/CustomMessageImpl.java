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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.lucko.luckperms.common.messaging.LuckPermsMessagingService;
import me.lucko.luckperms.common.util.gson.JObject;
import net.luckperms.api.messenger.message.type.CustomMessage;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.UUID;

public class CustomMessageImpl extends AbstractMessage implements CustomMessage {
    public static final String TYPE = "custom";

    public static CustomMessageImpl decode(@Nullable JsonElement content, UUID id) {
        if (content == null) {
            throw new IllegalStateException("Missing content");
        }

        JsonObject obj = content.getAsJsonObject();
        if (!obj.has("channelId")) {
            throw new IllegalStateException("Incoming message has no 'channelId' argument: " + content);
        }
        if (!obj.has("payload")) {
            throw new IllegalStateException("Incoming message has no 'payload' argument: " + content);
        }

        String channelId = obj.get("channelId").getAsString();
        String payload = obj.get("payload").getAsString();

        return new CustomMessageImpl(id, channelId, payload);
    }

    private final String channelId;
    private final String payload;

    public CustomMessageImpl(UUID id, String channelId, String payload) {
        super(id);
        this.channelId = channelId;
        this.payload = payload;
    }

    @Override
    public @NonNull String getChannelId() {
        return this.channelId;
    }

    @Override
    public @NonNull String getPayload() {
        return this.payload;
    }

    @Override
    public @NonNull String asEncodedString() {
        return LuckPermsMessagingService.encodeMessageAsString(
                TYPE, getId(), new JObject().add("channelId", this.channelId).add("payload", this.payload).toJson()
        );
    }
}
