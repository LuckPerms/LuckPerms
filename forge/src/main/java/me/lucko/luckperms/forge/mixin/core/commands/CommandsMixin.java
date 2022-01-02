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

package me.lucko.luckperms.forge.mixin.core.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import me.lucko.luckperms.forge.bridge.server.level.ServerPlayerBridge;
import net.luckperms.api.util.Tristate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Mixin(value = Commands.class)
public abstract class CommandsMixin {

    @Shadow
    @Final
    private CommandDispatcher<CommandSourceStack> dispatcher;

    private Map<CommandNode<CommandSourceStack>, String> luckperms$permissions;

    @Inject(
            method = "<init>",
            at = @At(
                    value = "RETURN"
            )
    )
    private void onInit(Commands.CommandSelection commandSelection, CallbackInfo callbackInfo) {
        this.luckperms$permissions = new HashMap<>();
        luckperms$getPermissions(this.dispatcher.getRoot()).forEach((key, value) -> this.luckperms$permissions.put(key, "command." + value));
    }

    @Redirect(
            method = "performCommand",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/brigadier/CommandDispatcher;parse(Lcom/mojang/brigadier/StringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/ParseResults;",
                    remap = false
            )
    )
    private ParseResults<CommandSourceStack> onPerformCommand(CommandDispatcher<CommandSourceStack> instance, StringReader command, Object object) throws Exception {
        CommandSourceStack source = (CommandSourceStack) object;
        if (!(source.getEntity() instanceof ServerPlayer)) {
            return instance.parse(command, source);
        }

        ParseResults<CommandSourceStack> parseResults = instance.parse(command, source.withPermission(4));
        for (ParsedCommandNode<CommandSourceStack> parsedNode : parseResults.getContext().getNodes()) {
            if (!luckperms$hasPermission(source, parsedNode.getNode())) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(parseResults.getReader());
            }
        }

        return parseResults;
    }

    @Redirect(
            method = "fillUsableCommands",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/brigadier/tree/CommandNode;canUse(Ljava/lang/Object;)Z",
                    remap = false
            )
    )
    private boolean onCanUse(CommandNode<CommandSourceStack> node, Object object) {
        CommandSourceStack source = (CommandSourceStack) object;
        if (!(source.getEntity() instanceof ServerPlayer)) {
            return node.canUse(source);
        }

        // If the node cannot be used with the max permission level then there is most likely an unmet requirement,
        // as a permission will not resolve this requirement we can just ignore the node.
        if (!node.canUse(source.withPermission(4))) {
            return false;
        }

        return luckperms$hasPermission(source, node);
    }

    private boolean luckperms$hasPermission(CommandSourceStack source, CommandNode<CommandSourceStack> node) {
        if (!(source.getEntity() instanceof ServerPlayer)) {
            return node.canUse(source);
        }

        String permission = this.luckperms$permissions.get(node);
        if (permission != null) {
            Tristate state = ((ServerPlayerBridge) source.getEntity()).bridge$hasPermission(permission);
            if (state != Tristate.UNDEFINED) {
                return state.asBoolean();
            }
        }

        return node.canUse(source);
    }

    private <T> Map<CommandNode<T>, String> luckperms$getPermissions(CommandNode<T> node) {
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
                luckperms$getPermissions(childNode).forEach((key, value) -> permissions.putIfAbsent(key, name + "." + value));
            }
        }

        return permissions;
    }

}
