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

package net.luckperms.api.event.player.lookup;

import net.luckperms.api.event.LuckPermsEvent;
import net.luckperms.api.event.type.ResultEvent;
import net.luckperms.api.event.util.Param;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Called when the platform needs to determine the type of a player's {@link UUID unique id}.
 *
 * @since 5.3
 */
public interface UniqueIdDetermineTypeEvent extends LuckPermsEvent, ResultEvent<String> {

    /**
     * The players UUID has been obtained by authenticating with the Mojang session servers.
     *
     * <p>Usually indicated by the UUID being {@link UUID#version() version} 4.</p>
     */
    String TYPE_AUTHENTICATED = "authenticated";

    /**
     * The players UUID has not been obtained through authentication, and instead is likely based
     * on the username they connected with.
     *
     * <p>Usually indicated by the UUID being {@link UUID#version() version} 3.</p>
     */
    String TYPE_UNAUTHENTICATED = "unauthenticated";

    /**
     * Gets the {@link UUID unique id} being queried.
     *
     * @return the unique id
     */
    @Param(0)
    @NonNull UUID getUniqueId();

    /**
     * Gets the current result unique id type.
     *
     * @return the type
     */
    default @NonNull String getType() {
        return result().get();
    }

    /**
     * Sets the result unique id type.
     *
     * @param type the type
     */
    default void setType(@NonNull String type) {
        Objects.requireNonNull(type, "type");
        result().set(type);
    }

}
