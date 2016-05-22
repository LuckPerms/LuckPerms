package me.lucko.luckperms.groups;

import lombok.Getter;
import me.lucko.luckperms.LuckPermsPlugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GroupManager {
    private final LuckPermsPlugin plugin;

    /**
     * A {@link Map} containing all loaded groups
     */
    @Getter
    private final Map<String, Group> groups = new ConcurrentHashMap<>();

    public GroupManager(LuckPermsPlugin plugin) {
        this.plugin = plugin;
        loadAllGroups();
    }

    /**
     * Get a group object by name
     * @param name The name to search by
     * @return a {@link Group} object if the group is loaded, returns null if the group is not loaded
     */
    public Group getGroup(String name) {
        return groups.get(name);
    }

    /**
     * Add a group to the loaded groups map
     * @param group The group to add
     */
    public void setGroup(Group group) {
        groups.put(group.getName(), group);
    }

    /**
     * Updates (or sets if the group wasn't already loaded) a group in the groups map
     * @param group The group to update or set
     */
    public void updateOrSetGroup(Group group) {
        if (!groups.containsKey(group.getName())) {
            // The group isn't already loaded
            groups.put(group.getName(), group);
        } else {
            groups.get(group.getName()).setNodes(group.getNodes());
        }
    }

    /**
     * Check to see if a group is loaded or not
     * @param name The name of the group
     * @return true if the group is loaded
     */
    public boolean isLoaded(String name) {
        return groups.containsKey(name);
    }

    /**
     * Removes and unloads the group from the plugins internal storage
     * @param group The group to unload
     */
    public void unloadGroup(Group group) {
        if (group != null) {
            groups.remove(group.getName());
        }
    }

    /**
     * Load all groups from the datastore
     */
    public void loadAllGroups() {
        plugin.getDatastore().loadAllGroups(success -> {
            String defaultGroup = plugin.getConfiguration().getDefaultGroupName();

            if (!groups.keySet().contains(defaultGroup)) {
                plugin.getDatastore().createAndLoadGroup(defaultGroup, success1 -> {});
            }
        });
    }

    /**
     * Makes a new group object
     * @param name The name of the group
     * @return a new {@link Group} object
     */
    public Group makeGroup(String name) {
        return new Group(name, plugin);
    }
}