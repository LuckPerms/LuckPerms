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

package me.lucko.luckperms.common.node.model;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;

import me.lucko.luckperms.api.nodetype.NodeType;
import me.lucko.luckperms.api.nodetype.NodeTypeKey;
import me.lucko.luckperms.api.nodetype.types.InheritanceType;
import me.lucko.luckperms.api.nodetype.types.MetaType;
import me.lucko.luckperms.api.nodetype.types.PrefixType;
import me.lucko.luckperms.api.nodetype.types.SuffixType;
import me.lucko.luckperms.api.nodetype.types.WeightType;
import me.lucko.luckperms.common.node.factory.LegacyNodeFactory;
import me.lucko.luckperms.common.utils.PatternCache;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nonnull;

public final class NodeTypes {

    public static final String PREFIX_KEY = "prefix";
    public static final String SUFFIX_KEY = "suffix";
    public static final String META_KEY = "meta";
    public static final String WEIGHT_KEY = "weight";

    public static final String GROUP_NODE_MARKER = "group.";
    public static final String PREFIX_NODE_MARKER = PREFIX_KEY + ".";
    public static final String SUFFIX_NODE_MARKER = SUFFIX_KEY + ".";
    public static final String META_NODE_MARKER = META_KEY + ".";
    public static final String WEIGHT_NODE_MARKER = WEIGHT_KEY + ".";

    // used to split prefix/suffix/meta nodes
    private static final Splitter META_SPLITTER = Splitter.on(PatternCache.compileDelimiterPattern(".", "\\")).limit(2);

    public static Map<NodeTypeKey<?>, NodeType> parseTypes(String s) {
        Map<NodeTypeKey<?>, NodeType> results = new IdentityHashMap<>();

        NodeType type = parseInheritanceType(s);
        if (type != null) {
            results.put(InheritanceType.KEY, type);
        }

        type = parseMetaType(s);
        if (type != null) {
            results.put(MetaType.KEY, type);
        }

        type = parsePrefixType(s);
        if (type != null) {
            results.put(PrefixType.KEY, type);
        }

        type = parseSuffixType(s);
        if (type != null) {
            results.put(SuffixType.KEY, type);
        }

        type = parseWeightType(s);
        if (type != null) {
            results.put(WeightType.KEY, type);
        }

        if (results.isEmpty()) {
            return ImmutableMap.of();
        }

        return results;
    }

    private static InheritanceType parseInheritanceType(String s) {
        s = s.toLowerCase();
        if (!s.startsWith(GROUP_NODE_MARKER)) {
            return null;
        }

        String groupName = s.substring(GROUP_NODE_MARKER.length()).intern();
        return new Inheritance(groupName);
    }

    private static MetaType parseMetaType(String s) {
        if (!s.toLowerCase().startsWith(META_NODE_MARKER)) {
            return null;
        }

        Iterator<String> metaParts = META_SPLITTER.split(s.substring(META_NODE_MARKER.length())).iterator();

        if (!metaParts.hasNext()) return null;
        String key = metaParts.next();

        if (!metaParts.hasNext()) return null;
        String value = metaParts.next();

        return new Meta(
                LegacyNodeFactory.unescapeCharacters(key).intern(),
                LegacyNodeFactory.unescapeCharacters(value).intern()
        );
    }

    private static PrefixType parsePrefixType(String s) {
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
            String v = LegacyNodeFactory.unescapeCharacters(value).intern();
            return new Prefix(p, v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static SuffixType parseSuffixType(String s) {
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
            String v = LegacyNodeFactory.unescapeCharacters(value).intern();
            return new Suffix(p, v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static WeightType parseWeightType(String s) {
        String lower = s.toLowerCase();
        if (!lower.startsWith(WEIGHT_NODE_MARKER)) {
            return null;
        }
        String i = lower.substring(WEIGHT_NODE_MARKER.length());
        try {
            return new Weight(Integer.parseInt(i));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static final class Inheritance implements InheritanceType {
        private final String groupName;

        private Inheritance(String groupName) {
            this.groupName = groupName;
        }

        @Nonnull
        @Override
        public String getGroupName() {
            return this.groupName;
        }
    }

    private static final class Meta implements MetaType {
        private final String key;
        private final String value;

        private Meta(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Nonnull
        @Override
        public String getKey() {
            return this.key;
        }

        @Nonnull
        @Override
        public String getValue() {
            return this.value;
        }
    }
    
    private static final class Prefix implements PrefixType, Map.Entry<Integer, String> {
        private final int priority;
        private final String prefix;

        private Prefix(int priority, String prefix) {
            this.priority = priority;
            this.prefix = prefix;
        }

        @Override
        public int getPriority() {
            return this.priority;
        }

        @Nonnull
        @Override
        public String getPrefix() {
            return this.prefix;
        }

        @Nonnull
        @Override
        public Map.Entry<Integer, String> getAsEntry() {
            return this;
        }

        @Override
        public Integer getKey() {
            return getPriority();
        }

        @Override
        public String getValue() {
            return getPrefix();
        }

        @Override
        public String setValue(String value) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class Suffix implements SuffixType, Map.Entry<Integer, String> {
        private final int priority;
        private final String suffix;

        private Suffix(int priority, String suffix) {
            this.priority = priority;
            this.suffix = suffix;
        }

        @Override
        public int getPriority() {
            return this.priority;
        }

        @Nonnull
        @Override
        public String getSuffix() {
            return this.suffix;
        }

        @Nonnull
        @Override
        public Map.Entry<Integer, String> getAsEntry() {
            return this;
        }

        @Override
        public Integer getKey() {
            return getPriority();
        }

        @Override
        public String getValue() {
            return getSuffix();
        }

        @Override
        public String setValue(String value) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class Weight implements WeightType {
        private final int weight;

        private Weight(int weight) {
            this.weight = weight;
        }

        @Override
        public int getWeight() {
            return this.weight;
        }
    }

    private NodeTypes() {}

}
