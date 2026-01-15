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

package me.lucko.luckperms.hytale.chat;

import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.hytale.HytaleSenderFactory;
import me.lucko.luckperms.hytale.LPHytalePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.TagPattern;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jspecify.annotations.NonNull;

public class LuckPermsChatFormatter implements PlayerChatEvent.Formatter {
    private final LPHytalePlugin plugin;
    private final String format;

    public LuckPermsChatFormatter(LPHytalePlugin plugin) {
        this.plugin = plugin;
        this.format = plugin.getConfiguration().get(ConfigKeys.CHAT_FORMATTER_MESSAGE_FORMAT);
    }

    public void register(EventRegistry registry) {
        registry.registerGlobal(PlayerChatEvent.class, this::onPlayerChat);
    }

    private void onPlayerChat(PlayerChatEvent e) {
        e.setFormatter(this);
    }

    @Override
    public @NonNull Message format(@NonNull PlayerRef playerRef, @NonNull String message) {
        String username = playerRef.getUsername();
        String prefix = "";
        String suffix = "";

        User user = this.plugin.getUserManager().getIfLoaded(playerRef.getUuid());
        if (user != null) {
            prefix = user.getCachedData().getMetaData().getPrefix();
            if (prefix == null) {
                prefix = "";
            }
            suffix = user.getCachedData().getMetaData().getSuffix();
            if (suffix == null) {
                suffix = "";
            }
        }

        Component component = MiniMessage.miniMessage().deserialize(
                this.format,
                parse("prefix", prefix),
                parse("suffix", suffix),
                Placeholder.unparsed("username", username),
                Placeholder.unparsed("message", message)
        );
        return HytaleSenderFactory.toHytaleMessage(component);
    }

    private static TagResolver parse(@TagPattern String key, String value) {
        boolean containsLegacyFormattingCharacter = value.indexOf(LegacyComponentSerializer.AMPERSAND_CHAR) != -1
                || value.indexOf(LegacyComponentSerializer.SECTION_CHAR) != -1;

        if (containsLegacyFormattingCharacter) {
            TextComponent component = LegacyComponentSerializer.legacyAmpersand().deserialize(value);
            return Placeholder.component(key, component);
        } else {
            return Placeholder.parsed(key, value);
        }
    }

}
