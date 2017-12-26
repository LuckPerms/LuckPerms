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

package me.lucko.luckperms.common.commands.impl.log;

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.common.actionlog.Log;
import me.lucko.luckperms.common.commands.abstraction.Command;
import me.lucko.luckperms.common.commands.abstraction.MainCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class LogMainCommand extends MainCommand<Log, Object> {
    private final ReentrantLock lock = new ReentrantLock();

    public LogMainCommand(LocaleManager locale) {
        super(CommandSpec.LOG.spec(locale), "Log", 1, ImmutableList.<Command<Log, ?>>builder()
                .add(new LogRecent(locale))
                .add(new LogSearch(locale))
                .add(new LogNotify(locale))
                .add(new LogUserHistory(locale))
                .add(new LogGroupHistory(locale))
                .add(new LogTrackHistory(locale))
                .build()
        );
    }

    @Override
    protected ReentrantLock getLockForTarget(Object target) {
        return lock; // all commands target the same log, so we share a lock between all "targets"
    }

    @Override
    protected Object parseTarget(String target, LuckPermsPlugin plugin, Sender sender) {
        return this;
    }

    @Override
    protected Log getTarget(Object target, LuckPermsPlugin plugin, Sender sender) {
        Log log = plugin.getStorage().getLog().join();

        if (log == null) {
            Message.LOG_LOAD_ERROR.send(sender);
        }

        return log;
    }

    @Override
    protected void cleanup(Log log, LuckPermsPlugin plugin) {

    }

    @Override
    protected List<String> getTargets(LuckPermsPlugin plugin) {
        return null; // only used for tab completion in super, and we override this method
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        final List<Command<Log, ?>> subs = getChildren().get().stream()
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

        Optional<Command<Log, ?>> o = subs.stream()
                .filter(s -> s.getName().equalsIgnoreCase(args.get(0)))
                .limit(1)
                .findAny();

        return o.map(cmd -> cmd.tabComplete(plugin, sender, args.subList(1, args.size()))).orElseGet(Collections::emptyList);
    }
}
