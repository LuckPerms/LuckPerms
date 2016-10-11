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

package me.lucko.luckperms.commands.meta;

import com.google.common.collect.ImmutableList;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.*;
import me.lucko.luckperms.commands.meta.subcommands.*;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.core.PermissionHolder;
import me.lucko.luckperms.users.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MetaCommands<T extends PermissionHolder> extends SubCommand<T> {
    private final List<MetaSubCommand> subCommands = ImmutableList.<MetaSubCommand>builder()
        .add(new MetaInfo())
        .add(new MetaAddPrefix())
        .add(new MetaAddSuffix())
        .add(new MetaRemovePrefix())
        .add(new MetaRemoveSuffix())
        .add(new MetaAddTempPrefix())
        .add(new MetaAddTempSuffix())
        .add(new MetaRemoveTempPrefix())
        .add(new MetaRemoveTempSuffix())
        .add(new MetaClear())
        .build();

    public MetaCommands() {
        super("meta", "Edit metadata values", null, Predicate.alwaysFalse(), null);
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, T t, List<String> args, String label) {
        boolean user = t instanceof User;

        if (args.size() == 0) {
            sendUsageMeta(sender, user, label);
            return CommandResult.INVALID_ARGS;
        }

        Optional<MetaSubCommand> o = subCommands.stream()
                .filter(s -> s.getName().equalsIgnoreCase(args.get(0)))
                .limit(1)
                .findAny();

        if (!o.isPresent()) {
            Message.COMMAND_NOT_RECOGNISED.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        final MetaSubCommand sub = o.get();
        if (!sub.isAuthorized(sender, user)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        List<String> strippedArgs = new ArrayList<>();
        if (args.size() > 1) {
            strippedArgs.addAll(args.subList(1, args.size()));
        }

        if (sub.getIsArgumentInvalid().test(strippedArgs.size())) {
            sub.sendDetailedUsage(sender);
            return CommandResult.INVALID_ARGS;
        }

        return sub.execute(plugin, sender, t, strippedArgs);
    }

    private void sendUsageMeta(Sender sender, boolean user, String label) {
        List<MetaSubCommand> subs = subCommands.stream()
                .filter(s -> s.isAuthorized(sender, user))
                .collect(Collectors.toList());

        if (subs.size() > 0) {
            if (user) {
                Util.sendPluginMessage(sender, "&bMeta Sub Commands: &7(" + String.format("/%s user <user> meta ...)", label));
            } else {
                Util.sendPluginMessage(sender, "&bMeta Sub Commands: &7(" + String.format("/%s group <group> meta ...)", label));
            }

            for (MetaSubCommand s : subs) {
                s.sendUsage(sender);
            }

        } else {
            Message.COMMAND_NO_PERMISSION.send(sender);
        }
    }
}
