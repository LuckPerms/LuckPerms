package me.lucko.luckperms.api;

/**
 * Wrapper interface for internal Group instances
 * The implementations of this interface limit access to the Group and add parameter checks to further prevent
 * errors and ensure all API interactions to not damage the state of the group.
 */
@SuppressWarnings("unused")
public interface Group extends PermissionObject {

    String getName();

    /**
     * Clear all of the groups permission nodes
     */
    void clearNodes();

}
