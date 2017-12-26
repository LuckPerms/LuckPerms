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

import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.context.ImmutableContextSet;

/**
 * An stripped down version of {@link Node}, without methods and cached values
 * for handling permission lookups.
 *
 * All values are non-null.
 */
public final class NodeModel {

    public static NodeModel fromNode(Node node) {
        NodeModel model = of(
                node.getPermission(),
                node.getValuePrimitive(),
                node.getServer().orElse("global"),
                node.getWorld().orElse("global"),
                node.isTemporary() ? node.getExpiryUnixTime() : 0L,
                node.getContexts().makeImmutable()
        );
        model.node = node;
        return model;
    }

    public static NodeModel of(String permission, boolean value, String server, String world, long expiry, ImmutableContextSet contexts) {
        return new NodeModel(permission, value, server, world, expiry, contexts);
    }

    private final String permission;
    private final boolean value;
    private final String server;
    private final String world;
    private final long expiry;
    private final ImmutableContextSet contexts;
    private Node node = null;

    private NodeModel(@NonNull String permission, boolean value, @NonNull String server, @NonNull String world, long expiry, @NonNull ImmutableContextSet contexts) {
        this.permission = permission;
        this.value = value;
        this.server = server;
        this.world = world;
        this.expiry = expiry;
        this.contexts = contexts;
    }

    public synchronized Node toNode() {
        if (node == null) {
            node = NodeFactory.builder(permission)
                    .setValue(value)
                    .setServer(server)
                    .setWorld(world)
                    .setExpiry(expiry)
                    .withExtraContext(contexts)
                    .build();
        }

        return node;
    }

    public String getPermission() {
        return this.permission;
    }

    public boolean getValue() {
        return this.value;
    }

    public String getServer() {
        return this.server;
    }

    public String getWorld() {
        return this.world;
    }

    public long getExpiry() {
        return this.expiry;
    }

    public ImmutableContextSet getContexts() {
        return this.contexts;
    }

    public NodeModel setPermission(String permission) {
        return of(permission, value, server, world, expiry, contexts);
    }

    public NodeModel setValue(boolean value) {
        return of(permission, value, server, world, expiry, contexts);
    }

    public NodeModel setServer(String server) {
        return of(permission, value, server, world, expiry, contexts);
    }

    public NodeModel setWorld(String world) {
        return of(permission, value, server, world, expiry, contexts);
    }

    public NodeModel setExpiry(long expiry) {
        return of(permission, value, server, world, expiry, contexts);
    }

    public NodeModel setContexts(ImmutableContextSet contexts) {
        return of(permission, value, server, world, expiry, contexts);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof NodeModel)) return false;
        final NodeModel other = (NodeModel) o;

        return this.getPermission().equals(other.getPermission()) &&
                this.getValue() == other.getValue() &&
                this.getServer().equals(other.getServer()) &&
                this.getWorld().equals(other.getWorld()) &&
                this.getExpiry() == other.getExpiry() &&
                this.getContexts().equals(other.getContexts());
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getPermission().hashCode();
        result = result * PRIME + Boolean.hashCode(this.getValue());
        result = result * PRIME + this.getServer().hashCode();
        result = result * PRIME + this.getWorld().hashCode();
        result = result * PRIME + Long.hashCode(this.getExpiry());
        result = result * PRIME + this.getContexts().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "NodeModel(" +
                "permission=" + this.getPermission() + ", " +
                "value=" + this.getValue() + ", " +
                "server=" + this.getServer() + ", " +
                "world=" + this.getWorld() + ", " +
                "expiry=" + this.getExpiry() + ", " +
                "contexts=" + this.getContexts() + ")";
    }
}
