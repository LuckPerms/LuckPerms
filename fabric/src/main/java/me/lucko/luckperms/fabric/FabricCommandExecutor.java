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
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.command.utils.ArgumentTokenizer;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.sender.Sender;

import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CompletableFuture;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class FabricCommandExecutor extends CommandManager implements Command<ServerCommandSource>, SuggestionProvider<ServerCommandSource> {
    private static final String[] COMMAND_ALIASES = new String[] {"luckperms", "lp", "perm", "perms", "permission", "permissions"};

    private final LPFabricPlugin plugin;

    public FabricCommandExecutor(LPFabricPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    public void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            for (String alias : COMMAND_ALIASES) {
                LiteralCommandNode<ServerCommandSource> cmd = literal(alias)
                        .executes(this)
                        .build();

                ArgumentCommandNode<ServerCommandSource, String> args = argument("args", greedyString())
                        .suggests(this)
                        .executes(this)
                        .build();

                cmd.addChild(args);
                dispatcher.getRoot().addChild(cmd);
            }
        });
    }

    @Override
    public int run(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        Sender wrapped = this.plugin.getSenderFactory().wrap(source);

        int start = ctx.getRange().getStart();
        List<String> arguments = resolveSelectors(source, ArgumentTokenizer.EXECUTE.tokenizeInput(ctx.getInput().substring(start)));

        String label = arguments.remove(0);
        if (label.startsWith("/")) {
            label = label.substring(1);
        }

        executeCommand(wrapped, label, arguments);
        return Command.SINGLE_SUCCESS;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        ServerCommandSource source = ctx.getSource();
        Sender wrapped = this.plugin.getSenderFactory().wrap(source);

        int idx = builder.getStart();

        String buffer = ctx.getInput().substring(idx);
        idx += buffer.length();

        List<String> arguments = resolveSelectors(source, ArgumentTokenizer.TAB_COMPLETE.tokenizeInput(buffer));
        if (!arguments.isEmpty()) {
            idx -= arguments.get(arguments.size() - 1).length();
        }

        List<String> completions = tabCompleteCommand(wrapped, arguments);

        // Offset the builder from the current string range so suggestions are placed in the right spot
        builder = builder.createOffset(idx);
        for (String completion : completions) {
            builder.suggest(completion);
        }
        return builder.buildFuture();
    }

    private List<String> resolveSelectors(ServerCommandSource source, List<String> args) {
        if (!this.plugin.getConfiguration().get(ConfigKeys.RESOLVE_COMMAND_SELECTORS)) {
            return args;
        }

        for (ListIterator<String> it = args.listIterator(); it.hasNext(); ) {
            String arg = it.next();
            if (arg.isEmpty() || arg.charAt(0) != '@') {
                continue;
            }

            List<ServerPlayerEntity> matchedPlayers;
            try {
                matchedPlayers = EntityArgumentType.entities().parse(new StringReader(arg))
                        .getPlayers(source.withLevel(2));   // usage of @ selectors requires at least level 2 permission
            } catch (CommandSyntaxException e) {
                this.plugin.getLogger().warn("Error parsing selector '" + arg + "' for " + source + " executing " + args, e);
                continue;
            }

            if (matchedPlayers.isEmpty()) {
                continue;
            }

            if (matchedPlayers.size() > 1) {
                this.plugin.getLogger().warn("Error parsing selector '" + arg + "' for " + source + " executing " + args +
                        ": ambiguous result (more than one player matched) - " + matchedPlayers);
                continue;
            }

            ServerPlayerEntity player = matchedPlayers.get(0);
            it.set(player.getUuidAsString());
        }

        return args;
    }

}
