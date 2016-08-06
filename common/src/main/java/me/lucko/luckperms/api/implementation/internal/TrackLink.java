package me.lucko.luckperms.api.implementation.internal;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.Track;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.List;

import static me.lucko.luckperms.api.implementation.internal.Utils.checkGroup;

/**
 * Provides a link between {@link Track} and {@link me.lucko.luckperms.tracks.Track}
 */
@SuppressWarnings("unused")
@AllArgsConstructor
public class TrackLink implements Track {

    @NonNull
    @Getter(AccessLevel.PACKAGE)
    private final me.lucko.luckperms.tracks.Track master;

    @Override
    public String getName() {
        return master.getName();
    }

    @Override
    public List<String> getGroups() {
        return master.getGroups();
    }

    @Override
    public int getSize() {
        return master.getSize();
    }

    @Override
    public String getNext(@NonNull Group current) throws ObjectLacksException {
        checkGroup(current);
        return master.getNext(((GroupLink) current).getMaster());
    }

    @Override
    public String getPrevious(@NonNull Group current) throws ObjectLacksException {
        checkGroup(current);
        return master.getPrevious(((GroupLink) current).getMaster());
    }

    @Override
    public void appendGroup(@NonNull Group group) throws ObjectAlreadyHasException {
        checkGroup(group);
        master.appendGroup(((GroupLink) group).getMaster());
    }

    @Override
    public void insertGroup(@NonNull Group group, @NonNull int position) throws ObjectAlreadyHasException, IndexOutOfBoundsException {
        checkGroup(group);
        master.insertGroup(((GroupLink) group).getMaster(), position);
    }

    @Override
    public void removeGroup(@NonNull Group group) throws ObjectLacksException {
        checkGroup(group);
        master.removeGroup(((GroupLink) group).getMaster());
    }

    @Override
    public void removeGroup(@NonNull String group) throws ObjectLacksException {
        master.removeGroup(group);
    }

    @Override
    public boolean containsGroup(@NonNull Group group) {
        checkGroup(group);
        return master.containsGroup(((GroupLink) group).getMaster());
    }

    @Override
    public boolean containsGroup(@NonNull String group) {
        return master.containsGroup(group);
    }

    @Override
    public void clearGroups() {
        master.clearGroups();
    }
}
