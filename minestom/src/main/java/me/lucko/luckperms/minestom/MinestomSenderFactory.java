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

package me.lucko.luckperms.minestom;

import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.sender.SenderFactory;
import me.lucko.luckperms.minestom.app.integration.MinestomPermissible;
import net.kyori.adventure.text.Component;
import net.luckperms.api.util.Tristate;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;

import java.util.Locale;
import java.util.UUID;

public class MinestomSenderFactory extends SenderFactory<LPMinestomPlugin, MinestomPermissible> {

    public MinestomSenderFactory(LPMinestomPlugin plugin) {
        super(plugin);
    }

    @Override
    protected UUID getUniqueId(MinestomPermissible sender) {
        if (sender instanceof Player player) {
            return player.getUuid();
        } else {
            return Sender.CONSOLE_UUID;
        }
    }

    @Override
    protected String getName(MinestomPermissible sender) {
        if (sender instanceof Player player) {
            return player.getUsername();
        } else {
            return Sender.CONSOLE_NAME;
        }
    }

    @Override
    protected void sendMessage(MinestomPermissible sender, Component message) {
        Locale locale = null;
        if (sender instanceof Player player) {
            locale = player.getSettings().locale();
        }
        Component rendered = TranslationManager.render(message, locale);
        sender.sendMessage(rendered);
    }

    @Override
    protected Tristate getPermissionValue(MinestomPermissible sender, String node) {
        if (sender.hasPermission(node)) {
            return Tristate.TRUE;
        } else {
            return Tristate.FALSE;
        }
    }

    @Override
    protected boolean hasPermission(MinestomPermissible sender, String node) {
        return sender.hasPermission(node);
    }

    @Override
    protected void performCommand(MinestomPermissible sender, String command) {
        if (!(sender instanceof CommandSender commandSender)) {
            return;
        }
        MinecraftServer.getCommandManager().execute(commandSender, command);
    }

    @Override
    protected boolean isConsole(MinestomPermissible sender) {
        return sender instanceof MinestomConsoleDelgated;
    }
}
