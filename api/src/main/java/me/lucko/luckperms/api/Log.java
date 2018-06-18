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

package me.lucko.luckperms.api;

import java.util.SortedSet;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * Represents the internal LuckPerms log.
 *
 * <p>The returned instance provides a copy of the data at the time of retrieval.</p>
 *
 * <p>Any changes made to log entries will only apply to this instance of the log.
 * You can add to the log using the {@link Storage}, and then request an updated copy.</p>
 *
 * <p>All methods are thread safe, and return immutable and thread safe collections.</p>
 */
@Immutable
public interface Log {

    /**
     * Gets the {@link LogEntry}s that make up this log.
     *
     * @return the content
     */
    @Nonnull
    SortedSet<LogEntry> getContent();

    /**
     * Gets the entries in the log performed by the given actor.
     *
     * @param actor the uuid of the actor to filter by
     * @return the content for the given actor
     */
    @Nonnull
    SortedSet<LogEntry> getContent(@Nonnull UUID actor);

    /**
     * Gets the log content for a given user
     *
     * @param uuid the uuid to filter by
     * @return all content in this log where the user = uuid
     */
    @Nonnull
    SortedSet<LogEntry> getUserHistory(@Nonnull UUID uuid);

    /**
     * Gets the log content for a given group
     *
     * @param name the name to filter by
     * @return all content in this log where the group = name
     */
    @Nonnull
    SortedSet<LogEntry> getGroupHistory(@Nonnull String name);

    /**
     * Gets the log content for a given track
     *
     * @param name the name to filter by
     * @return all content in this log where the track = name
     */
    @Nonnull
    SortedSet<LogEntry> getTrackHistory(@Nonnull String name);

}
