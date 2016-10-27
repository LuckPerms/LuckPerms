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
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.*;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.Node;
import me.lucko.luckperms.common.core.PermissionHolder;
import me.lucko.luckperms.common.groups.Group;
import me.lucko.luckperms.common.users.User;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;

import java.util.*;

public class MigrationPermissionsEx extends SubCommand<Object> {
    public MigrationPermissionsEx() {
        super("permissionsex", "Migration from PermissionsEx", Permission.MIGRATION, Predicate.alwaysFalse(), null);
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object o, List<String> args, String label) {
        final Logger log = plugin.getLog();

        Optional<PluginContainer> pex = Sponge.getPluginManager().getPlugin("permissionsex");
        if (!pex.isPresent()) {
            log.severe("PermissionsEx Migration: Error -> PermissionsEx is not loaded.");
            return CommandResult.STATE_ERROR;
        }

        // Cast to PermissionService. PEX has all of it's damned classes defined as package private.
        PermissionService pexService = (PermissionService) pex.get().getInstance().get();

        // Migrate groups
        log.info("PermissionsEx Migration: Starting group migration.");
        int groupCount = 0;
        for (Subject pexGroup : pexService.getGroupSubjects().getAllSubjects()) {
            groupCount++;

            // Make a LuckPerms group for the one being migrated
            plugin.getDatastore().createAndLoadGroup(pexGroup.getIdentifier().toLowerCase()).getUnchecked();
            Group group = plugin.getGroupManager().get(pexGroup.getIdentifier().toLowerCase());
            migrateSubject(pexGroup, group);
            plugin.getDatastore().saveGroup(group);
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
            plugin.getDatastore().loadUser(uuid, "null").getUnchecked();
            User user = plugin.getUserManager().get(uuid);
            migrateSubject(pexUser, user);
            plugin.getDatastore().saveUser(user);
            plugin.getUserManager().cleanup(user);
        }

        log.info("PermissionsEx Migration: Migrated " + userCount + " users.");
        log.info("PermissionsEx Migration: Success! Completed without any errors.");
        return CommandResult.SUCCESS;
    }

    private static void migrateSubject(Subject subject, PermissionHolder holder) {
        // Migrate permissions
        Map<Set<Context>, Map<String, Boolean>> perms = subject.getSubjectData().getAllPermissions();
        for (Map.Entry<Set<Context>, Map<String, Boolean>> e : perms.entrySet()) {
            ContextSet context = LuckPermsService.convertContexts(e.getKey());

            MutableContextSet contexts = MutableContextSet.fromSet(context);
            String server = contexts.getValues("server").stream().findAny().orElse(null);
            String world = contexts.getValues("world").stream().findAny().orElse(null);
            contexts.removeAll("server");
            contexts.removeAll("world");

            for (Map.Entry<String, Boolean> perm : e.getValue().entrySet()) {
                try {
                    holder.setPermission(new Node.Builder(perm.getKey()).setServerRaw(server).setWorld(world).withExtraContext(contexts).setValue(perm.getValue()).build());
                } catch (ObjectAlreadyHasException ignored) {}
            }
        }

        // Migrate options
        Map<Set<Context>, Map<String, String>> opts = subject.getSubjectData().getAllOptions();
        for (Map.Entry<Set<Context>, Map<String, String>> e : opts.entrySet()) {
            ContextSet context = LuckPermsService.convertContexts(e.getKey());

            MutableContextSet contexts = MutableContextSet.fromSet(context);
            String server = contexts.getValues("server").stream().findAny().orElse(null);
            String world = contexts.getValues("world").stream().findAny().orElse(null);
            contexts.removeAll("server");
            contexts.removeAll("world");

            for (Map.Entry<String, String> opt : e.getValue().entrySet()) {
                if (opt.getKey().equalsIgnoreCase("prefix") || opt.getKey().equalsIgnoreCase("suffix")) {
                    try {
                        holder.setPermission(new Node.Builder(opt.getKey().toLowerCase() + ".100." + opt.getValue()).setServerRaw(server).setWorld(world).withExtraContext(contexts).setValue(true).build());
                    } catch (ObjectAlreadyHasException ignored) {}
                } else {
                    try {
                        holder.setPermission(new Node.Builder("meta." + opt.getKey() + "." + opt.getValue()).setServerRaw(server).setWorld(world).withExtraContext(contexts).setValue(true).build());
                    } catch (ObjectAlreadyHasException ignored) {}
                }
            }
        }

        // Migrate parents
        Map<Set<Context>, List<Subject>> parents = subject.getSubjectData().getAllParents();
        for (Map.Entry<Set<Context>, List<Subject>> e : parents.entrySet()) {
            ContextSet context = LuckPermsService.convertContexts(e.getKey());

            MutableContextSet contexts = MutableContextSet.fromSet(context);
            String server = contexts.getValues("server").stream().findAny().orElse(null);
            String world = contexts.getValues("world").stream().findAny().orElse(null);
            contexts.removeAll("server");
            contexts.removeAll("world");

            for (Subject s : e.getValue()) {
                if (!s.getContainingCollection().getIdentifier().equalsIgnoreCase(PermissionService.SUBJECTS_GROUP)) {
                    continue; // LuckPerms does not support persisting other subject types.
                }

                try {
                    holder.setPermission(new Node.Builder("group." + s.getIdentifier().toLowerCase()).setServerRaw(server).setWorld(world).withExtraContext(contexts).setValue(true).build());
                } catch (ObjectAlreadyHasException ignored) {}
            }
        }
    }
}
