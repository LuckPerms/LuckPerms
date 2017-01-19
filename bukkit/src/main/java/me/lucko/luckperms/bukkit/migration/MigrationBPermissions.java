/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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
import de.bananaco.bpermissions.api.Group;
import de.bananaco.bpermissions.api.Permission;
import de.bananaco.bpermissions.api.World;
import de.bananaco.bpermissions.api.WorldManager;

import me.lucko.luckperms.api.MetaUtils;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.constants.Constants;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.core.model.PermissionHolder;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static me.lucko.luckperms.common.constants.Permission.MIGRATION;

public class MigrationBPermissions extends SubCommand<Object> {
    private static Field uConfigField;
    private static Method getConfigurationSectionMethod = null;
    private static Method getKeysMethod = null;

    static {
        try {
            uConfigField = Class.forName("de.bananaco.bpermissions.imp.YamlWorld").getDeclaredField("uconfig");
            uConfigField.setAccessible(true);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private static Set<String> getUsers(World world) {
        try {
            Object yamlWorldUsers = uConfigField.get(world);
            if (getConfigurationSectionMethod == null) {
                getConfigurationSectionMethod = yamlWorldUsers.getClass().getMethod("getConfigurationSection", String.class);
                getConfigurationSectionMethod.setAccessible(true);
            }

            Object configSection = getConfigurationSectionMethod.invoke(yamlWorldUsers, "users");
            if (configSection == null) {
                return Collections.emptySet();
            }

            if (getKeysMethod == null) {
                getKeysMethod = configSection.getClass().getMethod("getKeys", boolean.class);
                getKeysMethod.setAccessible(true);
            }

            return (Set<String>) getKeysMethod.invoke(configSection, false);
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    private static void migrateHolder(LuckPermsPlugin plugin, World world, Calculable c, PermissionHolder holder) {
        // Migrate the groups permissions in this world
        for (Permission p : c.getPermissions()) {
            try {
                holder.setPermission(p.name(), p.isTrue(), "global", world.getName());
                LogEntry.build()
                        .actor(Constants.CONSOLE_UUID).actorName(Constants.CONSOLE_NAME)
                        .acted(holder).action("set " + p.name() + " " + p.isTrue() + " global " + world.getName())
                        .build().submit(plugin);
            } catch (Exception ex) {
                if (!(ex instanceof ObjectAlreadyHasException)) {
                    ex.printStackTrace();
                }
            }

            // Include any child permissions
            for (Map.Entry<String, Boolean> child : p.getChildren().entrySet()) {
                try {
                    holder.setPermission(child.getKey(), child.getValue(), "global", world.getName());
                    LogEntry.build()
                            .actor(Constants.CONSOLE_UUID).actorName(Constants.CONSOLE_NAME)
                            .acted(holder).action("set " + child.getKey() + " " + child.getValue() + " global " + world.getName())
                            .build().submit(plugin);
                } catch (Exception ex) {
                    if (!(ex instanceof ObjectAlreadyHasException)) {
                        ex.printStackTrace();
                    }
                }
            }
        }

        // Migrate any inherited groups
        for (Group parent : c.getGroups()) {
            try {
                holder.setPermission("group." + parent.getName(), true, "global", world.getName());
                LogEntry.build()
                        .actor(Constants.CONSOLE_UUID).actorName(Constants.CONSOLE_NAME)
                        .acted(holder).action("setinherit " + parent.getName() + " global " + world.getName())
                        .build().submit(plugin);
            } catch (Exception ex) {
                if (!(ex instanceof ObjectAlreadyHasException)) {
                    ex.printStackTrace();
                }
            }
        }

        // Migrate existing meta
        for (Map.Entry<String, String> meta : c.getMeta().entrySet()) {
            if (meta.getKey().equalsIgnoreCase("prefix") || meta.getKey().equalsIgnoreCase("suffix")) {
                String chatMeta = MetaUtils.escapeCharacters(meta.getValue());
                try {
                    holder.setPermission(meta.getKey().toLowerCase() + "." + c.getPriority() + "." + chatMeta, true);
                    LogEntry.build()
                            .actor(Constants.CONSOLE_UUID).actorName(Constants.CONSOLE_NAME)
                            .acted(holder).action("set " + meta.getKey().toLowerCase() + "." + c.getPriority() + "." + chatMeta + " true")
                            .build().submit(plugin);
                } catch (Exception ex) {
                    if (!(ex instanceof ObjectAlreadyHasException)) {
                        ex.printStackTrace();
                    }
                }
                continue;
            }

            try {
                holder.setPermission("meta." + meta.getKey() + "." + meta.getValue(), true, "global", world.getName());
                LogEntry.build()
                        .actor(Constants.CONSOLE_UUID).actorName(Constants.CONSOLE_NAME)
                        .acted(holder).action("set meta." + meta.getKey() + "." + meta.getValue() + " true global " + world.getName())
                        .build().submit(plugin);
            } catch (Exception ex) {
                if (!(ex instanceof ObjectAlreadyHasException)) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public MigrationBPermissions() {
        super("bpermissions", "Migration from bPermissions", MIGRATION, Predicates.alwaysFalse(), null);
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object o, List<String> args, String label) throws CommandException {
        Consumer<String> log = s -> {
            Message.MIGRATION_LOG.send(sender, s);
            Message.MIGRATION_LOG.send(plugin.getConsoleSender(), s);
        };
        log.accept("Starting bPermissions migration.");

        WorldManager worldManager = WorldManager.getInstance();
        if (worldManager == null) {
            log.accept("Error -> bPermissions is not loaded.");
            return CommandResult.STATE_ERROR;
        }

        log.accept("Forcing the plugin to load all data. This could take a while.");
        for (World world : worldManager.getAllWorlds()) {
            Set<String> users = getUsers(world);
            if (users == null) {
                log.accept("Couldn't get a list of users.");
                return CommandResult.FAILURE;
            }
            users.forEach(s -> world.loadOne(s, CalculableType.USER));
        }

        // Migrate one world at a time.
        log.accept("Starting world migration.");
        for (World world : worldManager.getAllWorlds()) {
            log.accept("Migrating world: " + world.getName());

            // Migrate all groups
            log.accept("Starting group migration in world " + world.getName() + ".");
            int groupCount = 0;
            for (Calculable group : world.getAll(CalculableType.GROUP)) {
                groupCount++;
                String groupName = group.getName().toLowerCase();
                if (group.getName().equalsIgnoreCase(world.getDefaultGroup())) {
                    groupName = "default";
                }

                // Make a LuckPerms group for the one being migrated.
                plugin.getStorage().createAndLoadGroup(groupName).join();
                me.lucko.luckperms.common.core.model.Group lpGroup = plugin.getGroupManager().getIfLoaded(groupName);
                try {
                    LogEntry.build()
                            .actor(Constants.CONSOLE_UUID).actorName(Constants.CONSOLE_NAME)
                            .acted(lpGroup).action("create")
                            .build().submit(plugin);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                migrateHolder(plugin, world, group, lpGroup);
                plugin.getStorage().saveGroup(lpGroup);
            }
            log.accept("Migrated " + groupCount + " groups in world " + world.getName() + ".");

            // Migrate all users
            log.accept("Starting user migration in world " + world.getName() + ".");
            int userCount = 0;
            for (Calculable user : world.getAll(CalculableType.USER)) {
                userCount++;

                // There is no mention of UUIDs in the API. I assume that name = uuid. idk?
                UUID uuid;
                try {
                    uuid = UUID.fromString(user.getName());
                } catch (IllegalArgumentException e) {
                    uuid = plugin.getUUID(user.getName());
                }

                if (uuid == null) {
                    log.accept("Unable to migrate user " + user.getName() + ". Unable to get UUID.");
                    continue;
                }

                // Make a LuckPerms user for the one being migrated.
                plugin.getStorage().loadUser(uuid, "null").join();
                User lpUser = plugin.getUserManager().get(uuid);

                migrateHolder(plugin, world, user, lpUser);

                plugin.getStorage().saveUser(lpUser);
                plugin.getUserManager().cleanup(lpUser);
            }

            log.accept("Migrated " + userCount + " users in world " + world.getName() + ".");
        }

        log.accept("Success! Completed without any errors.");
        return CommandResult.SUCCESS;
    }
}
