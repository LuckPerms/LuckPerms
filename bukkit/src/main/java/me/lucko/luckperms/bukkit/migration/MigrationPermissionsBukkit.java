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

import com.platymuus.bukkit.permissions.PermissionsPlugin;

import me.lucko.luckperms.api.event.cause.CreationCause;
import me.lucko.luckperms.common.commands.CommandPermission;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SubCommand;
import me.lucko.luckperms.common.commands.impl.migration.MigrationUtils;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.logging.ProgressLogger;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.NodeFactory;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.common.utils.SafeIterator;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class MigrationPermissionsBukkit extends SubCommand<Object> {
    public MigrationPermissionsBukkit(LocaleManager locale) {
        super(CommandSpec.MIGRATION_COMMAND.spec(locale), "permissionsbukkit", CommandPermission.MIGRATION, Predicates.alwaysFalse());
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object o, List<String> args, String label) {
        ProgressLogger log = new ProgressLogger("PermissionsBukkit");
        log.addListener(plugin.getConsoleSender());
        log.addListener(sender);

        log.log("Starting.");
        
        if (!Bukkit.getPluginManager().isPluginEnabled("PermissionsBukkit")) {
            log.logErr("Plugin not loaded.");
            return CommandResult.STATE_ERROR;
        }

        PermissionsPlugin permissionsBukkit = (PermissionsPlugin) Bukkit.getPluginManager().getPlugin("PermissionsBukkit");
        FileConfiguration config = permissionsBukkit.getConfig();

        // Migrate all groups
        log.log("Starting group migration.");
        AtomicInteger groupCount = new AtomicInteger(0);

        ConfigurationSection groupsSection = config.getConfigurationSection("groups");

        SafeIterator.iterate(groupsSection.getKeys(false), key -> {
            final String groupName = MigrationUtils.standardizeName(key);
            plugin.getStorage().createAndLoadGroup(groupName, CreationCause.INTERNAL).join();
            Group lpGroup = plugin.getGroupManager().getIfLoaded(groupName);

            // migrate data
            if (groupsSection.isConfigurationSection(key)) {
                migrate(lpGroup, groupsSection.getConfigurationSection(key));
            }

            plugin.getStorage().saveGroup(lpGroup).join();
            log.logAllProgress("Migrated {} groups so far.", groupCount.incrementAndGet());
        });
        log.log("Migrated " + groupCount.get() + " groups");

        // Migrate all users
        log.log("Starting user migration.");
        AtomicInteger userCount = new AtomicInteger(0);

        ConfigurationSection usersSection = config.getConfigurationSection("users");

        SafeIterator.iterate(usersSection.getKeys(false), key -> {
            UUID uuid = BukkitMigrationUtils.lookupUuid(log, key);
            if (uuid == null) {
                return;
            }

            plugin.getStorage().loadUser(uuid, null).join();
            User lpUser = plugin.getUserManager().getIfLoaded(uuid);

            // migrate data
            if (usersSection.isConfigurationSection(key)) {
                migrate(lpUser, usersSection.getConfigurationSection(key));
            }

            plugin.getUserManager().cleanup(lpUser);
            plugin.getStorage().saveUser(lpUser);
            log.logProgress("Migrated {} users so far.", userCount.incrementAndGet());
        });

        log.log("Migrated " + userCount.get() + " users.");
        log.log("Success! Migration complete.");
        return CommandResult.SUCCESS;
    }

    private static void migrate(PermissionHolder holder, ConfigurationSection data) {
        // migrate permissions
        if (data.isConfigurationSection("permissions")) {
            ConfigurationSection permsSection = data.getConfigurationSection("permissions");
            for (String perm : permsSection.getKeys(false)) {
                boolean value = permsSection.getBoolean(perm);
                holder.setPermission(MigrationUtils.parseNode(perm, value).build());
            }
        }

        if (data.isConfigurationSection("worlds")) {
            ConfigurationSection worldSection = data.getConfigurationSection("worlds");
            for (String world : worldSection.getKeys(false)) {
                if (worldSection.isConfigurationSection(world)) {
                    ConfigurationSection permsSection = worldSection.getConfigurationSection(world);
                    for (String perm : permsSection.getKeys(false)) {
                        boolean value = permsSection.getBoolean(perm);
                        holder.setPermission(MigrationUtils.parseNode(perm, value).setWorld(world).build());
                    }
                }
            }
        }

        // migrate parents
        if (data.isList("groups")) {
            List<String> groups = data.getStringList("groups");
            for (String group : groups) {
                holder.setPermission(NodeFactory.buildGroupNode(MigrationUtils.standardizeName(group)).build());
            }
        }
        if (data.isList("inheritance")) {
            List<String> groups = data.getStringList("inheritance");
            for (String group : groups) {
                holder.setPermission(NodeFactory.buildGroupNode(MigrationUtils.standardizeName(group)).build());
            }
        }
    }

}
