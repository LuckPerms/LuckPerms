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

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.common.buffers.BufferedRequest;
import me.lucko.luckperms.common.caching.UserCachedData;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.primarygroup.PrimaryGroupHolder;
import me.lucko.luckperms.common.references.HolderType;
import me.lucko.luckperms.common.references.Identifiable;
import me.lucko.luckperms.common.references.UserIdentifier;
import me.lucko.luckperms.common.references.UserReference;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class User extends PermissionHolder implements Identifiable<UserIdentifier> {

    /**
     * The users Mojang UUID
     */
    private final UUID uuid;

    /**
     * The last known username of a player
     */
    private String name = null;

    /**
     * The users primary group
     */
    private final PrimaryGroupHolder primaryGroup;

    /**
     * The users data cache instance
     */
    private final UserCachedData cachedData;

    private final BufferedRequest<Void> refreshBuffer;

    public User(UUID uuid, String name, LuckPermsPlugin plugin) {
        super(uuid.toString(), plugin);
        this.uuid = uuid;
        setName(name, false);

        this.refreshBuffer = new UserRefreshBuffer(plugin, this);
        this.primaryGroup = plugin.getConfiguration().get(ConfigKeys.PRIMARY_GROUP_CALCULATION).apply(this);

        this.cachedData = new UserCachedData(this);
        getPlugin().getEventFactory().handleUserCacheLoad(this, this.cachedData);
    }

    public User(UUID uuid, LuckPermsPlugin plugin) {
        this(uuid, null, plugin);
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public PrimaryGroupHolder getPrimaryGroup() {
        return this.primaryGroup;
    }

    @Override
    public UserCachedData getCachedData() {
        return this.cachedData;
    }

    @Override
    public BufferedRequest<Void> getRefreshBuffer() {
        return this.refreshBuffer;
    }

    @Override
    public UserIdentifier getId() {
        return UserIdentifier.of(this.uuid, this.name);
    }

    public Optional<String> getName() {
        return Optional.ofNullable(this.name);
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
            if (name != null && this.name.equalsIgnoreCase(name)) {
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
    public String getFriendlyName() {
        return this.name != null ? this.name : this.uuid.toString();
    }

    @Override
    public UserReference toReference() {
        return UserReference.of(getId());
    }

    @Override
    public HolderType getType() {
        return HolderType.USER;
    }

    /**
     * Sets up the UserData cache
     * Blocking call.
     */
    public void preCalculateData() {
        // first try to refresh any existing permissions
        this.refreshBuffer.requestDirectly();

        // pre-calc the allowall & global contexts
        // since contexts change so frequently, it's not worth trying to calculate any more than this.
        this.cachedData.preCalculate(Contexts.allowAll());
        this.cachedData.preCalculate(Contexts.global());
    }

    public CompletableFuture<Void> reloadCachedData() {
        return CompletableFuture.allOf(
                this.cachedData.reloadPermissions(),
                this.cachedData.reloadMeta()
        ).thenAccept(n -> getPlugin().getEventFactory().handleUserDataRecalculate(this, this.cachedData));
    }

    /**
     * Clear all of the users permission nodes
     */
    @Override
    public boolean clearNodes() {
        boolean ret = super.clearNodes();
        if (!ret) {
            return false;
        }

        getPlugin().getUserManager().giveDefaultIfNeeded(this, false);
        return true;
    }

    public void clearNodes(boolean giveDefault) {
        super.clearNodes();
        if (giveDefault) {
            getPlugin().getUserManager().giveDefaultIfNeeded(this, false);
        }
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

    private static final class UserRefreshBuffer extends BufferedRequest<Void> {
        private final User user;

        private UserRefreshBuffer(LuckPermsPlugin plugin, User user) {
            super(50L, 5L, plugin.getScheduler().async());
            this.user = user;
        }

        @Override
        protected Void perform() {
            return this.user.reloadCachedData().join();
        }
    }

}
