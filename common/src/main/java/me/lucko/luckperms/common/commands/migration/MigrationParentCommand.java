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

import com.google.common.collect.ImmutableBiMap;

import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.ChildCommand;
import me.lucko.luckperms.common.command.abstraction.Command;
import me.lucko.luckperms.common.command.abstraction.ParentCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.command.CommandSpec;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class MigrationParentCommand extends ParentCommand<Object, Void> {
    private static final Map<String, String> PLUGINS = ImmutableBiMap.<String, String>builder()
            // bukkit
            .put("me.lucko.luckperms.bukkit.migration.MigrationGroupManager",       "org.anjocaido.groupmanager.GroupManager")
            .put("me.lucko.luckperms.bukkit.migration.MigrationPermissionsEx",      "ru.tehkode.permissions.bukkit.PermissionsEx")
            .put("me.lucko.luckperms.bukkit.migration.MigrationPowerfulPerms",      "com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin")
            .put("me.lucko.luckperms.bukkit.migration.MigrationZPermissions",       "org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService")
            .put("me.lucko.luckperms.bukkit.migration.MigrationBPermissions",       "de.bananaco.bpermissions.api.WorldManager")
            .put("me.lucko.luckperms.bukkit.migration.MigrationPermissionsBukkit",  "com.platymuus.bukkit.permissions.PermissionsPlugin")
            // bungee
            .put("me.lucko.luckperms.bungee.migration.MigrationBungeePerms",        "net.alpenblock.bungeeperms.BungeePerms")
            .build().inverse();

    private final ReentrantLock lock = new ReentrantLock();
    private List<Command<Object>> commands = null;
    private boolean display = true;

    public MigrationParentCommand(LocaleManager locale) {
        super(CommandSpec.MIGRATION.localize(locale), "Migration", Type.NO_TARGET_ARGUMENT, null);
    }

    @Override
    public synchronized @NonNull List<Command<Object>> getChildren() {
        if (this.commands == null) {
            this.commands = getAvailableCommands(getSpec().getLocaleManager());

            // Add dummy command to show in the list.
            if (this.commands.isEmpty()) {
                this.display = false;
                this.commands.add(new ChildCommand<Object>(CommandSpec.MIGRATION_COMMAND.localize(getSpec().getLocaleManager()), "No available plugins to migrate from", CommandPermission.MIGRATION, Predicates.alwaysFalse()) {
                    @Override
                    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object o, List<String> args, String label) {
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
    private static List<Command<Object>> getAvailableCommands(LocaleManager locale) {
        List<Command<Object>> available = new ArrayList<>();

        for (Map.Entry<String, String> plugin : PLUGINS.entrySet()) {
            try {
                Class.forName(plugin.getKey());
                available.add((ChildCommand<Object>) Class.forName(plugin.getValue()).getConstructor(LocaleManager.class).newInstance(locale));
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
