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

package net.luckperms.api.event.player;

import net.luckperms.api.event.LuckPermsEvent;
import net.luckperms.api.event.util.Param;
import net.luckperms.api.model.user.User;
import net.luckperms.api.util.Result;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.UUID;

/**
 * Called when LuckPerms has finished processing a Player's initial connection.
 *
 * <p>This event will always execute during the platforms async connection
 * event. The LuckPerms platform listener processing the connection will block
 * while this event is posted.</p>
 *
 * <p>This, among other things, allows you to wait until permission data is
 * loaded for a User during the BungeeCord 'LoginEvent', as event priorities are
 * ignored by the current implementation.</p>
 *
 * <p>The implementation will make an attempt to ensure this event is called
 * for all connections, even if the operation to load User data was not
 * successful. Note that LuckPerms will usually cancel the platform connection
 * event if data could not be loaded.</p>
 */
public interface PlayerLoginProcessEvent extends LuckPermsEvent, Result {

    /**
     * Gets the UUID of the connection which was processed
     *
     * @return the uuid of the connection which was processed
     */
    @Param(0)
    @NonNull UUID getUniqueId();

    /**
     * Gets the username of the connection which was processed
     *
     * @return the username of the connection which was processed
     */
    @Param(1)
    @NonNull String getUsername();

    /**
     * Gets if the login was processed successfully.
     *
     * @return true if the login was successful
     */
    @Override
    default boolean wasSuccessful() {
        return getUser() != null;
    }

    /**
     * Gets the resultant User instance which was loaded.
     *
     * <p>Returns {@code null} if the login was not processed
     * {@link #wasSuccessful() successfully.}</p>
     *
     * @return the user instance
     */
    @Param(2)
    @Nullable User getUser();

}
