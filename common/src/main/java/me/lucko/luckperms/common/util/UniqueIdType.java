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
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.event.player.lookup.UniqueIdDetermineTypeEvent;

import java.util.UUID;

/**
 * Encapsulates the type of a players unique id.
 */
public final class UniqueIdType {

    public static final UniqueIdType AUTHENTICATED = new UniqueIdType(
            UniqueIdDetermineTypeEvent.TYPE_AUTHENTICATED,
            Component.translatable("luckperms.command.user.info.uuid-type.mojang", NamedTextColor.DARK_GREEN)
    );

    public static final UniqueIdType UNAUTHENTICATED = new UniqueIdType(
            UniqueIdDetermineTypeEvent.TYPE_UNAUTHENTICATED,
            Component.translatable("luckperms.command.user.info.uuid-type.not-mojang", NamedTextColor.DARK_GRAY)
    );

    private static final String TYPE_NPC = "npc";
    public static final UniqueIdType NPC = new UniqueIdType(TYPE_NPC);

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
                // see: https://github.com/lucko/LuckPerms/issues/1470
                // and https://github.com/lucko/LuckPerms/issues/1470#issuecomment-475403162
                type = TYPE_NPC;
                break;
            default:
                type = "unknown";
                break;
        }

        // call the event
        type = plugin.getEventDispatcher().dispatchUniqueIdDetermineType(uniqueId, type);

        switch (type) {
            case UniqueIdDetermineTypeEvent.TYPE_AUTHENTICATED:
                return AUTHENTICATED;
            case UniqueIdDetermineTypeEvent.TYPE_UNAUTHENTICATED:
                return UNAUTHENTICATED;
            case TYPE_NPC:
                return NPC;
            default:
                return new UniqueIdType(type);
        }
    }

    private final String type;
    private final Component component;

    private UniqueIdType(String type) {
        this(type, Component.text(type, NamedTextColor.GOLD));
    }

    private UniqueIdType(String type, Component component) {
        this.type = type;
        this.component = component;
    }

    public String getType() {
        return this.type;
    }

    public Component describe() {
        return this.component;
    }
}
