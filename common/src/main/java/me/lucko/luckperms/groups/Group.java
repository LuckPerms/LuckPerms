package me.lucko.luckperms.groups;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.utils.PermissionObject;

@ToString(of = {"name"})
@EqualsAndHashCode(of = {"name"}, callSuper = false)
public class Group extends PermissionObject {

    /**
     * The name of the group
     */
    @Getter
    private final String name;

    Group(String name, LuckPermsPlugin plugin) {
        super(name, plugin);
        this.name = name;
    }

    /**
     * Clear all of the groups permission nodes
     */
    public void clearNodes() {
        getNodes().clear();
    }
}
