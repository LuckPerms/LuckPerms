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

package me.lucko.luckperms.bungee.migration;

import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.ChildCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.commands.migration.MigrationUtils;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.node.types.Prefix;
import me.lucko.luckperms.common.node.types.Suffix;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Iterators;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.common.util.ProgressLogger;

import net.alpenblock.bungeeperms.BungeePerms;
import net.alpenblock.bungeeperms.Group;
import net.alpenblock.bungeeperms.PermEntity;
import net.alpenblock.bungeeperms.Server;
import net.alpenblock.bungeeperms.World;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.event.cause.CreationCause;
import net.luckperms.api.model.data.DataType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MigrationBungeePerms extends ChildCommand<Object> {
    public MigrationBungeePerms() {
        super(CommandSpec.MIGRATION_COMMAND, "bungeeperms", CommandPermission.MIGRATION, Predicates.alwaysFalse());
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object ignored, ArgumentList args, String label) {
        ProgressLogger log = new ProgressLogger(Message.MIGRATION_LOG, Message.MIGRATION_LOG_PROGRESS, "BungeePerms");
        log.addListener(plugin.getConsoleSender());
        log.addListener(sender);

        log.log("Starting.");

        // Get BungeePerms instance
        BungeePerms bp = BungeePerms.getInstance();
        if (bp == null) {
            log.logError("Plugin not loaded.");
            return CommandResult.STATE_ERROR;
        }

        List<Group> groups = bp.getPermissionsManager().getBackEnd().loadGroups();

        log.log("Calculating group weightings.");
        int i = 0;
        for (Group group : groups) {
            i = Math.max(i, group.getRank());
        }
        int maxWeight = i + 5;

        // Migrate all groups.
        log.log("Starting group migration.");
        AtomicInteger groupCount = new AtomicInteger(0);
        Iterators.tryIterate(groups, g -> {
            int groupWeight = maxWeight - g.getRank();

            // Make a LuckPerms group for the one being migrated
            String groupName = MigrationUtils.standardizeName(g.getName());
            me.lucko.luckperms.common.model.Group group = plugin.getStorage().createAndLoadGroup(groupName, CreationCause.INTERNAL).join();

            MigrationUtils.setGroupWeight(group, groupWeight);
            migrateHolder(g, g.getInheritances(), groupWeight, group);

            plugin.getStorage().saveGroup(group);
            log.logAllProgress("Migrated {} groups so far.", groupCount.incrementAndGet());
        });
        log.log("Migrated " + groupCount.get() + " groups");

        // Migrate all users.
        log.log("Starting user migration.");
        AtomicInteger userCount = new AtomicInteger(0);

        // Increment the max weight from the group migrations. All user meta should override.
        int userWeight = maxWeight + 5;

        Iterators.tryIterate(bp.getPermissionsManager().getBackEnd().loadUsers(), u -> {
            if (u.getUUID() == null) {
                log.logError("Could not parse UUID for user: " + u.getName());
                return;
            }

            // Make a LuckPerms user for the one being migrated.
            me.lucko.luckperms.common.model.User user = plugin.getStorage().loadUser(u.getUUID(), u.getName()).join();

            migrateHolder(u, u.getGroupsString(), userWeight, user);

            plugin.getStorage().saveUser(user);
            plugin.getUserManager().getHouseKeeper().cleanup(user.getUniqueId());

            log.logProgress("Migrated {} users so far.", userCount.incrementAndGet(), ProgressLogger.DEFAULT_NOTIFY_FREQUENCY);
        });

        log.log("Migrated " + userCount.get() + " users.");
        log.log("Success! Migration complete.");
        log.log("Don't forget to remove the BungeePerms jar from your plugins folder & restart the server. " +
                "LuckPerms may not take over as the server permission handler until this is done.");
        return CommandResult.SUCCESS;
    }

    private static void migrateHolder(PermEntity entity, List<String> parents, int weight, PermissionHolder holder) {
        // Migrate global perms
        for (String perm : entity.getPerms()) {
            if (perm.isEmpty()) continue;
            holder.setNode(DataType.NORMAL, MigrationUtils.parseNode(perm, true).build(), true);
        }

        // Migrate per-server perms
        for (Map.Entry<String, Server> e : entity.getServers().entrySet()) {
            for (String perm : e.getValue().getPerms()) {
                if (perm.isEmpty()) continue;
                holder.setNode(DataType.NORMAL, MigrationUtils.parseNode(perm, true).withContext(DefaultContextKeys.SERVER_KEY, e.getKey()).build(), true);
            }

            // Migrate per-world perms
            for (Map.Entry<String, World> we : e.getValue().getWorlds().entrySet()) {
                for (String perm : we.getValue().getPerms()) {
                    if (perm.isEmpty()) continue;
                    holder.setNode(DataType.NORMAL, MigrationUtils.parseNode(perm, true).withContext(DefaultContextKeys.SERVER_KEY, e.getKey()).withContext(DefaultContextKeys.WORLD_KEY, we.getKey()).build(), true);
                }
            }
        }

        // Migrate any parent groups
        for (String inherit : parents) {
            if (inherit.isEmpty()) continue;
            holder.setNode(DataType.NORMAL, Inheritance.builder(MigrationUtils.standardizeName(inherit)).build(), true);
        }

        // Migrate prefix and suffix
        String prefix = entity.getPrefix();
        String suffix = entity.getSuffix();

        if (prefix != null && !prefix.isEmpty()) {
            holder.setNode(DataType.NORMAL, Prefix.builder(prefix, weight).build(), true);
        }
        if (suffix != null && !suffix.isEmpty()) {
            holder.setNode(DataType.NORMAL, Suffix.builder(suffix, weight).build(), true);
        }
    }
}
