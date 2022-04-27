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

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.forge.LPForgePlugin;
import me.lucko.luckperms.forge.event.SuggestCommandsEvent;
import me.lucko.luckperms.forge.model.ForgeUser;
import me.lucko.luckperms.forge.service.ForgePermissionHandler;
import net.luckperms.api.util.Tristate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ServerOpList;
import net.minecraftforge.common.ForgeConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.handler.DefaultPermissionHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ForgePlatformListener {
    private final LPForgePlugin plugin;
    private final Map<CommandNode<CommandSourceStack>, String> permissions;

    public ForgePlatformListener(LPForgePlugin plugin) {
        this.plugin = plugin;
        this.permissions = new HashMap<>();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        this.permissions.clear();
        getPermissions(event.getDispatcher().getRoot()).forEach((key, value) -> this.permissions.put(key, "command." + value));
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        CommandContextBuilder<CommandSourceStack> context = event.getParseResults().getContext();
        CommandSourceStack source = context.getSource();

        if (!this.plugin.getConfiguration().get(ConfigKeys.OPS_ENABLED)) {
            for (ParsedCommandNode<CommandSourceStack> node : context.getNodes()) {
                if (!(node.getNode() instanceof LiteralCommandNode)) {
                    continue;
                }

                String name = node.getNode().getName().toLowerCase(Locale.ROOT);
                if (name.equals("op") || name.equals("deop")) {
                    Message.OP_DISABLED.send(this.plugin.getSenderFactory().wrap(source));
                    event.setCanceled(true);
                    return;
                }
            }
        }

        if (!(source.getEntity() instanceof ServerPlayer)) {
            return;
        }

        StringReader stringReader = new StringReader(event.getParseResults().getReader().getString());
        if (stringReader.canRead() && stringReader.peek() == '/') {
            stringReader.skip();
        }

        ParseResults<CommandSourceStack> parseResults = context.getDispatcher().parse(stringReader, source.withPermission(4));
        for (ParsedCommandNode<CommandSourceStack> parsedNode : parseResults.getContext().getNodes()) {
            if (hasPermission(source, parsedNode.getNode())) {
                continue;
            }

            event.setException(CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(parseResults.getReader()));
            event.setCanceled(true);
            return;
        }

        event.setParseResults(parseResults);
    }

    @SubscribeEvent
    public void onSuggestCommands(SuggestCommandsEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer)) {
            return;
        }

        RootCommandNode<CommandSourceStack> node = new RootCommandNode<>();
        filterCommands(event.getSource(), event.getNode(), node);
        event.setNode(node);
    }

    @SubscribeEvent
    public void onPermissionGatherHandler(PermissionGatherEvent.Handler event) {
        ForgeConfigSpec.ConfigValue<String> permissionHandler = ForgeConfig.SERVER.permissionHandler;
        if (permissionHandler.get().equals(DefaultPermissionHandler.IDENTIFIER.toString())) {
            // Override the default permission handler with LuckPerms
            permissionHandler.set(ForgePermissionHandler.IDENTIFIER.toString());
        }

        event.addPermissionHandler(ForgePermissionHandler.IDENTIFIER, permissions -> new ForgePermissionHandler(this.plugin, permissions));
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

    private void filterCommands(CommandSourceStack source, CommandNode<CommandSourceStack> fromNode, CommandNode<CommandSourceStack> toNode) {
        for (CommandNode<CommandSourceStack> fromChildNode : fromNode.getChildren()) {
            if (!hasPermission(source, fromChildNode)) {
                continue;
            }

            CommandNode<CommandSourceStack> toChildNode = fromChildNode.createBuilder().build();
            toNode.addChild(toChildNode);

            if (!fromChildNode.getChildren().isEmpty()) {
                filterCommands(source, fromChildNode, toChildNode);
            }
        }
    }

    private boolean hasPermission(CommandSourceStack source, CommandNode<CommandSourceStack> node) {
        if (!(source.getEntity() instanceof ServerPlayer)) {
            return node.canUse(source);
        }

        ServerPlayer player = (ServerPlayer) source.getEntity();
        String permission = this.permissions.get(node);
        if (permission != null) {
            ForgeUser user = this.plugin.getContextManager().getUser(player);
            Tristate state = user.checkPermission(permission);
            if (state != Tristate.UNDEFINED) {
                return state.asBoolean();
            }
        }

        return node.canUse(source);
    }

    private <T> Map<CommandNode<T>, String> getPermissions(CommandNode<T> node) {
        Map<CommandNode<T>, String> permissions = new HashMap<>();
        for (CommandNode<T> childNode : node.getChildren()) {
            String name = childNode.getName().toLowerCase(Locale.ROOT)
                    .replace("=", "eq")
                    .replace("<", "lt")
                    .replace("<=", "le")
                    .replace(">", "gt")
                    .replace(">=", "ge")
                    .replace("*", "all");
            permissions.putIfAbsent(childNode, name);

            if (!childNode.getChildren().isEmpty()) {
                getPermissions(childNode).forEach((key, value) -> permissions.putIfAbsent(key, name + "." + value));
            }
        }

        return permissions;
    }

}
