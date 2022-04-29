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

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import me.lucko.luckperms.forge.event.SuggestCommandsEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

/**
 * Mixin into {@link Commands} for posting {@link SuggestCommandsEvent}
 */
@Mixin(value = Commands.class)
public abstract class CommandsMixin {

    @Shadow
    protected abstract void fillUsableCommands(CommandNode<CommandSourceStack> p_82113_, CommandNode<SharedSuggestionProvider> p_82114_, CommandSourceStack p_82115_, Map<CommandNode<CommandSourceStack>, CommandNode<SharedSuggestionProvider>> p_82116_);

    /**
     * Mixin into {@link Commands#sendCommands(ServerPlayer)} for posting {@link SuggestCommandsEvent},
     * this event allows modifications the command suggestions sent to the client.
     */
    @Redirect(
            method = "sendCommands",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/commands/Commands;fillUsableCommands(Lcom/mojang/brigadier/tree/CommandNode;Lcom/mojang/brigadier/tree/CommandNode;Lnet/minecraft/commands/CommandSourceStack;Ljava/util/Map;)V"
            )
    )
    private void onFillUsableCommands(Commands commands, CommandNode<CommandSourceStack> commandNode, CommandNode<SharedSuggestionProvider> suggestionNode, CommandSourceStack source, Map<CommandNode<CommandSourceStack>, CommandNode<SharedSuggestionProvider>> map) {
        SuggestCommandsEvent event = new SuggestCommandsEvent(source, (RootCommandNode<CommandSourceStack>) commandNode);
        MinecraftForge.EVENT_BUS.post(event);

        // This map will be populated with the original root node, so we must clear it
        map.clear();

        // Insert the root node as it may have been replaced during the event
        map.put(event.getNode(), suggestionNode);

        fillUsableCommands(event.getNode(), suggestionNode, source.withPermission(4), map);
    }

    /**
     * Forge will only re-throw the Exception passed in the {@link net.minecraftforge.event.CommandEvent CommandEvent} if it's an unchecked Exception,
     * this behaviour prevents a {@link CommandSyntaxException CommandSyntaxException} from being propagated through the event.
     *
     * TODO Fix upstream.
     */
    @Redirect(
            method = "performCommand",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/common/base/Throwables;throwIfUnchecked(Ljava/lang/Throwable;)V",
                    remap = false
            )
    )
    private void onThrowIfUnchecked(Throwable throwable) throws CommandSyntaxException {
        if (throwable instanceof CommandSyntaxException) {
            throw (CommandSyntaxException) throwable;
        }
    }
}
