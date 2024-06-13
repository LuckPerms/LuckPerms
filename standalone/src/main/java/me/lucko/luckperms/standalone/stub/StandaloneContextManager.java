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

package me.lucko.luckperms.standalone.stub;

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.context.manager.ContextManager;
import me.lucko.luckperms.common.context.manager.QueryOptionsCache;
import me.lucko.luckperms.standalone.LPStandalonePlugin;
import me.lucko.luckperms.standalone.app.integration.StandaloneSender;
import me.lucko.luckperms.standalone.app.integration.StandaloneUser;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.query.QueryOptions;

import java.util.UUID;

public class StandaloneContextManager extends ContextManager<StandaloneSender, StandaloneSender> {
    private final QueryOptionsCache<StandaloneSender> singletonCache = new QueryOptionsCache<>(StandaloneUser.INSTANCE, this);

    public StandaloneContextManager(LPStandalonePlugin plugin) {
        super(plugin, StandaloneSender.class, StandaloneSender.class);
    }

    @Override
    public UUID getUniqueId(StandaloneSender player) {
        return player.getUniqueId();
    }

    @Override
    public QueryOptionsCache<StandaloneSender> getCacheFor(StandaloneSender subject) {
        if (subject == null) {
            throw new NullPointerException("subject");
        }
        if (subject == StandaloneUser.INSTANCE) {
            return this.singletonCache;
        }

        // just return a new one every time - not optimal but this case should only be hit using unit tests anyway
        return new QueryOptionsCache<>(subject, this);
    }

    @Override
    protected void invalidateCache(StandaloneSender subject) {
        this.singletonCache.invalidate();
    }

    @Override
    public QueryOptions formQueryOptions(StandaloneSender subject, ImmutableContextSet contextSet) {
        QueryOptions.Builder queryOptions = this.plugin.getConfiguration().get(ConfigKeys.GLOBAL_QUERY_OPTIONS).toBuilder();
        return queryOptions.context(contextSet).build();
    }
}
