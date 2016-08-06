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

package me.lucko.luckperms;

import me.lucko.luckperms.commands.CommandManager;
import me.lucko.luckperms.commands.SenderFactory;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Arrays;

class BungeeCommand extends Command implements TabExecutor {
    private static final Factory FACTORY = new Factory();
    private final CommandManager manager;

    public BungeeCommand(CommandManager manager) {
        super("luckpermsbungee", null, "bperms", "lpb", "bpermissions", "bp", "bperm");
        this.manager = manager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        manager.onCommand(FACTORY.wrap(sender), "bperms", Arrays.asList(args));
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return manager.onTabComplete(FACTORY.wrap(sender), Arrays.asList(args));
    }

    private static class Factory extends SenderFactory<CommandSender> {

        @Override
        protected void sendMessage(CommandSender sender, String s) {
            sender.sendMessage(new TextComponent(s));
        }

        @Override
        protected boolean hasPermission(CommandSender sender, String node) {
            return sender.hasPermission(node);
        }
    }
}
