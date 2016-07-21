package me.lucko.luckperms.api;

import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.List;

/**
 * Wrapper interface for internal Track instances
 * The implementations of this interface limit access to the Track and add parameter checks to further prevent
 * errors and ensure all API interactions to not damage the state of the track.
 */
@SuppressWarnings("unused")
public interface Track {

    String getName();

    /**
     * Gets an ordered list of the groups on this track
     * @return am ordered {@link List} of the groups on this track
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
     */
    String getNext(Group current) throws ObjectLacksException;

    /**
     * Gets the group before the group provided
     * @param current the group after the group being requested
     * @return the group name, or null if the start of the track has been reached
     * @throws ObjectLacksException if the track does not contain the group given
     */
    String getPrevious(Group current) throws ObjectLacksException;

    /**
     * Appends a group to the end of this track
     * @param group the group to append
     * @throws ObjectAlreadyHasException if the group is already on this track somewhere
     */
    void appendGroup(Group group) throws ObjectAlreadyHasException;

    /**
     * Inserts a group at a certain position on this track
     * @param group the group to be inserted
     * @param position the index position (a value of 0 inserts at the start)
     * @throws ObjectAlreadyHasException if the group is already on this track somewhere
     * @throws IndexOutOfBoundsException if the position is less than 0 or greater than the size of the track
     */
    void insertGroup(Group group, int position) throws ObjectAlreadyHasException, IndexOutOfBoundsException;

    /**
     * Removes a group from this track
     * @param group the group to remove
     * @throws ObjectLacksException if the group is not on this track
     */
    void removeGroup(Group group) throws ObjectLacksException;

    /**
     * Removes a group from this track
     * @param group the group to remove
     * @throws ObjectLacksException if the group is not on this track
     */
    void removeGroup(String group) throws ObjectLacksException;

    /**
     * Checks if a group features on this track
     * @param group the group to check
     * @return true if the group is on this track
     */
    boolean containsGroup(Group group);

    /**
     * Checks if a group features on this track
     * @param group the group to check
     * @return true if the group is on this track
     */
    boolean containsGroup(String group);

    /**
     * Clear all of the groups within this track
     */
    void clearGroups();

}
