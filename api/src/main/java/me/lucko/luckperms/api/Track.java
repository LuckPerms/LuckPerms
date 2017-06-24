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

import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An ordered collection of groups for easy promotions and demotions
 */
public interface Track {

    /**
     * Gets the name of this track
     * @return the name of this track
     */
    @Nonnull
    String getName();

    /**
     * Gets an ordered list of the groups on this track
     *
     * <p>Index 0 is the first/lowest group in (or start of) the track</p>
     *
     * @return an ordered {@link List} of the groups on this track
     */
    @Nonnull
    List<String> getGroups();

    /**
     * Gets the number of groups on this track
     *
     * @return the number of groups on this track
     */
    int getSize();

    /**
     * Gets the next group on the track, after the one provided
     *
     * @param current the group before the group being requested
     * @return the group name, or null if the end of the track has been reached
     * @throws ObjectLacksException  if the track does not contain the group given
     * @throws NullPointerException  if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     */
    @Nullable
    String getNext(@Nonnull Group current) throws ObjectLacksException;

    /**
     * Gets the previous group on the track, before the one provided
     *
     * @param current the group after the group being requested
     * @return the group name, or null if the start of the track has been reached
     * @throws ObjectLacksException  if the track does not contain the group given
     * @throws NullPointerException  if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     */
    @Nullable
    String getPrevious(@Nonnull Group current) throws ObjectLacksException;

    /**
     * Appends a group to the end of this track
     *
     * @param group the group to append
     * @throws ObjectAlreadyHasException if the group is already on this track somewhere
     * @throws NullPointerException      if the group is null
     * @throws IllegalStateException     if the group instance was not obtained from LuckPerms.
     */
    void appendGroup(@Nonnull Group group) throws ObjectAlreadyHasException;

    /**
     * Inserts a group at a certain position on this track
     *
     * @param group    the group to be inserted
     * @param position the index position (a value of 0 inserts at the start)
     * @throws ObjectAlreadyHasException if the group is already on this track somewhere
     * @throws IndexOutOfBoundsException if the position is less than 0 or greater than the size of the track
     * @throws NullPointerException      if the group is null
     * @throws IllegalStateException     if the group instance was not obtained from LuckPerms.
     */
    void insertGroup(@Nonnull Group group, int position) throws ObjectAlreadyHasException, IndexOutOfBoundsException;

    /**
     * Removes a group from this track
     *
     * @param group the group to remove
     * @throws ObjectLacksException  if the group is not on this track
     * @throws NullPointerException  if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     */
    void removeGroup(@Nonnull Group group) throws ObjectLacksException;

    /**
     * Removes a group from this track
     *
     * @param group the group to remove
     * @throws ObjectLacksException if the group is not on this track
     * @throws NullPointerException if the group is null
     */
    void removeGroup(@Nonnull String group) throws ObjectLacksException;

    /**
     * Checks if a group features on this track
     *
     * @param group the group to check
     * @return true if the group is on this track
     * @throws NullPointerException  if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     */
    boolean containsGroup(@Nonnull Group group);

    /**
     * Checks if a group features on this track
     *
     * @param group the group to check
     * @return true if the group is on this track
     * @throws NullPointerException if the group is null
     */
    boolean containsGroup(@Nonnull String group);

    /**
     * Clear all of the groups from this track
     */
    void clearGroups();

}
