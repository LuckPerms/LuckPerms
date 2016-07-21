package me.lucko.luckperms.api.implementation;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.*;
import me.lucko.luckperms.api.implementation.internal.DatastoreLink;
import me.lucko.luckperms.api.implementation.internal.GroupLink;
import me.lucko.luckperms.api.implementation.internal.TrackLink;
import me.lucko.luckperms.api.implementation.internal.UserLink;

import java.util.UUID;

/**
 * Provides static access to LuckPerms
 */
@AllArgsConstructor
public class ApiProvider implements LuckPermsApi {
    private final LuckPermsPlugin plugin;

    @Override
    public void runUpdateTask() {
        plugin.runUpdateTask();
    }

    @Override
    public String getVersion() {
        return plugin.getVersion();
    }

    @Override
    public Datastore getDatastore() {
        return new DatastoreLink(plugin.getDatastore());
    }

    @Override
    public User getUser(@NonNull UUID uuid) {
        final me.lucko.luckperms.users.User user = plugin.getUserManager().getUser(uuid);
        if (user == null) {
            return null;
        }

        return new UserLink(user);
    }

    @Override
    public User getUser(@NonNull String name) {
        final me.lucko.luckperms.users.User user = plugin.getUserManager().getUser(name);
        if (user == null) {
            return null;
        }

        return new UserLink(user);
    }

    @Override
    public boolean isUserLoaded(@NonNull UUID uuid) {
        return plugin.getUserManager().isLoaded(uuid);
    }

    @Override
    public Group getGroup(@NonNull String name) {
        final me.lucko.luckperms.groups.Group group = plugin.getGroupManager().getGroup(name);
        if (group == null) {
            return null;
        }

        return new GroupLink(group);
    }

    @Override
    public boolean isGroupLoaded(@NonNull String name) {
        return plugin.getGroupManager().isLoaded(name);
    }

    @Override
    public Track getTrack(@NonNull String name) {
        final me.lucko.luckperms.tracks.Track track = plugin.getTrackManager().getTrack(name);
        if (track == null) {
            return null;
        }

        return new TrackLink(track);
    }

    @Override
    public boolean isTrackLoaded(@NonNull String name) {
        return plugin.getTrackManager().isLoaded(name);
    }
}
