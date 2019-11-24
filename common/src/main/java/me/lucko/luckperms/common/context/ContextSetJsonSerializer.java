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

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import me.lucko.luckperms.common.context.contextset.ImmutableContextSetImpl;
import me.lucko.luckperms.common.context.contextset.MutableContextSetImpl;

import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.MutableContextSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ContextSetJsonSerializer {
    private ContextSetJsonSerializer() {}

    public static JsonObject serializeContextSet(ContextSet contextSet) {
        JsonObject data = new JsonObject();
        Map<String, Set<String>> map = contextSet.toMap();

        for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
            List<String> values = new ArrayList<>(entry.getValue());
            int size = values.size();

            if (size == 1) {
                data.addProperty(entry.getKey(), values.get(0));
            } else if (size > 1) {
                JsonArray arr = new JsonArray();
                for (String s : values) {
                    arr.add(new JsonPrimitive(s));
                }
                data.add(entry.getKey(), arr);
            }
        }

        return data;
    }

    public static ContextSet deserializeContextSet(Gson gson, String json) {
        Objects.requireNonNull(json, "json");
        if (json.equals("{}")) {
            return ImmutableContextSetImpl.EMPTY;
        }

        JsonObject context = gson.fromJson(json, JsonObject.class);
        if (context == null) {
            return ImmutableContextSetImpl.EMPTY;
        }

        return deserializeContextSet(context);
    }

    public static ContextSet deserializeContextSet(JsonElement element) {
        Preconditions.checkArgument(element.isJsonObject());
        JsonObject data = element.getAsJsonObject();

        if (data.entrySet().isEmpty()) {
            return ImmutableContextSetImpl.EMPTY;
        }

        MutableContextSet map = new MutableContextSetImpl();
        for (Map.Entry<String, JsonElement> e : data.entrySet()) {
            String k = e.getKey();
            JsonElement v = e.getValue();
            if (v.isJsonArray()) {
                JsonArray values = v.getAsJsonArray();
                for (JsonElement value : values) {
                    map.add(k, value.getAsString());
                }
            } else {
                map.add(k, v.getAsString());
            }
        }

        return map;
    }

}
