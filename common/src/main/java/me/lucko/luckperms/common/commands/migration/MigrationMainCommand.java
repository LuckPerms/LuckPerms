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

package me.lucko.luckperms.common.commands.migration;

import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.Command;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.MainCommand;
import me.lucko.luckperms.common.commands.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.constants.Constants;
import me.lucko.luckperms.common.constants.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MigrationMainCommand extends MainCommand<Object> {
    @SuppressWarnings("unchecked")
    private static List<Command<Object, ?>> getAvailableCommands() {
        List<SubCommand<Object>> l = new ArrayList<>();

        try {
            Class.forName("org.anjocaido.groupmanager.GroupManager");
            l.add((SubCommand<Object>) Class.forName("me.lucko.luckperms.bukkit.migration.MigrationGroupManager").newInstance());
        } catch (Throwable ignored) {
        }

        try {
            Class.forName("ru.tehkode.permissions.bukkit.PermissionsEx");
            l.add((SubCommand<Object>) Class.forName("me.lucko.luckperms.bukkit.migration.MigrationPermissionsEx").newInstance());
        } catch (Throwable ignored) {
        }

        try {
            Class.forName("com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin");
            l.add((SubCommand<Object>) Class.forName("me.lucko.luckperms.bukkit.migration.MigrationPowerfulPerms").newInstance());
        } catch (Throwable ignored) {
        }

        try {
            Class.forName("org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService");
            l.add((SubCommand<Object>) Class.forName("me.lucko.luckperms.bukkit.migration.MigrationZPermissions").newInstance());
        } catch (Throwable ignored) {
        }

        try {
            Class.forName("net.alpenblock.bungeeperms.BungeePerms");
            l.add((SubCommand<Object>) Class.forName("me.lucko.luckperms.bungee.migration.MigrationBungeePerms").newInstance());
        } catch (Throwable ignored) {
        }

        try {
            Class.forName("de.bananaco.bpermissions.api.WorldManager");
            l.add((SubCommand<Object>) Class.forName("me.lucko.luckperms.bukkit.migration.MigrationBPermissions").newInstance());
        } catch (Throwable ignored) {
        }

        try {
            Class.forName("ninja.leaping.permissionsex.sponge.PermissionsExPlugin");
            l.add((SubCommand<Object>) Class.forName("me.lucko.luckperms.sponge.migration.MigrationPermissionsEx").newInstance());
        } catch (Throwable ignored) {
        }

        try {
            Class.forName("io.github.djxy.permissionmanager.PermissionManager");
            l.add((SubCommand<Object>) Class.forName("me.lucko.luckperms.sponge.migration.MigrationPermissionManager").newInstance());
        } catch (Throwable ignored) {
        }

        return l.stream().collect(Collectors.toList());
    }

    private List<Command<Object, ?>> commands = null;

    public MigrationMainCommand() {
        super("Migration", "Migration commands", "/%s migration", 1, null);
    }

    @SuppressWarnings("deprecation")
    @Deprecated
    @Override
    public synchronized Optional<List<Command<Object, ?>>> getChildren() {
        if (commands == null) {
            commands = getAvailableCommands();
        }

        return Optional.of(commands);
    }

    @SuppressWarnings("deprecation")
    public List<Command<Object, ?>> getSubCommands() {
        return getChildren().orElse(null);
    }

    @Override
    public boolean isAuthorized(Sender sender) {
        return sender.getUuid().equals(Constants.CONSOLE_UUID);
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Void v, List<String> args, String label) throws CommandException {
        if (!sender.getUuid().equals(Constants.CONSOLE_UUID)) {
            Message.MIGRATION_NOT_CONSOLE.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        return super.execute(plugin, sender, v, args, label);
    }

    @Override
    protected List<String> getTargets(LuckPermsPlugin plugin) {
        return Collections.emptyList();
    }

    @Override
    protected Object getTarget(String target, LuckPermsPlugin plugin, Sender sender) {
        return new Object();
    }

    @Override
    protected void cleanup(Object o, LuckPermsPlugin plugin) {

    }

}
