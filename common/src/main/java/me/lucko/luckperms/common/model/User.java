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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.common.api.delegates.model.ApiUser;
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

@ToString(of = {"uuid"})
@EqualsAndHashCode(of = {"uuid"}, callSuper = false)
public class User extends PermissionHolder implements Identifiable<UserIdentifier> {

    /**
     * The users Mojang UUID
     */
    @Getter
    private final UUID uuid;

    /**
     * The last known username of a player
     */
    private String name = null;

    /**
     * The users primary group
     */
    @Getter
    private final PrimaryGroupHolder primaryGroup;

    /**
     * The users data cache instance
     */
    @Getter
    private final UserCachedData cachedData;

    @Getter
    private final BufferedRequest<Void> refreshBuffer;

    @Getter
    private final ApiUser delegate = new ApiUser(this);

    public User(UUID uuid, LuckPermsPlugin plugin) {
        super(uuid.toString(), plugin);
        this.uuid = uuid;

        this.refreshBuffer = new UserRefreshBuffer(plugin, this);
        this.primaryGroup = plugin.getConfiguration().get(ConfigKeys.PRIMARY_GROUP_CALCULATION).apply(this);

        this.cachedData = new UserCachedData(this);
        getPlugin().getApiProvider().getEventFactory().handleUserCacheLoad(this, cachedData);
    }

    public User(UUID uuid, String name, LuckPermsPlugin plugin) {
        super(uuid.toString(), plugin);
        this.uuid = uuid;
        setName(name, false);

        this.refreshBuffer = new UserRefreshBuffer(plugin, this);
        this.primaryGroup = plugin.getConfiguration().get(ConfigKeys.PRIMARY_GROUP_CALCULATION).apply(this);

        this.cachedData = new UserCachedData(this);
        getPlugin().getApiProvider().getEventFactory().handleUserCacheLoad(this, cachedData);
    }

    @Override
    public UserIdentifier getId() {
        return UserIdentifier.of(uuid, name);
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
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
        return name != null ? name : uuid.toString();
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
        refreshBuffer.requestDirectly();

        // pre-calc the allowall & global contexts
        // since contexts change so frequently, it's not worth trying to calculate any more than this.
        cachedData.preCalculate(Contexts.allowAll());
        cachedData.preCalculate(Contexts.global());
    }

    public CompletableFuture<Void> reloadCachedData() {
        return CompletableFuture.allOf(
                cachedData.reloadPermissions(),
                cachedData.reloadMeta()
        ).thenAccept(n -> getPlugin().getApiProvider().getEventFactory().handleUserDataRecalculate(this, cachedData));
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

    private static final class UserRefreshBuffer extends BufferedRequest<Void> {
        private final User user;

        private UserRefreshBuffer(LuckPermsPlugin plugin, User user) {
            super(50L, 5L, plugin.getScheduler().async());
            this.user = user;
        }

        @Override
        protected Void perform() {
            return user.reloadCachedData().join();
        }
    }

}
