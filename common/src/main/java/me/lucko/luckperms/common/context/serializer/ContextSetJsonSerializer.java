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

package me.lucko.luckperms.common.context.serializer;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.lucko.luckperms.common.context.ImmutableContextSetImpl;
import me.lucko.luckperms.common.context.MutableContextSetImpl;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.MutableContextSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Serializes and deserializes {@link ContextSet}s to and from JSON.
 *
 * <p>The entries within the serialized output are sorted, this ensures that any two invocations
 * of {@link #serialize(ContextSet)} with the same {@link ContextSet} will produce
 * the same exact JSON string.</p>
 */
public final class ContextSetJsonSerializer {
    private ContextSetJsonSerializer() {}

    public static JsonObject serialize(ContextSet contextSet) {
        JsonObject output = new JsonObject();

        List<Map.Entry<String, Set<String>>> entries = new ArrayList<>(contextSet.toMap().entrySet());
        entries.sort(Map.Entry.comparingByKey()); // sort - consistent output order

        for (Map.Entry<String, Set<String>> entry : entries) {
            String[] values = entry.getValue().toArray(new String[0]);
            switch (values.length) {
                case 0:
                    break;
                case 1:
                    output.addProperty(entry.getKey(), values[0]);
                    break;
                default:
                    Arrays.sort(values); // sort - consistent output order
                    JsonArray arr = new JsonArray();
                    for (String value : values) {
                        arr.add(new JsonPrimitive(value));
                    }
                    output.add(entry.getKey(), arr);
                    break;
            }
        }

        return output;
    }

    public static ContextSet deserialize(Gson gson, String input) {
        Objects.requireNonNull(input, "input");
        if (input.equals("{}")) {
            return ImmutableContextSetImpl.EMPTY;
        }

        JsonObject jsonObject = gson.fromJson(input, JsonObject.class);
        if (jsonObject == null) {
            return ImmutableContextSetImpl.EMPTY;
        }

        return deserialize(jsonObject);
    }

    public static ContextSet deserialize(JsonElement element) {
        Preconditions.checkArgument(element.isJsonObject());
        JsonObject jsonObject = element.getAsJsonObject();

        Set<Map.Entry<String, JsonElement>> entries = jsonObject.entrySet();
        if (entries.isEmpty()) {
            return ImmutableContextSetImpl.EMPTY;
        }

        MutableContextSet contextSet = new MutableContextSetImpl();
        for (Map.Entry<String, JsonElement> entry : entries) {
            String k = entry.getKey();
            JsonElement v = entry.getValue();

            if (v.isJsonArray()) {
                JsonArray values = v.getAsJsonArray();
                for (JsonElement value : values) {
                    contextSet.add(k, value.getAsString());
                }
            } else {
                contextSet.add(k, v.getAsString());
            }
        }

        return contextSet;
    }

}
