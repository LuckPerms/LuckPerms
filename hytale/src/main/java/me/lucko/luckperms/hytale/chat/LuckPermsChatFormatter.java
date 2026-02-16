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

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import me.lucko.luckperms.common.cacheddata.type.MetaCache;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;
import me.lucko.luckperms.hytale.HytaleSenderFactory;
import me.lucko.luckperms.hytale.LPHytalePlugin;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class LuckPermsChatFormatter implements PlayerChatEvent.Formatter {
    private final LPHytalePlugin plugin;
    private final String format;
    private final MiniMessage mm;

    public LuckPermsChatFormatter(LPHytalePlugin plugin) {
        this.plugin = plugin;

        String format = plugin.getConfiguration().get(ConfigKeys.CHAT_FORMATTER_MESSAGE_FORMAT);
        PlaceholderApiHook placeholderHook;

        if (PlaceholderApiHook.containsPlaceholders(format)) {
            this.format = PlaceholderApiHook.transformFormat(format);
            placeholderHook = PlaceholderApiHook.init();
            if (placeholderHook == null) {
                plugin.getLogger().warn("Chat format contains PlaceholderAPI placeholders, but PlaceholderAPI is not loaded!");
            }
        } else {
            this.format = format;
            placeholderHook = null;
        }

        this.mm = MiniMessage.builder()
                .editTags(tags -> {
                    tags.resolver(new LuckPermsTagResolver(this.plugin));
                    if (placeholderHook != null) {
                        tags.resolver(new PlaceholderApiTagResolver(this.plugin, placeholderHook));
                    }
                })
                .build();
    }

    @Override
    public @NonNull Message format(@NonNull PlayerRef playerRef, @NonNull String message) {
        try {
            Component component = this.mm.deserialize(
                    this.format,
                    new WrappedPlayerRef(playerRef),
                    Placeholder.unparsed("username", playerRef.getUsername()),
                    Placeholder.unparsed("message", message)
            );
            return HytaleSenderFactory.toHytaleMessage(component);
        } catch (RuntimeException e) {
            this.plugin.getLogger().warn("Failed to format chat message", e);
            throw e;
        }
    }

    /** Empty tag **/
    private static final Tag EMPTY = Tag.selfClosingInserting(Component.empty());

    /**
     * A tag resolver for LuckPerms tags.
     *
     * @param plugin the plugin instance
     */
    private record LuckPermsTagResolver(LPHytalePlugin plugin) implements TagResolver {

        @Override
        public @Nullable Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull Context ctx) throws ParsingException {
            if (!has(name)) {
                return null;
            }

            PlayerRef playerRef = ctx.targetAsType(WrappedPlayerRef.class).playerRef();
            User user = this.plugin.getUserManager().getIfLoaded(playerRef.getUuid());
            if (user == null) {
                return EMPTY;
            }

            MetaCache metaData = user.getCachedData().getMetaData();
            return switch (name) {
                case "prefix" -> {
                    ensureNoArguments(name, arguments, ctx);
                    String value = metaData.getPrefix(CheckOrigin.INTERNAL).result();
                    yield parseFormattedText(value);
                }
                case "suffix" -> {
                    ensureNoArguments(name, arguments, ctx);
                    String value = metaData.getSuffix(CheckOrigin.INTERNAL).result();
                    yield parseFormattedText(value);
                }
                case "meta" -> {
                    String metaKey = arguments.popOr("Meta tag requires a 'meta key' argument").value();
                    ensureNoArguments(name, arguments, ctx);

                    String value = metaData.getMetaValue(metaKey, CheckOrigin.INTERNAL).result();
                    yield parseFormattedText(value);
                }
                default -> null;
            };
        }

        @Override
        public boolean has(@NotNull String name) {
            return "prefix".equalsIgnoreCase(name) || "suffix".equalsIgnoreCase(name) || "meta".equalsIgnoreCase(name);
        }
    }

    /**
     * A tag resolver for PlaceholderAPI placeholders.
     *
     * @param plugin the plugin instance
     * @param placeholderHook the PlaceholderAPI hook instance
     */
    private record PlaceholderApiTagResolver(LPHytalePlugin plugin, PlaceholderApiHook placeholderHook) implements TagResolver {

        @Override
        public @Nullable Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull Context ctx) throws ParsingException {
            if (!has(name)) {
                return null;
            }

            PlayerRef playerRef = ctx.targetAsType(WrappedPlayerRef.class).playerRef();
            String placeholder = arguments.popOr("Placeholder tag requires a 'placeholder' argument").value();
            ensureNoArguments(name, arguments, ctx);

            String value;
            try {
                value = this.placeholderHook.resolvePlaceholder(playerRef, placeholder);
            } catch (RuntimeException e) {
                this.plugin.getLogger().warn("Failed to resolve PlaceholderAPI placeholder '" + placeholder + "'", e);
                value = "";
            }
            return parseFormattedText(value);
        }

        @Override
        public boolean has(@NotNull String name) {
            return "papi".equalsIgnoreCase(name);
        }
    }

    private static void ensureNoArguments(String name, ArgumentQueue arguments, Context ctx) {
        if (arguments.hasNext()) {
            throw ctx.newException("Tag '<" + name + ">' was given more arguments than required");
        }
    }

    /**
     * A wrapper for PlayerRef to be used as a pointer in the MiniMessage context.
     */
    private record WrappedPlayerRef(PlayerRef playerRef) implements Pointered { }

    private static Tag parseFormattedText(String value) {
        if (value == null || value.isEmpty()) {
            return EMPTY;
        }

        char legacyChar = findLegacyColorCodes(value);
        if (legacyChar != 0) {
            TextComponent component = LegacyComponentSerializer.legacy(legacyChar).deserialize(value);
            return Tag.inserting(component);
        } else {
            return Tag.preProcessParsed(value);
        }
    }

    /**
     * Checks if the input string contains any legacy color codes (either '&x' or '§x').
     * If it does, it returns the character used for the color codes. If not, it returns 0.
     */
    private static char findLegacyColorCodes(String string) {
        final char[] charArray = string.toCharArray();
        for (int i = 0; i < charArray.length - 1; i++) {
            if ((charArray[i] == LegacyComponentSerializer.AMPERSAND_CHAR
                    || charArray[i] == LegacyComponentSerializer.SECTION_CHAR)
                    && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(charArray[i + 1]) > -1) {
                return charArray[i];
            }
        }
        return 0;
    }

}
