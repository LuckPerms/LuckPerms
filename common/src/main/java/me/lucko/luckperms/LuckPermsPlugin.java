package me.lucko.luckperms;

import com.google.gson.Gson;
import me.lucko.luckperms.data.Datastore;
import me.lucko.luckperms.groups.GroupManager;
import me.lucko.luckperms.users.UserManager;
import me.lucko.luckperms.utils.LPConfiguration;

import java.util.UUID;
import java.util.logging.Logger;

public interface LuckPermsPlugin {

    /**
     * Retrieves the {@link UserManager} used to manage users and their permissions/groups
     * @return the {@link UserManager} object
     */
    UserManager getUserManager();

    /**
     * Retrieves the {@link GroupManager} used to manage the loaded groups and modify their permissions
     * @return the {@link GroupManager} object
     */
    GroupManager getGroupManager();

    /**
     * Retrieves the {@link LPConfiguration} for getting values from the config
     * @return the {@link LPConfiguration} implementation for the platform
     */
    LPConfiguration getConfiguration();

    /**
     * Retrieves the {@link Datastore} for loading/saving plugin data
     * @return the {@link Datastore} object
     */
    Datastore getDatastore();

    /**
     * Retrieves the {@link Logger} for the plugin
     * @return the plugin's {@link Logger}
     */
    Logger getLogger();

    /**
     * @return the version of the plugin
     */
    String getVersion();

    /**
     * Returns a colored string indicating the status of a player
     * @param uuid The player's uuid
     * @return a formatted status string
     */
    String getPlayerStatus(UUID uuid);

    /**
     * Gets the number of users online on the platform
     * @return the number of users
     */
    int getPlayerCount();

    /**
     * Runs an update task
     */
    void runUpdateTask();

    /**
     * Execute a runnable asynchronously
     * @param r the task to run
     */
    void doAsync(Runnable r);

    /**
     * Execute a runnable synchronously
     * @param r the task to run
     */
    void doSync(Runnable r);

    Gson getGson();
}
