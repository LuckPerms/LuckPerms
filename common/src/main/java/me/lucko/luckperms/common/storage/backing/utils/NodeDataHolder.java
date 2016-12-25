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

import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import com.google.gson.Gson;

import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.core.NodeBuilder;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

@Getter
@ToString
@AllArgsConstructor(staticName = "of")
public class NodeDataHolder {
    public static NodeDataHolder fromNode(Node node) {
        long expiry = node.isTemporary() ? node.getExpiryUnixTime() : 0L;
        return NodeDataHolder.of(node.getPermission(), node.getValue(),
                node.getServer().orElse(null), node.getWorld().orElse(null),
                expiry, new Gson().toJson(node.getContexts().toMap()));
    }

    private final String permission;
    private final boolean value;
    private final String server;
    private final String world;
    private final long expiry;
    private final String contexts;

    public Node toNode() {
        Type type = new TypeToken<Map<String, String>>(){}.getType();
        Map<String, String> parsedContexts = new Gson().fromJson(contexts, type);
        if (parsedContexts == null) {
            parsedContexts = new HashMap<>();
        }

        NodeBuilder builder = new NodeBuilder(permission);
        builder.setValue(value);
        builder.setServer(server);
        builder.setWorld(world);
        builder.setExpiry(expiry);
        builder.withExtraContext(parsedContexts);
        return builder.build();
    }

}
