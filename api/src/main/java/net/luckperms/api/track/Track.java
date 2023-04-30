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

package net.luckperms.api.track;

import net.luckperms.api.context.ContextSet;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/**
 * An ordered chain of {@link Group}s.
 */
public interface Track {

    /**
     * Gets the name of this track
     *
     * @return the name of this track
     */
    @NonNull String getName();

    /**
     * Gets a list of the groups on this track
     *
     * <p>Index 0 is the first/lowest group in (or start of) the track.</p>
     *
     * <p>The returned collection is immutable, and cannot be modified.</p>
     *
     * @return an ordered {@link List} of the groups on this track
     */
    @NonNull @Unmodifiable List<String> getGroups();

    /**
     * Gets the next group on the track, after the one provided
     *
     * <p>{@code null} is returned if the group is not on the track.</p>
     *
     * @param current the group before the group being requested
     * @return the group name, or null if the end of the track has been reached
     * @throws NullPointerException  if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     */
    @Nullable String getNext(@NonNull Group current);

    /**
     * Gets the previous group on the track, before the one provided
     *
     * <p>{@code null} is returned if the group is not on the track.</p>
     *
     * @param current the group after the group being requested
     * @return the group name, or null if the start of the track has been reached
     * @throws NullPointerException  if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     */
    @Nullable String getPrevious(@NonNull Group current);

    /**
     * Promotes the given user along this track.
     *
     * @param user       the user to promote
     * @param contextSet the contexts to promote the user in
     * @return the result of the action
     */
    @NonNull PromotionResult promote(@NonNull User user, @NonNull ContextSet contextSet);

    /**
     * Demotes the given user along this track.
     *
     * @param user       the user to demote
     * @param contextSet the contexts to demote the user in
     * @return the result of the action
     */
    @NonNull DemotionResult demote(@NonNull User user, @NonNull ContextSet contextSet);

    /**
     * Appends a group to the end of this track
     *
     * @param group the group to append
     * @return the result of the operation
     * @throws NullPointerException  if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     */
    @NonNull DataMutateResult appendGroup(@NonNull Group group);

    /**
     * Inserts a group at a certain position on this track
     *
     * @param group    the group to be inserted
     * @param position the index position (a value of 0 inserts at the start)
     * @return the result of the operation
     * @throws IndexOutOfBoundsException if the position is less than 0 or greater than the size of the track
     * @throws NullPointerException      if the group is null
     * @throws IllegalStateException     if the group instance was not obtained from LuckPerms.
     */
    @NonNull DataMutateResult insertGroup(@NonNull Group group, int position) throws IndexOutOfBoundsException;

    /**
     * Removes a group from this track
     *
     * @param group the group to remove
     * @return the result of the operation
     * @throws NullPointerException  if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     */
    @NonNull DataMutateResult removeGroup(@NonNull Group group);

    /**
     * Removes a group from this track
     *
     * @param group the group to remove
     * @return the result of the operation
     * @throws NullPointerException if the group is null
     */
    @NonNull DataMutateResult removeGroup(@NonNull String group);

    /**
     * Checks if a group features on this track
     *
     * @param group the group to check
     * @return true if the group is on this track
     * @throws NullPointerException  if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     */
    boolean containsGroup(@NonNull Group group);

    /**
     * Checks if a group features on this track
     *
     * @param group the group to check
     * @return true if the group is on this track
     * @throws NullPointerException if the group is null
     */
    boolean containsGroup(@NonNull String group);

    /**
     * Clear all of the groups from this track
     */
    void clearGroups();

}
