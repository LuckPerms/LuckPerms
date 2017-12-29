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

package me.lucko.luckperms.common.commands.impl.migration;

import com.google.common.collect.ImmutableMap;

import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandPermission;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.Command;
import me.lucko.luckperms.common.commands.abstraction.MainCommand;
import me.lucko.luckperms.common.commands.abstraction.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

public class MigrationMainCommand extends MainCommand<Object, Object> {
    private static final Map<String, String> PLUGINS = ImmutableMap.<String, String>builder()
            .put("org.anjocaido.groupmanager.GroupManager", "me.lucko.luckperms.bukkit.migration.MigrationGroupManager")
            .put("ru.tehkode.permissions.bukkit.PermissionsEx", "me.lucko.luckperms.bukkit.migration.MigrationPermissionsEx")
            .put("com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin", "me.lucko.luckperms.bukkit.migration.MigrationPowerfulPerms")
            .put("org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService", "me.lucko.luckperms.bukkit.migration.MigrationZPermissions")
            .put("net.alpenblock.bungeeperms.BungeePerms", "me.lucko.luckperms.bungee.migration.MigrationBungeePerms")
            .put("de.bananaco.bpermissions.api.WorldManager", "me.lucko.luckperms.bukkit.migration.MigrationBPermissions")
            .put("ninja.leaping.permissionsex.sponge.PermissionsExPlugin", "me.lucko.luckperms.sponge.migration.MigrationPermissionsEx")
            .put("io.github.djxy.permissionmanager.sponge.SpongePlugin", "me.lucko.luckperms.sponge.migration.MigrationPermissionManager")
            .build();

    private final ReentrantLock lock = new ReentrantLock();
    private List<Command<Object, ?>> commands = null;
    private boolean display = true;

    public MigrationMainCommand(LocaleManager locale) {
        super(CommandSpec.MIGRATION.spec(locale), "Migration", 1, null);
    }

    @Override
    public synchronized Optional<List<Command<Object, ?>>> getChildren() {
        if (commands == null) {
            commands = getAvailableCommands(getSpec().getLocaleManager());

            // Add dummy command to show in the list.
            if (commands.isEmpty()) {
                display = false;
                commands.add(new SubCommand<Object>(CommandSpec.MIGRATION_COMMAND.spec(getSpec().getLocaleManager()), "No available plugins to migrate from", CommandPermission.MIGRATION, Predicates.alwaysFalse()) {
                    @Override
                    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object o, List<String> args, String label) throws CommandException {
                        return CommandResult.SUCCESS;
                    }
                });
            }
        }

        return Optional.of(commands);
    }

    @Override
    public boolean isAuthorized(Sender sender) {
        return sender.hasPermission(CommandPermission.MIGRATION);
    }

    @Override
    public boolean shouldDisplay() {
        getChildren();
        return display;
    }

    @SuppressWarnings("unchecked")
    private static List<Command<Object, ?>> getAvailableCommands(LocaleManager locale) {
        List<Command<Object, ?>> l = new ArrayList<>();

        for (Map.Entry<String, String> plugin : PLUGINS.entrySet()) {
            try {
                Class.forName(plugin.getKey());
                l.add((SubCommand<Object>) Class.forName(plugin.getValue()).getConstructor(LocaleManager.class).newInstance(locale));
            } catch (Throwable ignored) {}
        }

        return l;
    }

    @Override
    protected ReentrantLock getLockForTarget(Object target) {
        return lock; // share a lock between all migration commands
    }

    /* Dummy */

    @Override
    protected List<String> getTargets(LuckPermsPlugin plugin) {
        return Collections.emptyList(); // only used for tab complete, we're not bothered about it for this command.
    }

    @Override
    protected Object parseTarget(String target, LuckPermsPlugin plugin, Sender sender) {
        return this; // can't return null, but we don't need a target
    }

    @Override
    protected Object getTarget(Object target, LuckPermsPlugin plugin, Sender sender) {
        return this; // can't return null, but we don't need a target
    }

    @Override
    protected void cleanup(Object o, LuckPermsPlugin plugin) {

    }

}
