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

package me.lucko.luckperms.common.core.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.common.utils.Identifiable;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@ToString
@EqualsAndHashCode(of = {"name"})
@RequiredArgsConstructor
public class Track implements Identifiable<String> {

    /**
     * The name of the track
     */
    @Getter
    private final String name;
    @Getter
    private final Lock ioLock = new ReentrantLock();
    /**
     * The groups within this track
     */
    private List<String> groups = Collections.synchronizedList(new ArrayList<>());

    @Override
    public String getId() {
        return name.toLowerCase();
    }

    /**
     * Gets an ordered list of the groups on this track
     *
     * @return am ordered {@link List} of the groups on this track
     */
    public List<String> getGroups() {
        return ImmutableList.copyOf(groups);
    }

    public void setGroups(List<String> groups) {
        this.groups.clear();
        this.groups.addAll(groups);
    }

    /**
     * Gets the number of groups on this track
     *
     * @return the number of groups on this track
     */
    public int getSize() {
        return groups.size();
    }

    /**
     * Gets the next group on the track, after the one provided
     *
     * @param current the group before the group being requested
     * @return the group name, or null if the end of the track has been reached
     * @throws ObjectLacksException if the track does not contain the group given
     */
    public String getNext(Group current) throws ObjectLacksException {
        return getNext(current.getName());
    }

    /**
     * Gets the group before the group provided
     *
     * @param current the group after the group being requested
     * @return the group name, or null if the start of the track has been reached
     * @throws ObjectLacksException if the track does not contain the group given
     */
    public String getPrevious(Group current) throws ObjectLacksException {
        return getPrevious(current.getName());
    }

    /**
     * Gets the next group on the track, after the one provided
     *
     * @param current the group before the group being requested
     * @return the group name, or null if the end of the track has been reached
     * @throws ObjectLacksException if the track does not contain the group given
     */
    public String getNext(String current) throws ObjectLacksException {
        assertContains(current);

        if (groups.indexOf(current) == groups.size() - 1) {
            return null;
        }

        return groups.get(groups.indexOf(current) + 1);
    }

    /**
     * Gets the group before the group provided
     *
     * @param current the group after the group being requested
     * @return the group name, or null if the start of the track has been reached
     * @throws ObjectLacksException if the track does not contain the group given
     */
    public String getPrevious(String current) throws ObjectLacksException {
        assertContains(current);

        if (groups.indexOf(current) == 0) {
            return null;
        }

        return groups.get(groups.indexOf(current) - 1);
    }

    /**
     * Appends a group to the end of this track
     *
     * @param group the group to append
     * @throws ObjectAlreadyHasException if the group is already on this track somewhere
     */
    public void appendGroup(Group group) throws ObjectAlreadyHasException {
        assertNotContains(group);
        groups.add(group.getName());
    }

    /**
     * Inserts a group at a certain position on this track
     *
     * @param group    the group to be inserted
     * @param position the index position (a value of 0 inserts at the start)
     * @throws ObjectAlreadyHasException if the group is already on this track somewhere
     * @throws IndexOutOfBoundsException if the position is less than 0 or greater than the size of the track
     */
    public void insertGroup(Group group, int position) throws ObjectAlreadyHasException, IndexOutOfBoundsException {
        assertNotContains(group);
        groups.add(position, group.getName());
    }

    /**
     * Removes a group from this track
     *
     * @param group the group to remove
     * @throws ObjectLacksException if the group is not on this track
     */
    public void removeGroup(Group group) throws ObjectLacksException {
        removeGroup(group.getName());
    }

    /**
     * Removes a group from this track
     *
     * @param group the group to remove
     * @throws ObjectLacksException if the group is not on this track
     */
    public void removeGroup(String group) throws ObjectLacksException {
        assertContains(group);
        groups.remove(group);
    }

    /**
     * Checks if a group features on this track
     *
     * @param group the group to check
     * @return true if the group is on this track
     */
    public boolean containsGroup(Group group) {
        return containsGroup(group.getName());
    }

    /**
     * Checks if a group features on this track
     *
     * @param group the group to check
     * @return true if the group is on this track
     */
    public boolean containsGroup(String group) {
        return groups.contains(group);
    }

    /**
     * Clear all of the groups within this track
     */
    public void clearGroups() {
        groups.clear();
    }

    private void assertNotContains(Group g) throws ObjectAlreadyHasException {
        if (containsGroup(g)) {
            throw new ObjectAlreadyHasException();
        }
    }

    private void assertContains(String g) throws ObjectLacksException {
        if (!containsGroup(g)) {
            throw new ObjectLacksException();
        }
    }
}
