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
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.api.delegates.model.ApiGroup;
import me.lucko.luckperms.common.buffers.BufferedRequest;
import me.lucko.luckperms.common.caching.GroupCachedData;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.references.GroupReference;
import me.lucko.luckperms.common.references.HolderType;
import me.lucko.luckperms.common.references.Identifiable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class Group extends PermissionHolder implements Identifiable<String> {

    /**
     * The name of the group
     */
    private final String name;

    private final ApiGroup apiDelegate = new ApiGroup(this);

    /**
     * The groups data cache instance
     */
    private final GroupCachedData cachedData;

    private final BufferedRequest<Void> refreshBuffer;

    public Group(String name, LuckPermsPlugin plugin) {
        super(name, plugin);
        this.name = name.toLowerCase();

        this.refreshBuffer = new GroupRefreshBuffer(plugin, this);
        this.cachedData = new GroupCachedData(this);
        getPlugin().getEventFactory().handleGroupCacheLoad(this, this.cachedData);

        // invalidate our caches when data is updated
        getStateListeners().add(this.refreshBuffer::request);
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
        String name = null;
        for (Node n : getEnduringNodes().get(ImmutableContextSet.empty())) {
            if (!n.getPermission().startsWith("displayname.")) {
                continue;
            }

            name = n.getPermission().substring("displayname.".length());
            break;
        }

        if (name != null) {
            return Optional.of(name);
        }

        name = getPlugin().getConfiguration().get(ConfigKeys.GROUP_NAME_REWRITES).get(getObjectName());
        return name == null || name.equals(getObjectName()) ? Optional.empty() : Optional.of(name);
    }

    @Override
    public String getFriendlyName() {
        Optional<String> dn = getDisplayName();
        return dn.map(s -> this.name + " (" + s + ")").orElse(this.name);
    }

    @Override
    public GroupReference toReference() {
        return GroupReference.of(getId());
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
