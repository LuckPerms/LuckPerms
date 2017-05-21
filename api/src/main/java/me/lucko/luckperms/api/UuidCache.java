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

/**
 * A UUID cache for online users, between external Mojang UUIDs, and internal LuckPerms UUIDs.
 *
 * <p>This UuidCache is a means of allowing users to have the same internal UUID across a network of offline mode
 * servers or mixed offline mode and online mode servers. Platforms running in offline mode generate a UUID for a
 * user when they first join the server, but this UUID will then not be consistent across the network. LuckPerms will
 * instead check the datastore cache, to get a UUID for a user that is consistent across an entire network.</p>
 *
 * <p>If you want to get a user object from the Storage using the api on a server in offline mode, you will need to use
 * this cache, OR use Storage#getUUID, for users that are not online.</p>
 *
 * <p><strong>This is only effective for online players. Use {@link Storage#getUUID(String)} for offline players.</strong></p>
 */
public interface UuidCache {

    /**
     * Gets a users internal "LuckPerms" UUID, from the one given by the server.
     *
     * @param external the UUID assigned by the server, through Player#getUniqueId or ProxiedPlayer#getUniqueId
     * @return the corresponding internal UUID
     */
    UUID getUUID(UUID external);

    /**
     * Gets a users external, server assigned or Mojang assigned unique id, from the internal one used within LuckPerms.
     *
     * @param internal the UUID used within LuckPerms, through User#getUuid
     * @return the corresponding external UUID
     */
    UUID getExternalUUID(UUID internal);

}
