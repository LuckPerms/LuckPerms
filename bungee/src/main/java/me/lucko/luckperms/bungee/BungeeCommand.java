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

package me.lucko.luckperms.bungee;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import me.lucko.luckperms.api.data.Callback;
import me.lucko.luckperms.common.commands.CommandManager;
import me.lucko.luckperms.common.commands.Util;
import me.lucko.luckperms.common.constants.Patterns;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Arrays;

class BungeeCommand extends Command implements TabExecutor {
    private final LPBungeePlugin plugin;
    private final CommandManager manager;

    BungeeCommand(LPBungeePlugin plugin, CommandManager manager) {
        super("luckpermsbungee", null, "bperms", "lpb", "bpermissions", "bp", "bperm");
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        manager.onCommand(
                plugin.getSenderFactory().wrap(sender),
                "bperms",
                Util.stripQuotes(Splitter.on(Patterns.COMMAND_SEPARATOR).omitEmptyStrings().splitToList(Joiner.on(' ').join(args))),
                Callback.empty()
        );
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return manager.onTabComplete(plugin.getSenderFactory().wrap(sender), Arrays.asList(args));
    }
}
