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

import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.DefaultContextKeys;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.api.node.Node;
import me.lucko.luckperms.common.node.factory.NodeFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * An stripped down version of {@link Node}, without methods and cached values
 * for handling permission lookups.
 *
 * All values are non-null.
 */
public final class NodeDataContainer {

    public static NodeDataContainer fromNode(Node node) {
        ContextSet contexts = node.getContexts();

        Set<String> servers = contexts.getValues(DefaultContextKeys.SERVER_KEY);
        Optional<String> firstServer = servers.stream().sorted().findFirst();
        
        String server;
        if (firstServer.isPresent()) {
            server = firstServer.get();
            MutableContextSet mutableContextSet = contexts.mutableCopy();
            mutableContextSet.remove(DefaultContextKeys.SERVER_KEY, server);
            contexts = mutableContextSet;
        } else {
            server = "global";
        }

        Set<String> worlds = contexts.getValues(DefaultContextKeys.WORLD_KEY);
        Optional<String> firstWorld = worlds.stream().sorted().findFirst();

        String world;
        if (firstWorld.isPresent()) {
            world = firstWorld.get();
            MutableContextSet mutableContextSet = contexts instanceof MutableContextSet ? (MutableContextSet) contexts : contexts.mutableCopy();
            mutableContextSet.remove(DefaultContextKeys.WORLD_KEY, world);
            contexts = mutableContextSet;
        } else {
            world = "global";
        }
        

        NodeDataContainer model = of(node.getKey(), node.getValue(), server, world, node.hasExpiry() ? node.getExpiry().getEpochSecond() : 0L, contexts.immutableCopy());
        model.node = node;
        return model;
    }

    public static NodeDataContainer of(String permission, boolean value, String server, String world, long expiry, ImmutableContextSet contexts) {
        return new NodeDataContainer(permission, value, server, world, expiry, contexts);
    }

    public static NodeDataContainer of(String permission) {
        return of(permission, true, "global", "global", 0L, ImmutableContextSet.empty());
    }

    private final String permission;
    private final boolean value;
    private final String server;
    private final String world;
    private final long expiry;
    private final ImmutableContextSet contexts;
    private Node node = null;

    private NodeDataContainer(String permission, boolean value, String server, String world, long expiry, ImmutableContextSet contexts) {
        this.permission = Objects.requireNonNull(permission, "permission");
        this.value = value;
        this.server = Objects.requireNonNull(server, "server");
        this.world = Objects.requireNonNull(world, "world");
        this.expiry = expiry;
        this.contexts = Objects.requireNonNull(contexts, "contexts");
    }

    public synchronized Node toNode() {
        if (this.node == null) {
            this.node = NodeFactory.builder(this.permission)
                    .value(this.value)
                    .withContext(DefaultContextKeys.SERVER_KEY, this.server)
                    .withContext(DefaultContextKeys.WORLD_KEY, this.world)
                    .expiry(this.expiry)
                    .withContext(this.contexts)
                    .build();
        }

        return this.node;
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

    public NodeDataContainer setPermission(String permission) {
        return of(permission, this.value, this.server, this.world, this.expiry, this.contexts);
    }

    public NodeDataContainer setValue(boolean value) {
        return of(this.permission, value, this.server, this.world, this.expiry, this.contexts);
    }

    public NodeDataContainer setServer(String server) {
        return of(this.permission, this.value, server, this.world, this.expiry, this.contexts);
    }

    public NodeDataContainer setWorld(String world) {
        return of(this.permission, this.value, this.server, world, this.expiry, this.contexts);
    }

    public NodeDataContainer setExpiry(long expiry) {
        return of(this.permission, this.value, this.server, this.world, expiry, this.contexts);
    }

    public NodeDataContainer setContexts(ImmutableContextSet contexts) {
        return of(this.permission, this.value, this.server, this.world, this.expiry, contexts);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof NodeDataContainer)) return false;
        final NodeDataContainer other = (NodeDataContainer) o;

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
