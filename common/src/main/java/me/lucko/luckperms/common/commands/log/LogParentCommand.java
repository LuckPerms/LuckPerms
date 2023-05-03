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

package me.lucko.luckperms.common.commands.log;

import com.google.common.collect.ImmutableList;
import me.lucko.luckperms.common.actionlog.Log;
import me.lucko.luckperms.common.command.abstraction.Command;
import me.lucko.luckperms.common.command.abstraction.ParentCommand;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class LogParentCommand extends ParentCommand<Log, Void> {
    private final ReentrantLock lock = new ReentrantLock();

    public LogParentCommand() {
        super(CommandSpec.LOG, "Log", Type.NO_TARGET_ARGUMENT, ImmutableList.<Command<Log>>builder()
                .add(new LogRecent())
                .add(new LogSearch())
                .add(new LogNotify())
                .add(new LogUserHistory())
                .add(new LogGroupHistory())
                .add(new LogTrackHistory())
                .build()
        );
    }

    @Override
    protected ReentrantLock getLockForTarget(Void target) {
        return this.lock; // all commands target the same log, so we share a lock between all "targets"
    }

    @Override
    protected Log getTarget(Void target, LuckPermsPlugin plugin, Sender sender) {
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
        // should never be called if we specify Type.NO_TARGET_ARGUMENT in the constructor
        throw new UnsupportedOperationException();
    }

    @Override
    protected Void parseTarget(String target, LuckPermsPlugin plugin, Sender sender) {
        // should never be called if we specify Type.NO_TARGET_ARGUMENT in the constructor
        throw new UnsupportedOperationException();
    }

}
