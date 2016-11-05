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

package me.lucko.luckperms.bungee.migration;

import me.lucko.luckperms.api.Logger;
import me.lucko.luckperms.api.MetaUtils;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.Sender;
import me.lucko.luckperms.common.commands.SubCommand;
import me.lucko.luckperms.common.constants.Constants;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import net.alpenblock.bungeeperms.*;

import java.util.List;
import java.util.Map;

/**
 * BungeePerms is actually pretty nice. huh.
 */
public class MigrationBungeePerms extends SubCommand<Object> {
    public MigrationBungeePerms() {
        super("bungeeperms", "Migration from BungeePerms", Permission.MIGRATION, Predicates.alwaysFalse(), null);
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object o, List<String> args, String label) {
        final Logger log = plugin.getLog();

        BungeePerms bp = BungeePerms.getInstance();
        if (bp == null) {
            log.severe("BungeePerms Migration: Error -> BungeePerms is not loaded.");
            return CommandResult.STATE_ERROR;
        }

        // Migrate all groups.
        log.info("BungeePerms Migration: Starting group migration.");
        int groupCount = 0;
        for (Group g : bp.getPermissionsManager().getBackEnd().loadGroups()) {
            groupCount++;

            // Make a LuckPerms group for the one being migrated
            plugin.getDatastore().createAndLoadGroup(g.getName().toLowerCase()).getUnchecked();
            me.lucko.luckperms.common.groups.Group group = plugin.getGroupManager().get(g.getName().toLowerCase());
            try {
                LogEntry.build()
                        .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                        .acted(group).action("create")
                        .build().submit(plugin);
            } catch (Exception ex) {
                ex.printStackTrace();
            }


            // Migrate global perms
            for (String perm : g.getPerms()) {
                try {
                    group.setPermission(perm, true);
                    LogEntry.build()
                            .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                            .acted(group).action("set " + perm + " true")
                            .build().submit(plugin);
                } catch (Exception ex) {
                    if (!(ex instanceof ObjectAlreadyHasException)) {
                        ex.printStackTrace();
                    }
                }
            }

            // Migrate per-server perms
            for (Map.Entry<String, Server> e : g.getServers().entrySet()) {
                for (String perm : e.getValue().getPerms()) {
                    try {
                        group.setPermission(perm, true, e.getKey());
                        LogEntry.build()
                                .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                                .acted(group).action("set " + perm + " true " + e.getKey())
                                .build().submit(plugin);
                    } catch (Exception ex) {
                        if (!(ex instanceof ObjectAlreadyHasException)) {
                            ex.printStackTrace();
                        }
                    }
                }

                // Migrate per-world perms
                for (Map.Entry<String, World> we : e.getValue().getWorlds().entrySet()) {
                    for (String perm : we.getValue().getPerms()) {
                        try {
                            group.setPermission(perm, true, e.getKey(), we.getKey());
                            LogEntry.build()
                                    .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                                    .acted(group).action("set " + perm + " true " + e.getKey() + " " + we.getKey())
                                    .build().submit(plugin);
                        } catch (Exception ex) {
                            if (!(ex instanceof ObjectAlreadyHasException)) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }

            // Migrate any parent groups
            for (String inherit : g.getInheritances()) {
                try {
                    group.setPermission("group." + inherit, true);
                    LogEntry.build()
                            .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                            .acted(group).action("setinherit " + group)
                            .build().submit(plugin);
                } catch (Exception ex) {
                    if (!(ex instanceof ObjectAlreadyHasException)) {
                        ex.printStackTrace();
                    }
                }
            }

            // Migrate prefix and suffix
            String prefix = g.getPrefix();
            String suffix = g.getSuffix();

            if (prefix != null && !prefix.equals("")) {
                prefix = MetaUtils.escapeCharacters(prefix);
                try {
                    group.setPermission("prefix.50." + prefix, true);
                    LogEntry.build()
                            .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                            .acted(group).action("set prefix.50." + prefix + " true")
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
                    group.setPermission("suffix.50." + suffix, true);
                    LogEntry.build()
                            .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                            .acted(group).action("set suffix.50." + suffix + " true")
                            .build().submit(plugin);
                } catch (Exception ex) {
                    if (!(ex instanceof ObjectAlreadyHasException)) {
                        ex.printStackTrace();
                    }
                }
            }

            plugin.getDatastore().saveGroup(group);
        }

        log.info("BungeePerms Migration: Migrated " + groupCount + " groups");

        // Migrate all users.
        log.info("BungeePerms Migration: Starting user migration.");
        int userCount = 0;
        for (User u : bp.getPermissionsManager().getBackEnd().loadUsers()) {
            if (u.getUUID() == null) continue;

            userCount++;

            // Make a LuckPerms user for the one being migrated.
            plugin.getDatastore().loadUser(u.getUUID(), "null").getUnchecked();
            me.lucko.luckperms.common.users.User user = plugin.getUserManager().get(u.getUUID());

            // Migrate global perms
            for (String perm : u.getPerms()) {
                try {
                    user.setPermission(perm, true);
                    LogEntry.build()
                            .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                            .acted(user).action("set " + perm + " true")
                            .build().submit(plugin);
                } catch (Exception ex) {
                    if (!(ex instanceof ObjectAlreadyHasException)) {
                        ex.printStackTrace();
                    }
                }
            }

            // Migrate per-server perms
            for (Map.Entry<String, Server> e : u.getServers().entrySet()) {
                for (String perm : e.getValue().getPerms()) {
                    try {
                        user.setPermission(perm, true, e.getKey());
                        LogEntry.build()
                                .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                                .acted(user).action("set " + perm + " true " + e.getKey())
                                .build().submit(plugin);
                    } catch (Exception ex) {
                        if (!(ex instanceof ObjectAlreadyHasException)) {
                            ex.printStackTrace();
                        }
                    }
                }

                // Migrate per-world perms
                for (Map.Entry<String, World> we : e.getValue().getWorlds().entrySet()) {
                    for (String perm : we.getValue().getPerms()) {
                        try {
                            user.setPermission(perm, true, e.getKey(), we.getKey());
                            LogEntry.build()
                                    .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                                    .acted(user).action("set " + perm + " true " + e.getKey() + " " + we.getKey())
                                    .build().submit(plugin);
                        } catch (Exception ex) {
                            if (!(ex instanceof ObjectAlreadyHasException)) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }

            // Migrate groups
            for (String group : u.getGroupsString()) {
                try {
                    user.setPermission("group." + group, true);
                    LogEntry.build()
                            .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                            .acted(user).action("addgroup " + group)
                            .build().submit(plugin);
                } catch (Exception ex) {
                    if (!(ex instanceof ObjectAlreadyHasException)) {
                        ex.printStackTrace();
                    }
                }
            }

            // Migrate prefix & suffix
            String prefix = u.getPrefix();
            String suffix = u.getSuffix();

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

            plugin.getDatastore().saveUser(user);
            plugin.getUserManager().cleanup(user);
        }

        log.info("BungeePerms Migration: Migrated " + userCount + " users.");
        log.info("BungeePerms Migration: Success! Completed without any errors.");
        return CommandResult.SUCCESS;
    }
}
