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

package me.lucko.luckperms.common.actionlog;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.luckperms.api.actionlog.Action;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ActionJsonSerializerTest {

    private static final UUID SOURCE = UUID.fromString("725d585e-4ff1-4f18-acca-6ac538364080");
    private static final UUID TARGET = UUID.fromString("c1d60c50-70b5-4722-8057-87767557e50d");

    private static JsonElement parse(String json) {
        return new JsonParser().parse(json);
    }

    @Test
    public void testRoundTrip() {
        LoggedAction userAction = LoggedAction.build()
                .timestamp(Instant.ofEpochSecond(1575000000L))
                .source(SOURCE)
                .sourceName("Luck")
                .targetType(Action.Target.Type.USER)
                .target(TARGET)
                .targetName("Notch")
                .description("permission set test true")
                .build();
        assertEquals(userAction, ActionJsonSerializer.deserialize(ActionJsonSerializer.serialize(userAction)));

        LoggedAction groupAction = LoggedAction.build()
                .timestamp(Instant.ofEpochSecond(1575000000L))
                .source(SOURCE)
                .sourceName("Luck")
                .targetType(Action.Target.Type.GROUP)
                .targetName("admin")
                .description("permission set test true")
                .build();
        assertEquals(groupAction, ActionJsonSerializer.deserialize(ActionJsonSerializer.serialize(groupAction)));
    }

    // Between the 5.0 serialization changes and the fix for #1843, the target's
    // uniqueId and name were written next to the "target" object instead of inside it.
    @Test
    public void testTargetFieldsOutsideTargetObject() {
        LoggedAction userAction = ActionJsonSerializer.deserialize(parse("{" +
                "\"timestamp\":1575000000," +
                "\"source\":{\"uniqueId\":\"" + SOURCE + "\",\"name\":\"Luck\"}," +
                "\"target\":{\"type\":\"USER\"}," +
                "\"uniqueId\":\"" + TARGET + "\"," +
                "\"name\":\"Notch\"," +
                "\"description\":\"permission set test true\"" +
                "}"));
        assertEquals(Instant.ofEpochSecond(1575000000L), userAction.getTimestamp());
        assertEquals(SOURCE, userAction.getSource().getUniqueId());
        assertEquals("Luck", userAction.getSource().getName());
        assertEquals(Action.Target.Type.USER, userAction.getTarget().getType());
        assertEquals(Optional.of(TARGET), userAction.getTarget().getUniqueId());
        assertEquals("Notch", userAction.getTarget().getName());
        assertEquals("permission set test true", userAction.getDescription());

        LoggedAction groupAction = ActionJsonSerializer.deserialize(parse("{" +
                "\"timestamp\":1575000000," +
                "\"source\":{\"uniqueId\":\"" + SOURCE + "\",\"name\":\"Luck\"}," +
                "\"target\":{\"type\":\"GROUP\"}," +
                "\"name\":\"admin\"," +
                "\"description\":\"permission set test true\"" +
                "}"));
        assertEquals(Action.Target.Type.GROUP, groupAction.getTarget().getType());
        assertEquals(Optional.empty(), groupAction.getTarget().getUniqueId());
        assertEquals("admin", groupAction.getTarget().getName());
    }

}
