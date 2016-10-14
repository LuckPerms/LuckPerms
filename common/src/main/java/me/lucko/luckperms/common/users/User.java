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

package me.lucko.luckperms.common.users;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.lucko.luckperms.api.event.events.UserPermissionRefreshEvent;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.api.internal.UserLink;
import me.lucko.luckperms.common.caching.UserData;
import me.lucko.luckperms.common.core.PermissionHolder;
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

    @Getter
    private UserData userData = null;

    protected User(UUID uuid, LuckPermsPlugin plugin) {
        super(uuid.toString(), plugin);
        this.uuid = uuid;
        this.name = null;
    }

    protected User(UUID uuid, String name, LuckPermsPlugin plugin) {
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

    /**
     * Sets up the UserData cache
     * Blocking call.
     */
    public void setupData(boolean op) {
        if (userData != null) {
            throw new IllegalStateException("Data already setup");
        }

        userData = new UserData(this, getPlugin().getCalculatorFactory());
        userData.preCalculate(getPlugin().getPreProcessContexts(op));
    }

    /**
     * Removes the UserData cache from this user
     */
    public void unregisterData() {
        if (userData != null) {
            userData.invalidateCache();
            userData = null;
        }
    }

    /**
     * Refresh and re-assign the users permissions
     * Blocking call.
     */
    public synchronized void refreshPermissions() {
        if (userData == null) {
            return;
        }

        UserData ud = userData;
        ud.recalculatePermissions();
        ud.recalculateMeta();
        getPlugin().getApiProvider().fireEventAsync(new UserPermissionRefreshEvent(new UserLink(this)));
    }

    /**
     * Clear all of the users permission nodes
     */
    @Override
    public void clearNodes() {
        super.clearNodes();
        getPlugin().getUserManager().giveDefaultIfNeeded(this, false);
    }
}
