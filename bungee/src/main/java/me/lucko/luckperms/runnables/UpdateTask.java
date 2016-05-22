package me.lucko.luckperms.runnables;

import lombok.AllArgsConstructor;
import me.lucko.luckperms.LPBungeePlugin;
import net.md_5.bungee.api.connection.ProxiedPlayer;

@AllArgsConstructor
public class UpdateTask implements Runnable {
    private final LPBungeePlugin plugin;

    @Override
    public void run() {
        plugin.getLogger().info("Running update task.");

        // Re-load all of the groups
        plugin.getGroupManager().loadAllGroups();

        // Refresh all online users.
        for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
            plugin.getDatastore().loadUser(p.getUniqueId(), success -> {});
        }
    }
}
