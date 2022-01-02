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

package me.lucko.luckperms.forge.listeners;

import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.forge.LPForgePlugin;
import me.lucko.luckperms.forge.service.ForgePermissionHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.players.ServerOpList;
import net.minecraftforge.common.ForgeConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.handler.DefaultPermissionHandler;

import java.io.IOException;
import java.util.Locale;

public class ForgePlatformListener {
    private final LPForgePlugin plugin;

    public ForgePlatformListener(LPForgePlugin plugin) {
        this.plugin = plugin;
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        if (this.plugin.getConfiguration().get(ConfigKeys.OPS_ENABLED)) {
            return;
        }

        CommandContextBuilder<CommandSourceStack> context = event.getParseResults().getContext();
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

    @SubscribeEvent
    public void onPermissionGatherHandler(PermissionGatherEvent.Handler event) {
        ForgeConfigSpec.ConfigValue<String> permissionHandler = ForgeConfig.SERVER.permissionHandler;
        if (permissionHandler.get().equals(DefaultPermissionHandler.IDENTIFIER.toString())) {
            // Override the default permission handler with LuckPerms
            permissionHandler.set(ForgePermissionHandler.IDENTIFIER.toString());
        }

        event.addPermissionHandler(ForgePermissionHandler.IDENTIFIER, ForgePermissionHandler::new);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
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
