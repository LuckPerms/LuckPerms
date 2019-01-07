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

package me.lucko.luckperms.velocity;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;

import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.sender.Sender;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;

public class VelocityCommandExecutor implements Command {
    private static final Splitter TAB_COMPLETE_ARGUMENT_SPLITTER = Splitter.on(CommandManager.COMMAND_SEPARATOR_PATTERN);
    private static final Splitter ARGUMENT_SPLITTER = Splitter.on(CommandManager.COMMAND_SEPARATOR_PATTERN).omitEmptyStrings();
    private static final Joiner ARGUMENT_JOINER = Joiner.on(' ');

    private final LPVelocityPlugin plugin;
    private final CommandManager manager;

    public VelocityCommandExecutor(LPVelocityPlugin plugin, CommandManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public void execute(@NonNull CommandSource source, @NonNull String[] args) {
        Sender lpSender = this.plugin.getSenderFactory().wrap(source);
        List<String> arguments = CommandManager.stripQuotes(ARGUMENT_SPLITTER.splitToList(ARGUMENT_JOINER.join(args)));

        this.manager.onCommand(lpSender, "lpv", arguments);
    }

    @Override
    public List<String> suggest(@NonNull CommandSource source, @NonNull String[] args) {
        Sender lpSender = this.plugin.getSenderFactory().wrap(source);
        List<String> arguments = CommandManager.stripQuotes(TAB_COMPLETE_ARGUMENT_SPLITTER.splitToList(ARGUMENT_JOINER.join(args)));

        return this.manager.onTabComplete(lpSender, arguments);
    }
}
