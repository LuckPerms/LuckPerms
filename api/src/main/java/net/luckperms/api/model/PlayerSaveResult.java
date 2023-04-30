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

package net.luckperms.api.model;

import net.luckperms.api.model.user.UserManager;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Encapsulates the result of an operation to save uuid data about a player.
 *
 * <p>The corresponding method can be found at
 * {@link UserManager#savePlayerData(UUID, String)}.</p>
 */
public interface PlayerSaveResult {

    /**
     * Gets the status returned by the operation
     *
     * @return the status
     */
    @NonNull @Unmodifiable Set<Outcome> getOutcomes();

    /**
     * Gets if the result includes a certain outcome.
     *
     * @param outcome the outcome to check for
     * @return if the result includes the outcome
     */
    default boolean includes(@NonNull Outcome outcome) {
        Objects.requireNonNull(outcome, "outcome");
        return getOutcomes().contains(outcome);
    }

    /**
     * Gets the previous username involved in the result.
     *
     * <p>Returns null when the result doesn't {@link #includes(Outcome) include} the
     * {@link Outcome#USERNAME_UPDATED} status.</p>
     *
     * @return the previous username
     * @see Outcome#USERNAME_UPDATED
     */
    @Nullable String getPreviousUsername();

    /**
     * Gets the other uuids involved in the result.
     *
     * <p>Returns null when the result doesn't {@link #includes(Outcome) include} the
     * {@link Outcome#OTHER_UNIQUE_IDS_PRESENT_FOR_USERNAME} status.</p>
     *
     * @return the other uuids
     * @see Outcome#OTHER_UNIQUE_IDS_PRESENT_FOR_USERNAME
     */
    @Nullable @Unmodifiable Set<UUID> getOtherUniqueIds();

    /**
     * The various states the result can take
     */
    enum Outcome {

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
        OTHER_UNIQUE_IDS_PRESENT_FOR_USERNAME,
    }
}
