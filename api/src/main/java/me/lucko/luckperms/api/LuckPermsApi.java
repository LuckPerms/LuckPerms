package me.lucko.luckperms.api;

import java.util.UUID;

@SuppressWarnings("unused")
public interface LuckPermsApi {

    void runUpdateTask();
    String getVersion();

    Datastore getDatastore();
    Logger getLogger();

    User getUser(UUID uuid);
    User getUser(String name);
    boolean isUserLoaded(UUID uuid);

    Group getGroup(String name);
    boolean isGroupLoaded(String name);

    Track getTrack(String name);
    boolean isTrackLoaded(String name);

}
