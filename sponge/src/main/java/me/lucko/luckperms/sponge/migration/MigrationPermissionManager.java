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

package me.lucko.luckperms.sponge.migration;

import me.lucko.luckperms.api.event.cause.CreationCause;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandPermission;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SubCommand;
import me.lucko.luckperms.common.commands.impl.migration.MigrationUtils;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.CommandUtils;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.logging.ProgressLogger;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.common.utils.SafeIterator;
import me.lucko.luckperms.sponge.LPSpongePlugin;
import me.lucko.luckperms.sponge.service.LuckPermsService;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.permission.PermissionService;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static me.lucko.luckperms.sponge.migration.SpongeMigrationUtils.migrateSubject;
import static me.lucko.luckperms.sponge.migration.SpongeMigrationUtils.migrateSubjectData;

public class MigrationPermissionManager extends SubCommand<Object> {
    public MigrationPermissionManager(LocaleManager locale) {
        super(CommandSpec.MIGRATION_COMMAND.spec(locale), "permissionmanager", CommandPermission.MIGRATION, Predicates.alwaysFalse());
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
            Object pmPlugin = pm.get().getInstance().get();
            Method method = pmPlugin.getClass().getDeclaredMethod("getPermissionService");
            pmService = (PermissionService) method.invoke(pmPlugin);
        } catch (Throwable t) {
            t.printStackTrace();
            return CommandResult.FAILURE;
        }

        // Migrate defaults
        log.log("Migrating default subjects.");
        SafeIterator.iterate(pmService.getKnownSubjects().values(), collection -> {
            migrateSubjectData(
                    collection.getDefaults().getSubjectData(),
                    lpService.getCollection("defaults").loadSubject(collection.getIdentifier()).join().sponge().getSubjectData()
            );
        });
        migrateSubjectData(pmService.getDefaults().getSubjectData(), lpService.getDefaults().sponge().getSubjectData());

        // Migrate groups
        log.log("Starting group migration.");
        AtomicInteger groupCount = new AtomicInteger(0);
        SafeIterator.iterate(pmService.getGroupSubjects().getAllSubjects(), pmGroup -> {
            String pmName = MigrationUtils.standardizeName(pmGroup.getIdentifier());

            // Make a LuckPerms group for the one being migrated
            plugin.getStorage().createAndLoadGroup(pmName, CreationCause.INTERNAL).join();
            Group group = plugin.getGroupManager().getIfLoaded(pmName);
            migrateSubject(pmGroup, group, 100);
            plugin.getStorage().saveGroup(group);

            log.logAllProgress("Migrated {} groups so far.", groupCount.incrementAndGet());
        });
        log.log("Migrated " + groupCount.get() + " groups");

        // Migrate users
        log.log("Starting user migration.");
        AtomicInteger userCount = new AtomicInteger(0);
        SafeIterator.iterate(pmService.getUserSubjects().getAllSubjects(), pmUser -> {
            UUID uuid = CommandUtils.parseUuid(pmUser.getIdentifier());
            if (uuid == null) {
                log.logErr("Could not parse UUID for user: " + pmUser.getIdentifier());
                return;
            }

            // Make a LuckPerms user for the one being migrated
            plugin.getStorage().loadUser(uuid, "null").join();
            User user = plugin.getUserManager().getIfLoaded(uuid);
            if (user.getEnduringNodes().size() <= 1) {
                user.clearNodes(false);
            }
            migrateSubject(pmUser, user, 100);
            plugin.getStorage().saveUser(user);
            plugin.getUserManager().cleanup(user);

            log.logProgress("Migrated {} users so far.", userCount.incrementAndGet());
        });

        log.log("Migrated " + userCount.get() + " users.");
        log.log("Success! Migration complete.");
        return CommandResult.SUCCESS;
    }
}
