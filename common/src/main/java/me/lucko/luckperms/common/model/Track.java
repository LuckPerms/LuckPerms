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

package me.lucko.luckperms.common.model;

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.api.DataMutateResult;
import me.lucko.luckperms.common.api.delegates.model.ApiTrack;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.references.Identifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class Track implements Identifiable<String> {

    /**
     * The name of the track
     */
    private final String name;

    private final LuckPermsPlugin plugin;

    private final Lock ioLock = new ReentrantLock();

    /**
     * The groups within this track
     */
    private final List<String> groups = Collections.synchronizedList(new ArrayList<>());

    private final ApiTrack delegate = new ApiTrack(this);

    public Track(String name, LuckPermsPlugin plugin) {
        this.name = name;
        this.plugin = plugin;
    }

    public String getName() {
        return this.name;
    }

    public Lock getIoLock() {
        return this.ioLock;
    }

    public ApiTrack getDelegate() {
        return this.delegate;
    }

    @Override
    public String getId() {
        return this.name.toLowerCase();
    }

    /**
     * Gets an ordered list of the groups on this track
     *
     * @return am ordered {@link List} of the groups on this track
     */
    public List<String> getGroups() {
        return ImmutableList.copyOf(this.groups);
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
        return this.groups.size();
    }

    /**
     * Gets the next group on the track, after the one provided
     *
     * @param current the group before the group being requested
     * @return the group name, or null if the end of the track has been reached
     * @throws IllegalArgumentException if the track does not contain the group given
     */
    public String getNext(Group current) throws IllegalArgumentException {
        return getNext(current.getName());
    }

    /**
     * Gets the group before the group provided
     *
     * @param current the group after the group being requested
     * @return the group name, or null if the start of the track has been reached
     * @throws IllegalArgumentException if the track does not contain the group given
     */
    public String getPrevious(Group current) throws IllegalArgumentException {
        return getPrevious(current.getName());
    }

    /**
     * Gets the next group on the track, after the one provided
     *
     * @param current the group before the group being requested
     * @return the group name, or null if the end of the track has been reached
     * @throws IllegalArgumentException if the track does not contain the group given
     */
    public String getNext(String current) throws IllegalArgumentException {
        if (!containsGroup(current)) {
            throw new IllegalArgumentException();
        }

        if (this.groups.indexOf(current) == this.groups.size() - 1) {
            return null;
        }

        return this.groups.get(this.groups.indexOf(current) + 1);
    }

    /**
     * Gets the group before the group provided
     *
     * @param current the group after the group being requested
     * @return the group name, or null if the start of the track has been reached
     * @throws IllegalArgumentException if the track does not contain the group given
     */
    public String getPrevious(String current) throws IllegalArgumentException {
        if (!containsGroup(current)) {
            throw new IllegalArgumentException();
        }

        if (this.groups.indexOf(current) == 0) {
            return null;
        }

        return this.groups.get(this.groups.indexOf(current) - 1);
    }

    /**
     * Appends a group to the end of this track
     *
     * @param group the group to append
     * @return the result of the operation
     */
    public DataMutateResult appendGroup(Group group) {
        if (containsGroup(group)) {
            return DataMutateResult.ALREADY_HAS;
        }

        List<String> before = ImmutableList.copyOf(this.groups);
        this.groups.add(group.getName());
        List<String> after = ImmutableList.copyOf(this.groups);

        this.plugin.getEventFactory().handleTrackAddGroup(this, group.getName(), before, after);
        return DataMutateResult.SUCCESS;
    }

    /**
     * Inserts a group at a certain position on this track
     *
     * @param group    the group to be inserted
     * @param position the index position (a value of 0 inserts at the start)
     * @throws IndexOutOfBoundsException if the position is less than 0 or greater than the size of the track
     * @return the result of the operation
     */
    public DataMutateResult insertGroup(Group group, int position) throws IndexOutOfBoundsException {
        if (containsGroup(group)) {
            return DataMutateResult.ALREADY_HAS;
        }

        List<String> before = ImmutableList.copyOf(this.groups);
        this.groups.add(position, group.getName());
        List<String> after = ImmutableList.copyOf(this.groups);

        this.plugin.getEventFactory().handleTrackAddGroup(this, group.getName(), before, after);
        return DataMutateResult.SUCCESS;
    }

    /**
     * Removes a group from this track
     *
     * @param group the group to remove
     * @return the result of the operation
     */
    public DataMutateResult removeGroup(Group group) {
        return removeGroup(group.getName());
    }

    /**
     * Removes a group from this track
     *
     * @param group the group to remove
     * @return the result of the operation
     */
    public DataMutateResult removeGroup(String group) {
        if (!containsGroup(group)) {
            return DataMutateResult.LACKS;
        }

        List<String> before = ImmutableList.copyOf(this.groups);
        this.groups.remove(group);
        List<String> after = ImmutableList.copyOf(this.groups);

        this.plugin.getEventFactory().handleTrackRemoveGroup(this, group, before, after);
        return DataMutateResult.SUCCESS;
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
        return this.groups.contains(group);
    }

    /**
     * Clear all of the groups within this track
     */
    public void clearGroups() {
        List<String> before = ImmutableList.copyOf(this.groups);
        this.groups.clear();
        this.plugin.getEventFactory().handleTrackClear(this, before);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Track)) return false;
        final Track other = (Track) o;
        return this.name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public String toString() {
        return "Track(name=" + this.name + ", groups=" + this.getGroups() + ")";
    }

}
