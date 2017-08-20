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

package me.lucko.luckperms.bungee;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.bungee.event.TristateCheckEvent;
import me.lucko.luckperms.common.commands.sender.SenderFactory;
import me.lucko.luckperms.common.constants.Constants;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import net.kyori.text.Component;
import net.kyori.text.LegacyComponent;
import net.kyori.text.serializer.ComponentSerializer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;

public class BungeeSenderFactory extends SenderFactory<CommandSender> {
    public BungeeSenderFactory(LuckPermsPlugin plugin) {
        super(plugin);
    }

    @Override
    protected String getName(CommandSender sender) {
        if (sender instanceof ProxiedPlayer) {
            return sender.getName();
        }
        return Constants.CONSOLE_NAME;
    }

    @Override
    protected UUID getUuid(CommandSender sender) {
        if (sender instanceof ProxiedPlayer) {
            return ((ProxiedPlayer) sender).getUniqueId();
        }
        return Constants.CONSOLE_UUID;
    }

    @Override
    protected void sendMessage(CommandSender sender, String s) {
        sender.sendMessage(new TextComponent(s));
    }

    @Override
    protected void sendMessage(CommandSender sender, Component message) {
        try {
            sender.sendMessage(net.md_5.bungee.chat.ComponentSerializer.parse(ComponentSerializer.serialize(message)));
        } catch (Exception e) {
            //noinspection deprecation
            sendMessage(sender, LegacyComponent.to(message));
        }
    }

    @Override
    protected Tristate getPermissionValue(CommandSender sender, String node) {
        return TristateCheckEvent.call(sender, node);
    }

    @Override
    protected boolean hasPermission(CommandSender sender, String node) {
        return sender.hasPermission(node);
    }
}
