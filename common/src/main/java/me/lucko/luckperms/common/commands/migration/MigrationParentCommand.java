/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
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

import com.google.common.collect.ImmutableMap;

import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.ChildCommand;
import me.lucko.luckperms.common.command.abstraction.Command;
import me.lucko.luckperms.common.command.abstraction.ParentCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class MigrationParentCommand extends ParentCommand<Object, Void> {
    private static final Map<String, String> PLUGINS = ImmutableMap.<String, String>builder()
            // bukkit
            .put("org.anjocaido.groupmanager.GroupManager",                     "me.lucko.luckperms.bukkit.migration.MigrationGroupManager")
            .put("ru.tehkode.permissions.bukkit.PermissionsEx",                 "me.lucko.luckperms.bukkit.migration.MigrationPermissionsEx")
            .put("com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin",  "me.lucko.luckperms.bukkit.migration.MigrationPowerfulPerms")
            .put("org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService", "me.lucko.luckperms.bukkit.migration.MigrationZPermissions")
            .put("de.bananaco.bpermissions.api.WorldManager",                   "me.lucko.luckperms.bukkit.migration.MigrationBPermissions")
            .put("com.platymuus.bukkit.permissions.PermissionsPlugin",          "me.lucko.luckperms.bukkit.migration.MigrationPermissionsBukkit")
            .put("nl.svenar.PowerRanks.PowerRanks",                             "me.lucko.luckperms.bukkit.migration.MigrationPowerRanks")
            // bungee
            .put("net.alpenblock.bungeeperms.BungeePerms",                      "me.lucko.luckperms.bungee.migration.MigrationBungeePerms")
            .build();

    private final ReentrantLock lock = new ReentrantLock();
    private List<Command<Object>> commands = null;
    private boolean display = true;

    public MigrationParentCommand() {
        super(CommandSpec.MIGRATION, "Migration", Type.NO_TARGET_ARGUMENT, null);
    }

    @Override
    public synchronized @NonNull List<Command<Object>> getChildren() {
        if (this.commands == null) {
            this.commands = getAvailableCommands();

            // Add dummy command to show in the list.
            if (this.commands.isEmpty()) {
                this.display = false;
                this.commands.add(new ChildCommand<Object>(CommandSpec.MIGRATION_COMMAND, "No available plugins to migrate from", CommandPermission.MIGRATION, Predicates.alwaysFalse()) {
                    @Override
                    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object o, ArgumentList args, String label) {
                        return CommandResult.SUCCESS;
                    }
                });
            }
        }
        return this.commands;
    }

    @Override
    public boolean isAuthorized(Sender sender) {
        return sender.hasPermission(CommandPermission.MIGRATION);
    }

    @Override
    public boolean shouldDisplay() {
        getChildren();
        return this.display;
    }

    @SuppressWarnings("unchecked")
    private static List<Command<Object>> getAvailableCommands() {
        List<Command<Object>> available = new ArrayList<>();

        for (Map.Entry<String, String> plugin : PLUGINS.entrySet()) {
            try {
                Class.forName(plugin.getKey());
                available.add((ChildCommand<Object>) Class.forName(plugin.getValue()).getConstructor().newInstance());
            } catch (Throwable ignored) {}
        }

        return available;
    }

    @Override
    protected ReentrantLock getLockForTarget(Void target) {
        return this.lock; // share a lock between all migration commands
    }

    /* Dummy */

    @Override
    protected List<String> getTargets(LuckPermsPlugin plugin) {
        // should never be called if we specify Type.NO_TARGET_ARGUMENT in the constructor
        throw new UnsupportedOperationException();
    }

    @Override
    protected Void parseTarget(String target, LuckPermsPlugin plugin, Sender sender) {
        // should never be called if we specify Type.NO_TARGET_ARGUMENT in the constructor
        throw new UnsupportedOperationException();
    }

    @Override
    protected Object getTarget(Void target, LuckPermsPlugin plugin, Sender sender) {
        return this; // can't return null, but we don't need a target
    }

    @Override
    protected void cleanup(Object o, LuckPermsPlugin plugin) {

    }

}
