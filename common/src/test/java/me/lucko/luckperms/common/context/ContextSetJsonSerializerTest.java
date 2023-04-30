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

package me.lucko.luckperms.common.context;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import me.lucko.luckperms.common.context.serializer.ContextSetJsonSerializer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ContextSetJsonSerializerTest {

    private static final Gson GSON = new Gson();

    private static final ImmutableContextSet EXAMPLE_1 = new ImmutableContextSetImpl.BuilderImpl()
            .add("server", "foo")
            .add("world", "foo")
            .add("foo", "foo")
            .add("foo", "bar")
            .build();

    private static final ImmutableContextSet EXAMPLE_2 = new ImmutableContextSetImpl.BuilderImpl()
            .add("cc", "foo")
            .add("bb", "foo")
            .add("aa", "foo")
            .build();

    @Test
    public void testDeserialize() {
        String string = "{\"foo\":[\"bar\",\"foo\"],\"server\":\"foo\",\"world\":[\"foo\"]}";
        ContextSet set = ContextSetJsonSerializer.deserialize(GSON, string);
        assertEquals(EXAMPLE_1, set);
    }

    @Test
    public void testSerialize() {
        JsonObject obj1 = ContextSetJsonSerializer.serialize(EXAMPLE_1);
        assertEquals(3, obj1.size());
        assertEquals("{\"foo\":[\"bar\",\"foo\"],\"server\":\"foo\",\"world\":\"foo\"}", obj1.toString());

        JsonObject obj2 = ContextSetJsonSerializer.serialize(EXAMPLE_2);
        assertEquals(3, obj2.size());
        assertEquals("{\"aa\":\"foo\",\"bb\":\"foo\",\"cc\":\"foo\"}", obj2.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{ }",
            ""
    })
    public void testDeserializeEmpty(String json) {
        assertEquals(ImmutableContextSetImpl.EMPTY, ContextSetJsonSerializer.deserialize(GSON, json));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "null",
            "[]",
            "foo"
    })
    public void testDeserializeThrows(String json) {
        assertThrows(JsonParseException.class, () -> ContextSetJsonSerializer.deserialize(GSON, json));
    }

}
