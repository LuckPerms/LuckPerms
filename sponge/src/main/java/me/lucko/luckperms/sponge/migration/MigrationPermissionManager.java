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

import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.common.utils.ProgressLogger;
import me.lucko.luckperms.sponge.LPSpongePlugin;
import me.lucko.luckperms.sponge.service.LuckPermsService;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static me.lucko.luckperms.sponge.migration.MigrationUtils.migrateSubject;

public class MigrationPermissionManager extends SubCommand<Object> {
    public MigrationPermissionManager() {
        super("permissionmanager", "Migration from PermissionManager", Permission.MIGRATION, Predicates.alwaysFalse(), null);
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object o, List<String> args, String label) throws CommandException {
        ProgressLogger log = new ProgressLogger("PermissionManager");
        log.addListener(plugin.getConsoleSender());
        log.addListener(sender);

        log.log("Starting.");

        final LuckPermsService lpService = ((LPSpongePlugin) plugin).getService();

        Optional<PluginContainer> pm = Sponge.getPluginManager().getPlugin("permissionmanager");
        if (!pm.isPresent()) {
            log.logErr("Plugin not loaded.");
            return CommandResult.STATE_ERROR;
        }

        // Get PM's PermissionService
        PermissionService pmService;

        try {
            Class clazz = Class.forName("io.github.djxy.permissionmanager.PermissionService");
            Field instance = clazz.getDeclaredField("instance");
            pmService = (PermissionService) instance.get(null);
        } catch (Throwable t) {
            t.printStackTrace();
            return CommandResult.FAILURE;
        }

        // Migrate defaults
        log.log("Migrating default subjects.");
        for (SubjectCollection collection : pmService.getKnownSubjects().values()) {
            MigrationUtils.migrateSubjectData(
                    collection.getDefaults().getSubjectData(),
                    lpService.getSubjects("defaults").get(collection.getIdentifier()).getSubjectData()
            );
        }
        MigrationUtils.migrateSubjectData(pmService.getDefaults().getSubjectData(), lpService.getDefaults().getSubjectData());

        // Migrate groups
        log.log("Starting group migration.");

        // Forcefully load all groups.
        try {
            Method method = pmService.getGroupSubjects().getClass().getMethod("load");
            method.invoke(pmService.getGroupSubjects());
        } catch (Throwable t) {
            t.printStackTrace();
        }

        AtomicInteger groupCount = new AtomicInteger(0);
        for (Subject pmGroup : pmService.getGroupSubjects().getAllSubjects()) {
            String pmName = MigrationUtils.convertGroupName(pmGroup.getIdentifier().toLowerCase());

            // Make a LuckPerms group for the one being migrated
            plugin.getStorage().createAndLoadGroup(pmName).join();
            Group group = plugin.getGroupManager().getIfLoaded(pmName);
            migrateSubject(pmGroup, group);
            plugin.getStorage().saveGroup(group);

            log.logAllProgress("Migrated {} groups so far.", groupCount.incrementAndGet());
        }
        log.log("Migrated " + groupCount.get() + " groups");

        // Migrate users
        log.log("Starting user migration.");

        // Forcefully load all users.
        try {
            Method method = pmService.getUserSubjects().getClass().getMethod("load");
            method.invoke(pmService.getUserSubjects());
        } catch (Throwable t) {
            t.printStackTrace();
        }

        AtomicInteger userCount = new AtomicInteger(0);
        for (Subject pmUser : pmService.getUserSubjects().getAllSubjects()) {
            UUID uuid = Util.parseUuid(pmUser.getIdentifier());
            if (uuid == null) {
                log.logErr("Could not parse UUID for user: " + pmUser.getIdentifier());
                continue;
            }

            // Make a LuckPerms user for the one being migrated
            plugin.getStorage().loadUser(uuid, "null").join();
            User user = plugin.getUserManager().get(uuid);
            if (user.getNodes().size() <= 1) {
                user.clearNodes(false);
            }
            migrateSubject(pmUser, user);
            plugin.getStorage().saveUser(user);
            plugin.getUserManager().cleanup(user);

            log.logProgress("Migrated {} users so far.", userCount.incrementAndGet());
        }

        log.log("Migrated " + userCount.get() + " users.");
        log.log("Success! Migration complete.");
        return CommandResult.SUCCESS;
    }
}
