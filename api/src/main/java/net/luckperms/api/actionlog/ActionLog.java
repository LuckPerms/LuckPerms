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

package net.luckperms.api.actionlog;

import net.luckperms.api.actionlog.filter.ActionFilter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.SortedSet;
import java.util.UUID;

/**
 * Represents the internal LuckPerms log.
 *
 * <p>The returned instance provides a copy of the data at the time of retrieval.</p>
 *
 * <p>Any changes made to log entries will only apply to this instance of the log.
 * You can add to the log using the {@link ActionLogger}, and then request an updated copy.</p>
 *
 * <p>All methods are thread safe, and return immutable and thread safe collections.</p>
 *
 * @deprecated Use {@link ActionLogger#queryActions(ActionFilter)} or
 * {@link ActionLogger#queryActions(ActionFilter, int, int)} instead.
 */
@Deprecated
public interface ActionLog {

    /**
     * Gets the {@link Action}s that make up this log.
     *
     * @return the content
     */
    @NonNull @Unmodifiable SortedSet<Action> getContent();

    /**
     * Gets the entries in the log performed by the given actor.
     *
     * @param actor the uuid of the actor to filter by
     * @return the content for the given actor
     */
    @NonNull @Unmodifiable SortedSet<Action> getContent(@NonNull UUID actor);

    /**
     * Gets the log content for a given user
     *
     * @param uniqueId the uuid to filter by
     * @return all content in this log where the user = uuid
     */
    @NonNull @Unmodifiable SortedSet<Action> getUserHistory(@NonNull UUID uniqueId);

    /**
     * Gets the log content for a given group
     *
     * @param name the name to filter by
     * @return all content in this log where the group = name
     */
    @NonNull @Unmodifiable SortedSet<Action> getGroupHistory(@NonNull String name);

    /**
     * Gets the log content for a given track
     *
     * @param name the name to filter by
     * @return all content in this log where the track = name
     */
    @NonNull @Unmodifiable SortedSet<Action> getTrackHistory(@NonNull String name);

}
