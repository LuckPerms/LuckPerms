package me.lucko.luckperms.api.implementation.internal;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import me.lucko.luckperms.api.Group;

/**
 * Provides a link between {@link Group} and {@link me.lucko.luckperms.groups.Group}
 */
@SuppressWarnings("unused")
public class GroupLink extends PermissionObjectLink implements Group {

    @Getter(AccessLevel.PACKAGE)
    private final me.lucko.luckperms.groups.Group master;

    public GroupLink(@NonNull me.lucko.luckperms.groups.Group master) {
        super(master);
        this.master = master;
    }

    @Override
    public String getName() {
        return master.getName();
    }

    @Override
    public void clearNodes() {
        master.clearNodes();
    }
}
