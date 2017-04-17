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

package me.lucko.luckperms.common.core.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import me.lucko.luckperms.api.caching.UserData;
import me.lucko.luckperms.common.api.delegates.UserDelegate;
import me.lucko.luckperms.common.caching.UserCache;
import me.lucko.luckperms.common.caching.handlers.HolderReference;
import me.lucko.luckperms.common.caching.handlers.UserReference;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.core.UserIdentifier;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.primarygroup.PrimaryGroupHolder;
import me.lucko.luckperms.common.utils.BufferedRequest;
import me.lucko.luckperms.common.utils.Identifiable;

import java.util.Optional;
import java.util.UUID;

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
     * The users data cache instance, if present.
     */
    @Getter
    private UserCache userData = null;

    @Getter
    private BufferedRequest<Void> refreshBuffer = new BufferedRequest<Void>(1000L, r -> getPlugin().doAsync(r)) {
        @Override
        protected Void perform() {
            refreshPermissions();
            return null;
        }
    };

    @Getter
    private final UserDelegate delegate = new UserDelegate(this);

    public User(UUID uuid, LuckPermsPlugin plugin) {
        super(uuid.toString(), plugin);
        this.uuid = uuid;
        this.primaryGroup = plugin.getConfiguration().get(ConfigKeys.PRIMARY_GROUP_CALCULATION).apply(this);
    }

    public User(UUID uuid, String name, LuckPermsPlugin plugin) {
        super(uuid.toString(), plugin);
        this.uuid = uuid;
        setName(name, true);

        this.primaryGroup = plugin.getConfiguration().get(ConfigKeys.PRIMARY_GROUP_CALCULATION).apply(this);
    }

    @Override
    public UserIdentifier getId() {
        return UserIdentifier.of(uuid, name);
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public boolean setName(String name, boolean force) {
        // if the value being set is null
        if (name == null || name.equalsIgnoreCase("null") || name.isEmpty()) {
            // only apply the change if it is being forced
            if (force) {
                // if the name is already null, return false
                if (this.name == null) {
                    return false;
                } else {
                    // set the new null value
                    this.name = null;
                    return true;
                }
            } else {
                // we already have a non-null value, so return false
                return false;
            }
        } else {
            // the name being set is not null
            if (this.name == null) {
                this.name = name;
                return true;
            }

            // update the capitalisation, but still return false
            if (this.name.equalsIgnoreCase(name)) {
                this.name = name;
                return false;
            }

            // completely new value, just set & return true
            this.name = name;
            return true;
        }
    }

    @Override
    public String getFriendlyName() {
        return name != null ? name : uuid.toString();
    }

    @Override
    public HolderReference<UserIdentifier> toReference() {
        return UserReference.of(getId());
    }

    /**
     * Sets up the UserData cache
     * Blocking call.
     */
    public synchronized void setupData(boolean op) {
        if (userData != null) {
            return;
        }

        userData = new UserCache(this, getPlugin().getCalculatorFactory());
        userData.preCalculate(getPlugin().getPreProcessContexts(op));

        getPlugin().getApiProvider().getEventFactory().handleUserCacheLoad(this, userData);
        getPlugin().onUserRefresh(this);
    }

    /**
     * Removes the UserData cache from this user
     */
    public void unregisterData() {
        userData = null;
    }

    /**
     * Refresh and re-assign the users permissions
     * Blocking call.
     */
    private synchronized void refreshPermissions() {
        UserData ud = userData;
        if (ud == null) {
            return;
        }

        ud.recalculatePermissions();
        ud.recalculateMeta();
        getPlugin().getApiProvider().getEventFactory().handleUserDataRecalculate(this, ud);
        getPlugin().onUserRefresh(this);
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

    public void cleanup() {
        UserCache cache = userData;
        if (cache != null) {
            cache.cleanup();
        }
    }
}
