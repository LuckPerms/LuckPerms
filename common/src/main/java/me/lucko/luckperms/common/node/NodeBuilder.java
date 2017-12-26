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

package me.lucko.luckperms.common.node;

import lombok.NonNull;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;

import java.util.Map;
import java.util.Set;

/**
 * Builds node instances
 */
class NodeBuilder implements Node.Builder {
    protected String permission;
    private final ImmutableContextSet.Builder extraContexts = ImmutableContextSet.builder();
    private Boolean value = true;
    private boolean override = false;
    private String server = null;
    private String world = null;
    private long expireAt = 0L;

    protected NodeBuilder() {

    }

    NodeBuilder(String permission) {
        this.permission = permission;
    }

    NodeBuilder(Node other) {
        this.permission = other.getPermission();
        this.value = other.getValuePrimitive();
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
        return new ImmutableNode(permission, value, override, expireAt, server, world, extraContexts.build());
    }
}
