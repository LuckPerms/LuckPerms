package me.lucko.luckperms.groups;

import lombok.Getter;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.utils.PermissionObject;

public class Group extends PermissionObject {

    /**
     * The name of the group
     */
    @Getter
    private final String name;

    Group(String name, LuckPermsPlugin plugin) {
        super(plugin, name);
        this.name = name;
    }

    /**
     * Clear all of the groups permission nodes
     */
    public void clearNodes() {
        getNodes().clear();
    }

    @Override
    public String toString() {
        return getName();
    }
}
