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

package me.lucko.luckperms.bukkit;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;

import me.lucko.luckperms.common.command.utils.ArgumentTokenizer;
import me.lucko.luckperms.common.sender.Sender;

import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

public class BukkitAsyncCommandExecutor extends BukkitCommandExecutor implements Listener {
    public BukkitAsyncCommandExecutor(LPBukkitPlugin plugin, PluginCommand command) {
        super(plugin, command);
    }

    public void register() {
        super.register();
        this.plugin.getBootstrap().getServer().getPluginManager().registerEvents(this, this.plugin.getBootstrap());
    }

    @EventHandler(ignoreCancelled = true)
    public void onAsyncTabComplete(AsyncTabCompleteEvent e) {
        if (!e.isCommand()) {
            return;
        }

        String buffer = e.getBuffer();
        if (buffer.isEmpty()) {
            return;
        }

        if (buffer.charAt(0) == '/') {
            buffer = buffer.substring(1);
        }

        int firstSpace = buffer.indexOf(' ');
        if (firstSpace < 0) {
            return;
        }

        String commandLabel = buffer.substring(0, firstSpace);
        Command command = this.plugin.getBootstrap().getServer().getCommandMap().getCommand(commandLabel);
        if (command != this.command) {
            return;
        }

        Sender wrapped = this.plugin.getSenderFactory().wrap(e.getSender());
        List<String> arguments = ArgumentTokenizer.TAB_COMPLETE.tokenizeInput(buffer.substring(firstSpace + 1));
        List<String> completions = tabCompleteCommand(wrapped, arguments);

        e.setCompletions(completions);
        e.setHandled(true);
    }

}
