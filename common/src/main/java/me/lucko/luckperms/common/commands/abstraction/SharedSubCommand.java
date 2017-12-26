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

package me.lucko.luckperms.common.commands.abstraction;

import lombok.Getter;

import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandPermission;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.CommandUtils;
import me.lucko.luckperms.common.locale.LocalizedSpec;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * A sub command which can be be applied to both groups and users.
 * This doesn't extend the other Command or SubCommand classes to avoid generics hell.
 */
@Getter
public abstract class SharedSubCommand {

    private final LocalizedSpec spec;

    /**
     * The name of the sub command
     */
    private final String name;

    /**
     * The permission needed to use this command
     */
    private final CommandPermission userPermission;
    private final CommandPermission groupPermission;

    /**
     * Predicate to test if the argument length given is invalid
     */
    private final Predicate<? super Integer> argumentCheck;

    public SharedSubCommand(LocalizedSpec spec, String name, CommandPermission userPermission, CommandPermission groupPermission, Predicate<? super Integer> argumentCheck) {
        this.spec = spec;
        this.name = name;
        this.userPermission = userPermission;
        this.groupPermission = groupPermission;
        this.argumentCheck = argumentCheck;
    }

    public abstract CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args, String label, CommandPermission permission) throws CommandException;

    public List<String> onTabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return Collections.emptyList();
    }

    public void sendUsage(Sender sender) {
        StringBuilder sb = new StringBuilder();
        if (getArgs() != null) {
            sb.append("&3 - &7");
            for (Arg arg : getArgs()) {
                sb.append(arg.asPrettyString()).append(" ");
            }
        }

        CommandUtils.sendPluginMessage(sender, "&3> &a" + getName() + sb.toString());
    }

    public void sendDetailedUsage(Sender sender) {
        CommandUtils.sendPluginMessage(sender, "&3&lCommand Usage &3- &b" + getName());
        CommandUtils.sendPluginMessage(sender, "&b> &7" + getDescription());
        if (getArgs() != null) {
            CommandUtils.sendPluginMessage(sender, "&3Arguments:");
            for (Arg arg : getArgs()) {
                CommandUtils.sendPluginMessage(sender, "&b- " + arg.asPrettyString() + "&3 -> &7" + arg.getDescription());
            }
        }
    }

    public boolean isAuthorized(Sender sender, boolean user) {
        return user ? userPermission.isAuthorized(sender) : groupPermission.isAuthorized(sender);
    }

    public String getDescription() {
        return spec.description();
    }

    public List<Arg> getArgs() {
        return spec.args();
    }

    public static void save(PermissionHolder holder, Sender sender, LuckPermsPlugin plugin) {
        if (holder.getType().isUser()) {
            User user = ((User) holder);
            SubCommand.save(user, sender, plugin);
            return;
        }

        if (holder.getType().isGroup()) {
            Group group = ((Group) holder);
            SubCommand.save(group, sender, plugin);
            return;
        }

        throw new IllegalArgumentException();
    }

}
