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

package net.luckperms.api.messaging;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A means to push changes to other servers using the platforms networking
 */
public interface MessagingService {

    /**
     * Gets the name of this messaging service
     *
     * @return the name of this messaging service
     */
    @NonNull String getName();

    /**
     * Uses the messaging service to inform other servers about a general
     * change.
     *
     * <p>The standard response by other servers will be to execute a overall
     * sync of all live data, equivalent to calling
     * {@link LuckPerms#runUpdateTask()}.</p>
     *
     * <p>This will push the update asynchronously, and this method will return
     * immediately. Note that this method will not cause an update to be
     * processed on the local server.</p>
     */
    void pushUpdate();

    /**
     * Uses the messaging service to inform other servers about a change to a
     * specific user.
     *
     * <p>The standard response by other servers is undefined, however the
     * current implementation will reload the corresponding users data if they
     * are online.</p>
     *
     * <p>This will push the update asynchronously, and this method will return
     * immediately. Note that this method will not cause an update to be
     * processed on the local server.</p>
     *
     * @param user the user to push the update for
     */
    void pushUserUpdate(@NonNull User user);

}
