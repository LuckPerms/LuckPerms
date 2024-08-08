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

package me.lucko.luckperms.neoforge.listeners;

import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.neoforge.LPNeoForgePlugin;
import me.lucko.luckperms.neoforge.util.BrigadierInjector;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.players.ServerOpList;

import java.io.IOException;
import java.util.Locale;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

public class NeoForgePlatformListener {
    private final LPNeoForgePlugin plugin;

    public NeoForgePlatformListener(LPNeoForgePlugin plugin) {
        this.plugin = plugin;
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        CommandContextBuilder<CommandSourceStack> context = event.getParseResults().getContext();

        if (!this.plugin.getConfiguration().get(ConfigKeys.OPS_ENABLED)) {
            for (ParsedCommandNode<CommandSourceStack> node : context.getNodes()) {
                if (!(node.getNode() instanceof LiteralCommandNode)) {
                    continue;
                }

                String name = node.getNode().getName().toLowerCase(Locale.ROOT);
                if (name.equals("op") || name.equals("deop")) {
                    Message.OP_DISABLED.send(this.plugin.getSenderFactory().wrap(context.getSource()));
                    event.setCanceled(true);
                    return;
                }
            }
        }
    }

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        Commands commands = event.getServerResources().getCommands();
        BrigadierInjector.inject(this.plugin, commands.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        if (!this.plugin.getConfiguration().get(ConfigKeys.OPS_ENABLED)) {
            ServerOpList ops = event.getServer().getPlayerList().getOps();
            ops.getEntries().clear();
            try {
                ops.save();
            } catch (IOException ex) {
                this.plugin.getLogger().severe("Encountered an error while saving ops", ex);
            }
        }
    }

}
