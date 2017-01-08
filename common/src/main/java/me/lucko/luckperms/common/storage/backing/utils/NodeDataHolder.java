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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.core.NodeBuilder;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor(staticName = "of")
public class NodeDataHolder {
    private static final Gson GSON = new Gson();
    private static final Type CONTEXT_TYPE = new TypeToken<Map<String, Collection<String>>>(){}.getType();

    public static NodeDataHolder fromNode(Node node) {
        return NodeDataHolder.of(
                node.getPermission(),
                node.getValue(),
                node.getServer().orElse("global"),
                node.getWorld().orElse("global"),
                node.isTemporary() ? node.getExpiryUnixTime() : 0L,
                GSON.toJson(node.getContexts().toMultimap().asMap())
        );
    }

    private final String permission;
    private final boolean value;
    private final String server;
    private final String world;
    private final long expiry;
    private final String contexts;

    public Node toNode() {
        NodeBuilder builder = new NodeBuilder(permission);
        builder.setValue(value);
        builder.setServer(server);
        builder.setWorld(world);
        builder.setExpiry(expiry);

        try {
            Map<String, Collection<String>> deserializedContexts = GSON.fromJson(contexts, CONTEXT_TYPE);
            if (deserializedContexts != null && !deserializedContexts.isEmpty()) {
                for (Map.Entry<String, Collection<String>> c : deserializedContexts.entrySet()) {
                    for (String val : c.getValue()) {
                        builder.withExtraContext(c.getKey(), val);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return builder.build();
    }

}
