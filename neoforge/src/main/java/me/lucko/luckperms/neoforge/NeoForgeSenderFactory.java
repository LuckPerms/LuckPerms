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

package me.lucko.luckperms.neoforge;

import com.mojang.brigadier.ParseResults;
import me.lucko.luckperms.common.cacheddata.result.TristateResult;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.query.QueryOptionsImpl;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.sender.SenderFactory;
import me.lucko.luckperms.common.verbose.VerboseCheckTarget;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.rcon.RconConsoleSource;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;
import java.util.UUID;

public class NeoForgeSenderFactory extends SenderFactory<LPNeoForgePlugin, CommandSourceStack> {
    public NeoForgeSenderFactory(LPNeoForgePlugin plugin) {
        super(plugin);
    }

    @Override
    protected UUID getUniqueId(CommandSourceStack commandSource) {
        if (commandSource.getEntity() instanceof Player) {
            return commandSource.getEntity().getUUID();
        }
        return Sender.CONSOLE_UUID;
    }

    @Override
    protected String getName(CommandSourceStack commandSource) {
        if (commandSource.getEntity() instanceof Player) {
            return commandSource.getTextName();
        }
        return Sender.CONSOLE_NAME;
    }

    @Override
    protected void sendMessage(CommandSourceStack sender, Component message) {
        Locale locale = sender.getEntity() instanceof ServerPlayer player
                ? TranslationManager.parseLocale(player.getLanguage())
                : null;
        sender.sendSuccess(() -> toNativeText(TranslationManager.render(message, locale)), false);
    }

    @Override
    protected Tristate getPermissionValue(CommandSourceStack commandSource, String node) {
        if (commandSource.getEntity() instanceof ServerPlayer player) {
            User user = getPlugin().getUserManager().getIfLoaded(player.getUUID());
            if (user == null) {
                return Tristate.UNDEFINED;
            }

            QueryOptions queryOptions = getPlugin().getContextManager().getQueryOptions(player);
            return user.getCachedData().getPermissionData(queryOptions).checkPermission(node, CheckOrigin.PLATFORM_API_HAS_PERMISSION).result();
        }

        VerboseCheckTarget target = VerboseCheckTarget.internal(commandSource.getTextName());
        getPlugin().getVerboseHandler().offerPermissionCheckEvent(CheckOrigin.PLATFORM_API_HAS_PERMISSION, target, QueryOptionsImpl.DEFAULT_CONTEXTUAL, node, TristateResult.UNDEFINED);
        getPlugin().getPermissionRegistry().offer(node);
        return Tristate.UNDEFINED;
    }

    @Override
    protected boolean hasPermission(CommandSourceStack commandSource, String node) {
        return getPermissionValue(commandSource, node).asBoolean();
    }

    @Override
    protected void performCommand(CommandSourceStack sender, String command) {
        ParseResults<CommandSourceStack> results = sender.getServer().getCommands().getDispatcher().parse(command, sender);
        sender.getServer().getCommands().performCommand(results, command);
    }

    @Override
    protected boolean isConsole(CommandSourceStack sender) {
        CommandSource output = sender.source;
        return output == sender.getServer() || // Console
                output.getClass() == RconConsoleSource.class || // Rcon
                (output == CommandSource.NULL && sender.getTextName().equals("")); // Functions
    }

    public static net.minecraft.network.chat.Component toNativeText(Component component) {
        return Serializer.fromJson(GsonComponentSerializer.gson().serializeToTree(component), RegistryAccess.EMPTY);
    }

}
