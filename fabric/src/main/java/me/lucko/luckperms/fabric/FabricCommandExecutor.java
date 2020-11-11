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

package me.lucko.luckperms.fabric;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.command.utils.ArgumentTokenizer;
import me.lucko.luckperms.common.sender.Sender;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Fabric uses brigadier.
 * All parsing has been designed in a way where "/execute ... ... ... run lp ..." works fine.
 */
class FabricCommandExecutor extends CommandManager implements Command<ServerCommandSource>, SuggestionProvider<ServerCommandSource> {

    private final LPFabricPlugin plugin;

    FabricCommandExecutor(LPFabricPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    public int run(CommandContext<ServerCommandSource> ctx) {
        Sender lpSender = this.plugin.getSenderFactory().wrap(ctx.getSource());
        String rawInput = ctx.getInput();

        int start = ctx.getRange().getStart();
        String lpArguments = rawInput.substring(start);

        List<String> arguments = ArgumentTokenizer.EXECUTE.tokenizeInput(lpArguments);
        // Pop the literal LP uses off the arguments
        String label = arguments.remove(0);

        if (label.startsWith("/")) {
            label = label.substring(1);
        }

        this.executeCommand(lpSender, label, arguments);

        return 1;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        Sender lpSender = this.plugin.getSenderFactory().wrap(ctx.getSource());
        String rawInput = ctx.getInput();

        int start = builder.getStart();
        String lpArguments = rawInput.substring(start);

        start += lpArguments.length();

        List<String> arguments = ArgumentTokenizer.TAB_COMPLETE.tokenizeInput(lpArguments);

        if (arguments.size() != 0) {
            start -= arguments.get(arguments.size() - 1).length();
        }

        List<String> completions = this.tabCompleteCommand(lpSender, arguments);

        // Offset the builder from the current string range so suggestions are placed in the right spot
        builder = builder.createOffset(start);

        for (String completion : completions) {
            builder.suggest(completion);
        }

        return builder.buildFuture();
    }
}
