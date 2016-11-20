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
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.common.constants.Patterns;
import me.lucko.luckperms.common.utils.ArgumentChecker;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds Nodes
 */
@RequiredArgsConstructor
public class NodeBuilder implements Node.Builder {
    private final String permission;
    private Boolean value = true;
    private boolean override = false;
    private String server = null;
    private String world = null;
    private long expireAt = 0L;
    private final MutableContextSet extraContexts = new MutableContextSet();

    NodeBuilder(String permission, boolean shouldConvertContexts) {
        if (!shouldConvertContexts) {
            this.permission = permission;
        } else {
            if (!Patterns.NODE_CONTEXTS.matcher(permission).matches()) {
                this.permission = permission;
            } else {
                List<String> contextParts = Splitter.on(')').limit(2).splitToList(permission.substring(1));
                // 0 = context, 1 = node

                this.permission = contextParts.get(1);
                try {
                    extraContexts.addAll(Splitter.on(',').withKeyValueSeparator('=').split(contextParts.get(0)));
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
        if (server != null && ArgumentChecker.checkServer(server)) {
            throw new IllegalArgumentException("Server name invalid.");
        }

        this.server = server;
        return this;
    }

    public Node.Builder setServerRaw(String server) {
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
