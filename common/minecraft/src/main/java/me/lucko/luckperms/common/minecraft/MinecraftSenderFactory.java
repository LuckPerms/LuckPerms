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

package me.lucko.luckperms.common.minecraft;

import com.mojang.serialization.JsonOps;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.sender.SenderFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.rcon.RconConsoleSource;

import java.util.Locale;
import java.util.UUID;

public abstract class MinecraftSenderFactory<P extends LuckPermsPlugin> extends SenderFactory<P, CommandSourceStack> {
    private final P plugin;

    public MinecraftSenderFactory(P plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    protected P getPlugin() {
        return this.plugin;
    }

    protected abstract CommandSource getSource(CommandSourceStack sender);

    @Override
    protected UUID getUniqueId(CommandSourceStack commandSource) {
        if (commandSource.getEntity() != null) {
            return commandSource.getEntity().getUUID();
        }
        return Sender.CONSOLE_UUID;
    }

    @Override
    protected String getName(CommandSourceStack commandSource) {
        String name = commandSource.getTextName();
        if (commandSource.getEntity() != null && name.equals("Server")) {
            return Sender.CONSOLE_NAME;
        }
        return name;
    }

    @Override
    protected void sendMessage(CommandSourceStack sender, Component message) {
        Locale locale = sender.getEntity() instanceof ServerPlayer player
                ? TranslationManager.parseLocale(player.clientInformation().language())
                : null;
        sender.sendSuccess(() -> toNativeText(TranslationManager.render(message, locale)), false);
    }

    @Override
    protected boolean hasPermission(CommandSourceStack commandSource, String node) {
        return getPermissionValue(commandSource, node).asBoolean();
    }

    @Override
    protected void performCommand(CommandSourceStack sender, String command) {
        sender.getServer().getCommands().performPrefixedCommand(sender, command);
    }

    @Override
    protected boolean isConsole(CommandSourceStack sender) {
        CommandSource output = getSource(sender);
        return output == sender.getServer() || // Console
            output.getClass() == RconConsoleSource.class || // Rcon
            (output == CommandSource.NULL && sender.getTextName().equals("")); // Functions
    }

    public static net.minecraft.network.chat.Component toNativeText(Component component) {
        return ComponentSerialization.CODEC.decode(
                RegistryAccess.EMPTY.createSerializationContext(JsonOps.INSTANCE),
                GsonComponentSerializer.gson().serializeToTree(component)
        ).getOrThrow(IllegalArgumentException::new).getFirst();
    }
}
