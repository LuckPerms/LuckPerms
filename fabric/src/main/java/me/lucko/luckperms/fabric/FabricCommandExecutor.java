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

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.lucko.luckperms.common.command.BrigadierCommandExecutor;
import me.lucko.luckperms.common.sender.Sender;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.ListIterator;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class FabricCommandExecutor extends BrigadierCommandExecutor<ServerCommandSource> {

    private final LPFabricPlugin plugin;

    public FabricCommandExecutor(LPFabricPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    public void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
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
    public Sender getSender(ServerCommandSource source) {
        return this.plugin.getSenderFactory().wrap(source);
    }

    @Override
    public List<String> resolveSelectors(ServerCommandSource source, List<String> args) {
        // usage of @ selectors requires at least level 2 permission
        ServerCommandSource atAllowedSource = source.hasPermissionLevel(2) ? source : source.withLevel(2);
        for (ListIterator<String> it = args.listIterator(); it.hasNext(); ) {
            String arg = it.next();
            if (arg.isEmpty() || arg.charAt(0) != '@') {
                continue;
            }

            List<ServerPlayerEntity> matchedPlayers;
            try {
                matchedPlayers = EntityArgumentType.entities().parse(new StringReader(arg)).getPlayers(atAllowedSource);
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
