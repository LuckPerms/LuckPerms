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

package me.lucko.luckperms.hytale;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.ParseResult;
import com.hypixel.hytale.server.core.command.system.ParserContext;
import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.command.utils.ArgumentTokenizer;
import me.lucko.luckperms.common.sender.Sender;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class HytaleCommandManager extends CommandManager {
    private final LPHytalePlugin plugin;

    public HytaleCommandManager(LPHytalePlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    public void register() {
        this.plugin.getLoader().getCommandRegistry().registerCommand(new Command());
    }

    public CompletableFuture<Void> execute(CommandSender sender, String inputString) {
        Sender wrapped = this.plugin.getSenderFactory().wrap(sender);
        List<String> arguments = ArgumentTokenizer.EXECUTE.tokenizeInput(inputString);
        if (!arguments.isEmpty()) {
            String first = arguments.get(0);
            if (first.equals("luckperms") || first.equals("lp") || first.equals("/luckperms") || first.equals("/lp")) {
                arguments.remove(0);
            }
        }
        return executeCommand(wrapped, "lp", arguments);
    }

    public boolean hasPermissionForAny(CommandSender sender) {
        Sender wrapped = this.plugin.getSenderFactory().wrap(sender);
        return hasPermissionForAny(wrapped);
    }

    // public List<String> suggest(CommandContext ctx) {
    //     Sender wrapped = this.plugin.getSenderFactory().wrap(ctx.sender());
    //     List<String> arguments = ArgumentTokenizer.TAB_COMPLETE.tokenizeInput(ctx.getInputString());
    //     return tabCompleteCommand(wrapped, arguments);
    // }

    private class Command extends AbstractCommand {
        Command() {
            super("luckperms", "LuckPerms command");
            addAliases("lp");
            setAllowsExtraArguments(true);
        }

        @Override
        public @Nullable CompletableFuture<Void> acceptCall(@NonNull CommandSender sender, @NonNull ParserContext parserContext, @NonNull ParseResult parseResult) {
            return HytaleCommandManager.this.execute(sender, parserContext.getInputString());
        }

        @Override
        protected @Nullable CompletableFuture<Void> execute(@NonNull CommandContext ctx) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected boolean canGeneratePermission() {
            return false;
        }

        @Override
        public boolean hasPermission(@NonNull CommandSender sender) {
            return HytaleCommandManager.this.hasPermissionForAny(sender);
        }
    }
}
