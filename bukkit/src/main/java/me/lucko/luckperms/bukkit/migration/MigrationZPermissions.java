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

import me.lucko.luckperms.api.Logger;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.NodeFactory;
import me.lucko.luckperms.common.core.PermissionHolder;
import me.lucko.luckperms.common.groups.Group;
import me.lucko.luckperms.common.tracks.Track;
import me.lucko.luckperms.common.users.User;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService;
import org.tyrannyofheaven.bukkit.zPermissions.dao.PermissionService;
import org.tyrannyofheaven.bukkit.zPermissions.model.EntityMetadata;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Inheritance;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

public class MigrationZPermissions extends SubCommand<Object> {
    public MigrationZPermissions() {
        super("zpermissions", "Migration from zPermissions", Permission.MIGRATION, Predicates.alwaysFalse(), null);
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object o, List<String> args, String label) throws CommandException {
        final Logger log = plugin.getLog();
        if (!plugin.isPluginLoaded("zPermissions")) {
            log.severe("zPermissions Migration: Error -> zPermissions is not loaded.");
            return CommandResult.STATE_ERROR;
        }

        ZPermissionsService service = (ZPermissionsService) plugin.getService(ZPermissionsService.class);
        if (service == null) {
            log.severe("zPermissions Migration: Error -> zPermissions is not loaded.");
            return CommandResult.STATE_ERROR;
        }

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
        log.info("zPermissions Migration: Starting group migration.");
        for (String g : service.getAllGroups()) {
            plugin.getStorage().createAndLoadGroup(g.toLowerCase()).join();
            Group group = plugin.getGroupManager().get(g.toLowerCase());

            PermissionEntity entity = internalService.getEntity(g, null, true);
            migrateEntity(group, entity);

            plugin.getStorage().saveGroup(group);
        }

        // Migrate all tracks
        log.info("zPermissions Migration: Starting track migration.");
        for (String t : service.getAllTracks()) {
            plugin.getStorage().createAndLoadTrack(t.toLowerCase()).join();
            Track track = plugin.getTrackManager().get(t.toLowerCase());
            track.setGroups(service.getTrackGroups(t));
            plugin.getStorage().saveTrack(track);
        }

        // Migrate all users.
        log.info("zPermissions Migration: Starting user migration.");
        for (UUID u : service.getAllPlayersUUID()) {
            plugin.getStorage().loadUser(u, "null").join();
            User user = plugin.getUserManager().get(u);

            PermissionEntity entity = internalService.getEntity(null, u, false);
            migrateEntity(user, entity);

            user.setPrimaryGroup(service.getPlayerPrimaryGroup(u));

            plugin.getUserManager().cleanup(user);
            plugin.getStorage().saveUser(user);
        }

        log.info("zPermissions Migration: Success! Completed without any errors.");
        return CommandResult.SUCCESS;
    }

    private void migrateEntity(PermissionHolder group, PermissionEntity entity) {
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

        for (Inheritance inheritance : entity.getInheritancesAsChild()) {
            if (!inheritance.getChild().getId().equals(entity.getId())) {
                new Throwable("Illegal inheritance").printStackTrace();
                continue;
            }

            try {
                group.setPermission("group." + inheritance.getParent(), true);
            } catch (ObjectAlreadyHasException ignored) {}
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
