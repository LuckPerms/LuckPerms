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

package me.lucko.luckperms.common.util.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class JObject implements JElement {
    private final JsonObject object = new JsonObject();

    @Override
    public JsonObject toJson() {
        return this.object;
    }

    public JObject add(String key, JsonElement value) {
        this.object.add(key, value);
        return this;
    }

    public JObject add(String key, String value) {
        if (value == null) {
            return add(key, JsonNull.INSTANCE);
        }
        return add(key, new JsonPrimitive(value));
    }

    public JObject add(String key, Number value) {
        if (value == null) {
            return add(key, JsonNull.INSTANCE);
        }
        return add(key, new JsonPrimitive(value));
    }

    public JObject add(String key, Boolean value) {
        if (value == null) {
            return add(key, JsonNull.INSTANCE);
        }
        return add(key, new JsonPrimitive(value));
    }

    public JObject add(String key, JElement value) {
        if (value == null) {
            return add(key, JsonNull.INSTANCE);
        }
        return add(key, value.toJson());
    }

    public JObject add(String key, Supplier<? extends JElement> value) {
        return add(key, value.get().toJson());
    }

    public JObject consume(Consumer<? super JObject> consumer) {
        consumer.accept(this);
        return this;
    }
}
