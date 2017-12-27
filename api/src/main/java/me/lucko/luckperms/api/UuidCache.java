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

/**
 * A UUID cache for online users, between external Mojang UUIDs, and internal
 * LuckPerms UUIDs.
 *
 * <p>A user's internal LuckPerms UUID is always the same as their Mojang one,
 * unless <code>use-server-uuids</code> is disabled.</p>
 *
 * <p>When this setting is disabled, this cache becomes active, and allows you
 * to convert between 'internal' and 'server provided' uuids.</p>
 *
 * <p><strong>This is only effective for online players.
 * Use {@link Storage#getUUID(String)} for offline players.</strong></p>
 */
public interface UuidCache {

    /**
     * Gets a users "internal" LuckPerms UUID, from the one given by the server.
     *
     * <p>When <code>use-server-uuids</code> is true, this returns the same UUID
     * instance.</p>
     *
     * @param mojangUuid the UUID assigned by the server, through
     *                   <code>Player#getUniqueId</code> or
     *                   <code>ProxiedPlayer#getUniqueId</code>
     * @return the corresponding internal UUID
     */
    @Nonnull
    UUID getUUID(@Nonnull UUID mojangUuid);

    /**
     * Gets a users "external", server assigned unique id, from the internal
     * one used within LuckPerms.
     *
     * @param internalUuid the UUID used within LuckPerms, through
     *                     <code>User#getUuid</code>
     * @return the corresponding external UUID
     */
    @Nonnull
    UUID getExternalUUID(@Nonnull UUID internalUuid);

}
