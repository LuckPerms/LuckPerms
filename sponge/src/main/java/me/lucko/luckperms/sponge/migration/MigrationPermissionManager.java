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

package me.lucko.luckperms.sponge.migration;

import me.lucko.luckperms.api.Logger;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.groups.Group;
import me.lucko.luckperms.common.users.User;
import me.lucko.luckperms.common.utils.Predicates;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static me.lucko.luckperms.sponge.migration.MigrationUtils.migrateSubject;

public class MigrationPermissionManager extends SubCommand<Object> {
    public MigrationPermissionManager() {
        super("permissionmanager", "Migration from PermissionManager", Permission.MIGRATION, Predicates.alwaysFalse(), null);
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object o, List<String> args, String label) throws CommandException {
        final Logger log = plugin.getLog();

        Optional<PluginContainer> pm = Sponge.getPluginManager().getPlugin("permissionmanager");
        if (!pm.isPresent()) {
            log.severe("PermissionManager Migration: Error -> PermissionManager is not loaded.");
            return CommandResult.STATE_ERROR;
        }

        // Cast to PermissionService. PEX has all of it's damned classes defined as package private.
        PermissionService pmService = (PermissionService) pm.get().getInstance().get();

        // Migrate groups
        log.info("PermissionManager Migration: Starting group migration.");

        // Forcefully load all groups.
        try {
            Method method = pmService.getGroupSubjects().getClass().getMethod("load");
            method.invoke(pmService.getGroupSubjects());
        } catch (Throwable t) {
            t.printStackTrace();
        }

        int groupCount = 0;
        for (Subject pmGroup : pmService.getGroupSubjects().getAllSubjects()) {
            groupCount++;

            // Make a LuckPerms group for the one being migrated
            plugin.getDatastore().createAndLoadGroup(pmGroup.getIdentifier().toLowerCase()).getUnchecked();
            Group group = plugin.getGroupManager().get(pmGroup.getIdentifier().toLowerCase());
            migrateSubject(pmGroup, group);
            plugin.getDatastore().saveGroup(group);
        }
        log.info("PermissionManager Migration: Migrated " + groupCount + " groups");

        // Migrate users
        log.info("PermissionManager Migration: Starting user migration.");

        // Forcefully load all users.
        try {
            Method method = pmService.getUserSubjects().getClass().getMethod("load");
            method.invoke(pmService.getUserSubjects());
        } catch (Throwable t) {
            t.printStackTrace();
        }

        int userCount = 0;
        for (Subject pmUser : pmService.getUserSubjects().getAllSubjects()) {
            userCount++;
            UUID uuid = Util.parseUuid(pmUser.getIdentifier());
            if (uuid == null) {
                log.severe("PermissionManager Migration: Error -> Could not parse UUID for user: " + pmUser.getIdentifier());
                continue;
            }

            // Make a LuckPerms user for the one being migrated
            plugin.getDatastore().loadUser(uuid, "null").getUnchecked();
            User user = plugin.getUserManager().get(uuid);
            migrateSubject(pmUser, user);
            plugin.getDatastore().saveUser(user);
            plugin.getUserManager().cleanup(user);
        }

        log.info("PermissionManager Migration: Migrated " + userCount + " users.");
        log.info("PermissionManager Migration: Success! Completed without any errors.");
        return CommandResult.SUCCESS;
    }
}
