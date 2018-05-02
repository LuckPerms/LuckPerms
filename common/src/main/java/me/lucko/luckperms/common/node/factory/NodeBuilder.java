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

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.node.model.ImmutableNode;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;

/**
 * Builds node instances
 */
public class NodeBuilder implements Node.Builder {
    protected String permission;
    private ImmutableContextSet.Builder extraContexts = ImmutableContextSet.builder();
    private Boolean value = true;
    private boolean override = false;
    private String server = null;
    private String world = null;
    private long expireAt = 0L;

    NodeBuilder() {

    }

    NodeBuilder(String permission) {
        this.permission = permission;
    }

    public NodeBuilder(Node other) {
        this.permission = other.getPermission();
        copyFrom(other);
    }

    @Override
    public Node.Builder copyFrom(@Nonnull Node node) {
        Objects.requireNonNull(node, "node");
        this.value = node.getValue();
        this.override = node.isOverride();
        this.server = node.getServer().orElse(null);
        this.world = node.getWorld().orElse(null);
        this.expireAt = node.isPermanent() ? 0L : node.getExpiryUnixTime();
        this.extraContexts = ImmutableContextSet.builder().addAll(node.getContexts());
        return this;
    }

    @Nonnull
    @Override
    public Node.Builder setNegated(boolean negated) {
        this.value = !negated;
        return this;
    }

    @Nonnull
    @Override
    public Node.Builder setValue(boolean value) {
        this.value = value;
        return this;
    }

    @Nonnull
    @Override
    public Node.Builder setOverride(boolean override) {
        this.override = override;
        return this;
    }

    @Nonnull
    @Override
    public Node.Builder setExpiry(long expireAt) {
        this.expireAt = expireAt;
        return this;
    }

    @Nonnull
    @Override
    public Node.Builder clearExpiry() {
        this.expireAt = 0L;
        return this;
    }

    @Nonnull
    @Override
    public Node.Builder setWorld(String world) {
        this.world = world;
        return this;
    }

    @Nonnull
    @Override
    public Node.Builder setServer(String server) {
        this.server = server;
        return this;
    }

    @Nonnull
    @Override
    public Node.Builder withExtraContext(@Nonnull String key, @Nonnull String value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        switch (key.toLowerCase()) {
            case Contexts.SERVER_KEY:
                setServer(value);
                break;
            case Contexts.WORLD_KEY:
                setWorld(value);
                break;
            default:
                this.extraContexts.add(key, value);
                break;
        }

        return this;
    }

    @Nonnull
    @Override
    public Node.Builder withExtraContext(@Nonnull Map.Entry<String, String> entry) {
        Objects.requireNonNull(entry, "entry");
        withExtraContext(entry.getKey(), entry.getValue());
        return this;
    }

    @Nonnull
    @Override
    public Node.Builder withExtraContext(@Nonnull Map<String, String> map) {
        Objects.requireNonNull(map, "map");
        withExtraContext(ContextSet.fromMap(map));
        return this;
    }

    @Nonnull
    @Override
    public Node.Builder withExtraContext(@Nonnull Set<Map.Entry<String, String>> context) {
        Objects.requireNonNull(context, "context");
        withExtraContext(ContextSet.fromEntries(context));
        return this;
    }

    @Nonnull
    @Override
    public Node.Builder withExtraContext(@Nonnull ContextSet set) {
        Objects.requireNonNull(set, "set");
        set.toSet().forEach(this::withExtraContext);
        return this;
    }

    @Nonnull
    @Override
    public Node.Builder setExtraContext(@Nonnull ContextSet contextSet) {
        this.extraContexts = ImmutableContextSet.builder().addAll(contextSet);
        return this;
    }

    @Nonnull
    @Override
    public Node build() {
        return new ImmutableNode(this.permission, this.value, this.override, this.expireAt, this.server, this.world, this.extraContexts.build());
    }
}
