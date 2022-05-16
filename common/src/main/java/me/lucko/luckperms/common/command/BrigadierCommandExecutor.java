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

package me.lucko.luckperms.common.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.lucko.luckperms.common.command.utils.ArgumentTokenizer;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class BrigadierCommandExecutor<S> extends CommandManager implements Command<S>, SuggestionProvider<S> {

    protected static final String[] COMMAND_ALIASES = new String[]{"luckperms", "lp", "perm", "perms", "permission", "permissions"};

    private final LuckPermsPlugin plugin;

    protected BrigadierCommandExecutor(LuckPermsPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    public abstract Sender getSender(S source);

    public abstract List<String> resolveSelectors(S source, List<String> args);

    @Override
    public int run(CommandContext<S> context) throws CommandSyntaxException {
        S source = context.getSource();
        Sender sender = getSender(source);

        int start = context.getRange().getStart();
        String buffer = context.getInput().substring(start);

        List<String> arguments;
        if (this.plugin.getConfiguration().get(ConfigKeys.RESOLVE_COMMAND_SELECTORS)) {
            arguments = resolveSelectors(source, ArgumentTokenizer.EXECUTE.tokenizeInput(buffer));
        } else {
            arguments = ArgumentTokenizer.EXECUTE.tokenizeInput(buffer);
        }

        String label = arguments.remove(0);
        if (label.startsWith("/")) {
            label = label.substring(1);
        }

        executeCommand(sender, label, arguments);
        return Command.SINGLE_SUCCESS;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<S> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        S source = context.getSource();
        Sender sender = getSender(source);

        int idx = builder.getStart();

        String buffer = builder.getInput().substring(idx);
        idx += buffer.length();

        List<String> arguments = ArgumentTokenizer.TAB_COMPLETE.tokenizeInput(buffer);
        List<String> resolvedArguments;
        if (this.plugin.getConfiguration().get(ConfigKeys.RESOLVE_COMMAND_SELECTORS)) {
            resolvedArguments = resolveSelectors(source, new ArrayList<>(arguments));
        } else {
            resolvedArguments = arguments;
        }

        if (!arguments.isEmpty() && !resolvedArguments.isEmpty()) {
            idx -= arguments.get(arguments.size() - 1).length();
        }

        List<String> completions = tabCompleteCommand(sender, resolvedArguments);

        // Offset the builder from the current string range so suggestions are placed in the right spot
        builder = builder.createOffset(idx);
        for (String completion : completions) {
            builder.suggest(completion);
        }
        return builder.buildFuture();
    }

}
