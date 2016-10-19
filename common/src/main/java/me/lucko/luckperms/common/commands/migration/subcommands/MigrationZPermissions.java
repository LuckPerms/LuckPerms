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

package me.lucko.luckperms.common.commands.migration.subcommands;

import me.lucko.luckperms.api.Logger;
import me.lucko.luckperms.api.MetaUtils;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.*;
import me.lucko.luckperms.common.constants.Constants;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.groups.Group;
import me.lucko.luckperms.common.tracks.Track;
import me.lucko.luckperms.common.users.User;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * <3 <3  zPermissions  <3 <3
 * Finally a permissions plugin with a decent API. *sigh*
 */
public class MigrationZPermissions extends SubCommand<Object> {
    public MigrationZPermissions() {
        super("zpermissions", "Migration from zPermissions", Permission.MIGRATION, Predicate.alwaysFalse(),
                Arg.list(Arg.create("world names...", false, "a list of worlds to migrate permissions from"))
        );
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

        final List<String> worlds = args.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        // Migrate all groups
        log.info("zPermissions Migration: Starting group migration.");
        for (String g : service.getAllGroups()) {
            plugin.getDatastore().createAndLoadGroup(g.toLowerCase());
            Group group = plugin.getGroupManager().get(g.toLowerCase());
            try {
                LogEntry.build()
                        .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                        .acted(group).action("create")
                        .build().submit(plugin);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            for (Map.Entry<String, Boolean> e : service.getGroupPermissions(null, null, g).entrySet()) {
                try {
                    group.setPermission(e.getKey(), e.getValue());
                    LogEntry.build()
                            .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                            .acted(group).action("set " + e.getKey() + " " + e.getValue())
                            .build().submit(plugin);
                } catch (Exception ex) {
                    if (!(ex instanceof ObjectAlreadyHasException)) {
                        ex.printStackTrace();
                    }
                }
            }

            if (worlds != null && !worlds.isEmpty()) {
                for (String world : worlds) {
                    for (Map.Entry<String, Boolean> e : service.getGroupPermissions(world, null, g).entrySet()) {
                        try {
                            group.setPermission(e.getKey(), e.getValue(), "global", world);
                            LogEntry.build()
                                    .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                                    .acted(group).action("set " + e.getKey() + " true " + e.getValue() + " " + world)
                                    .build().submit(plugin);
                        } catch (Exception ex) {
                            if (!(ex instanceof ObjectAlreadyHasException)) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }

            plugin.getDatastore().saveGroup(group);
        }

        // Migrate all tracks
        log.info("zPermissions Migration: Starting track migration.");
        for (String t : service.getAllTracks()) {
            plugin.getDatastore().createAndLoadTrack(t.toLowerCase());
            Track track = plugin.getTrackManager().get(t.toLowerCase());
            try {
                LogEntry.build()
                        .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                        .acted(track).action("create")
                        .build().submit(plugin);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            track.setGroups(service.getTrackGroups(t));
            for (String group : track.getGroups()) {
                try {
                    LogEntry.build()
                            .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                            .acted(track).action("append " + group)
                            .build().submit(plugin);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            plugin.getDatastore().saveTrack(track);
        }

        // Migrate all users.
        log.info("zPermissions Migration: Starting user migration.");
        for (UUID u : service.getAllPlayersUUID()) {
            plugin.getDatastore().loadUser(u, "null");
            User user = plugin.getUserManager().get(u);

            for (Map.Entry<String, Boolean> e : service.getPlayerPermissions(null, null, u).entrySet()) {
                try {
                    user.setPermission(e.getKey(), e.getValue());
                    LogEntry.build()
                            .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                            .acted(user).action("set " + e.getKey() + " " + e.getValue())
                            .build().submit(plugin);
                } catch (Exception ex) {
                    if (!(ex instanceof ObjectAlreadyHasException)) {
                        ex.printStackTrace();
                    }
                }
            }

            if (worlds != null && !worlds.isEmpty()) {
                for (String world : worlds) {
                    for (Map.Entry<String, Boolean> e : service.getPlayerPermissions(world, null, u).entrySet()) {
                        try {
                            user.setPermission(e.getKey(), e.getValue(), "global", world);
                            LogEntry.build()
                                    .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                                    .acted(user).action("set " + e.getKey() + " true " + e.getValue() + " " + world)
                                    .build().submit(plugin);
                        } catch (Exception ex) {
                            if (!(ex instanceof ObjectAlreadyHasException)) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }

            for (String g : service.getPlayerAssignedGroups(u)) {
                try {
                    user.setPermission("group." + g.toLowerCase(), true);
                    LogEntry.build()
                            .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                            .acted(user).action("addgroup " + g.toLowerCase())
                            .build().submit(plugin);
                } catch (Exception ex) {
                    if (!(ex instanceof ObjectAlreadyHasException)) {
                        ex.printStackTrace();
                    }
                }
            }

            user.setPrimaryGroup(service.getPlayerPrimaryGroup(u));
            try {
                LogEntry.build()
                        .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                        .acted(user).action("setprimarygroup " + service.getPlayerPrimaryGroup(u))
                        .build().submit(plugin);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            String prefix = service.getPlayerPrefix(u);
            String suffix = service.getPlayerSuffix(u);

            if (prefix != null && !prefix.equals("")) {
                prefix = MetaUtils.escapeCharacters(prefix);
                try {
                    user.setPermission("prefix.100." + prefix, true);
                    LogEntry.build()
                            .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                            .acted(user).action("set prefix.100." + prefix + " true")
                            .build().submit(plugin);
                } catch (Exception ex) {
                    if (!(ex instanceof ObjectAlreadyHasException)) {
                        ex.printStackTrace();
                    }
                }
            }

            if (suffix != null && !suffix.equals("")) {
                suffix = MetaUtils.escapeCharacters(suffix);
                try {
                    user.setPermission("suffix.100." + suffix, true);
                    LogEntry.build()
                            .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                            .acted(user).action("set suffix.100." + suffix + " true")
                            .build().submit(plugin);
                } catch (Exception ex) {
                    if (!(ex instanceof ObjectAlreadyHasException)) {
                        ex.printStackTrace();
                    }
                }
            }

            plugin.getUserManager().cleanup(user);
            plugin.getDatastore().saveUser(user);
        }

        log.info("zPermissions Migration: Success! Completed without any errors.");
        return CommandResult.SUCCESS;
    }
}
