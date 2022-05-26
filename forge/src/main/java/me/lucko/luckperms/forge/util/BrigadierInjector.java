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

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import me.lucko.luckperms.common.graph.Graph;
import me.lucko.luckperms.common.graph.TraversalAlgorithm;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.forge.capabilities.UserCapability;
import me.lucko.luckperms.forge.capabilities.UserCapabilityImpl;

import net.luckperms.api.util.Tristate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Utility for injecting permission requirements into a Brigadier command tree.
 */
public final class BrigadierInjector {
    private BrigadierInjector() {}

    private static final Field REQUIREMENT_FIELD;

    static {
        Field requirementField;
        try {
            requirementField = CommandNode.class.getDeclaredField("requirement");
            requirementField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
        REQUIREMENT_FIELD = requirementField;
    }

    /**
     * Inject permission requirements into the commands in the given dispatcher.
     *
     * @param plugin the plugin
     * @param dispatcher the command dispatcher
     */
    public static void inject(LuckPermsPlugin plugin, CommandDispatcher<CommandSourceStack> dispatcher) {
        Iterable<CommandNodeWithParent> tree = CommandNodeGraph.INSTANCE.traverse(
                TraversalAlgorithm.DEPTH_FIRST_PRE_ORDER,
                new CommandNodeWithParent(null, dispatcher.getRoot())
        );

        for (CommandNodeWithParent node : tree) {
            Predicate<CommandSourceStack> requirement = node.node.getRequirement();

            // already injected - skip
            if (requirement instanceof InjectedPermissionRequirement) {
                continue;
            }

            String permission = buildPermissionNode(node);
            if (permission == null) {
                continue;
            }

            plugin.getPermissionRegistry().insert(permission);

            InjectedPermissionRequirement newRequirement = new InjectedPermissionRequirement(permission, requirement);
            try {
                REQUIREMENT_FIELD.set(node.node, newRequirement);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static String buildPermissionNode(CommandNodeWithParent node) {
        StringBuilder builder = new StringBuilder();

        while (node != null) {
            if (node.node instanceof LiteralCommandNode) {
                if (builder.length() != 0) {
                    builder.insert(0, '.');
                }

                String name = node.node.getName().toLowerCase(Locale.ROOT);
                builder.insert(0, name);
            }

            node = node.parent;
        }

        if (builder.length() == 0) {
            return null;
        }

        builder.insert(0, "command.");
        return builder.toString();
    }

    /**
     * Injected {@link CommandNode#getRequirement() requirement} that checks for a permission, before
     * delegating to the existing requirement.
     */
    private static final class InjectedPermissionRequirement implements Predicate<CommandSourceStack> {
        private final String permission;
        private final Predicate<CommandSourceStack> delegate;

        private InjectedPermissionRequirement(String permission, Predicate<CommandSourceStack> delegate) {
            this.permission = permission;
            this.delegate = delegate;
        }

        @Override
        public boolean test(CommandSourceStack source) {
            if (source.getEntity() instanceof ServerPlayer) {
                ServerPlayer player = (ServerPlayer) source.getEntity();

                UserCapability user = UserCapabilityImpl.get(player);
                Tristate state = user.checkPermission(this.permission);

                if (state != Tristate.UNDEFINED) {
                    return state.asBoolean() && this.delegate.test(source.withPermission(4));
                }
            }

            return this.delegate.test(source);
        }
    }

    /**
     * A {@link Graph} to represent the brigadier command node tree.
     */
    private enum CommandNodeGraph implements Graph<CommandNodeWithParent> {
        INSTANCE;

        @Override
        public Iterable<? extends CommandNodeWithParent> successors(CommandNodeWithParent ctx) {
            CommandNode<CommandSourceStack> node = ctx.node;
            Collection<CommandNodeWithParent> successors = new ArrayList<>();

            for (CommandNode<CommandSourceStack> child : node.getChildren()) {
                successors.add(new CommandNodeWithParent(ctx, child));
            }

            return successors;
        }
    }

    private static final class CommandNodeWithParent {
        private final CommandNodeWithParent parent;
        private final CommandNode<CommandSourceStack> node;

        private CommandNodeWithParent(CommandNodeWithParent parent, CommandNode<CommandSourceStack> node) {
            this.parent = parent;
            this.node = node;
        }
    }

}
