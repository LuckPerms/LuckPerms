/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

package me.lucko.luckperms.common.storage.backing.utils;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.core.NodeFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor(staticName = "of")
public class NodeDataHolder {
    private static final Gson GSON = new Gson();

    public static NodeDataHolder fromNode(Node node) {
        return NodeDataHolder.of(
                node.getPermission(),
                node.getValue(),
                node.getServer().orElse("global"),
                node.getWorld().orElse("global"),
                node.isTemporary() ? node.getExpiryUnixTime() : 0L,
                ImmutableSetMultimap.copyOf(node.getContexts().toMultimap())
        );
    }

    public static NodeDataHolder of(String permission, boolean value, String server, String world, long expiry, String contexts) {
        JsonObject context = GSON.fromJson(contexts, JsonObject.class);
        ImmutableSetMultimap.Builder<String, String> map = ImmutableSetMultimap.builder();
        for (Map.Entry<String, JsonElement> e : context.entrySet()) {
            JsonElement val = e.getValue();
            if (val.isJsonArray()) {
                JsonArray vals = val.getAsJsonArray();
                for (JsonElement element : vals) {
                    map.put(e.getKey(), element.getAsString());
                }
            } else {
                map.put(e.getKey(), val.getAsString());
            }
        }
        return new NodeDataHolder(permission, value, server, world, expiry, map.build());
    }

    private final String permission;
    private final boolean value;
    private final String server;
    private final String world;
    private final long expiry;
    private final ImmutableSetMultimap<String, String> contexts;

    public String serialiseContext() {
        JsonObject context = new JsonObject();
        ImmutableMap<String, Collection<String>> map = getContexts().asMap();

        map.forEach((key, value) -> {
            List<String> vals = new ArrayList<>(value);
            int size = vals.size();

            if (size == 1) {
                context.addProperty(key, vals.get(0));
            } else if (size > 1) {
                JsonArray arr = new JsonArray();
                for (String s : vals) {
                    arr.add(new JsonPrimitive(s));
                }
                context.add(key, arr);
            }
        });

        return GSON.toJson(context);
    }

    public Node toNode() {
        Node.Builder builder = NodeFactory.newBuilder(permission);
        builder.setValue(value);
        builder.setServer(server);
        builder.setWorld(world);
        builder.setExpiry(expiry);

        for (Map.Entry<String, String> e : contexts.entries()) {
            builder.withExtraContext(e);
        }

        return builder.build();
    }

}
