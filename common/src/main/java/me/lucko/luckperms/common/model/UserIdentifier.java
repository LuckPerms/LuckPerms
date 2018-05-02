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

package me.lucko.luckperms.common.model;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Used to identify a specific {@link User}.
 */
public final class UserIdentifier implements Identifiable<UUID> {

    /**
     * Creates a {@link UserIdentifier}.
     *
     * @param uuid the uuid of the user
     * @param username the username of the user, nullable
     * @return
     */
    public static UserIdentifier of(@Nonnull UUID uuid, @Nullable String username) {
        Objects.requireNonNull(uuid, "uuid");
        if (username == null || username.equalsIgnoreCase("null") || username.isEmpty()) {
            username = null;
        }

        return new UserIdentifier(uuid, username);
    }

    private final UUID uuid;
    private final String username;

    private UserIdentifier(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    @Override
    public UUID getId() {
        return getUuid();
    }

    public Optional<String> getUsername() {
        return Optional.ofNullable(this.username);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof UserIdentifier)) return false;
        final UserIdentifier other = (UserIdentifier) o;
        return this.uuid.equals(other.uuid);
    }

    @Override
    public int hashCode() {
        return this.uuid.hashCode();
    }

    @Override
    public String toString() {
        return "UserIdentifier(uuid=" + this.uuid + ", username=" + this.username + ")";
    }
}
