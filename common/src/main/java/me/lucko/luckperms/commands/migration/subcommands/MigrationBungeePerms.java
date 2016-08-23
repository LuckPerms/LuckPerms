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
import me.lucko.luckperms.utils.ArgumentChecker;
import net.alpenblock.bungeeperms.*;

import java.util.List;
import java.util.Map;

/**
 * BungeePerms is actually pretty nice. huh.
 */
public class MigrationBungeePerms extends SubCommand<Object> {
    public MigrationBungeePerms() {
        super("bungeeperms", "Migration from BungeePerms", "/%s migration bungeeperms",
                Permission.MIGRATION, Predicate.alwaysFalse());
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object o, List<String> args, String label) {
        final Logger log = plugin.getLog();

        BungeePerms bp = BungeePerms.getInstance();
        if (bp == null) {
            log.severe("BungeePerms Migration: Error -> BungeePerms is not loaded.");
            return CommandResult.STATE_ERROR;
        }

        // Migrate all users.
        log.info("BungeePerms Migration: Starting user migration.");
        int userCount = 0;
        for (User u : bp.getPermissionsManager().getBackEnd().loadUsers()) {
            if (u.getUUID() == null) continue;

            userCount++;
            plugin.getDatastore().loadOrCreateUser(u.getUUID(), "null");
            me.lucko.luckperms.users.User user = plugin.getUserManager().get(u.getUUID());

            for (String perm : u.getPerms()) {
                try {
                    user.setPermission(perm, true);
                } catch (ObjectAlreadyHasException ignored) {}
            }

            for (Map.Entry<String, Server> e : u.getServers().entrySet()) {
                for (String perm : e.getValue().getPerms()) {
                    try {
                        user.setPermission(perm, true, e.getKey());
                    } catch (ObjectAlreadyHasException ignored) {}
                }

                for (Map.Entry<String, World> we : e.getValue().getWorlds().entrySet()) {
                    for (String perm : we.getValue().getPerms()) {
                        try {
                            user.setPermission(perm, true, e.getKey(), we.getKey());
                        } catch (ObjectAlreadyHasException ignored) {}
                    }
                }
            }

            for (String group : u.getGroupsString()) {
                try {
                    user.setPermission("group." + group, true);
                } catch (ObjectAlreadyHasException ignored) {}
            }

            String prefix = u.getPrefix();
            String suffix = u.getSuffix();

            if (prefix != null && !prefix.equals("")) {
                prefix = ArgumentChecker.escapeCharacters(prefix);
                try {
                    user.setPermission("prefix.100." + prefix, true);
                } catch (ObjectAlreadyHasException ignored) {}
            }

            if (suffix != null && !suffix.equals("")) {
                suffix = ArgumentChecker.escapeCharacters(suffix);
                try {
                    user.setPermission("suffix.100." + suffix, true);
                } catch (ObjectAlreadyHasException ignored) {}
            }

            plugin.getDatastore().saveUser(user);
            plugin.getUserManager().cleanup(user);
        }

        log.info("BungeePerms Migration: Migrated " + userCount + " users.");

        // Migrate all groups.
        log.info("BungeePerms Migration: Starting group migration.");
        int groupCount = 0;
        for (Group g : bp.getPermissionsManager().getBackEnd().loadGroups()) {
            groupCount ++;
            plugin.getDatastore().createAndLoadGroup(g.getName().toLowerCase());
            me.lucko.luckperms.groups.Group group = plugin.getGroupManager().get(g.getName().toLowerCase());

            for (String perm : g.getPerms()) {
                try {
                    group.setPermission(perm, true);
                } catch (ObjectAlreadyHasException ignored) {}
            }

            for (Map.Entry<String, Server> e : g.getServers().entrySet()) {
                for (String perm : e.getValue().getPerms()) {
                    try {
                        group.setPermission(perm, true, e.getKey());
                    } catch (ObjectAlreadyHasException ignored) {}
                }

                for (Map.Entry<String, World> we : e.getValue().getWorlds().entrySet()) {
                    for (String perm : we.getValue().getPerms()) {
                        try {
                            group.setPermission(perm, true, e.getKey(), we.getKey());
                        } catch (ObjectAlreadyHasException ignored) {}
                    }
                }
            }

            for (String inherit : g.getInheritances()) {
                try {
                    group.setPermission("group." + inherit, true);
                } catch (ObjectAlreadyHasException ignored) {}
            }

            String prefix = g.getPrefix();
            String suffix = g.getSuffix();

            if (prefix != null && !prefix.equals("")) {
                prefix = ArgumentChecker.escapeCharacters(prefix);
                try {
                    group.setPermission("prefix.50." + prefix, true);
                } catch (ObjectAlreadyHasException ignored) {}
            }

            if (suffix != null && !suffix.equals("")) {
                suffix = ArgumentChecker.escapeCharacters(suffix);
                try {
                    group.setPermission("suffix.50." + suffix, true);
                } catch (ObjectAlreadyHasException ignored) {}
            }



            plugin.getDatastore().saveGroup(group);
        }

        log.info("BungeePerms Migration: Migrated " + groupCount + " groups");
        log.info("BungeePerms Migration: Success! Completed without any errors.");
        return CommandResult.SUCCESS;
    }
}
