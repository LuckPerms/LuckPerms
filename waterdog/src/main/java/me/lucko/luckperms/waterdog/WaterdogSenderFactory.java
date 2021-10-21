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

package me.lucko.luckperms.waterdog;

import dev.waterdog.waterdogpe.command.CommandSender;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import me.lucko.luckperms.common.sender.SenderFactory;
import me.lucko.luckperms.waterdog.event.TristateCheckEvent;
import me.lucko.luckperms.common.sender.Sender;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.util.Tristate;

import java.util.UUID;

public class WaterdogSenderFactory extends SenderFactory<LPWaterdogPlugin, CommandSender> {

    public WaterdogSenderFactory(LPWaterdogPlugin plugin) {
        super(plugin);
    }

    @Override
    protected String getName(CommandSender sender) {
        if (sender instanceof ProxiedPlayer) {
            return sender.getName();
        }
        return Sender.CONSOLE_NAME;
    }

    @Override
    protected UUID getUniqueId(CommandSender sender) {
        if (sender instanceof ProxiedPlayer) {
            return ((ProxiedPlayer) sender).getUniqueId();
        }
        return Sender.CONSOLE_UUID;
    }

    @Override
    protected void sendMessage(CommandSender sender, Component message) {
        sender.sendMessage(LegacyComponentSerializer.legacySection().serialize(message));
    }

    @Override
    protected Tristate getPermissionValue(CommandSender sender, String node) {
        return TristateCheckEvent.call(sender, node);
    }

    @Override
    protected boolean hasPermission(CommandSender sender, String node) {
        return sender.hasPermission(node);
    }

    @Override
    protected void performCommand(CommandSender sender, String command) {
        getPlugin().getBootstrap().getProxy().getCommandMap().handleCommand(sender, command, new String[]{});
    }

    @Override
    protected boolean isConsole(CommandSender sender) {
        return !(sender instanceof ProxiedPlayer);
    }

    @Override
    public void close() {
        // Do nothing
    }
}