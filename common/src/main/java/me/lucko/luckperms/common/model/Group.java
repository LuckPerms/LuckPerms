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

package me.lucko.luckperms.common.model;

import me.lucko.luckperms.common.api.implementation.ApiGroup;
import me.lucko.luckperms.common.cache.Cache;
import me.lucko.luckperms.common.cacheddata.GroupCachedDataManager;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.DisplayNameNode;
import net.luckperms.api.query.QueryOptions;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Optional;
import java.util.OptionalInt;

public class Group extends PermissionHolder {
    private final ApiGroup apiProxy = new ApiGroup(this);

    /**
     * The name of the group
     */
    private final String name;

    /**
     * Caches the groups weight
     */
    private final Cache<OptionalInt> weightCache = new WeightCache(this);

    /**
     * Caches the groups display name
     */
    private final Cache<Optional<String>> displayNameCache = new DisplayNameCache();

    /**
     * The groups data cache instance
     */
    private final GroupCachedDataManager cachedData;

    public Group(String name, LuckPermsPlugin plugin) {
        super(plugin);
        this.name = name.toLowerCase();

        this.cachedData = new GroupCachedDataManager(this);
        getPlugin().getEventDispatcher().dispatchGroupCacheLoad(this, this.cachedData);
    }

    @Override
    protected void invalidateCache() {
        super.invalidateCache();

        // invalidate our caches
        this.weightCache.invalidate();
        this.displayNameCache.invalidate();
    }

    // name getters
    public String getName() {
        return this.name;
    }

    @Override
    public String getObjectName() {
        return this.name;
    }

    public ApiGroup getApiProxy() {
        return this.apiProxy;
    }

    @Override
    public QueryOptions getQueryOptions() {
        return getPlugin().getContextManager().getStaticQueryOptions();
    }

    @Override
    public GroupCachedDataManager getCachedData() {
        return this.cachedData;
    }

    @Override
    public Component getFormattedDisplayName() {
        String displayName = getDisplayName().orElse(null);
        if (displayName != null) {
            return Component.text()
                    .content(this.name)
                    .append(Component.space())
                    .append(Component.text()
                            .color(NamedTextColor.WHITE)
                            .append(Message.OPEN_BRACKET)
                            .append(Component.text(displayName))
                            .append(Message.CLOSE_BRACKET)
                    )
                    .build();
        } else {
            return Component.text(this.name);
        }
    }

    @Override
    public String getPlainDisplayName() {
        return getDisplayName().orElse(getName());
    }

    public Optional<String> getDisplayName() {
        return this.displayNameCache.get();
    }

    public Optional<String> calculateDisplayName(QueryOptions queryOptions) {
        // query for a displayname node
        for (DisplayNameNode n : getOwnNodes(NodeType.DISPLAY_NAME, queryOptions)) {
            return Optional.of(n.getDisplayName());
        }

        // fallback to config
        String name = getPlugin().getConfiguration().get(ConfigKeys.GROUP_NAME_REWRITES).get(this.name);
        return name == null || name.equals(this.name) ? Optional.empty() : Optional.of(name);
    }

    @Override
    public OptionalInt getWeight() {
        return this.weightCache.get();
    }

    @Override
    public HolderType getType() {
        return HolderType.GROUP;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Group)) return false;
        final Group other = (Group) o;
        return this.name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public String toString() {
        return "Group(name=" + this.name + ")";
    }

    /**
     * Cache instance to supply the display name of a {@link Group}.
     */
    public class DisplayNameCache extends Cache<Optional<String>> {
        @Override
        protected @NonNull Optional<String> supply() {
            return calculateDisplayName(getPlugin().getContextManager().getStaticQueryOptions());
        }
    }
}
