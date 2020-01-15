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

package me.lucko.luckperms.bungee;

import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.command.utils.ArgumentTokenizer;
import me.lucko.luckperms.common.sender.Sender;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.List;

public class BungeeCommandExecutor extends Command implements TabExecutor {
    private final LPBungeePlugin plugin;
    private final CommandManager manager;

    public BungeeCommandExecutor(LPBungeePlugin plugin, CommandManager manager) {
        super("luckpermsbungee", null, "lpb", "bperm", "bperms", "bpermission", "bpermissions");
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Sender wrapped = this.plugin.getSenderFactory().wrap(sender);
        List<String> arguments = ArgumentTokenizer.EXECUTE.tokenizeInput(args);
        this.manager.executeCommand(wrapped, "lpb", arguments);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        Sender wrapped = this.plugin.getSenderFactory().wrap(sender);
        List<String> arguments = ArgumentTokenizer.TAB_COMPLETE.tokenizeInput(args);
        return this.manager.tabCompleteCommand(wrapped, arguments);
    }
}
