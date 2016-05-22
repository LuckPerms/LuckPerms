package me.lucko.luckperms.runnables;

import lombok.AllArgsConstructor;
import me.lucko.luckperms.LPBukkitPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

@AllArgsConstructor
public class UpdateTask extends BukkitRunnable {
    private final LPBukkitPlugin plugin;

    @Override
    public void run() {
        plugin.getLogger().info("Running update task.");

        // Re-load all of the groups
        plugin.getGroupManager().loadAllGroups();

        // Refresh all online users.
        for (Player p : Bukkit.getOnlinePlayers()) {
            plugin.getDatastore().loadUser(p.getUniqueId(), success -> {});
        }
    }
}
