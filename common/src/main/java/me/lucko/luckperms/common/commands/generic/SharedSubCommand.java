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

package me.lucko.luckperms.common.commands.generic;

import lombok.AllArgsConstructor;
import lombok.Getter;

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.core.model.PermissionHolder;
import me.lucko.luckperms.common.core.model.User;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

@Getter
@AllArgsConstructor
public abstract class SharedSubCommand {

    protected static void save(PermissionHolder holder, Sender sender, LuckPermsPlugin plugin) {
        if (holder instanceof User) {
            User user = ((User) holder);
            SubCommand.save(user, sender, plugin);
            return;
        }

        if (holder instanceof Group) {
            Group group = ((Group) holder);
            SubCommand.save(group, sender, plugin);
            return;
        }

        throw new IllegalArgumentException();
    }

    /**
     * The name of the sub command
     */
    private final String name;
    /**
     * A brief description of what the sub command does
     */
    private final String description;
    /**
     * The permission needed to use this command
     */
    private final Permission userPermission;
    private final Permission groupPermission;
    /**
     * Predicate to test if the argument length given is invalid
     */
    private final Predicate<? super Integer> isArgumentInvalid;
    private final ImmutableList<Arg> args;

    public abstract CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args, String label) throws CommandException;

    public List<String> onTabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return Collections.emptyList();
    }

    public void sendUsage(Sender sender) {
        StringBuilder sb = new StringBuilder();
        if (args != null) {
            sb.append("&3 - &7");
            for (Arg arg : args) {
                sb.append(arg.asPrettyString()).append(" ");
            }
        }

        Util.sendPluginMessage(sender, "&3> &a" + getName() + sb.toString());
    }

    public void sendDetailedUsage(Sender sender) {
        Util.sendPluginMessage(sender, "&3&lCommand Usage &3- &b" + getName());
        Util.sendPluginMessage(sender, "&b> &7" + getDescription());
        if (args != null) {
            Util.sendPluginMessage(sender, "&3Arguments:");
            for (Arg arg : args) {
                Util.sendPluginMessage(sender, "&b- " + arg.asPrettyString() + "&3 -> &7" + arg.getDescription());
            }
        }
    }

    public boolean isAuthorized(Sender sender, boolean user) {
        return user ? userPermission.isAuthorized(sender) : groupPermission.isAuthorized(sender);
    }

}
