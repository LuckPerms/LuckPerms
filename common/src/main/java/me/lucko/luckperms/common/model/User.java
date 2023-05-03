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
import net.kyori.adventure.text.Component;
import net.luckperms.api.query.QueryOptions;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import java.util.UUID;

public class User extends PermissionHolder {
    private final ApiUser apiProxy = new ApiUser(this);

    /**
     * The users Mojang UUID
     */
    private final UUID uniqueId;

    /**
     * The last known username of a player
     */
    private @Nullable String username = null;

    /**
     * The users primary group
     */
    private final PrimaryGroupHolder primaryGroup;

    /**
     * The users data cache instance
     */
    private final UserCachedDataManager cachedData;

    public User(UUID uniqueId, LuckPermsPlugin plugin) {
        super(plugin, uniqueId.toString());
        this.uniqueId = uniqueId;
        this.primaryGroup = plugin.getConfiguration().get(ConfigKeys.PRIMARY_GROUP_CALCULATION).apply(this);
        this.cachedData = new UserCachedDataManager(this);
        getPlugin().getEventDispatcher().dispatchUserCacheLoad(this, this.cachedData);
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public Optional<String> getUsername() {
        return Optional.ofNullable(this.username);
    }

    @Override
    public Component getFormattedDisplayName() {
        return Component.text(getPlainDisplayName());
    }

    @Override
    public String getPlainDisplayName() {
        return this.username != null ? this.username : this.uniqueId.toString();
    }

    @Override
    public QueryOptions getQueryOptions() {
        QueryOptions queryOptions = getPlugin().getQueryOptionsForUser(this).orElse(null);
        if (queryOptions != null) {
            return queryOptions;
        }

        return getPlugin().getContextManager().getStaticQueryOptions();
    }

    public ApiUser getApiProxy() {
        return this.apiProxy;
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
    public boolean setUsername(String name, boolean weak) {
        if (name != null && name.length() > 16) {
            return false; // nope
        }

        // if weak is true, only update the value in the User if it's null
        if (weak && this.username != null) {

            // try to update casing if they're equalIgnoreCase
            if (this.username.equalsIgnoreCase(name)) {
                this.username = name;
            }

            return false;
        }

        // consistency. if the name being set is equivalent to null, just make it null.
        if (name != null && (name.isEmpty() || name.equalsIgnoreCase("null"))) {
            name = null;
        }

        // if one or the other is null, just update and return true
        if ((this.username == null) != (name == null)) {
            this.username = name;
            return true;
        }

        if (this.username == null) {
            // they're both null
            return false;
        } else {
            // both non-null
            if (this.username.equalsIgnoreCase(name)) {
                this.username = name; // update case anyway, but return false
                return false;
            } else {
                this.username = name;
                return true;
            }
        }
    }

    @Override
    public HolderType getType() {
        return HolderType.USER;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof User)) return false;
        final User other = (User) o;
        return this.uniqueId.equals(other.uniqueId);
    }

    @Override
    public int hashCode() {
        return this.uniqueId.hashCode();
    }

    @Override
    public String toString() {
        return "User(uuid=" + this.uniqueId + ")";
    }

}
