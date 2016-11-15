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

package me.lucko.luckperms.common.core;

import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import lombok.experimental.UtilityClass;
import me.lucko.luckperms.api.MetaUtils;
import me.lucko.luckperms.api.Node;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Utility class to make Node(Builder) instances from serialised strings or existing Nodes
 */
@UtilityClass
public class NodeFactory {
    private static final LoadingCache<String, Node> CACHE = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, Node>() {
                @Override
                public Node load(String s) throws Exception {
                    return builderFromSerialisedNode(s, true).build();
                }
            });

    private static final LoadingCache<String, Node> CACHE_NEGATED = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, Node>() {
                @Override
                public Node load(String s) throws Exception {
                    return builderFromSerialisedNode(s, false).build();
                }
            });

    public static Node fromSerialisedNode(String s, Boolean b) {
        try {
            return b ? CACHE.get(s) : CACHE_NEGATED.get(s);
        } catch (UncheckedExecutionException | ExecutionException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Node.Builder newBuilder(String s) {
        return new NodeBuilder(s, false);
    }

    public static Node.Builder builderFromSerialisedNode(String s, Boolean b) {
        if (s.contains("/")) {
            List<String> parts = Splitter.on('/').limit(2).splitToList(s);
            // 0=server(+world)   1=node

            // WORLD SPECIFIC
            if (parts.get(0).contains("-")) {
                List<String> serverParts = Splitter.on('-').limit(2).splitToList(parts.get(0));
                // 0=server   1=world

                if (parts.get(1).contains("$")) {
                    List<String> tempParts = Splitter.on('$').limit(2).splitToList(parts.get(1));
                    return new NodeBuilder(tempParts.get(0), true).setServerRaw(serverParts.get(0)).setWorld(serverParts.get(1))
                            .setExpiry(Long.parseLong(tempParts.get(1))).setValue(b);
                } else {
                    return new NodeBuilder(parts.get(1), true).setServerRaw(serverParts.get(0)).setWorld(serverParts.get(1)).setValue(b);
                }

            } else {
                // SERVER BUT NOT WORLD SPECIFIC
                if (parts.get(1).contains("$")) {
                    List<String> tempParts = Splitter.on('$').limit(2).splitToList(parts.get(1));
                    return new NodeBuilder(tempParts.get(0), true).setServerRaw(parts.get(0)).setExpiry(Long.parseLong(tempParts.get(1))).setValue(b);
                } else {
                    return new NodeBuilder(parts.get(1), true).setServerRaw(parts.get(0)).setValue(b);
                }
            }
        } else {
            // NOT SERVER SPECIFIC
            if (s.contains("$")) {
                List<String> tempParts = Splitter.on('$').limit(2).splitToList(s);
                return new NodeBuilder(tempParts.get(0), true).setExpiry(Long.parseLong(tempParts.get(1))).setValue(b);
            } else {
                return new NodeBuilder(s, true).setValue(b);
            }
        }
    }

    public static Node.Builder builderFromExisting(Node other) {
        return new NodeBuilder(other);
    }

    public static NodeBuilder makeMetaNode(String key, String value) {
        return new NodeBuilder("meta." + MetaUtils.escapeCharacters(key) + "." + MetaUtils.escapeCharacters(value));
    }

    public static NodeBuilder makePrefixNode(int priority, String prefix) {
        return new NodeBuilder("prefix." + priority + "." + MetaUtils.escapeCharacters(prefix));
    }

    public static NodeBuilder makeSuffixNode(int priority, String suffix) {
        return new NodeBuilder("suffix." + priority + "." + MetaUtils.escapeCharacters(suffix));
    }
}
