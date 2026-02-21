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

package me.lucko.luckperms.hytale;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.receiver.IMessageReceiver;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.sender.SenderFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.luckperms.api.util.Tristate;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class HytaleSenderFactory extends SenderFactory<LPHytalePlugin, IMessageReceiver> {

    public HytaleSenderFactory(LPHytalePlugin plugin) {
        super(plugin);
    }

    // Enforce that sender is either a `PlayerRef` or `CommandSender`
    private void checkType(IMessageReceiver sender) {
        if (sender instanceof PlayerRef || sender instanceof CommandSender) {
            return;
        }
        throw new IllegalArgumentException("Unsupported sender type: " + sender.getClass().getName());
    }

    @Override
    protected String getName(IMessageReceiver sender) {
        checkType(sender);
        if (sender instanceof Player player) {
            return player.getDisplayName();
        } else if (sender instanceof PlayerRef playerRef) {
            return playerRef.getUsername();
        }
        return Sender.CONSOLE_NAME;
    }

    @Override
    protected UUID getUniqueId(IMessageReceiver sender) {
        checkType(sender);
        if (sender instanceof Player player) {
            //noinspection removal
            return player.getPlayerRef().getUuid();
        } else if (sender instanceof PlayerRef playerRef) {
            return playerRef.getUuid();
        }
        return Sender.CONSOLE_UUID;
    }

    @Override
    protected void sendMessage(IMessageReceiver sender, Component message) {
        checkType(sender);

        Locale locale = null;
        if (sender instanceof Player player) {
            //noinspection removal
            locale = TranslationManager.parseLocale(player.getPlayerRef().getLanguage());
        } else if (sender instanceof PlayerRef playerRef) {
            locale = TranslationManager.parseLocale(playerRef.getLanguage());
        }

        Component rendered = TranslationManager.render(message, locale);
        sender.sendMessage(toHytaleMessage(rendered));
    }

    @Override
    protected Tristate getPermissionValue(IMessageReceiver sender, String node) {
        return Tristate.of(hasPermission(sender, node));
    }

    @Override
    protected boolean hasPermission(IMessageReceiver sender, String node) {
        checkType(sender);

        if (sender instanceof CommandSender commandSender) {
            return commandSender.hasPermission(node);
        }
        if (sender instanceof PlayerRef playerRef) {
            return PermissionsModule.get().hasPermission(playerRef.getUuid(), node);
        }

        throw new AssertionError();
    }

    @Override
    protected void performCommand(IMessageReceiver sender, String command) {
        checkType(sender);

        if (sender instanceof CommandSender commandSender) {
            CommandManager.get().handleCommand(commandSender, command).join();
            return;
        }
        if (sender instanceof PlayerRef playerRef) {
            CommandManager.get().handleCommand(playerRef, command).join();
            return;
        }

        throw new AssertionError();
    }

    @Override
    protected boolean isConsole(IMessageReceiver sender) {
        return sender instanceof ConsoleSender;
    }

    public static Message toHytaleMessage(Component component) {
        Message message;
        if (component instanceof TextComponent text) {
            message = Message.raw(text.content());
        } else {
            throw new UnsupportedOperationException("Unsupported component type: " + component.getClass());
        }

        TextColor color = component.color();
        if (color != null) {
            message.color(color.asHexString());
        }

        TextDecoration.State bold = component.decoration(TextDecoration.BOLD);
        if (bold != TextDecoration.State.NOT_SET) {
            message.bold(bold == TextDecoration.State.TRUE);
        }

        TextDecoration.State italic = component.decoration(TextDecoration.ITALIC);
        if (italic != TextDecoration.State.NOT_SET) {
            message.italic(italic == TextDecoration.State.TRUE);
        }

        ClickEvent clickEvent = component.clickEvent();
        if (clickEvent != null && clickEvent.action() == ClickEvent.Action.OPEN_URL) {
            message.link(clickEvent.value());
        }

        List<Message> children = component.children().stream()
                .map(HytaleSenderFactory::toHytaleMessage)
                .toList();

        if (!children.isEmpty()) {
            message.insertAll(children);
        }

        return message;
    }
}
