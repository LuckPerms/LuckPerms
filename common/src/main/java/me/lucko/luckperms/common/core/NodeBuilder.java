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

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import com.google.common.base.Splitter;

import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.common.core.model.ImmutableNode;
import me.lucko.luckperms.common.utils.PatternCache;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Builds Nodes
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class NodeBuilder implements Node.Builder {
    private static final Pattern NODE_CONTEXTS_PATTERN = Pattern.compile("\\(.+\\).*");

    private final String permission;
    private final MutableContextSet extraContexts = MutableContextSet.create();
    private Boolean value = true;
    private boolean override = false;
    private String server = null;
    private String world = null;
    private long expireAt = 0L;

    NodeBuilder(String permission, boolean shouldConvertContexts) {
        if (!shouldConvertContexts) {
            this.permission = permission;
        } else {
            if (!NODE_CONTEXTS_PATTERN.matcher(permission).matches()) {
                this.permission = permission;
            } else {
                List<String> contextParts = Splitter.on(PatternCache.compileDelimitedMatcher(")", "\\")).limit(2).splitToList(permission.substring(1));
                // 0 = context, 1 = node

                this.permission = contextParts.get(1);
                try {
                    Map<String, String> map = Splitter.on(PatternCache.compileDelimitedMatcher(",", "\\")).withKeyValueSeparator(Splitter.on(PatternCache.compileDelimitedMatcher("=", "\\"))).split(contextParts.get(0));
                    for (Map.Entry<String, String> e : map.entrySet()) {
                        this.withExtraContext(NodeFactory.unescapeDelimiters(e.getKey(), "=", "(", ")", ","), NodeFactory.unescapeDelimiters(e.getValue(), "=", "(", ")", ","));
                    }

                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    NodeBuilder(Node other) {
        this.permission = other.getPermission();
        this.value = other.getValue();
        this.override = other.isOverride();
        this.server = other.getServer().orElse(null);
        this.world = other.getWorld().orElse(null);
        this.expireAt = other.isPermanent() ? 0L : other.getExpiryUnixTime();
        this.extraContexts.addAll(other.getContexts());
    }

    @Override
    public Node.Builder setNegated(boolean negated) {
        value = !negated;
        return this;
    }

    @Override
    public Node.Builder setValue(boolean value) {
        this.value = value;
        return this;
    }

    @Override
    public Node.Builder setOverride(boolean override) {
        this.override = override;
        return this;
    }

    @Override
    public Node.Builder setExpiry(long expireAt) {
        this.expireAt = expireAt;
        return this;
    }

    @Override
    public Node.Builder setWorld(String world) {
        this.world = world;
        return this;
    }

    @Override
    public Node.Builder setServer(String server) {
        this.server = server;
        return this;
    }

    @Override
    public Node.Builder withExtraContext(@NonNull String key, @NonNull String value) {
        switch (key.toLowerCase()) {
            case "server":
                setServer(value);
                break;
            case "world":
                setWorld(value);
                break;
            default:
                this.extraContexts.add(key, value);
                break;
        }

        return this;
    }

    @Override
    public Node.Builder withExtraContext(Map.Entry<String, String> entry) {
        withExtraContext(entry.getKey(), entry.getValue());
        return this;
    }

    @Override
    public Node.Builder withExtraContext(Map<String, String> map) {
        withExtraContext(ContextSet.fromMap(map));
        return this;
    }

    @Override
    public Node.Builder withExtraContext(Set<Map.Entry<String, String>> context) {
        withExtraContext(ContextSet.fromEntries(context));
        return this;
    }

    @Override
    public Node.Builder withExtraContext(ContextSet set) {
        set.toSet().forEach(this::withExtraContext);
        return this;
    }

    @Override
    public Node build() {
        return new ImmutableNode(permission, value, override, expireAt, server, world, extraContexts);
    }
}
