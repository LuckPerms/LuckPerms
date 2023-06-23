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

package me.lucko.luckperms.common.util;

import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.luckperms.api.event.player.lookup.UniqueIdDetermineTypeEvent;

import java.util.UUID;

/**
 * Encapsulates the type of a players unique id.
 */
public final class UniqueIdType {

    public static final UniqueIdType AUTHENTICATED = new UniqueIdType(
            UniqueIdDetermineTypeEvent.TYPE_AUTHENTICATED,
            NamedTextColor.DARK_GREEN,
            "luckperms.command.user.info.uuid-type.mojang",
            "luckperms.command.user.info.uuid-type.desc.mojang"
    );

    public static final UniqueIdType UNAUTHENTICATED = new UniqueIdType(
            UniqueIdDetermineTypeEvent.TYPE_UNAUTHENTICATED,
            NamedTextColor.DARK_GRAY,
            "luckperms.command.user.info.uuid-type.not-mojang",
            "luckperms.command.user.info.uuid-type.desc.not-mojang"
    );

    public static final UniqueIdType NPC = new UniqueIdType(
            UniqueIdDetermineTypeEvent.TYPE_NPC,
            NamedTextColor.GOLD,
            "luckperms.command.user.info.uuid-type.npc",
            "luckperms.command.user.info.uuid-type.desc.npc"
    );

    public static final UniqueIdType UNKNOWN = new UniqueIdType(
            UniqueIdDetermineTypeEvent.TYPE_UNKNOWN,
            NamedTextColor.RED,
            "luckperms.command.user.info.uuid-type.unknown",
            "luckperms.command.user.info.uuid-type.desc.unknown"
    );

    public static UniqueIdType determineType(UUID uniqueId, LuckPermsPlugin plugin) {
        // determine initial type based on the uuid version
        String type;
        switch (uniqueId.version()) {
            case 4:
                type = UniqueIdDetermineTypeEvent.TYPE_AUTHENTICATED;
                break;
            case 3:
                type = UniqueIdDetermineTypeEvent.TYPE_UNAUTHENTICATED;
                break;
            case 2:
                // if the uuid is version 2, assume it is an NPC
                // see: https://github.com/LuckPerms/LuckPerms/issues/1470
                // and https://github.com/LuckPerms/LuckPerms/issues/1470#issuecomment-475403162
                type = UniqueIdDetermineTypeEvent.TYPE_NPC;
                break;
            default:
                type = UniqueIdDetermineTypeEvent.TYPE_UNKNOWN;
                break;
        }

        // call the event
        type = plugin.getEventDispatcher().dispatchUniqueIdDetermineType(uniqueId, type);

        switch (type) {
            case UniqueIdDetermineTypeEvent.TYPE_AUTHENTICATED:
                return AUTHENTICATED;
            case UniqueIdDetermineTypeEvent.TYPE_UNAUTHENTICATED:
                return UNAUTHENTICATED;
            case UniqueIdDetermineTypeEvent.TYPE_NPC:
                return NPC;
            case UniqueIdDetermineTypeEvent.TYPE_UNKNOWN:
                return UNKNOWN;
            default:
                return new UniqueIdType(type);
        }
    }

    private final String type;
    private final Component component;

    // constructor used for built-in types
    private UniqueIdType(String type, TextColor displayColor, String translationKey, String translationKeyHover) {
        this.type = type;
        this.component = Component.translatable()
                .key(translationKey)
                .color(displayColor)
                .hoverEvent(HoverEvent.showText(Component.translatable(
                        translationKeyHover,
                        NamedTextColor.DARK_GRAY
                )))
                .build();
    }

    // constructor used for types provided via the API
    private UniqueIdType(String type) {
        this.type = type;
        this.component = Component.text()
                .content(type)
                .color(NamedTextColor.GOLD)
                .hoverEvent(HoverEvent.showText(Component.translatable(
                        "luckperms.command.user.info.uuid-type.desc.api",
                        NamedTextColor.GRAY
                )))
                .build();
    }

    public String getType() {
        return this.type;
    }

    public Component describe() {
        return this.component;
    }
}
