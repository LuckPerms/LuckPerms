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

package me.lucko.luckperms.common.commands.misc;

import me.lucko.luckperms.api.Logger;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.SingleCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.constants.Constants;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.core.model.Track;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.storage.Storage;
import me.lucko.luckperms.common.utils.Predicates;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ExportCommand extends SingleCommand {
    private static void write(BufferedWriter writer, String s) {
        try {
            writer.write(s);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String nodeToString(Node node, String id, boolean group) {
        StringBuilder sb = new StringBuilder();
        sb.append("/luckperms ").append(group ? "group " : "user ").append(id).append(" ");

        if (node.isGroupNode()) {
            if (node.isTemporary()) {
                sb.append("parent addtemp ");
                sb.append(node.getGroupName());
                sb.append(" ").append(node.getExpiryUnixTime());
            } else {
                sb.append("parent add ");
                sb.append(node.getGroupName());
            }

            if (node.isWorldSpecific()) {
                sb.append(" ").append(node.getServer().get()).append(" ").append(node.getWorld().get());
            } else if (node.isServerSpecific()) {
                sb.append(" ").append(node.getServer().get());
            }

            return sb.toString();
        }

        sb.append(node.isTemporary() ? "permission settemp " : "permission set ");
        if (node.getPermission().contains(" ")) {
            sb.append("\"").append(node.getPermission()).append("\"");
        } else {
            sb.append(node.getPermission());
        }
        sb.append(" ").append(node.getValue());

        if (node.isTemporary()) {
            sb.append(" ").append(node.getExpiryUnixTime());
        }

        if (node.isWorldSpecific()) {
            sb.append(" ").append(node.getServer().get()).append(" ").append(node.getWorld().get());
        } else if (node.isServerSpecific()) {
            sb.append(" ").append(node.getServer().get());
        }

        return sb.toString();
    }

    public ExportCommand() {
        super("Export", "Export data to a file", "/%s export <file>", Permission.MIGRATION, Predicates.not(1),
                Arg.list(
                        Arg.create("file", true, "the file to export to")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        final Logger log = plugin.getLog();

        if (!sender.getUuid().equals(Constants.getConsoleUUID())) {
            Message.MIGRATION_NOT_CONSOLE.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        File f = new File(plugin.getMainDir(), args.get(0));
        if (f.exists()) {
            Message.LOG_EXPORT_ALREADY_EXISTS.send(sender, f.getAbsolutePath());
            return CommandResult.INVALID_ARGS;
        }

        try {
            f.createNewFile();
        } catch (IOException e) {
            Message.LOG_EXPORT_FAILURE.send(sender);
            e.printStackTrace();
            return CommandResult.FAILURE;
        }

        if (!Files.isWritable(f.toPath())) {
            Message.LOG_EXPORT_NOT_WRITABLE.send(sender, f.getAbsolutePath());
            return CommandResult.FAILURE;
        }

        try (FileWriter fWriter = new FileWriter(f, true); BufferedWriter writer = new BufferedWriter(fWriter)) {
            log.info("Export: Starting export process.");

            // Export Groups
            log.info("Export: Exporting all groups.");

            // Create the groups first
            for (Group group : plugin.getGroupManager().getAll().values()) {
                write(writer, "/luckperms creategroup " + group.getName());
            }

            int groupCount = 0;
            for (Group group : plugin.getGroupManager().getAll().values()) {
                groupCount++;
                for (Node node : group.getNodes()) {
                    write(writer, nodeToString(node, group.getName(), true));
                }
            }
            log.info("Export: Exported " + groupCount + " groups.");

            // Export tracks
            log.info("Export: Exporting all tracks.");

            // Create the tracks first
            for (Track track : plugin.getTrackManager().getAll().values()) {
                write(writer, "/luckperms createtrack " + track.getName());
            }

            int trackCount = 0;
            for (Track track : plugin.getTrackManager().getAll().values()) {
                trackCount++;
                for (String group : track.getGroups()) {
                    write(writer, "/luckperms track " + track.getName() + " append " + group);
                }
            }
            log.info("Export: Exported " + trackCount + " tracks.");

            // Export users
            log.info("Export: Exporting all users. Finding a list of unique users to export.");
            Storage ds = plugin.getStorage();
            Set<UUID> users = ds.getUniqueUsers().join();
            log.info("Export: Found " + users.size() + " unique users to export.");

            int userCount = 0;
            for (UUID uuid : users) {
                userCount++;
                plugin.getStorage().loadUser(uuid, "null").join();
                User user = plugin.getUserManager().get(uuid);

                boolean inDefault = false;
                for (Node node : user.getNodes()) {
                    if (node.isGroupNode() && node.getGroupName().equalsIgnoreCase("default")) {
                        inDefault = true;
                        continue;
                    }

                    write(writer, nodeToString(node, user.getUuid().toString(), false));
                }

                if (!user.getPrimaryGroup().equalsIgnoreCase("default")) {
                    write(writer, "/luckperms user " + user.getUuid().toString() + " setprimarygroup " + user.getPrimaryGroup());
                }

                if (!inDefault) {
                    write(writer, "/luckperms user " + user.getUuid().toString() + " parent remove default");
                }

                plugin.getUserManager().cleanup(user);
            }
            log.info("Export: Exported " + userCount + " users.");

            try {
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Message.LOG_EXPORT_SUCCESS.send(sender, f.getAbsolutePath());
            return CommandResult.SUCCESS;
        } catch (Throwable t) {
            t.printStackTrace();
            return CommandResult.FAILURE;
        }
    }

    @Override
    public boolean isAuthorized(Sender sender) {
        return sender.getUuid().equals(Constants.getConsoleUUID());
    }

}
