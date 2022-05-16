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

package me.lucko.luckperms.forge.util;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import me.lucko.luckperms.forge.LPForgePlugin;
import me.lucko.luckperms.forge.capabilities.UserCapability;
import net.luckperms.api.util.Tristate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

public class BrigadierRewriter {

    private final LPForgePlugin plugin;
    private final Map<CommandNode<CommandSourceStack>, CommandNode<CommandSourceStack>> nodes;
    private final Map<CommandNode<CommandSourceStack>, String> permissions;

    public BrigadierRewriter(LPForgePlugin plugin) {
        this.plugin = plugin;
        this.nodes = new IdentityHashMap<>();
        this.permissions = new IdentityHashMap<>();
    }

    public CommandDispatcher<CommandSourceStack> rebuild(CommandDispatcher<CommandSourceStack> dispatcher) {
        RootCommandNode<CommandSourceStack> node = (RootCommandNode<CommandSourceStack>) rebuild(dispatcher.getRoot());
        this.nodes.clear();
        this.permissions.clear();
        getPermissions(dispatcher.getRoot()).forEach((key, value) -> this.permissions.put(key, "command." + value));
        return new CommandDispatcher<>(node);
    }

    @SuppressWarnings("unchecked")
    private CommandNode<CommandSourceStack> rebuild(CommandNode<CommandSourceStack> node) {
        if (node == null) return null;
        // If we are getting to this state and re-wrapping LPCommandNodes then something is incredibly broken.
        if (node instanceof LPRootCommandNode) throw new IllegalArgumentException("Unable to rebuild LPRootCommandNode");
        if (node instanceof LPArgumentCommandNode) throw new IllegalArgumentException("Unable to rebuild LPArgumentCommandNode");
        if (node instanceof LPLiteralCommandNode) throw new IllegalArgumentException("Unable to rebuild LPLiteralCommandNode");

        CommandNode<CommandSourceStack> existingNode = nodes.get(node);
        if (existingNode != null) {
            return existingNode;
        }

        Class<?> nodeClass = node.getClass();
        CommandNode<CommandSourceStack> rebuilt;
        if (nodeClass == ArgumentCommandNode.class) {
            ArgumentCommandNode<CommandSourceStack, Object> argNode = (ArgumentCommandNode<CommandSourceStack, Object>) node;
            rebuilt = new LPArgumentCommandNode(argNode.getName(), argNode.getType(), argNode.getCommand(),
                    appendToRequirement(node, argNode.getRequirement()), rebuild(argNode.getRedirect()),
                    argNode.getRedirectModifier(), argNode.isFork(), argNode.getCustomSuggestions()
            );
        } else if (nodeClass == LiteralCommandNode.class) {
            LiteralCommandNode<CommandSourceStack> litNode = (LiteralCommandNode<CommandSourceStack>) node;
            rebuilt = new LPLiteralCommandNode(litNode.getName(), litNode.getCommand(),
                    appendToRequirement(node, litNode.getRequirement()), rebuild(litNode.getRedirect()),
                    litNode.getRedirectModifier(), litNode.isFork()
            );
        } else if (nodeClass == RootCommandNode.class) {
            rebuilt = new LPRootCommandNode();
        } else {
            // We can't rebuild this node. Someone has a custom CommandNode implementation that we don't know about.
            // The use cases for these are basically non-existent, so I doubt anyone has these.
            this.plugin.getLogger().warn("Unsupported CommandNode: " + nodeClass.getName());
            return node;
        }

        nodes.put(node, rebuilt);
        for (CommandNode<CommandSourceStack> child : node.getChildren()) {
            rebuilt.addChild(child);
        }

        return rebuilt;
    }

    private Predicate<CommandSourceStack> appendToRequirement(CommandNode<CommandSourceStack> node, Predicate<CommandSourceStack> prev) {
        return source -> {
            if (source.getEntity() instanceof ServerPlayer) {
                ServerPlayer player = (ServerPlayer) source.getEntity();
                String permission = this.permissions.get(node);
                if (permission != null) {
                    UserCapability user = this.plugin.getContextManager().getUser(player);
                    Tristate state = user.checkPermission(permission);

                    if (state != Tristate.UNDEFINED) {
                        return state.asBoolean() && prev.test(source.withPermission(4));
                    }
                }
            }

            return prev.test(source);
        };
    }

    private static <T> Map<CommandNode<T>, String> getPermissions(CommandNode<T> node) {
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

    private class LPRootCommandNode extends RootCommandNode<CommandSourceStack> {

        @Override
        public void addChild(CommandNode<CommandSourceStack> node) {
            super.addChild(rebuild(node));
        }
    }

    private class LPArgumentCommandNode extends ArgumentCommandNode<CommandSourceStack, Object> {

        public LPArgumentCommandNode(String name, ArgumentType<Object> type, Command<CommandSourceStack> command,
                                     Predicate<CommandSourceStack> requirement, CommandNode<CommandSourceStack> redirect,
                                     RedirectModifier<CommandSourceStack> modifier, boolean forks, SuggestionProvider<CommandSourceStack> customSuggestions) {
            super(name, type, command, requirement, redirect, modifier, forks, customSuggestions);
        }

        @Override
        public void addChild(CommandNode<CommandSourceStack> node) {
            super.addChild(rebuild(node));
        }
    }

    private class LPLiteralCommandNode extends LiteralCommandNode<CommandSourceStack> {

        public LPLiteralCommandNode(String literal, Command<CommandSourceStack> command, Predicate<CommandSourceStack> requirement,
                                    CommandNode<CommandSourceStack> redirect, RedirectModifier<CommandSourceStack> modifier, boolean forks) {
            super(literal, command, requirement, redirect, modifier, forks);
        }

        @Override
        public void addChild(CommandNode<CommandSourceStack> node) {
            super.addChild(rebuild(node));
        }
    }

}
