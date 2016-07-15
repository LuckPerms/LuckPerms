package me.lucko.luckperms.runnables;

import lombok.AllArgsConstructor;
import me.lucko.luckperms.LuckPermsPlugin;

@AllArgsConstructor
public class UpdateTask implements Runnable {
    private final LuckPermsPlugin plugin;

    @Override
    public void run() {
        plugin.getLogger().info("Running update task.");

        // Reload all of the groups
        plugin.getDatastore().loadAllGroups();
        String defaultGroup = plugin.getConfiguration().getDefaultGroupName();
        if (!plugin.getGroupManager().isLoaded(defaultGroup)) {
            plugin.getDatastore().createAndLoadGroup(defaultGroup);
        }

        // Reload all of the tracks
        plugin.getDatastore().loadAllTracks();

        // Refresh all online users.
        plugin.getUserManager().updateAllUsers();
    }
}
