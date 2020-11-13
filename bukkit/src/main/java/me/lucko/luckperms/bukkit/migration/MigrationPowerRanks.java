/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.bukkit.migration;

import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.ChildCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.commands.migration.MigrationUtils;
import me.lucko.luckperms.common.node.factory.NodeBuilders;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.common.util.ProgressLogger;
import me.lucko.luckperms.common.util.Uuids;

import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.event.cause.CreationCause;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeBuilder;

import nl.svenar.PowerRanks.PowerRanks;
import nl.svenar.PowerRanks.api.PowerRanksAPI;
import nl.svenar.PowerRanks.Data.Users;
import nl.svenar.PowerRanks.Cache.CachedPlayers;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
Migrate PowerRanks v1 to LuckPerms
*/
public class MigrationPowerRanks extends ChildCommand<Object> {

    public MigrationPowerRanks() {
        super(CommandSpec.MIGRATION_COMMAND, "powerranks", CommandPermission.MIGRATION, Predicates.alwaysFalse());
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object ignored, ArgumentList args, String label) {
        ProgressLogger log = new ProgressLogger(Message.MIGRATION_LOG, Message.MIGRATION_LOG_PROGRESS, "PowerRanks");
        log.addListener(plugin.getConsoleSender());
        log.addListener(sender);

        log.log("Starting.");

        if (!Bukkit.getPluginManager().isPluginEnabled("PowerRanks")) {
            log.logError("Plugin not loaded.");
            return CommandResult.STATE_ERROR;
        }

        PowerRanksAPI prAPI = ((PowerRanks) Bukkit.getServer().getPluginManager().getPlugin("PowerRanks")).loadAPI();
	    Users users = new Users(prAPI.plugin);

        log.log("Starting ranks migration.");

        Set<String> ranks = prAPI.getRanks();
        String default_rank = users.getDefaultRanks();
        AtomicInteger rankCount = new AtomicInteger(0);

        for (String rank : ranks) {
            Group lp_group = plugin.getStorage().createAndLoadGroup(rank, CreationCause.INTERNAL).join();

            for (String node : prAPI.getPermissions(rank)) {
                if (node.isEmpty()) continue;
                lp_group.setNode(DataType.NORMAL, 
                MigrationUtils.parseNode(node.replaceFirst("-", ""), !node.startsWith("-")).build(), true);
            }

            for (String s : prAPI.getInheritances(rank)) {
                if (s.isEmpty()) continue;
                lp_group.setNode(DataType.NORMAL, Inheritance.builder(MigrationUtils.standardizeName(s)).build(), true);
            }

            plugin.getStorage().saveGroup(lp_group);
            log.logAllProgress("Migrated {} ranks so far.", rankCount.incrementAndGet());
        }

        log.log(String.format("Migrated %d ranks.", rankCount.get()));
        
        log.log("Starting player migration.");
        
        Set<String> players = users.getCachedPlayers();
        AtomicInteger playerCount = new AtomicInteger(0);

        for (String player_uuid : players) {
            UUID uuid = BukkitUuids.lookupUuid(log, player_uuid);
            if (uuid == null) {
                continue;
            }

            User lp_user = plugin.getStorage().loadUser(uuid, null).join();

            lp_user.setNode(DataType.NORMAL, Inheritance.builder(CachedPlayers.getString("players." + player_uuid + ".rank")).build(), true);
            
            try {
                for (String subrank_name : CachedPlayers.getConfigurationSection("players." + player_uuid + ".subranks").getKeys(false)) {
                    NodeBuilder<?, ?> ib = Inheritance.builder(subrank_name);

                    for (String world_name : CachedPlayers.getStringList("players." + player_uuid + ".subranks." + subrank_name + ".worlds")) {
                        if (world_name.equalsIgnoreCase("all")) {
                            break;
                        } else {
                            ib.withContext(DefaultContextKeys.WORLD_KEY, world_name);
                        }
                    }

                    lp_user.setNode(DataType.NORMAL, ib.build(), true);
                }
            } catch (Exception e) {/* Such emptyness. '.subranks' might not be an configuration section and will fail, but that is not an issue. */}

            for (String permission_node : CachedPlayers.getStringList("players." + player_uuid + ".permissions")) {
                if (permission_node.isEmpty()) continue;
                lp_user.setNode(DataType.NORMAL,  MigrationUtils.parseNode(permission_node.replaceFirst("-", ""), !permission_node.startsWith("-")).build(), true);
            }

            lp_user.getPrimaryGroup().setStoredValue(CachedPlayers.getString("players." + player_uuid + ".rank"));

            plugin.getUserManager().getHouseKeeper().cleanup(lp_user.getUniqueId());
            plugin.getStorage().saveUser(lp_user);

            log.logAllProgress("Migrated {} players so far.", playerCount.incrementAndGet());
        }

        log.log(String.format("Migrated %d players.", playerCount.get()));
        
        log.log("Success! Migration complete.");
        log.log("Don't forget to remove the PowerRanks jar from your plugins folder & restart the server. " +
                "LuckPerms may not take over as the server permission handler until this is done.");
        return CommandResult.SUCCESS;
    }
}
