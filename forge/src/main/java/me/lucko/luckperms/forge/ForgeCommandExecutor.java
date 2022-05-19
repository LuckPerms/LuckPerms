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

package me.lucko.luckperms.forge;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import me.lucko.luckperms.common.command.BrigadierCommandExecutor;
import me.lucko.luckperms.common.sender.Sender;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.ListIterator;

public class ForgeCommandExecutor extends BrigadierCommandExecutor<CommandSourceStack> {

    private final LPForgePlugin plugin;

    public ForgeCommandExecutor(LPForgePlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        for (String alias : COMMAND_ALIASES) {
            LiteralCommandNode<CommandSourceStack> command = Commands.literal(alias).executes(this).build();
            ArgumentCommandNode<CommandSourceStack, String> argument = Commands.argument("args", StringArgumentType.greedyString())
                    .suggests(this)
                    .executes(this)
                    .build();

            command.addChild(argument);
            event.getDispatcher().getRoot().addChild(command);
        }
    }

    @Override
    public Sender getSender(CommandSourceStack source) {
        return this.plugin.getSenderFactory().wrap(source);
    }

    @Override
    public List<String> resolveSelectors(CommandSourceStack source, List<String> args) {
        // usage of @ selectors requires at least level 2 permission
        CommandSourceStack atAllowedSource = source.hasPermission(2) ? source : source.withPermission(2);
        for (ListIterator<String> it = args.listIterator(); it.hasNext(); ) {
            String arg = it.next();
            if (arg.isEmpty() || arg.charAt(0) != '@') {
                continue;
            }

            List<ServerPlayer> matchedPlayers;
            try {
                matchedPlayers = EntityArgument.entities().parse(new StringReader(arg)).findPlayers(atAllowedSource);
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

            ServerPlayer player = matchedPlayers.get(0);
            it.set(player.getStringUUID());
        }

        return args;
    }

}
