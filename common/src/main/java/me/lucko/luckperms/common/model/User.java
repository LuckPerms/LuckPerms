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

import me.lucko.luckperms.common.api.implementation.ApiUser;
import me.lucko.luckperms.common.cacheddata.UserCachedDataManager;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.primarygroup.ContextualHolder;
import me.lucko.luckperms.common.primarygroup.PrimaryGroupHolder;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import java.util.UUID;

public class User extends PermissionHolder implements Identifiable<UserIdentifier> {
    private final ApiUser apiDelegate = new ApiUser(this);

    /**
     * The users Mojang UUID
     */
    private final UUID uuid;

    /**
     * The last known username of a player
     */
    private @Nullable String name = null;

    /**
     * The users primary group
     */
    private final PrimaryGroupHolder primaryGroup;

    /**
     * The users data cache instance
     */
    private final UserCachedDataManager cachedData;

    public User(UUID uuid, String name, LuckPermsPlugin plugin) {
        super(plugin);
        this.uuid = uuid;
        setName(name, false);

        this.primaryGroup = plugin.getConfiguration().get(ConfigKeys.PRIMARY_GROUP_CALCULATION).apply(this);

        this.cachedData = new UserCachedDataManager(this);
        getPlugin().getEventFactory().handleUserCacheLoad(this, this.cachedData);
    }

    public User(UUID uuid, LuckPermsPlugin plugin) {
        this(uuid, null, plugin);
    }

    @Override
    protected void invalidateCache() {
        super.invalidateCache();

        // invalidate our caches
        if (this.primaryGroup instanceof ContextualHolder) {
            ((ContextualHolder) this.primaryGroup).invalidateCache();
        }
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(this.name);
    }

    @Override
    public String getObjectName() {
        return this.uuid.toString();
    }

    @Override
    public UserIdentifier getId() {
        return UserIdentifier.of(this.uuid, this.name);
    }

    @Override
    public String getFormattedDisplayName() {
        return this.name != null ? this.name : this.uuid.toString();
    }

    @Override
    public String getPlainDisplayName() {
        return getFormattedDisplayName();
    }

    public ApiUser getApiDelegate() {
        return this.apiDelegate;
    }

    @Override
    public UserCachedDataManager getCachedData() {
        return this.cachedData;
    }

    public PrimaryGroupHolder getPrimaryGroup() {
        return this.primaryGroup;
    }

    /**
     * Sets the users name
     *
     * @param name the name to set
     * @param weak if true, the value will only be updated if a value hasn't been set previously.
     * @return true if a change was made
     */
    public boolean setName(String name, boolean weak) {
        if (name != null && name.length() > 16) {
            return false; // nope
        }

        // if weak is true, only update the value in the User if it's null
        if (weak && this.name != null) {

            // try to update casing if they're equalIgnoreCase
            if (this.name.equalsIgnoreCase(name)) {
                this.name = name;
            }

            return false;
        }

        // consistency. if the name being set is equivalent to null, just make it null.
        if (name != null && (name.isEmpty() || name.equalsIgnoreCase("null"))) {
            name = null;
        }

        // if one or the other is null, just update and return true
        if ((this.name == null) != (name == null)) {
            this.name = name;
            return true;
        }

        if (this.name == null) {
            // they're both null
            return false;
        } else {
            // both non-null
            if (this.name.equalsIgnoreCase(name)) {
                this.name = name; // update case anyway, but return false
                return false;
            } else {
                this.name = name;
                return true;
            }
        }
    }

    @Override
    public HolderType getType() {
        return HolderType.USER;
    }

    /**
     * Clear all of the users permission nodes
     */
    @Override
    public boolean clearEnduringNodes() {
        boolean ret = super.clearEnduringNodes();
        if (!ret) {
            return false;
        }

        getPlugin().getUserManager().giveDefaultIfNeeded(this, false);
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof User)) return false;
        final User other = (User) o;
        return this.uuid.equals(other.uuid);
    }

    @Override
    public int hashCode() {
        return this.uuid.hashCode();
    }

    @Override
    public String toString() {
        return "User(uuid=" + this.uuid + ")";
    }

}
