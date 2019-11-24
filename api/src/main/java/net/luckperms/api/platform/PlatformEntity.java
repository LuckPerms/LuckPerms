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

package net.luckperms.api.platform;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.UUID;

/**
 * Represents an entity on the server.
 *
 * <p>This does not relate directly to a "Minecraft Entity". The closest
 * comparison is to a "CommandSender" or "CommandSource".</p>
 *
 * <p>The various types of {@link PlatformEntity} are detailed in {@link Type}.</p>
 */
public interface PlatformEntity {

    /**
     * Gets the unique id of the entity, if it has one.
     *
     * <p>For players, this returns their uuid assigned by the server.</p>
     *
     * @return the uuid of the object, if available
     */
    @Nullable UUID getUniqueId();

    /**
     * Gets the name of the object
     *
     * @return the object name
     */
    @NonNull String getName();

    /**
     * Gets the entities type.
     *
     * @return the type
     */
    @NonNull Type getType();

    /**
     * The different types of {@link PlatformEntity}
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
