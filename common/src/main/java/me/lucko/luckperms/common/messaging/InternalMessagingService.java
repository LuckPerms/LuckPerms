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

package me.lucko.luckperms.common.messaging;

import me.lucko.luckperms.common.cache.BufferedRequest;
import me.lucko.luckperms.common.model.User;
import net.luckperms.api.actionlog.Action;
import net.luckperms.api.messenger.Messenger;
import net.luckperms.api.messenger.MessengerProvider;

import java.util.concurrent.CompletableFuture;

public interface InternalMessagingService {

    /**
     * Gets the name of this messaging service
     *
     * @return the name of this messaging service
     */
    String getName();

    Messenger getMessenger();

    MessengerProvider getMessengerProvider();

    /**
     * Closes the messaging service
     */
    void close();

    /**
     * Gets the buffer for sending updates to other servers
     *
     * @return the update buffer
     */
    BufferedRequest<Void> getUpdateBuffer();

    /**
     * Uses the messaging service to inform other servers about a general
     * change.
     */
    CompletableFuture<Void> pushUpdate();

    /**
     * Pushes an update for a specific user.
     *
     * @param user the user
     */
    CompletableFuture<Void> pushUserUpdate(User user);

    /**
     * Pushes a log entry to connected servers.
     *
     * @param logEntry the log entry
     */
    CompletableFuture<Void> pushLog(Action logEntry);

    /**
     * Pushes a custom payload to connected servers.
     *
     * @param channelId the channel id
     * @param payload the payload
     */
    CompletableFuture<Void> pushCustomPayload(String channelId, String payload);

}
