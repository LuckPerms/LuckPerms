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

package me.lucko.luckperms.common.messaging;

import com.google.gson.JsonObject;
import me.lucko.luckperms.common.actionlog.ActionJsonSerializer;
import me.lucko.luckperms.common.actionlog.LoggedAction;
import me.lucko.luckperms.common.messaging.message.ActionLogMessageImpl;
import me.lucko.luckperms.common.messaging.message.CustomMessageImpl;
import me.lucko.luckperms.common.messaging.message.UpdateMessageImpl;
import me.lucko.luckperms.common.messaging.message.UserUpdateMessageImpl;
import me.lucko.luckperms.common.util.gson.JObject;
import net.luckperms.api.actionlog.Action;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MessageImplTest {

    @Test
    public void testUpdateMessage() {
        UUID uuid = UUID.fromString("22f9e168-8815-44f1-83c8-b642ebfbcef2");

        // encode
        UpdateMessageImpl msg = new UpdateMessageImpl(uuid);
        assertEquals("{\"id\":\"22f9e168-8815-44f1-83c8-b642ebfbcef2\",\"type\":\"update\"}", msg.asEncodedString());

        // decode
        msg = UpdateMessageImpl.decode(new JsonObject(), uuid);
        assertEquals(uuid, msg.getId());
    }

    @Test
    public void testUserUpdateMessage() {
        UUID uuid = UUID.fromString("22f9e168-8815-44f1-83c8-b642ebfbcef2");
        UUID userUuid = UUID.fromString("4c087cd9-f444-4c52-8438-e03e57ba2e8e");

        // encode
        UserUpdateMessageImpl msg = new UserUpdateMessageImpl(uuid, userUuid);
        // {"id":"22f9e168-8815-44f1-83c8-b642ebfbcef2","type":"userupdate","content":{"userUuid":"4c087cd9-f444-4c52-8438-e03e57ba2e8e"}}
        assertEquals("{\"id\":\"22f9e168-8815-44f1-83c8-b642ebfbcef2\",\"type\":\"userupdate\",\"content\":{\"userUuid\":\"4c087cd9-f444-4c52-8438-e03e57ba2e8e\"}}", msg.asEncodedString());

        // decode
        msg = UserUpdateMessageImpl.decode(new JObject().add("userUuid", userUuid.toString()).toJson(), uuid);
        assertEquals(uuid, msg.getId());
        assertEquals(userUuid, msg.getUserUniqueId());
    }

    @Test
    public void testActionLogMessage() {
        UUID uuid = UUID.fromString("22f9e168-8815-44f1-83c8-b642ebfbcef2");
        LoggedAction action = LoggedAction.build()
                .source(UUID.fromString("d3500320-564c-436e-87a0-026d7f2c92f6"))
                .sourceName("Test")
                .targetType(Action.Target.Type.GROUP)
                .targetName("test")
                .description("test")
                .timestamp(Instant.ofEpochSecond(1))
                .build();

        // encode
        ActionLogMessageImpl msg = new ActionLogMessageImpl(uuid, action);
        // {"id":"22f9e168-8815-44f1-83c8-b642ebfbcef2","type":"log","content":{"timestamp":1,"source":{"uniqueId":"d3500320-564c-436e-87a0-026d7f2c92f6","name":"Test"},"target":{"type":"GROUP","name":"test"},"description":"test"}}
        assertEquals("{\"id\":\"22f9e168-8815-44f1-83c8-b642ebfbcef2\",\"type\":\"log\",\"content\":{\"timestamp\":1,\"source\":{\"uniqueId\":\"d3500320-564c-436e-87a0-026d7f2c92f6\",\"name\":\"Test\"},\"target\":{\"type\":\"GROUP\",\"name\":\"test\"},\"description\":\"test\"}}", msg.asEncodedString());

        // decode
        msg = ActionLogMessageImpl.decode(ActionJsonSerializer.serialize(action), uuid);
        assertEquals(uuid, msg.getId());
        assertEquals(action, msg.getAction());
    }

    @Test
    public void testCustomMessage() {
        UUID uuid = UUID.fromString("22f9e168-8815-44f1-83c8-b642ebfbcef2");
        String channelId = "test";
        String payload = "test";

        // encode
        CustomMessageImpl msg = new CustomMessageImpl(uuid, channelId, payload);
        // {"id":"22f9e168-8815-44f1-83c8-b642ebfbcef2","type":"custom","content":{"channelId":"test","payload":"test"}}
        assertEquals("{\"id\":\"22f9e168-8815-44f1-83c8-b642ebfbcef2\",\"type\":\"custom\",\"content\":{\"channelId\":\"test\",\"payload\":\"test\"}}", msg.asEncodedString());

        // decode
        msg = CustomMessageImpl.decode(new JObject().add("channelId", channelId).add("payload", payload).toJson(), uuid);
        assertEquals(uuid, msg.getId());
        assertEquals(channelId, msg.getChannelId());
        assertEquals(payload, msg.getPayload());
    }

}
