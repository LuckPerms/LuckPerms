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

import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.NodeFactory;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.core.model.PermissionHolder;
import me.lucko.luckperms.common.core.model.Track;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.common.utils.ProgressLogger;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;

import org.bukkit.Bukkit;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService;
import org.tyrannyofheaven.bukkit.zPermissions.dao.PermissionService;
import org.tyrannyofheaven.bukkit.zPermissions.model.EntityMetadata;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Inheritance;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class MigrationZPermissions extends SubCommand<Object> {
    public MigrationZPermissions() {
        super("zpermissions", "Migration from zPermissions", Permission.MIGRATION, Predicates.alwaysFalse(), null);
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object o, List<String> args, String label) throws CommandException {
        ProgressLogger log = new ProgressLogger("PermissionManager");
        log.addListener(plugin.getConsoleSender());
        log.addListener(sender);

        log.log("Starting.");
        
        if (!Bukkit.getPluginManager().isPluginEnabled("zPermissions")) {
            log.logErr("zPermissions is not loaded.");
            return CommandResult.STATE_ERROR;
        }

        if (!Bukkit.getServicesManager().isProvidedFor(ZPermissionsService.class)) {
            log.logErr("zPermissions is not loaded.");
            return CommandResult.STATE_ERROR;
        }

        ZPermissionsService service = Bukkit.getServicesManager().getRegistration(ZPermissionsService.class).getProvider();

        PermissionService internalService;
        try {
            Field psField = service.getClass().getDeclaredField("permissionService");
            psField.setAccessible(true);
            internalService = (PermissionService) psField.get(service);
        } catch (Exception e) {
            e.printStackTrace();
            return CommandResult.FAILURE;
        }

        // Migrate all groups
        log.log("Starting group migration.");
        AtomicInteger groupCount = new AtomicInteger(0);
        for (String g : service.getAllGroups()) {
            plugin.getStorage().createAndLoadGroup(g.toLowerCase()).join();
            Group group = plugin.getGroupManager().getIfLoaded(g.toLowerCase());

            PermissionEntity entity = internalService.getEntity(g, null, true);
            migrateEntity(group, entity, null);

            plugin.getStorage().saveGroup(group);
            log.logAllProgress("Migrated {} groups so far.", groupCount.incrementAndGet());
        }
        log.log("Migrated " + groupCount.get() + " groups");

        // Migrate all tracks
        log.log("Starting track migration.");
        AtomicInteger trackCount = new AtomicInteger(0);
        for (String t : service.getAllTracks()) {
            plugin.getStorage().createAndLoadTrack(t.toLowerCase()).join();
            Track track = plugin.getTrackManager().getIfLoaded(t.toLowerCase());
            track.setGroups(service.getTrackGroups(t));
            plugin.getStorage().saveTrack(track);
            log.logAllProgress("Migrated {} tracks so far.", trackCount.incrementAndGet());
        }
        log.log("Migrated " + trackCount.get() + " tracks");

        // Migrate all users.
        log.log("Starting user migration.");
        AtomicInteger userCount = new AtomicInteger(0);
        for (UUID u : service.getAllPlayersUUID()) {
            plugin.getStorage().loadUser(u, "null").join();
            User user = plugin.getUserManager().get(u);

            PermissionEntity entity = internalService.getEntity(null, u, false);
            migrateEntity(user, entity, internalService.getGroups(u));

            user.setPrimaryGroup(service.getPlayerPrimaryGroup(u));

            if (!entity.isGroup()) {
                user.setName(entity.getDisplayName());
            }

            plugin.getUserManager().cleanup(user);
            plugin.getStorage().saveUser(user);
            log.logProgress("Migrated {} users so far.", userCount.incrementAndGet());
        }

        log.log("Migrated " + userCount.get() + " users.");
        log.log("Success! Migration complete.");
        return CommandResult.SUCCESS;
    }

    private void migrateEntity(PermissionHolder group, PermissionEntity entity, List<Membership> memberships) {
        for (Entry e : entity.getPermissions()) {
            if (e.getWorld() != null) {
                try {
                    group.setPermission(e.getPermission(), true, "global", e.getWorld().getName());
                } catch (ObjectAlreadyHasException ignored) {}
            } else {
                try {
                    group.setPermission(e.getPermission(), true); // TODO handle negated.
                } catch (ObjectAlreadyHasException ignored) {}
            }
        }

        if (entity.isGroup()) {
            for (Inheritance inheritance : entity.getInheritancesAsChild()) {
                try {
                    if (!inheritance.getParent().getName().equals(group.getObjectName())) {
                        group.setPermission("group." + inheritance.getParent().getName(), true);
                    }
                } catch (ObjectAlreadyHasException ignored) {}
            }
        } else {
            // entity.getMemberships() doesn't work (always returns 0 records)
            for (Membership membership : memberships) {
                try {
                    group.setPermission("group." + membership.getGroup().getDisplayName(), true);
                } catch (ObjectAlreadyHasException ignored) {}
            }
        }

        for (EntityMetadata metadata : entity.getMetadata()) {
            try {
                group.setPermission(NodeFactory.makeMetaNode(metadata.getName(), metadata.getStringValue()).build());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
