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

package me.lucko.luckperms.api.event.user;

import me.lucko.luckperms.api.event.LuckPermsEvent;

import java.util.UUID;

import javax.annotation.Nonnull;

/**
 * Called when the user logs into the network for the first time.
 *
 * <p>Particularly useful for networks with multiple
 * lobbies, who want to welcome a user when they join for the first time.</p>
 *
 * <p>This event is fired before the player has actually joined the game on the async login / auth event. If you want to
 * do something with the user, store the UUID in a set, and then check the set in the PlayerJoinEvent o.e.</p>
 *
 * <p>The users data will not be loaded when this event is called.</p>
 */
public interface UserFirstLoginEvent extends LuckPermsEvent {

    /**
     * Gets the UUID of the user
     *
     * @return the uuid of the user
     */
    @Nonnull
    UUID getUuid();

    /**
     * Gets the username of the user
     *
     * @return the username of the user
     */
    @Nonnull
    String getUsername();

}
