/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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
import lombok.Setter;
import lombok.ToString;

import me.lucko.luckperms.api.caching.UserData;
import me.lucko.luckperms.api.event.events.UserPermissionRefreshEvent;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.api.delegate.UserDelegate;
import me.lucko.luckperms.common.caching.UserCache;
import me.lucko.luckperms.common.caching.handlers.HolderReference;
import me.lucko.luckperms.common.caching.handlers.UserReference;
import me.lucko.luckperms.common.core.UserIdentifier;
import me.lucko.luckperms.common.utils.BufferedRequest;
import me.lucko.luckperms.common.utils.Identifiable;

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
    @Getter
    @Setter
    private String name;

    /**
     * The users primary group
     */
    @Getter
    @Setter
    private String primaryGroup = null;

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

    public User(UUID uuid, LuckPermsPlugin plugin) {
        super(uuid.toString(), plugin);
        this.uuid = uuid;
        this.name = null;
    }

    public User(UUID uuid, String name, LuckPermsPlugin plugin) {
        super(uuid.toString(), plugin);
        this.uuid = uuid;
        this.name = name;
    }

    @Override
    public UserIdentifier getId() {
        return UserIdentifier.of(uuid, name);
    }

    @Override
    public String getFriendlyName() {
        return name;
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
        getPlugin().getApiProvider().fireEventAsync(new UserPermissionRefreshEvent(new UserDelegate(this)));
        getPlugin().onUserRefresh(this);
    }

    /**
     * Clear all of the users permission nodes
     */
    @Override
    public void clearNodes() {
        super.clearNodes();
        getPlugin().getUserManager().giveDefaultIfNeeded(this, false);
    }

    public void cleanup() {
        UserCache cache = userData;
        if (cache != null) {
            cache.cleanup();
        }
        forceCleanup();
    }
}
