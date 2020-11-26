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
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.common.util.ProgressLogger;

import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.event.cause.CreationCause;
import net.luckperms.api.model.data.DataType;

import nl.svenar.PowerRanks.Cache.CachedPlayers;
import nl.svenar.PowerRanks.Cache.PowerConfigurationSection;
import nl.svenar.PowerRanks.Data.Users;
import nl.svenar.PowerRanks.PowerRanks;
import nl.svenar.PowerRanks.api.PowerRanksAPI;

import org.bukkit.Bukkit;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

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

        PowerRanks pr = (PowerRanks) Bukkit.getServer().getPluginManager().getPlugin("PowerRanks");
        PowerRanksAPI prApi = (pr).loadAPI();
        Users prUsers = new Users(pr);

        // Migrate all groups
        log.log("Starting groups migration.");
        Set<String> ranks = prApi.getRanks();
        AtomicInteger groupCount = new AtomicInteger(0);
        for (String rank : ranks) {
            Group group = plugin.getStorage().createAndLoadGroup(rank, CreationCause.INTERNAL).join();

            for (String node : prApi.getPermissions(rank)) {
                if (node.isEmpty()) continue;
                group.setNode(DataType.NORMAL, MigrationUtils.parseNode(node, true).build(), true);
            }

            for (String parent : prApi.getInheritances(rank)) {
                if (parent.isEmpty()) continue;
                group.setNode(DataType.NORMAL, Inheritance.builder(MigrationUtils.standardizeName(parent)).build(), true);
            }

            plugin.getStorage().saveGroup(group);
            log.logAllProgress("Migrated {} groups so far.", groupCount.incrementAndGet());
        }
        log.log("Migrated " + groupCount.get() + " groups");

        // Migrate all users
        log.log("Starting user migration.");
        Set<String> playerUuids = prUsers.getCachedPlayers();
        AtomicInteger userCount = new AtomicInteger(0);
        for (String uuidString : playerUuids) {
            UUID uuid = BukkitUuids.lookupUuid(log, uuidString);
            if (uuid == null) {
                continue;
            }

            User user = plugin.getStorage().loadUser(uuid, null).join();

            user.setNode(DataType.NORMAL, Inheritance.builder(CachedPlayers.getString("players." + uuidString + ".rank")).build(), true);

            final PowerConfigurationSection subGroups = CachedPlayers.getConfigurationSection("players." + uuidString + ".subranks");
            if (subGroups != null) {
                for (String subGroup : subGroups.getKeys(false)) {
                    Inheritance.Builder builder = Inheritance.builder(subGroup);
                    for (String worldName : CachedPlayers.getStringList("players." + uuidString + ".subranks." + subGroup + ".worlds")) {
                        if (!worldName.equalsIgnoreCase("all")) {
                            builder.withContext(DefaultContextKeys.WORLD_KEY, worldName);
                        }
                    }
                    user.setNode(DataType.NORMAL, builder.build(), true);
                }
            }

            for (String node : CachedPlayers.getStringList("players." + uuidString + ".permissions")) {
                if (node.isEmpty()) continue;
                user.setNode(DataType.NORMAL,  MigrationUtils.parseNode(node, true).build(), true);
            }

            user.getPrimaryGroup().setStoredValue(CachedPlayers.getString("players." + uuidString + ".rank"));

            plugin.getUserManager().getHouseKeeper().cleanup(user.getUniqueId());
            plugin.getStorage().saveUser(user);
            log.logAllProgress("Migrated {} users so far.", userCount.incrementAndGet());
        }

        log.log("Migrated " + userCount.get() + " users.");
        log.log("Success! Migration complete.");
        log.log("Don't forget to remove the PowerRanks jar from your plugins folder & restart the server. " +
                "LuckPerms may not take over as the server permission handler until this is done.");
        return CommandResult.SUCCESS;
    }
}
