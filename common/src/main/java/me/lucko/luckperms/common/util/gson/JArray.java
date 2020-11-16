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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class JArray implements JElement {
    private final JsonArray array = new JsonArray();

    @Override
    public JsonArray toJson() {
        return this.array;
    }

    public JArray add(JsonElement value) {
        if (value == null) {
            return add(JsonNull.INSTANCE);
        }
        this.array.add(value);
        return this;
    }

    public JArray add(String value) {
        if (value == null) {
            return add(JsonNull.INSTANCE);
        }
        this.array.add(new JsonPrimitive(value));
        return this;
    }

    public JArray addAll(Iterable<String> iterable) {
        for (String s : iterable) {
            add(s);
        }
        return this;
    }

    public JArray add(JElement value) {
        if (value == null) {
            return add(JsonNull.INSTANCE);
        }
        return add(value.toJson());
    }

    public JArray add(Supplier<? extends JElement> value) {
        return add(value.get().toJson());
    }

    public JArray consume(Consumer<? super JArray> consumer) {
        consumer.accept(this);
        return this;
    }
}