package me.lucko.luckperms.api.implementation.internal;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.List;
import java.util.UUID;

/**
 * Provides a link between {@link User} and {@link me.lucko.luckperms.users.User}
 */
@SuppressWarnings("unused")
public class UserLink extends PermissionObjectLink implements User {

    @Getter(AccessLevel.PACKAGE)
    private final me.lucko.luckperms.users.User master;

    public UserLink(@NonNull me.lucko.luckperms.users.User master) {
        super(master);
        this.master = master;
    }

    @Override
    public UUID getUuid() {
        return master.getUuid();
    }

    @Override
    public String getName() {
        return master.getName();
    }

    @Override
    public String getPrimaryGroup() {
        return master.getPrimaryGroup();
    }

    @Override
    public void setPrimaryGroup(String s) throws ObjectAlreadyHasException {
        if (getPrimaryGroup().equalsIgnoreCase(s)) {
            throw new ObjectAlreadyHasException();
        }

        if (!getGroupNames().contains(s.toLowerCase())) {
            throw new IllegalStateException("User is not a member of that group.");
        }

        master.setPrimaryGroup(s.toLowerCase());
    }

    @Override
    public void refreshPermissions() {
        master.refreshPermissions();
    }

    @Override
    public boolean isInGroup(@NonNull Group group) {
        Utils.checkGroup(group);
        return master.isInGroup(((GroupLink) group).getMaster());
    }

    @Override
    public boolean isInGroup(@NonNull Group group, @NonNull String server) {
        Utils.checkGroup(group);
        return master.isInGroup(((GroupLink) group).getMaster(), server);
    }

    @Override
    public void addGroup(@NonNull Group group) throws ObjectAlreadyHasException {
        Utils.checkGroup(group);
        master.addGroup(((GroupLink) group).getMaster());
    }

    @Override
    public void addGroup(@NonNull Group group, @NonNull String server) throws ObjectAlreadyHasException {
        Utils.checkGroup(group);
        master.addGroup(((GroupLink) group).getMaster(), checkServer(server));
    }

    @Override
    public void addGroup(@NonNull Group group, @NonNull long expireAt) throws ObjectAlreadyHasException {
        Utils.checkGroup(group);
        master.addGroup(((GroupLink) group).getMaster(), checkTime(expireAt));
    }

    @Override
    public void addGroup(@NonNull Group group, @NonNull String server, @NonNull long expireAt) throws ObjectAlreadyHasException {
        Utils.checkGroup(group);
        master.addGroup(((GroupLink) group).getMaster(), checkServer(server), checkTime(expireAt));
    }

    @Override
    public void removeGroup(@NonNull Group group) throws ObjectLacksException {
        Utils.checkGroup(group);
        master.removeGroup(((GroupLink) group).getMaster());
    }

    @Override
    public void removeGroup(@NonNull Group group, @NonNull boolean temporary) throws ObjectLacksException {
        Utils.checkGroup(group);
        master.removeGroup(((GroupLink) group).getMaster(), temporary);
    }

    @Override
    public void removeGroup(@NonNull Group group, @NonNull String server) throws ObjectLacksException {
        Utils.checkGroup(group);
        master.removeGroup(((GroupLink) group).getMaster(), checkServer(server));
    }

    @Override
    public void removeGroup(@NonNull Group group, @NonNull String server, @NonNull boolean temporary) throws ObjectLacksException {
        Utils.checkGroup(group);
        master.removeGroup(((GroupLink) group).getMaster(), checkServer(server), temporary);
    }

    @Override
    public void clearNodes() {
        master.clearNodes();
    }

    @Override
    public List<String> getGroupNames() {
        return master.getGroupNames();
    }

    @Override
    public List<String> getLocalGroups(@NonNull String server) {
        return master.getLocalGroups(checkServer(server));
    }

}
