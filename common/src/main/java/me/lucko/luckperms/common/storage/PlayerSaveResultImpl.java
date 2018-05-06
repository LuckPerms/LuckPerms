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

package me.lucko.luckperms.common.storage;

import com.google.common.collect.ImmutableSet;

import me.lucko.luckperms.api.PlayerSaveResult;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents the result to a player history save operation
 */
public final class PlayerSaveResultImpl implements PlayerSaveResult {
    private static final PlayerSaveResultImpl CLEAN_INSERT = new PlayerSaveResultImpl(Status.CLEAN_INSERT);
    private static final PlayerSaveResultImpl NO_CHANGE = new PlayerSaveResultImpl(Status.NO_CHANGE);

    public static PlayerSaveResultImpl cleanInsert() {
        return CLEAN_INSERT;
    }

    public static PlayerSaveResultImpl noChange() {
        return NO_CHANGE;
    }

    public static PlayerSaveResultImpl usernameUpdated(String oldUsername) {
        return new PlayerSaveResultImpl(EnumSet.of(Status.USERNAME_UPDATED), oldUsername, null);
    }

    public static PlayerSaveResultImpl determineBaseResult(String username, String oldUsername) {
        PlayerSaveResultImpl result;
        if (oldUsername == null) {
            result = PlayerSaveResultImpl.cleanInsert();
        } else if (oldUsername.equalsIgnoreCase(username)) {
            result = PlayerSaveResultImpl.noChange();
        } else {
            result = PlayerSaveResultImpl.usernameUpdated(oldUsername);
        }
        return result;
    }

    private final Set<Status> status;
    @Nullable private final String oldUsername;
    @Nullable private final Set<UUID> otherUuids;

    private PlayerSaveResultImpl(EnumSet<Status> status, @Nullable String oldUsername, @Nullable Set<UUID> otherUuids) {
        this.status = ImmutableSet.copyOf(status);
        this.oldUsername = oldUsername;
        this.otherUuids = otherUuids;
    }

    private PlayerSaveResultImpl(Status status) {
        this(EnumSet.of(status), null, null);
    }

    /**
     * Returns a new {@link PlayerSaveResultImpl} with the {@link Status#OTHER_UUIDS_PRESENT_FOR_USERNAME}
     * status attached to the state of this result.
     *
     * @param otherUuids the other uuids
     * @return a new result
     */
    public PlayerSaveResultImpl withOtherUuidsPresent(@Nonnull Set<UUID> otherUuids) {
        EnumSet<Status> status = EnumSet.copyOf(this.status);
        status.add(Status.OTHER_UUIDS_PRESENT_FOR_USERNAME);
        return new PlayerSaveResultImpl(status, this.oldUsername, ImmutableSet.copyOf(otherUuids));
    }

    @Nonnull
    @Override
    public Set<Status> getStatus() {
        return this.status;
    }

    @Override
    public boolean includes(@Nonnull Status status) {
        return this.status.contains(status);
    }

    @Nullable
    @Override
    public String getOldUsername() {
        return this.oldUsername;
    }

    @Nullable
    @Override
    public Set<UUID> getOtherUuids() {
        return this.otherUuids;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;
        PlayerSaveResultImpl result = (PlayerSaveResultImpl) that;
        return Objects.equals(this.status, result.status) &&
                Objects.equals(this.oldUsername, result.oldUsername) &&
                Objects.equals(this.otherUuids, result.otherUuids);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.status, this.oldUsername, this.otherUuids);
    }

    @Override
    public String toString() {
        return "PlayerSaveResult(status=" + this.status + ", oldUsername=" + this.oldUsername + ", otherUuids=" + this.otherUuids + ")";
    }
}
