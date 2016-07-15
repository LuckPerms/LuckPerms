package me.lucko.luckperms.tracks;

import lombok.Getter;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.groups.Group;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Track {

    /**
     * The name of the track
     */
    @Getter
    private final String name;

    /**
     * The groups within this track
     */
    private List<String> groups = new ArrayList<>();

    Track(String name) {
        this.name = name;
    }

    /**
     * Gets an ordered list of the groups on this track
     * @return am ordered {@link List} of the groups on this track
     */
    public List<String> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    public void setGroups(List<String> groups) {
        this.groups.clear();
        this.groups.addAll(groups);
    }

    /**
     * Gets the number of groups on this track
     * @return the number of groups on this track
     */
    public int getSize() {
        return groups.size();
    }

    /**
     * Gets the next group on the track, after the one provided
     * @param current the group before the group being requested
     * @return the group name, or null if the end of the track has been reached
     * @throws ObjectLacksException if the track does not contain the group given
     */
    public String getNext(Group current) throws ObjectLacksException {
        return getNext(current.getName());
    }

    /**
     * Gets the group before the group provided
     * @param current the group after the group being requested
     * @return the group name, or null if the start of the track has been reached
     * @throws ObjectLacksException if the track does not contain the group given
     */
    public String getPrevious(Group current) throws ObjectLacksException {
        return getPrevious(current.getName());
    }

    /**
     * Gets the next group on the track, after the one provided
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
     * @param group the group to append
     * @throws ObjectAlreadyHasException if the group is already on this track somewhere
     */
    public void appendGroup(Group group) throws ObjectAlreadyHasException {
        assertNotContains(group);
        groups.add(group.getName());
    }

    /**
     * Inserts a group at a certain position on this track
     * @param group the group to be inserted
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
     * @param group the group to remove
     * @throws ObjectLacksException if the group is not on this track
     */
    public void removeGroup(Group group) throws ObjectLacksException {
        removeGroup(group.getName());
    }

    /**
     * Removes a group from this track
     * @param group the group to remove
     * @throws ObjectLacksException if the group is not on this track
     */
    public void removeGroup(String group) throws ObjectLacksException {
        assertContains(group);
        groups.remove(group);
    }

    /**
     * Checks if a group features on this track
     * @param group the group to check
     * @return true if the group is on this track
     */
    public boolean containsGroup(Group group) {
        return containsGroup(group.getName());
    }

    /**
     * Checks if a group features on this track
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

    private void assertContains(Group g) throws ObjectLacksException {
        if (!containsGroup(g)) {
            throw new ObjectLacksException();
        }
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

    private void assertNotContains(String g) throws ObjectAlreadyHasException {
        if (containsGroup(g)) {
            throw new ObjectAlreadyHasException();
        }
    }

    @Override
    public String toString() {
        return name;
    }

}
