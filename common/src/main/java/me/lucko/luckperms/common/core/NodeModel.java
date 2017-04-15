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

package me.lucko.luckperms.common.core;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.api.context.MutableContextSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * An stripped down version of {@link Node}, without methods and cached values for handling permission lookups.
 *
 * All values are non-null.
 */
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor(staticName = "of")
public final class NodeModel {
    private static final Gson GSON = new Gson();

    public static NodeModel fromNode(Node node) {
        return NodeModel.of(
                node.getPermission(),
                node.getValue(),
                node.getServer().orElse("global"),
                node.getWorld().orElse("global"),
                node.isTemporary() ? node.getExpiryUnixTime() : 0L,
                node.getContexts().makeImmutable()
        );
    }

    public static NodeModel deserialize(String permission, boolean value, String server, String world, long expiry, String contexts) {
        JsonObject context = GSON.fromJson(contexts, JsonObject.class);
        return of(permission, value, server, world, expiry, deserializeContextSet(context).makeImmutable());
    }

    private final String permission;
    private final boolean value;
    private final String server;
    private final String world;
    private final long expiry;
    private final ImmutableContextSet contexts;

    public String serializeContext() {
        return GSON.toJson(getContextsAsJson());
    }

    public JsonObject getContextsAsJson() {
        return serializeContextSet(contexts);
    }

    public Node toNode() {
        Node.Builder builder = NodeFactory.newBuilder(permission);
        builder.setValue(value);
        builder.setServer(server);
        builder.setWorld(world);
        builder.setExpiry(expiry);
        builder.withExtraContext(contexts);
        return builder.build();
    }

    public NodeModel setPermission(String permission) {
        return of(permission, value, server, world, expiry, contexts);
    }

    public NodeModel setValue(boolean value) {
        return of(permission, value, server, world, expiry, contexts);
    }

    public NodeModel setServer(String server) {
        return of(permission, value, server, world, expiry, contexts);
    }

    public NodeModel setWorld(String world) {
        return of(permission, value, server, world, expiry, contexts);
    }

    public NodeModel setExpiry(long expiry) {
        return of(permission, value, server, world, expiry, contexts);
    }

    public NodeModel setContexts(ImmutableContextSet contexts) {
        return of(permission, value, server, world, expiry, contexts);
    }

    public static JsonObject serializeContextSet(ContextSet contextSet) {
        JsonObject data = new JsonObject();
        Map<String, Collection<String>> map = contextSet.toMultimap().asMap();

        map.forEach((k, v) -> {
            List<String> values = new ArrayList<>(v);
            int size = values.size();

            if (size == 1) {
                data.addProperty(k, values.get(0));
            } else if (size > 1) {
                JsonArray arr = new JsonArray();
                for (String s : values) {
                    arr.add(new JsonPrimitive(s));
                }
                data.add(k, arr);
            }
        });

        return data;
    }

    public static MutableContextSet deserializeContextSet(JsonElement element) {
        Preconditions.checkArgument(element.isJsonObject());
        JsonObject data = element.getAsJsonObject();

        ImmutableSetMultimap.Builder<String, String> map = ImmutableSetMultimap.builder();
        for (Map.Entry<String, JsonElement> e : data.entrySet()) {
            String k = e.getKey();
            JsonElement v = e.getValue();
            if (v.isJsonArray()) {
                JsonArray values = v.getAsJsonArray();
                for (JsonElement value : values) {
                    map.put(k, value.getAsString());
                }
            } else {
                map.put(k, v.getAsString());
            }
        }

        return MutableContextSet.fromMultimap(map.build());
    }

}
