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

package me.lucko.luckperms.api.event.log;

import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.api.event.Cancellable;
import me.lucko.luckperms.api.event.LuckPermsEvent;

import javax.annotation.Nonnull;

/**
 * Called when a log entry is about to be sent to notifiable players on the platform
 */
public interface LogBroadcastEvent extends LuckPermsEvent, Cancellable {

    /**
     * Gets the log entry to be broadcasted
     *
     * @return the log entry to be broadcasted
     */
    @Nonnull
    LogEntry getEntry();

    /**
     * Gets where the log entry originated from.
     *
     * @return the origin of the log
     * @since 3.3
     */
    @Nonnull
    Origin getOrigin();

    /**
     * Represents where a log entry is from
     *
     * @since 3.3
     */
    enum Origin {

        /**
         * Marks a log entry which originated from the current server instance
         */
        LOCAL,

        /**
         * Marks a log entry which originated from an API call on the current server instance
         */
        LOCAL_API,

        /**
         * Marks a log entry which was sent to this server via the messaging service
         */
        REMOTE
    }

}
