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

package me.lucko.luckperms.common.commands.impl.migration;

import com.google.common.collect.ImmutableMap;

import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.Command;
import me.lucko.luckperms.common.commands.abstraction.MainCommand;
import me.lucko.luckperms.common.commands.abstraction.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class MigrationMainCommand extends MainCommand<Object> {
    private static final Map<String, String> PLUGINS = ImmutableMap.<String, String>builder()
            .put("org.anjocaido.groupmanager.GroupManager", "me.lucko.luckperms.bukkit.migration.MigrationGroupManager")
            .put("ru.tehkode.permissions.bukkit.PermissionsEx", "me.lucko.luckperms.bukkit.migration.MigrationPermissionsEx")
            .put("com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin", "me.lucko.luckperms.bukkit.migration.MigrationPowerfulPerms")
            .put("org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService", "me.lucko.luckperms.bukkit.migration.MigrationZPermissions")
            .put("net.alpenblock.bungeeperms.BungeePerms", "me.lucko.luckperms.bungee.migration.MigrationBungeePerms")
            .put("de.bananaco.bpermissions.api.WorldManager", "me.lucko.luckperms.bukkit.migration.MigrationBPermissions")
            .put("ninja.leaping.permissionsex.sponge.PermissionsExPlugin", "me.lucko.luckperms.sponge.migration.MigrationPermissionsEx")
            .put("io.github.djxy.permissionmanager.sponge.SpongePlugin", "me.lucko.luckperms.sponge.migration.MigrationPermissionManager")
            .build();

    private List<Command<Object, ?>> commands = null;
    private boolean display = true;

    public MigrationMainCommand() {
        super("Migration", "Migration commands", "/%s migration", 1, null);
    }

    @Override
    public synchronized Optional<List<Command<Object, ?>>> getChildren() {
        if (commands == null) {
            commands = getAvailableCommands();

            // Add dummy command to show in the list.
            if (commands.isEmpty()) {
                display = false;
                commands.add(new SubCommand<Object>("No available plugins to migrate from", "No available plugins to migrate from.", Permission.MIGRATION, Predicates.alwaysFalse(), null) {
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
        return sender.hasPermission(Permission.MIGRATION);
    }

    @Override
    public boolean shouldDisplay() {
        getChildren();
        return display;
    }

    @SuppressWarnings("unchecked")
    private static List<Command<Object, ?>> getAvailableCommands() {
        List<SubCommand<Object>> l = new ArrayList<>();

        for (Map.Entry<String, String> plugin : PLUGINS.entrySet()) {
            try {
                Class.forName(plugin.getKey());
                l.add((SubCommand<Object>) Class.forName(plugin.getValue()).newInstance());
            } catch (Throwable ignored) {}
        }

        return l.stream().collect(Collectors.toList());
    }

    /* Dummy */

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
