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
import me.lucko.luckperms.api.event.cause.CreationCause;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.core.model.PermissionHolder;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.common.utils.ProgressLogger;

import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

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

    public MigrationBPermissions() {
        super("bpermissions", "Migration from bPermissions", MIGRATION, Predicates.alwaysFalse(), null);
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object o, List<String> args, String label) throws CommandException {
        ProgressLogger log = new ProgressLogger("bPermissions");
        log.addListener(plugin.getConsoleSender());
        log.addListener(sender);

        log.log("Starting.");

        WorldManager worldManager = WorldManager.getInstance();
        if (worldManager == null) {
            log.logErr("Plugin not loaded.");
            return CommandResult.STATE_ERROR;
        }

        log.log("Forcing the plugin to load all data. This could take a while.");
        for (World world : worldManager.getAllWorlds()) {
            log.log("Loading users in world " + world.getName());
            Set<String> users = getUsers(world);
            if (users == null) {
                log.logErr("Couldn't get a list of users.");
                return CommandResult.FAILURE;
            }
            AtomicInteger userLoadCount = new AtomicInteger(0);
            users.forEach(s -> {
                world.loadOne(s, CalculableType.USER);
                log.logProgress("Forcefully loaded {} users so far.", userLoadCount.incrementAndGet());
            });
        }
        log.log("Forcefully loaded all users.");

        // Migrate one world at a time.
        log.log("Starting world migration.");
        for (World world : worldManager.getAllWorlds()) {
            log.log("Migrating world: " + world.getName());

            // Migrate all groups
            log.log("Starting group migration in world " + world.getName() + ".");
            AtomicInteger groupCount = new AtomicInteger(0);
            for (Calculable group : world.getAll(CalculableType.GROUP)) {
                String groupName = group.getName().toLowerCase();
                if (group.getName().equalsIgnoreCase(world.getDefaultGroup())) {
                    groupName = "default";
                }

                // Make a LuckPerms group for the one being migrated.
                plugin.getStorage().createAndLoadGroup(groupName, CreationCause.INTERNAL).join();
                me.lucko.luckperms.common.core.model.Group lpGroup = plugin.getGroupManager().getIfLoaded(groupName);

                migrateHolder(log, world, group, lpGroup);
                plugin.getStorage().saveGroup(lpGroup);

                log.logAllProgress("Migrated {} groups so far.", groupCount.incrementAndGet());
            }
            log.log("Migrated " + groupCount.get() + " groups in world " + world.getName() + ".");

            // Migrate all users
            log.log("Starting user migration in world " + world.getName() + ".");
            AtomicInteger userCount = new AtomicInteger(0);
            for (Calculable user : world.getAll(CalculableType.USER)) {
                // There is no mention of UUIDs in the API. I assume that name = uuid. idk?
                UUID uuid = null;
                try {
                    uuid = UUID.fromString(user.getName());
                } catch (IllegalArgumentException e) {
                    try {
                        //noinspection deprecation
                        uuid = Bukkit.getOfflinePlayer(user.getName()).getUniqueId();
                    } catch (Exception ex) {
                        e.printStackTrace();
                    }
                }

                if (uuid == null) {
                    log.logErr("Unable to migrate user " + user.getName() + ". Cannot to get UUID.");
                    continue;
                }

                // Make a LuckPerms user for the one being migrated.
                plugin.getStorage().loadUser(uuid, "null").join();
                User lpUser = plugin.getUserManager().get(uuid);

                migrateHolder(log, world, user, lpUser);

                plugin.getStorage().saveUser(lpUser);
                plugin.getUserManager().cleanup(lpUser);

                log.logProgress("Migrated {} users so far.", userCount.incrementAndGet());
            }

            log.log("Migrated " + userCount.get() + " users in world " + world.getName() + ".");
        }

        log.log("Success! Migration complete.");
        return CommandResult.SUCCESS;
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

    private static void migrateHolder(ProgressLogger log, World world, Calculable c, PermissionHolder holder) {
        // Migrate the groups permissions in this world
        for (Permission p : c.getPermissions()) {
            try {
                holder.setPermission(p.name(), p.isTrue(), "global", world.getName());
            } catch (Exception ex) {
                log.handleException(ex);
            }

            // Include any child permissions
            for (Map.Entry<String, Boolean> child : p.getChildren().entrySet()) {
                try {
                    holder.setPermission(child.getKey(), child.getValue(), "global", world.getName());
                } catch (Exception ex) {
                    log.handleException(ex);
                }
            }
        }

        // Migrate any inherited groups
        for (Group parent : c.getGroups()) {
            try {
                holder.setPermission("group." + parent.getName(), true, "global", world.getName());
            } catch (Exception ex) {
                log.handleException(ex);
            }
        }

        // Migrate existing meta
        for (Map.Entry<String, String> meta : c.getMeta().entrySet()) {
            if (meta.getKey().equalsIgnoreCase("prefix") || meta.getKey().equalsIgnoreCase("suffix")) {
                String chatMeta = MetaUtils.escapeCharacters(meta.getValue());
                try {
                    holder.setPermission(meta.getKey().toLowerCase() + "." + c.getPriority() + "." + chatMeta, true);
                } catch (Exception ex) {
                    log.handleException(ex);
                }
                continue;
            }

            try {
                holder.setPermission("meta." + meta.getKey() + "." + meta.getValue(), true, "global", world.getName());
            } catch (Exception ex) {
                log.handleException(ex);
            }
        }
    }

}
