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

package me.lucko.luckperms.common.storage.implementation.sql;

import me.lucko.luckperms.common.context.ContextSetJsonSerializer;
import me.lucko.luckperms.common.node.factory.NodeBuilders;
import me.lucko.luckperms.common.util.gson.GsonProvider;

import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.context.MutableContextSet;
import net.luckperms.api.node.Node;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A version of {@link Node}, more closely following the model used by the SQL
 * datastore.
 *
 * All values are non-null.
 */
public final class SqlNode {

    public static SqlNode fromNode(Node node) {
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


        long expiry = node.hasExpiry() ? node.getExpiry().getEpochSecond() : 0L;
        return new SqlNode(node.getKey(), node.getValue(), server, world, expiry, contexts.immutableCopy(), -1);
    }

    public static SqlNode fromSqlFields(String permission, boolean value, String server, String world, long expiry, String contexts) {
        return new SqlNode(permission, value, server, world, expiry, ContextSetJsonSerializer.deserializeContextSet(GsonProvider.normal(), contexts).immutableCopy(), -1);
    }

    public static SqlNode fromSqlFields(long sqlId, String permission, boolean value, String server, String world, long expiry, String contexts) {
        return new SqlNode(permission, value, server, world, expiry, ContextSetJsonSerializer.deserializeContextSet(GsonProvider.normal(), contexts).immutableCopy(), sqlId);
    }

    private final String permission;
    private final boolean value;
    private final String server;
    private final String world;
    private final long expiry;
    private final ImmutableContextSet contexts;
    private final long sqlId;

    private SqlNode(String permission, boolean value, String server, String world, long expiry, ImmutableContextSet contexts, long sqlId) {
        this.permission = Objects.requireNonNull(permission, "permission");
        this.value = value;
        this.server = Objects.requireNonNull(server, "server");
        this.world = Objects.requireNonNull(world, "world");
        this.expiry = expiry;
        this.contexts = Objects.requireNonNull(contexts, "contexts");
        this.sqlId = sqlId;
    }

    public Node toNode() {
        return NodeBuilders.determineMostApplicable(this.permission)
                .value(this.value)
                .withContext(DefaultContextKeys.SERVER_KEY, this.server)
                .withContext(DefaultContextKeys.WORLD_KEY, this.world)
                .expiry(this.expiry)
                .withContext(this.contexts)
                .build();
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

    public long getSqlId() {
        if (this.sqlId == -1) {
            throw new IllegalStateException("sql id not set");
        }
        return this.sqlId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof SqlNode)) return false;
        final SqlNode other = (SqlNode) o;

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
