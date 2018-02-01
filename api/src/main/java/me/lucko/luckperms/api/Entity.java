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

package me.lucko.luckperms.api;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents an entity on the server.
 *
 * <p>This does not relate directly to a "Minecraft Entity". The closest
 * comparison is to a "CommandSender" or "CommandSource" in Bukkit/Sponge.</p>
 *
 * <p>The various types of {@link Entity} are detailed in {@link Type}.</p>
 *
 * @since 4.1
 */
public interface Entity {

    /**
     * Gets the unique id of the entity, if it has one.
     *
     * <p>For players, this returns their uuid assigned by the server.</p>
     *
     * @return the uuid of the object, if available
     */
    @Nullable
    UUID getUniqueId();

    /**
     * Gets the name of the object
     *
     * @return the object name
     */
    @Nonnull
    String getName();

    /**
     * Gets the entities type.
     *
     * @return the type
     */
    @Nonnull
    Type getType();

    /**
     * The different types of {@link Entity}
     */
    enum Type {

        /**
         * Represents a player connected to the server
         */
        PLAYER,

        /**
         * Represents the server console
         */
        CONSOLE
    }

}
