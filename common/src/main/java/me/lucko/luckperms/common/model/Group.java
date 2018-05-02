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

import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.nodetype.types.DisplayNameType;
import me.lucko.luckperms.common.api.delegates.model.ApiGroup;
import me.lucko.luckperms.common.buffers.BufferedRequest;
import me.lucko.luckperms.common.buffers.Cache;
import me.lucko.luckperms.common.caching.GroupCachedData;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;

public class Group extends PermissionHolder implements Identifiable<String> {
    private final ApiGroup apiDelegate = new ApiGroup(this);

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
    private final Cache<Optional<String>> displayNameCache = new DisplayNameCache(this);

    /**
     * The groups data cache instance
     */
    private final GroupCachedData cachedData;

    /**
     * The group's cached data refresh buffer
     */
    private final GroupRefreshBuffer refreshBuffer;

    public Group(String name, LuckPermsPlugin plugin) {
        super(name, plugin);
        this.name = name.toLowerCase();

        this.refreshBuffer = new GroupRefreshBuffer(plugin, this);
        this.cachedData = new GroupCachedData(this);
        getPlugin().getEventFactory().handleGroupCacheLoad(this, this.cachedData);

        // invalidate our caches when data is updated
        getStateListeners().add(this.refreshBuffer::request);
    }

    @Override
    protected void invalidateCache() {
        this.weightCache.invalidate();
        this.displayNameCache.invalidate();
        super.invalidateCache();
    }

    public String getName() {
        return this.name;
    }

    public ApiGroup getApiDelegate() {
        return this.apiDelegate;
    }

    @Override
    public GroupCachedData getCachedData() {
        return this.cachedData;
    }

    @Override
    public BufferedRequest<Void> getRefreshBuffer() {
        return this.refreshBuffer;
    }

    @Override
    public String getId() {
        return this.name;
    }

    public Optional<String> getDisplayName() {
        return this.displayNameCache.get();
    }

    public Optional<String> getDisplayName(ContextSet contextSet) {
        for (Node n : getData(NodeMapType.ENDURING).immutable().get(contextSet.makeImmutable())) {
            Optional<DisplayNameType> displayName = n.getTypeData(DisplayNameType.KEY);
            if (displayName.isPresent()) {
                return Optional.of(displayName.get().getDisplayName());
            }
        }
        return Optional.empty();
    }

    @Override
    public String getFriendlyName() {
        Optional<String> dn = getDisplayName();
        return dn.map(s -> this.name + " (" + s + ")").orElse(this.name);
    }

    @Override
    public OptionalInt getWeight() {
        return this.weightCache.get();
    }

    @Override
    public HolderType getType() {
        return HolderType.GROUP;
    }

    private CompletableFuture<Void> reloadCachedData() {
        return CompletableFuture.allOf(
                this.cachedData.reloadPermissions(),
                this.cachedData.reloadMeta()
        ).thenAccept(n -> getPlugin().getEventFactory().handleGroupDataRecalculate(this, this.cachedData));
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

    private static final class GroupRefreshBuffer extends BufferedRequest<Void> {
        private final Group group;

        private GroupRefreshBuffer(LuckPermsPlugin plugin, Group group) {
            super(50L, 5L, plugin.getBootstrap().getScheduler().async());
            this.group = group;
        }

        @Override
        protected Void perform() {
            return this.group.reloadCachedData().join();
        }
    }

}
