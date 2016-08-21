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

package me.lucko.luckperms.commands.migration.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.Logger;
import me.lucko.luckperms.commands.CommandResult;
import me.lucko.luckperms.commands.Predicate;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.tracks.Track;
import me.lucko.luckperms.users.User;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <3 <3  zPermissions  <3 <3
 * Finally a permissions plugin with a decent API. *sigh*
 */
public class MigrationZPermissions extends SubCommand<Object> {
    public MigrationZPermissions() {
        super("zpermissions", "Migration from zPermissions",
                "/%s migration zpermissions [world names]", Permission.MIGRATION, Predicate.alwaysFalse());
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object o, List<String> args, String label) {
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

        // Migrate all users.
        log.info("zPermissions Migration: Starting user migration.");
        for (UUID u : service.getAllPlayersUUID()) {
            plugin.getDatastore().loadOrCreateUser(u, "null");
            User user = plugin.getUserManager().get(u);

            for (Map.Entry<String, Boolean> e : service.getPlayerPermissions(null, null, u).entrySet()) {
                try {
                    user.setPermission(e.getKey(), e.getValue());
                } catch (ObjectAlreadyHasException ignored) {}
            }

            if (args != null && !args.isEmpty()) {
                for (String world : args) {
                    for (Map.Entry<String, Boolean> e : service.getPlayerPermissions(world, null, u).entrySet()) {
                        try {
                            user.setPermission(e.getKey(), e.getValue(), "global", world);
                        } catch (ObjectAlreadyHasException ignored) {}
                    }
                }
            }

            for (String g : service.getPlayerAssignedGroups(u)) {
                try {
                    user.setPermission("group." + g.toLowerCase(), true);
                } catch (ObjectAlreadyHasException ignored) {}
            }

            user.setPrimaryGroup(service.getPlayerPrimaryGroup(u));
            plugin.getUserManager().cleanup(user);
            plugin.getDatastore().saveUser(user);
        }

        // Migrate all tracks
        log.info("zPermissions Migration: Starting track migration.");
        for (String t : service.getAllTracks()) {
            plugin.getDatastore().createAndLoadTrack(t.toLowerCase());
            Track track = plugin.getTrackManager().get(t.toLowerCase());

            track.setGroups(service.getTrackGroups(t));

            plugin.getDatastore().saveTrack(track);
        }

        // Migrate all groups
        log.info("zPermissions Migration: Starting group migration.");
        for (String g : service.getAllGroups()) {
            plugin.getDatastore().createAndLoadGroup(g.toLowerCase());
            Group group = plugin.getGroupManager().get(g.toLowerCase());

            for (Map.Entry<String, Boolean> e : service.getGroupPermissions(null, null, g).entrySet()) {
                try {
                    group.setPermission(e.getKey(), e.getValue());
                } catch (ObjectAlreadyHasException ignored) {}
            }

            if (args != null && !args.isEmpty()) {
                for (String world : args) {
                    for (Map.Entry<String, Boolean> e : service.getGroupPermissions(world, null, g).entrySet()) {
                        try {
                            group.setPermission(e.getKey(), e.getValue(), "global", world);
                        } catch (ObjectAlreadyHasException ignored) {}
                    }
                }
            }

            plugin.getDatastore().saveGroup(group);
        }

        log.info("zPermissions Migration: Complete!");
        return CommandResult.SUCCESS;
    }
}
