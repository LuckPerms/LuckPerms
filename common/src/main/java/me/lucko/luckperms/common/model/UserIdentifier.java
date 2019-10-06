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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Used to identify a specific {@link User}.
 */
public final class UserIdentifier {

    /**
     * Creates a {@link UserIdentifier}.
     *
     * @param uniqueId the uuid of the user
     * @param username the username of the user, nullable
     * @return a new identifier
     */
    public static UserIdentifier of(@NonNull UUID uniqueId, @Nullable String username) {
        Objects.requireNonNull(uniqueId, "uuid");
        if (username == null || username.equalsIgnoreCase("null") || username.isEmpty()) {
            username = null;
        }

        return new UserIdentifier(uniqueId, username);
    }

    private final UUID uniqueId;
    private final String username;

    private UserIdentifier(UUID uniqueId, String username) {
        this.uniqueId = uniqueId;
        this.username = username;
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public Optional<String> getUsername() {
        return Optional.ofNullable(this.username);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof UserIdentifier)) return false;
        final UserIdentifier other = (UserIdentifier) o;
        return this.uniqueId.equals(other.uniqueId);
    }

    @Override
    public int hashCode() {
        return this.uniqueId.hashCode();
    }

    @Override
    public String toString() {
        return "UserIdentifier(uniqueId=" + this.uniqueId + ", username=" + this.username + ")";
    }
}
