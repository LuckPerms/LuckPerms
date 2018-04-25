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

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents the result to a player history save operation
 */
public final class PlayerSaveResult {
    private static final PlayerSaveResult CLEAN_INSERT = new PlayerSaveResult(Status.CLEAN_INSERT);
    private static final PlayerSaveResult NO_CHANGE = new PlayerSaveResult(Status.NO_CHANGE);

    public static PlayerSaveResult cleanInsert() {
        return CLEAN_INSERT;
    }

    public static PlayerSaveResult noChange() {
        return NO_CHANGE;
    }

    public static PlayerSaveResult usernameUpdated(String oldUsername) {
        return new PlayerSaveResult(EnumSet.of(Status.USERNAME_UPDATED), oldUsername, null);
    }

    public static PlayerSaveResult determineBaseResult(String username, String oldUsername) {
        PlayerSaveResult result;
        if (oldUsername == null) {
            result = PlayerSaveResult.cleanInsert();
        } else if (oldUsername.equalsIgnoreCase(username)) {
            result = PlayerSaveResult.noChange();
        } else {
            result = PlayerSaveResult.usernameUpdated(oldUsername);
        }
        return result;
    }

    private final Set<Status> status;
    @Nullable private final String oldUsername;
    @Nullable private final Set<UUID> otherUuids;

    private PlayerSaveResult(EnumSet<Status> status, @Nullable String oldUsername, @Nullable Set<UUID> otherUuids) {
        this.status = ImmutableSet.copyOf(status);
        this.oldUsername = oldUsername;
        this.otherUuids = otherUuids;
    }

    private PlayerSaveResult(Status status) {
        this(EnumSet.of(status), null, null);
    }

    /**
     * Returns a new {@link PlayerSaveResult} with the {@link Status#OTHER_UUIDS_PRESENT_FOR_USERNAME}
     * status attached to the state of this result.
     *
     * @param otherUuids the other uuids
     * @return a new result
     */
    public PlayerSaveResult withOtherUuidsPresent(@Nonnull Set<UUID> otherUuids) {
        EnumSet<Status> status = EnumSet.copyOf(this.status);
        status.add(Status.OTHER_UUIDS_PRESENT_FOR_USERNAME);
        return new PlayerSaveResult(status, this.oldUsername, ImmutableSet.copyOf(otherUuids));
    }

    /**
     * Gets the status returned by the operation
     *
     * @return the status
     */
    public Set<Status> getStatus() {
        return this.status;
    }

    /**
     * Gets if the result includes a certain status code.
     *
     * @param status the status to check for
     * @return if the result includes the status
     */
    public boolean includes(Status status) {
        return this.status.contains(status);
    }

    /**
     * Gets the old username involved in the result
     *
     * @return the old username
     * @see Status#USERNAME_UPDATED
     */
    @Nullable
    public String getOldUsername() {
        return this.oldUsername;
    }

    /**
     * Gets the other uuids involved in the result
     *
     * @return the other uuids
     * @see Status#OTHER_UUIDS_PRESENT_FOR_USERNAME
     */
    @Nullable
    public Set<UUID> getOtherUuids() {
        return this.otherUuids;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;
        PlayerSaveResult result = (PlayerSaveResult) that;
        return Objects.equals(this.status, result.status) &&
                Objects.equals(this.oldUsername, result.oldUsername) &&
                Objects.equals(this.otherUuids, result.otherUuids);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.status, this.oldUsername, this.otherUuids);
    }

    /**
     * The various states the result can take
     */
    public enum Status {

        /**
         * There was no existing data saved for either the uuid or username
         */
        CLEAN_INSERT,

        /**
         * There was existing data for the player, no change was needed.
         */
        NO_CHANGE,

        /**
         * There was already a record for the UUID saved, but it was for a different username.
         *
         * <p>This is normal, players are able to change their usernames.</p>
         */
        USERNAME_UPDATED,

        /**
         * There was already a record for the username saved, but it was under a different uuid.
         *
         * <p>This is a bit of a cause for concern. It's possible that "player1" has changed
         * their username to "player2", and "player3" has changed their username to "player1".
         * If the original "player1" doesn't join after changing their name, this conflict could
         * occur.</p>
         *
         * <p>However, what's more likely is that the server is not setup to authenticate
         * correctly. Usually this is a problem with BungeeCord "ip-forwarding", but could be
         * that the user of the plugin is running a network off a shared database with one
         * server in online mode and another in offline mode.</p>
         */
        OTHER_UUIDS_PRESENT_FOR_USERNAME,
    }
}
