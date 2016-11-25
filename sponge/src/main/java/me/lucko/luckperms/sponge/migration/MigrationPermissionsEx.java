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
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.sponge.LPSpongePlugin;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static me.lucko.luckperms.sponge.migration.MigrationUtils.migrateSubject;

public class MigrationPermissionsEx extends SubCommand<Object> {
    public MigrationPermissionsEx() {
        super("permissionsex", "Migration from PermissionsEx", Permission.MIGRATION, Predicates.alwaysFalse(), null);
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object o, List<String> args, String label) throws CommandException {
        final Logger log = plugin.getLog();
        final LuckPermsService lpService = ((LPSpongePlugin) plugin).getService();

        Optional<PluginContainer> pex = Sponge.getPluginManager().getPlugin("permissionsex");
        if (!pex.isPresent()) {
            log.severe("PermissionsEx Migration: Error -> PermissionsEx is not loaded.");
            return CommandResult.STATE_ERROR;
        }

        // Cast to PermissionService. PEX has all of it's damned classes defined as package private.
        PermissionService pexService = (PermissionService) pex.get().getInstance().get();

        // Migrate defaults
        for (SubjectCollection collection : pexService.getKnownSubjects().values()) {
            MigrationUtils.migrateSubjectData(
                    collection.getDefaults().getSubjectData(),
                    lpService.getSubjects("defaults").get(collection.getIdentifier()).getSubjectData()
            );
        }

        MigrationUtils.migrateSubjectData(pexService.getDefaults().getSubjectData(), lpService.getDefaults().getSubjectData());

        // Migrate groups
        log.info("PermissionsEx Migration: Starting group migration.");
        int groupCount = 0;
        for (Subject pexGroup : pexService.getGroupSubjects().getAllSubjects()) {
            groupCount++;

            // Make a LuckPerms group for the one being migrated
            plugin.getStorage().createAndLoadGroup(pexGroup.getIdentifier().toLowerCase()).join();
            Group group = plugin.getGroupManager().getIfLoaded(pexGroup.getIdentifier().toLowerCase());
            migrateSubject(pexGroup, group);
            plugin.getStorage().saveGroup(group);
        }
        log.info("PermissionsEx Migration: Migrated " + groupCount + " groups");

        // Migrate users
        log.info("PermissionsEx Migration: Starting user migration.");
        int userCount = 0;
        for (Subject pexUser : pexService.getUserSubjects().getAllSubjects()) {
            userCount++;
            UUID uuid = Util.parseUuid(pexUser.getIdentifier());
            if (uuid == null) {
                log.severe("PermissionsEx Migration: Error -> Could not parse UUID for user: " + pexUser.getIdentifier());
                continue;
            }

            // Make a LuckPerms user for the one being migrated
            plugin.getStorage().loadUser(uuid, "null").join();
            User user = plugin.getUserManager().get(uuid);
            migrateSubject(pexUser, user);
            plugin.getStorage().saveUser(user);
            plugin.getUserManager().cleanup(user);
        }

        log.info("PermissionsEx Migration: Migrated " + userCount + " users.");
        log.info("PermissionsEx Migration: Success! Completed without any errors.");
        return CommandResult.SUCCESS;
    }
}
