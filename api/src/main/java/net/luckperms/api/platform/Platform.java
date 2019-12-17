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

import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * Provides information about the platform LuckPerms is running on.
 */
public interface Platform {

    /**
     * Gets the type of platform LuckPerms is running on
     *
     * @return the type of platform LuckPerms is running on
     */
    Platform.@NonNull Type getType();

    /**
     * Gets the type of environment LuckPerms is running on.
     *
     * @return the environment LuckPerms is running on.
     */
    Platform.@NonNull Environment getEnvironment();

    /**
     * Gets the unique players which have connected to the server since it started.
     *
     * @return the unique connections
     */
    @NonNull Set<UUID> getUniqueConnections();

    /**
     * Gets a {@link Collection} of all known permission strings.
     *
     * @return a collection of the known permissions
     */
    @NonNull Collection<String> getKnownPermissions();

    /**
     * Gets the time when the plugin first started.
     *
     * @return the enable time
     */
    @NonNull Instant getStartTime();

    /**
     * Represents a type of platform which LuckPerms can run on.
     */
    enum Type {
        BUKKIT("Bukkit"),
        BUNGEECORD("BungeeCord"),
        SPONGE("Sponge"),
        NUKKIT("Nukkit"),
        VELOCITY("Velocity"),
        FABRIC("Fabric");

        private final String friendlyName;

        Type(String friendlyName) {
            this.friendlyName = friendlyName;
        }

        /**
         * Gets a readable name for the platform type.
         *
         * @return a readable name
         */
        public @NonNull String getFriendlyName() {
            return this.friendlyName;
        }
    }

    /**
     * Represents the environment LuckPerms is being run on.
     */
    enum Environment {
        CLIENT("Client"),
        SERVER("Server"),
        PROXY("Proxy");

        private final String friendlyName;

        Environment(String friendlyName) {
            this.friendlyName = friendlyName;
        }

        /**
         * Gets a readable name for the environment type.
         *
         * @return a readable name.
         */
        public @NonNull String getFriendlyName() {
            return this.friendlyName;
        }
    }
}
