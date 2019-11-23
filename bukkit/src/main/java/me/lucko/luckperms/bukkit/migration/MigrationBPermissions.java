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

import de.bananaco.bpermissions.api.Calculable;
import de.bananaco.bpermissions.api.CalculableType;
import de.bananaco.bpermissions.api.Permission;
import de.bananaco.bpermissions.api.World;
import de.bananaco.bpermissions.api.WorldManager;

import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.SubCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.commands.migration.MigrationUtils;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.command.CommandSpec;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.model.manager.group.GroupManager;
import me.lucko.luckperms.common.node.factory.NodeBuilders;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.node.types.Meta;
import me.lucko.luckperms.common.node.types.Prefix;
import me.lucko.luckperms.common.node.types.Suffix;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Iterators;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.common.util.ProgressLogger;

import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.event.cause.CreationCause;
import net.luckperms.api.model.data.DataType;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class MigrationBPermissions extends SubCommand<Object> {
    private static final Field UCONFIG_FIELD;
    static {
        try {
            UCONFIG_FIELD = Class.forName("de.bananaco.bpermissions.imp.YamlWorld").getDeclaredField("uconfig");
            UCONFIG_FIELD.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public MigrationBPermissions(LocaleManager locale) {
        super(CommandSpec.MIGRATION_COMMAND.localize(locale), "bpermissions", CommandPermission.MIGRATION, Predicates.alwaysFalse());
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object o, List<String> args, String label) {
        ProgressLogger log = new ProgressLogger(Message.MIGRATION_LOG, Message.MIGRATION_LOG_PROGRESS, "bPermissions");
        log.addListener(plugin.getConsoleSender());
        log.addListener(sender);

        log.log("Starting.");

        WorldManager worldManager = WorldManager.getInstance();
        if (worldManager == null) {
            log.logError("Plugin not loaded.");
            return CommandResult.STATE_ERROR;
        }

        log.log("Forcing the plugin to load all data. This could take a while.");
        for (World world : worldManager.getAllWorlds()) {
            log.log("Loading users in world " + world.getName());

            YamlConfiguration yamlWorldUsers = null;
            try {
                yamlWorldUsers = (YamlConfiguration) UCONFIG_FIELD.get(world);
            } catch (Throwable t) {
                t.printStackTrace();
            }

            if (yamlWorldUsers == null) {
                continue;
            }

            ConfigurationSection configSection = yamlWorldUsers.getConfigurationSection("users");
            if (configSection == null) {
                continue;
            }

            Set<String> users = configSection.getKeys(false);
            if (users == null) {
                log.logError("Couldn't get a list of users.");
                return CommandResult.FAILURE;
            }
            AtomicInteger userLoadCount = new AtomicInteger(0);
            for (String user : users) {
                world.loadOne(user, CalculableType.USER);
                log.logProgress("Forcefully loaded {} users so far.", userLoadCount.incrementAndGet(), ProgressLogger.DEFAULT_NOTIFY_FREQUENCY);
            }
        }
        log.log("Forcefully loaded all users.");

        // Migrate one world at a time.
        log.log("Starting world migration.");
        Iterators.tryIterate(worldManager.getAllWorlds(), world -> {
            log.log("Migrating world: " + world.getName());

            // Migrate all groups
            log.log("Starting group migration in world " + world.getName() + ".");
            AtomicInteger groupCount = new AtomicInteger(0);

            Iterators.tryIterate(world.getAll(CalculableType.GROUP), group -> {
                String groupName = MigrationUtils.standardizeName(group.getName());
                if (group.getName().equalsIgnoreCase(world.getDefaultGroup())) {
                    groupName = GroupManager.DEFAULT_GROUP_NAME;
                }

                // Make a LuckPerms group for the one being migrated.
                Group lpGroup = plugin.getStorage().createAndLoadGroup(groupName, CreationCause.INTERNAL).join();

                MigrationUtils.setGroupWeight(lpGroup, group.getPriority());
                migrateHolder(world, group, lpGroup);

                plugin.getStorage().saveGroup(lpGroup);

                log.logAllProgress("Migrated {} groups so far.", groupCount.incrementAndGet());
            });
            log.log("Migrated " + groupCount.get() + " groups in world " + world.getName() + ".");


            // Migrate all users
            log.log("Starting user migration in world " + world.getName() + ".");
            AtomicInteger userCount = new AtomicInteger(0);
            Iterators.tryIterate(world.getAll(CalculableType.USER), user -> {
                // There is no mention of UUIDs in the API. I assume that name = uuid. idk?
                UUID uuid = BukkitUuids.lookupUuid(log, user.getName());
                if (uuid == null) {
                    return;
                }

                // Make a LuckPerms user for the one being migrated.
                User lpUser = plugin.getStorage().loadUser(uuid, null).join();

                migrateHolder(world, user, lpUser);

                plugin.getStorage().saveUser(lpUser);
                plugin.getUserManager().getHouseKeeper().cleanup(lpUser.getUniqueId());

                log.logProgress("Migrated {} users so far.", userCount.incrementAndGet(), ProgressLogger.DEFAULT_NOTIFY_FREQUENCY);
            });

            log.log("Migrated " + userCount.get() + " users in world " + world.getName() + ".");
        });

        log.log("Success! Migration complete.");
        return CommandResult.SUCCESS;
    }

    private static void migrateHolder(World world, Calculable c, PermissionHolder holder) {
        // Migrate the groups permissions in this world
        for (Permission p : c.getPermissions()) {
            if (p.name().isEmpty()) {
                continue;
            }
            holder.setNode(DataType.NORMAL, NodeBuilders.determineMostApplicable(p.name()).value(p.isTrue()).withContext(DefaultContextKeys.SERVER_KEY, "global").withContext(DefaultContextKeys.WORLD_KEY, world.getName()).build(), true);

            // Include any child permissions
            for (Map.Entry<String, Boolean> child : p.getChildren().entrySet()) {
                if (child.getKey().isEmpty()) {
                    continue;
                }

                holder.setNode(DataType.NORMAL, NodeBuilders.determineMostApplicable(child.getKey()).value((boolean) child.getValue()).withContext(DefaultContextKeys.SERVER_KEY, "global").withContext(DefaultContextKeys.WORLD_KEY, world.getName()).build(), true);
            }
        }

        // Migrate any inherited groups
        c.getGroups().forEach(parent -> {
            String parentName = MigrationUtils.standardizeName(parent.getName());
            if (parent.getName().equalsIgnoreCase(world.getDefaultGroup())) {
                parentName = GroupManager.DEFAULT_GROUP_NAME;
            }

            holder.setNode(DataType.NORMAL, Inheritance.builder(parentName).value(true).withContext(DefaultContextKeys.SERVER_KEY, "global").withContext(DefaultContextKeys.WORLD_KEY, world.getName()).build(), true);
        });

        // Migrate existing meta
        for (Map.Entry<String, String> meta : c.getMeta().entrySet()) {
            if (meta.getKey().isEmpty() || meta.getValue().isEmpty()) {
                continue;
            }

            if (meta.getKey().equalsIgnoreCase("prefix")) {
                holder.setNode(DataType.NORMAL, Prefix.builder(meta.getValue(), c.getPriority()).withContext(DefaultContextKeys.WORLD_KEY, world.getName()).build(), true);
                continue;
            }

            if (meta.getKey().equalsIgnoreCase("suffix")) {
                holder.setNode(DataType.NORMAL, Suffix.builder(meta.getValue(), c.getPriority()).withContext(DefaultContextKeys.WORLD_KEY, world.getName()).build(), true);
                continue;
            }

            holder.setNode(DataType.NORMAL, Meta.builder(meta.getKey(), meta.getValue()).withContext(DefaultContextKeys.WORLD_KEY, world.getName()).build(), true);
        }
    }
}
