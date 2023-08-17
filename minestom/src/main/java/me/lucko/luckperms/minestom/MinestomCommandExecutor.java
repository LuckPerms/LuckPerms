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

package me.lucko.luckperms.minestom;

import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.command.utils.ArgumentTokenizer;
import me.lucko.luckperms.common.sender.Sender;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MinestomCommandExecutor extends CommandManager {
    private final LuckPermsCommand command;
    private final LPMinestomPlugin plugin;

    public MinestomCommandExecutor(LPMinestomPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
        this.command = new LuckPermsCommand(this);
    }

    public void register() {
        MinecraftServer.getCommandManager().register(this.command);
    }

    public void unregister() {
        MinecraftServer.getCommandManager().unregister(this.command);
    }

    private class LuckPermsCommand extends Command {
        private final MinestomCommandExecutor commandExecutor;

        public LuckPermsCommand(@NotNull MinestomCommandExecutor commandExecutor) {
            super("luckperms", "lp", "perm", "perms", "permission", "permissions");
            this.commandExecutor = commandExecutor;

            final var params = ArgumentType.StringArray("params");

            params.setSuggestionCallback((sender, context, suggestion) -> {
                Sender wrapped = this.commandExecutor.plugin.getSenderFactory().wrap(sender);
                String input = context.getInput();
                String[] split = input.split(" ", 2);
                String args = split.length > 1 ? split[1] : "";
                List<String> arguments = ArgumentTokenizer.TAB_COMPLETE.tokenizeInput(args);
                tabCompleteCommand(wrapped, arguments).stream().map(SuggestionEntry::new).forEach(suggestion::addEntry);
            });

            setDefaultExecutor((sender, context) -> process(sender, context.getCommandName(), new String[0]));

            addSyntax((sender, context) -> process(sender, context.getCommandName(), context.get(params)), params);
        }

        public void process(@NotNull CommandSender sender, @NotNull String command, @NotNull String[] args) {
            List<String> arguments = ArgumentTokenizer.EXECUTE.tokenizeInput(args);

            this.commandExecutor.executeCommand(this.commandExecutor.plugin.getSenderFactory().wrap(sender), command, arguments);
        }
    }
}
