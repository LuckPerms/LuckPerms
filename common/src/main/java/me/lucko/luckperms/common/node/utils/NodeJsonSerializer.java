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

package me.lucko.luckperms.common.node.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import me.lucko.luckperms.common.context.ContextSetJsonSerializer;
import me.lucko.luckperms.common.node.factory.NodeBuilders;

import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeBuilder;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class NodeJsonSerializer {

    private NodeJsonSerializer() {

    }

    public static JsonArray serializeNodes(Collection<Node> nodes) {
        JsonArray arr = new JsonArray();
        for (Node node : nodes) {
            JsonObject attributes = new JsonObject();

            attributes.addProperty("type", node.getType().name().toLowerCase());
            attributes.addProperty("key", node.getKey());
            attributes.addProperty("value", node.getValue());

            Instant expiry = node.getExpiry();
            if (expiry != null) {
                attributes.addProperty("expiry", expiry.getEpochSecond());
            }

            if (!node.getContexts().isEmpty()) {
                attributes.add("context", ContextSetJsonSerializer.serializeContextSet(node.getContexts()));
            }

            arr.add(attributes);
        }
        return arr;
    }

    public static Set<Node> deserializeNodes(JsonArray arr) {
        Set<Node> nodes = new HashSet<>();
        for (JsonElement ent : arr) {
            JsonObject attributes = ent.getAsJsonObject();

            String key = attributes.get("key").getAsString();
            boolean value = attributes.get("value").getAsBoolean();

            NodeBuilder<?, ?> builder = NodeBuilders.determineMostApplicable(key).value(value);

            if (attributes.has("expiry")) {
                builder.expiry(attributes.get("expiry").getAsLong());
            }

            if (attributes.has("context")) {
                builder.context(ContextSetJsonSerializer.deserializeContextSet(attributes.get("context")));
            }

            nodes.add(builder.build());
        }
        return nodes;
    }
}
