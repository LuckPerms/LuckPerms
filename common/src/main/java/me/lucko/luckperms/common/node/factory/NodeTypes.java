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

package me.lucko.luckperms.common.node.factory;

import com.google.common.base.Splitter;

import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.DefaultContextKeys;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.api.node.Node;
import me.lucko.luckperms.api.node.NodeBuilder;
import me.lucko.luckperms.api.node.types.DisplayNameNode;
import me.lucko.luckperms.api.node.types.InheritanceNode;
import me.lucko.luckperms.api.node.types.MetaNode;
import me.lucko.luckperms.api.node.types.PrefixNode;
import me.lucko.luckperms.api.node.types.RegexPermissionNode;
import me.lucko.luckperms.api.node.types.SuffixNode;
import me.lucko.luckperms.api.node.types.WeightNode;
import me.lucko.luckperms.common.cache.PatternCache;
import me.lucko.luckperms.common.node.types.DisplayName;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.node.types.Meta;
import me.lucko.luckperms.common.node.types.Permission;
import me.lucko.luckperms.common.node.types.Prefix;
import me.lucko.luckperms.common.node.types.RegexPermission;
import me.lucko.luckperms.common.node.types.Suffix;
import me.lucko.luckperms.common.node.types.Weight;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;

public final class NodeTypes {
    private NodeTypes() {}

    public static final String GROUP_KEY = "group";
    public static final String PREFIX_KEY = "prefix";
    public static final String SUFFIX_KEY = "suffix";
    public static final String META_KEY = "meta";
    public static final String WEIGHT_KEY = "weight";
    public static final String DISPLAY_NAME_KEY = "displayname";

    public static final String GROUP_NODE_MARKER = GROUP_KEY + ".";
    public static final String PREFIX_NODE_MARKER = PREFIX_KEY + ".";
    public static final String SUFFIX_NODE_MARKER = SUFFIX_KEY + ".";
    public static final String META_NODE_MARKER = META_KEY + ".";
    public static final String WEIGHT_NODE_MARKER = WEIGHT_KEY + ".";
    public static final String DISPLAY_NAME_NODE_MARKER = DISPLAY_NAME_KEY + ".";
    public static final String REGEX_MARKER_1 = "r=";
    public static final String REGEX_MARKER_2 = "R=";

    // used to split prefix/suffix/meta nodes
    private static final Splitter META_SPLITTER = Splitter.on(PatternCache.compileDelimiterPattern(".", "\\")).limit(2);

    public static Node makeNode(String key, boolean value, long expireAt, String server, String world, ContextSet contexts) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Empty permission");
        }

        server = standardizeServerWorld(server);
        world = standardizeServerWorld(world);
        key = key.intern();
        contexts = formContextSet(contexts, server, world);

        return newBuilder(key).value(value).expiry(expireAt).context(contexts).build();
    }

    private static String standardizeServerWorld(String s) {
        if (s != null) {
            s = s.toLowerCase();

            if (s.equals("global") || s.isEmpty()) {
                s = null;
            }
        }

        return s;
    }

    private static ImmutableContextSet formContextSet(ContextSet contexts, String server, String world) {
        if ((contexts == null || contexts.isEmpty()) && server == null && world == null) {
            return ImmutableContextSet.empty();
        }

        ImmutableContextSet.Builder builder = ImmutableContextSet.builder();

        if (contexts != null) {
            builder.addAll(contexts);
        }
        if (server != null) {
            builder.add(DefaultContextKeys.SERVER_KEY, server);
        }
        if (world != null) {
            builder.add(DefaultContextKeys.WORLD_KEY, world);
        }

        return builder.build();
    }

    public static @NonNull NodeBuilder<?, ?> newBuilder(String s) {
        NodeBuilder<?, ?> b = parseInheritanceType(s);
        if (b != null) {
            return b;
        }

        b = parseMetaType(s);
        if (b != null) {
            return b;
        }

        b = parsePrefixType(s);
        if (b != null) {
            return b;
        }

        b = parseSuffixType(s);
        if (b != null) {
            return b;
        }

        b = parseWeightType(s);
        if (b != null) {
            return b;
        }

        b = parseDisplayNameType(s);
        if (b != null) {
            return b;
        }

        b = parseRegexType(s);
        if (b != null) {
            return b;
        }

        return new Permission.Builder().permission(s);
    }

    private static InheritanceNode.@Nullable Builder parseInheritanceType(String s) {
        s = s.toLowerCase();
        if (!s.startsWith(GROUP_NODE_MARKER)) {
            return null;
        }

        String groupName = s.substring(GROUP_NODE_MARKER.length()).intern();
        return new Inheritance.Builder().group(groupName);
    }

    private static MetaNode.@Nullable Builder parseMetaType(String s) {
        if (!s.toLowerCase().startsWith(META_NODE_MARKER)) {
            return null;
        }

        Iterator<String> metaParts = META_SPLITTER.split(s.substring(META_NODE_MARKER.length())).iterator();

        if (!metaParts.hasNext()) return null;
        String key = metaParts.next();

        if (!metaParts.hasNext()) return null;
        String value = metaParts.next();

        return new Meta.Builder()
                .key(Delimiters.unescapeCharacters(key).intern())
                .value(Delimiters.unescapeCharacters(value).intern());
    }

    private static PrefixNode.@Nullable Builder parsePrefixType(String s) {
        if (!s.toLowerCase().startsWith(PREFIX_NODE_MARKER)) {
            return null;
        }

        Iterator<String> metaParts = META_SPLITTER.split(s.substring(PREFIX_NODE_MARKER.length())).iterator();

        if (!metaParts.hasNext()) return null;
        String priority = metaParts.next();

        if (!metaParts.hasNext()) return null;
        String value = metaParts.next();

        try {
            int p = Integer.parseInt(priority);
            String v = Delimiters.unescapeCharacters(value).intern();
            return new Prefix.Builder().priority(p).prefix(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static SuffixNode.@Nullable Builder parseSuffixType(String s) {
        if (!s.toLowerCase().startsWith(SUFFIX_NODE_MARKER)) {
            return null;
        }

        Iterator<String> metaParts = META_SPLITTER.split(s.substring(SUFFIX_NODE_MARKER.length())).iterator();

        if (!metaParts.hasNext()) return null;
        String priority = metaParts.next();

        if (!metaParts.hasNext()) return null;
        String value = metaParts.next();

        try {
            int p = Integer.parseInt(priority);
            String v = Delimiters.unescapeCharacters(value).intern();
            return new Suffix.Builder().priority(p).suffix(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static WeightNode.@Nullable Builder parseWeightType(String s) {
        String lower = s.toLowerCase();
        if (!lower.startsWith(WEIGHT_NODE_MARKER)) {
            return null;
        }
        String i = lower.substring(WEIGHT_NODE_MARKER.length());
        try {
            return new Weight.Builder().weight(Integer.parseInt(i));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static DisplayNameNode.@Nullable Builder parseDisplayNameType(String s) {
        if (!s.toLowerCase().startsWith(DISPLAY_NAME_NODE_MARKER)) {
            return null;
        }

        return new DisplayName.Builder().displayName(s.substring(DISPLAY_NAME_NODE_MARKER.length()));
    }

    private static RegexPermissionNode.@Nullable Builder parseRegexType(String s) {
        if (!s.startsWith(REGEX_MARKER_1) && !s.startsWith(REGEX_MARKER_2)) {
            return null;
        }

        return new RegexPermission.Builder().pattern(s.substring(2));
    }

}
