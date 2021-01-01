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

package me.lucko.luckperms.fabric.context;

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.context.ContextManager;
import me.lucko.luckperms.common.context.QueryOptionsCache;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.fabric.model.MixinUser;

import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.query.OptionKey;
import net.luckperms.api.query.QueryOptions;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public class FabricContextManager extends ContextManager<ServerPlayerEntity, ServerPlayerEntity> {
    public static final OptionKey<Boolean> INTEGRATED_SERVER_OWNER = OptionKey.of("integrated_server_owner", Boolean.class);

    public FabricContextManager(LuckPermsPlugin plugin) {
        super(plugin, ServerPlayerEntity.class, ServerPlayerEntity.class);
    }

    @Override
    public UUID getUniqueId(ServerPlayerEntity player) {
        return player.getUuid();
    }

    public QueryOptionsCache<ServerPlayerEntity> newQueryOptionsCache(ServerPlayerEntity player) {
        return new QueryOptionsCache<>(player, this);
    }

    @Override
    public QueryOptionsCache<ServerPlayerEntity> getCacheFor(ServerPlayerEntity subject) {
        if (subject == null) {
            throw new NullPointerException("subject");
        }

        return ((MixinUser) subject).getQueryOptionsCache(this);
    }

    @Override
    public void invalidateCache(ServerPlayerEntity subject) {
        getCacheFor(subject).invalidate();
    }

    @Override
    public QueryOptions formQueryOptions(ServerPlayerEntity subject, ImmutableContextSet contextSet) {
        QueryOptions.Builder queryOptions = this.plugin.getConfiguration().get(ConfigKeys.GLOBAL_QUERY_OPTIONS).toBuilder();
        if (subject.getServer().isHost(subject.getGameProfile())) {
            queryOptions.option(INTEGRATED_SERVER_OWNER, true);
        }

        return queryOptions.context(contextSet).build();
    }

}
