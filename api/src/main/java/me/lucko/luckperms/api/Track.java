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

package me.lucko.luckperms.api;

import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.List;

/**
 * Wrapper interface for internal Track instances
 */
@SuppressWarnings("unused")
public interface Track {

    /**
     * @return the name of this track
     */
    String getName();

    /**
     * Gets an ordered list of the groups on this track
     * Index 0 is the first/lowest group in (or start of) the track
     * @return an ordered {@link List} of the groups on this track
     */
    List<String> getGroups();

    /**
     * Gets the number of groups on this track
     * @return the number of groups on this track
     */
    int getSize();

    /**
     * Gets the next group on the track, after the one provided
     * @param current the group before the group being requested
     * @return the group name, or null if the end of the track has been reached
     * @throws ObjectLacksException if the track does not contain the group given
     * @throws NullPointerException if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     */
    String getNext(Group current) throws ObjectLacksException;

    /**
     * Gets the previous group on the track, before the one provided
     * @param current the group after the group being requested
     * @return the group name, or null if the start of the track has been reached
     * @throws ObjectLacksException if the track does not contain the group given
     * @throws NullPointerException if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     */
    String getPrevious(Group current) throws ObjectLacksException;

    /**
     * Appends a group to the end of this track
     * @param group the group to append
     * @throws ObjectAlreadyHasException if the group is already on this track somewhere
     * @throws NullPointerException if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     */
    void appendGroup(Group group) throws ObjectAlreadyHasException;

    /**
     * Inserts a group at a certain position on this track
     * @param group the group to be inserted
     * @param position the index position (a value of 0 inserts at the start)
     * @throws ObjectAlreadyHasException if the group is already on this track somewhere
     * @throws IndexOutOfBoundsException if the position is less than 0 or greater than the size of the track
     * @throws NullPointerException if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     */
    void insertGroup(Group group, int position) throws ObjectAlreadyHasException, IndexOutOfBoundsException;

    /**
     * Removes a group from this track
     * @param group the group to remove
     * @throws ObjectLacksException if the group is not on this track
     * @throws NullPointerException if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     */
    void removeGroup(Group group) throws ObjectLacksException;

    /**
     * Removes a group from this track
     * @param group the group to remove
     * @throws ObjectLacksException if the group is not on this track
     * @throws NullPointerException if the group is null
     */
    void removeGroup(String group) throws ObjectLacksException;

    /**
     * Checks if a group features on this track
     * @param group the group to check
     * @return true if the group is on this track
     * @throws NullPointerException if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     */
    boolean containsGroup(Group group);

    /**
     * Checks if a group features on this track
     * @param group the group to check
     * @return true if the group is on this track
     * @throws NullPointerException if the group is null
     */
    boolean containsGroup(String group);

    /**
     * Clear all of the groups within this track
     */
    void clearGroups();

}
