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
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.MainCommand;
import me.lucko.luckperms.common.commands.Sender;
import me.lucko.luckperms.common.commands.SubCommand;
import me.lucko.luckperms.common.constants.Constants;
import me.lucko.luckperms.common.constants.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MigrationMainCommand extends MainCommand<Object> {
    private final List<SubCommand<Object>> subCommands = new ArrayList<>();

    public MigrationMainCommand() {
        super("Migration", "/%s migration", 1, null);

        try {
            Class.forName("org.anjocaido.groupmanager.GroupManager");
            subCommands.add((SubCommand<Object>) Class.forName("me.lucko.luckperms.bukkit.migration.MigrationGroupManager").newInstance());
        } catch (Throwable ignored) {}

        try {
            Class.forName("ru.tehkode.permissions.bukkit.PermissionsEx");
            subCommands.add((SubCommand<Object>) Class.forName("me.lucko.luckperms.bukkit.migration.MigrationPermissionsEx").newInstance());
        } catch (Throwable ignored) {}

        try {
            Class.forName("com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin");
            subCommands.add((SubCommand<Object>) Class.forName("me.lucko.luckperms.bukkit.migration.MigrationPowerfulPerms").newInstance());
        } catch (Throwable ignored) {}

        try {
            Class.forName("org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService");
            subCommands.add((SubCommand<Object>) Class.forName("me.lucko.luckperms.bukkit.migration.MigrationZPermissions").newInstance());
        } catch (Throwable ignored) {}

        try {
            Class.forName("net.alpenblock.bungeeperms.BungeePerms");
            subCommands.add((SubCommand<Object>) Class.forName("me.lucko.luckperms.bungee.migration.MigrationBungeePerms").newInstance());
        } catch (Throwable ignored) {}

        try {
            Class.forName("de.bananaco.bpermissions.api.WorldManager");
            subCommands.add((SubCommand<Object>) Class.forName("me.lucko.luckperms.bukkit.migration.MigrationBPermissions").newInstance());
        } catch (Throwable ignored) {}

        try {
            Class.forName("ninja.leaping.permissionsex.sponge.PermissionsExPlugin");
            subCommands.add((SubCommand<Object>) Class.forName("me.lucko.luckperms.sponge.migration.MigrationPermissionsEx").newInstance());
        } catch (Throwable ignored) {}
    }

    @Override
    public List<SubCommand<Object>> getSubCommands() {
        return subCommands;
    }

    @Override
    protected boolean isAuthorized(Sender sender) {
        return sender.getUuid().equals(Constants.getConsoleUUID());
    }

    @Override
    protected CommandResult execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        if (!sender.getUuid().equals(Constants.getConsoleUUID())) {
            Message.MIGRATION_NOT_CONSOLE.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        return super.execute(plugin, sender, args, label);
    }

    @Override
    protected Object getTarget(String target, LuckPermsPlugin plugin, Sender sender) {
        return new Object();
    }

    @Override
    protected void cleanup(Object object, LuckPermsPlugin plugin) {

    }

    @Override
    protected List<String> getObjects(LuckPermsPlugin plugin) {
        return Collections.emptyList();
    }

    @Override
    protected List<String> onTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin) {
        return Collections.emptyList();
    }

}
