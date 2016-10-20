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

package me.lucko.luckperms.common.commands.log;

import com.google.common.collect.ImmutableList;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.MainCommand;
import me.lucko.luckperms.common.commands.Sender;
import me.lucko.luckperms.common.commands.SubCommand;
import me.lucko.luckperms.common.commands.log.subcommands.*;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.data.Log;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class LogMainCommand extends MainCommand<Log> {
    public LogMainCommand() {
        super("Log", "/%s log", 1, ImmutableList.<SubCommand<Log>>builder()
            .add(new LogRecent())
            .add(new LogSearch())
            .add(new LogNotify())
            .add(new LogExport())
            .add(new LogUserHistory())
            .add(new LogGroupHistory())
            .add(new LogTrackHistory())
            .build()
        );
    }

    @Override
    protected Log getTarget(String target, LuckPermsPlugin plugin, Sender sender) {
        Log log = plugin.getDatastore().getLog().getOrDefault(null);

        if (log == null) {
            Message.LOG_LOAD_ERROR.send(sender);
        }

        return log;
    }

    @Override
    protected void cleanup(Log log, LuckPermsPlugin plugin) {

    }

    @Override
    protected List<String> getObjects(LuckPermsPlugin plugin) {
        return null;
    }

    @Override
    protected List<String> onTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin) {
        final List<SubCommand<Log>> subs = getSubCommands().stream()
                .filter(s -> s.isAuthorized(sender))
                .collect(Collectors.toList());

        if (args.size() <= 1) {
            if (args.isEmpty() || args.get(0).equalsIgnoreCase("")) {
                return subs.stream()
                        .map(m -> m.getName().toLowerCase())
                        .collect(Collectors.toList());
            }

            return subs.stream()
                    .map(m -> m.getName().toLowerCase())
                    .filter(s -> s.toLowerCase().startsWith(args.get(0).toLowerCase()))
                    .collect(Collectors.toList());
        }

        Optional<SubCommand<Log>> o = subs.stream()
                .filter(s -> s.getName().equalsIgnoreCase(args.get(0)))
                .limit(1)
                .findAny();

        if (!o.isPresent()) {
            return Collections.emptyList();
        }

        return o.get().onTabComplete(plugin, sender, args.subList(1, args.size()));
    }
}
