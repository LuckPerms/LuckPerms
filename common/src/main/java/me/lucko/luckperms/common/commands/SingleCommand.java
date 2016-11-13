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

package me.lucko.luckperms.common.commands;

import lombok.Getter;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Permission;

import java.util.List;
import java.util.function.Predicate;

public abstract class SingleCommand extends BaseCommand<Void, Void> {

    @Getter
    private final String usage;

    public SingleCommand(String name, String description, String usage, Permission permission, Predicate<Integer> argumentCheck, List<Arg> args) {
        super(name, description, permission, argumentCheck, args, null);
        this.usage = usage;
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Void v, List<String> args, String label) throws CommandException {
        return execute(plugin, sender, args, label);
    }

    public abstract CommandResult execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) throws CommandException;

    @Override
    public void sendUsage(Sender sender, String label) {
        StringBuilder sb = new StringBuilder();
        if (getArgs().isPresent()) {
            sb.append("&3 - &7");
            for (Arg arg : getArgs().get()) {
                sb.append(arg.asPrettyString()).append(" ");
            }
        }

        Util.sendPluginMessage(sender, "&3> &a" + getName().toLowerCase() + sb.toString());
    }

    @Override
    public void sendDetailedUsage(Sender sender, String label) {
        Util.sendPluginMessage(sender, "&3&lCommand Usage &3- &b" + getName());
        Util.sendPluginMessage(sender, "&b> &7" + getDescription());
        if (getArgs().isPresent()) {
            Util.sendPluginMessage(sender, "&3Arguments:");
            for (Arg arg : getArgs().get()) {
                Util.sendPluginMessage(sender, "&b- " + arg.asPrettyString() + "&3 -> &7" + arg.getDescription());
            }
        }
    }
}
