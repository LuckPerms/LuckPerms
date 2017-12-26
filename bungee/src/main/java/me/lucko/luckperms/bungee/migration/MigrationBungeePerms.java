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

import me.lucko.luckperms.api.event.cause.CreationCause;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandPermission;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SubCommand;
import me.lucko.luckperms.common.commands.impl.migration.MigrationUtils;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.logging.ProgressLogger;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.node.NodeFactory;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.common.utils.SafeIterator;

import net.alpenblock.bungeeperms.BungeePerms;
import net.alpenblock.bungeeperms.Group;
import net.alpenblock.bungeeperms.PermEntity;
import net.alpenblock.bungeeperms.Server;
import net.alpenblock.bungeeperms.World;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MigrationBungeePerms extends SubCommand<Object> {
    public MigrationBungeePerms(LocaleManager locale) {
        super(CommandSpec.MIGRATION_COMMAND.spec(locale), "bungeeperms", CommandPermission.MIGRATION, Predicates.alwaysFalse());
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object o, List<String> args, String label) throws CommandException {
        ProgressLogger log = new ProgressLogger("BungeePerms");
        log.addListener(plugin.getConsoleSender());
        log.addListener(sender);

        log.log("Starting.");

        // Get BungeePerms instance
        BungeePerms bp = BungeePerms.getInstance();
        if (bp == null) {
            log.logErr("Plugin not loaded.");
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
        SafeIterator.iterate(groups, g -> {
            int groupWeight = maxWeight - g.getRank();

            // Make a LuckPerms group for the one being migrated
            String groupName = MigrationUtils.standardizeName(g.getName());
            plugin.getStorage().createAndLoadGroup(groupName, CreationCause.INTERNAL).join();
            me.lucko.luckperms.common.model.Group group = plugin.getGroupManager().getIfLoaded(groupName);

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

        SafeIterator.iterate(bp.getPermissionsManager().getBackEnd().loadUsers(), u -> {
            if (u.getUUID() == null) {
                log.logErr("Could not parse UUID for user: " + u.getName());
                return;
            }

            // Make a LuckPerms user for the one being migrated.
            plugin.getStorage().loadUser(u.getUUID(), u.getName()).join();
            me.lucko.luckperms.common.model.User user = plugin.getUserManager().getIfLoaded(u.getUUID());

            migrateHolder(u, u.getGroupsString(), userWeight, user);

            plugin.getStorage().saveUser(user);
            plugin.getUserManager().cleanup(user);

            log.logProgress("Migrated {} users so far.", userCount.incrementAndGet());
        });

        log.log("Migrated " + userCount.get() + " users.");
        log.log("Success! Migration complete.");
        return CommandResult.SUCCESS;
    }

    private static void migrateHolder(PermEntity entity, List<String> parents, int weight, PermissionHolder holder) {
        // Migrate global perms
        for (String perm : entity.getPerms()) {
            if (perm.isEmpty()) continue;
            holder.setPermission(MigrationUtils.parseNode(perm, true).build());
        }

        // Migrate per-server perms
        for (Map.Entry<String, Server> e : entity.getServers().entrySet()) {
            for (String perm : e.getValue().getPerms()) {
                if (perm.isEmpty()) continue;
                holder.setPermission(MigrationUtils.parseNode(perm, true).setServer(e.getKey()).build());
            }

            // Migrate per-world perms
            for (Map.Entry<String, World> we : e.getValue().getWorlds().entrySet()) {
                for (String perm : we.getValue().getPerms()) {
                    if (perm.isEmpty()) continue;
                    holder.setPermission(MigrationUtils.parseNode(perm, true).setServer(e.getKey()).setWorld(we.getKey()).build());
                }
            }
        }

        // Migrate any parent groups
        for (String inherit : parents) {
            if (inherit.isEmpty()) continue;
            holder.setPermission(NodeFactory.buildGroupNode(MigrationUtils.standardizeName(inherit)).build());
        }

        // Migrate prefix and suffix
        String prefix = entity.getPrefix();
        String suffix = entity.getSuffix();

        if (prefix != null && !prefix.isEmpty()) {
            holder.setPermission(NodeFactory.buildPrefixNode(weight, prefix).build());
        }
        if (suffix != null && !suffix.isEmpty()) {
            holder.setPermission(NodeFactory.buildSuffixNode(weight, suffix).build());
        }
    }
}
