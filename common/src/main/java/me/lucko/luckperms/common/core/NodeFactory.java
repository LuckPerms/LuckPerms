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

import lombok.experimental.UtilityClass;

import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import me.lucko.luckperms.api.MetaUtils;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.constants.Patterns;

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
        // if contains /
        if (Patterns.compileDelimitedMatcher("/", "\\").matcher(s).find()) {
            List<String> parts = Splitter.on(Patterns.compileDelimitedMatcher("/", "\\")).limit(2).splitToList(s);
            // 0=server(+world)   1=node

            // WORLD SPECIFIC
            // if parts[0] contains -
            if (Patterns.compileDelimitedMatcher("-", "\\").matcher(parts.get(0)).find()) {
                List<String> serverParts = Splitter.on(Patterns.compileDelimitedMatcher("-", "\\")).limit(2).splitToList(parts.get(0));
                // 0=server   1=world

                // if parts[1] contains $
                if (Patterns.compileDelimitedMatcher("$", "\\").matcher(parts.get(1)).find()) {
                    List<String> tempParts = Splitter.on('$').limit(2).splitToList(parts.get(1));
                    return new NodeBuilder(tempParts.get(0), true).setServer(serverParts.get(0)).setWorld(serverParts.get(1))
                            .setExpiry(Long.parseLong(tempParts.get(1))).setValue(b);
                } else {
                    return new NodeBuilder(parts.get(1), true).setServer(serverParts.get(0)).setWorld(serverParts.get(1)).setValue(b);
                }
            } else {
                // SERVER BUT NOT WORLD SPECIFIC

                // if parts[1] contains $
                if (Patterns.compileDelimitedMatcher("$", "\\").matcher(parts.get(1)).find()) {
                    List<String> tempParts = Splitter.on(Patterns.compileDelimitedMatcher("$", "\\")).limit(2).splitToList(parts.get(1));
                    return new NodeBuilder(tempParts.get(0), true).setServer(parts.get(0)).setExpiry(Long.parseLong(tempParts.get(1))).setValue(b);
                } else {
                    return new NodeBuilder(parts.get(1), true).setServer(parts.get(0)).setValue(b);
                }
            }
        } else {
            // NOT SERVER SPECIFIC

            // if s contains $
            if (Patterns.compileDelimitedMatcher("$", "\\").matcher(s).find()) {
                List<String> tempParts = Splitter.on(Patterns.compileDelimitedMatcher("$", "\\")).limit(2).splitToList(s);
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
        if (key.equalsIgnoreCase("prefix")) {
            return makePrefixNode(100, value);
        }
        if (key.equalsIgnoreCase("suffix")) {
            return makeSuffixNode(100, value);
        }

        return new NodeBuilder("meta." + MetaUtils.escapeCharacters(key) + "." + MetaUtils.escapeCharacters(value));
    }

    public static NodeBuilder makePrefixNode(int priority, String prefix) {
        return new NodeBuilder("prefix." + priority + "." + MetaUtils.escapeCharacters(prefix));
    }

    public static NodeBuilder makeSuffixNode(int priority, String suffix) {
        return new NodeBuilder("suffix." + priority + "." + MetaUtils.escapeCharacters(suffix));
    }

    public static String nodeAsCommand(Node node, String id, boolean group) {
        StringBuilder sb = new StringBuilder();
        sb.append("/luckperms ").append(group ? "group " : "user ").append(id).append(" ");

        if (node.isGroupNode()) {
            if (node.isTemporary()) {
                sb.append("parent addtemp ");
                sb.append(node.getGroupName());
                sb.append(" ").append(node.getExpiryUnixTime());
            } else {
                sb.append("parent add ");
                sb.append(node.getGroupName());
            }

            if (node.isWorldSpecific()) {
                sb.append(" ").append(node.getServer().get()).append(" ").append(node.getWorld().get());
            } else if (node.isServerSpecific()) {
                sb.append(" ").append(node.getServer().get());
            }

            return sb.toString();
        }

        sb.append(node.isTemporary() ? "permission settemp " : "permission set ");
        if (node.getPermission().contains(" ")) {
            sb.append("\"").append(node.getPermission()).append("\"");
        } else {
            sb.append(node.getPermission());
        }
        sb.append(" ").append(node.getValue());

        if (node.isTemporary()) {
            sb.append(" ").append(node.getExpiryUnixTime());
        }

        if (node.isWorldSpecific()) {
            sb.append(" ").append(node.getServer().get()).append(" ").append(node.getWorld().get());
        } else if (node.isServerSpecific()) {
            sb.append(" ").append(node.getServer().get());
        }

        return sb.toString();
    }

    public static String escapeDelimiters(String s, String... delims) {
        if (s == null) {
            return null;
        }

        for (String delim : delims) {
            s = s.replace(delim, "\\" + delim);
        }
        return s;
    }

    public static String unescapeDelimiters(String s, String... delims) {
        if (s == null) {
            return null;
        }

        for (String delim : delims) {
            s = s.replace("\\" + delim, delim);
        }
        return s;
    }

    public static boolean isMetaNode(String s) {
        if (!s.startsWith("meta.")) {
            return false;
        }
        String parts = s.substring("meta.".length());
        return Patterns.compileDelimitedMatcher(".", "\\").matcher(parts).find();
    }

    private static boolean isChatMetaNode(String type, String s) {
        if (!s.startsWith(type + ".")) {
            return false;
        }
        String parts = s.substring((type + ".").length());

        if (!Patterns.compileDelimitedMatcher(".", "\\").matcher(parts).find()) {
            return false;
        }

        List<String> metaParts = Splitter.on(Patterns.compileDelimitedMatcher(".", "\\")).limit(2).splitToList(parts);
        String priority = metaParts.get(0);
        try {
            Integer.parseInt(priority);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isPrefixNode(String s) {
        return isChatMetaNode("prefix", s);
    }

    public static boolean isSuffixNode(String s) {
        return isChatMetaNode("suffix", s);
    }
}
