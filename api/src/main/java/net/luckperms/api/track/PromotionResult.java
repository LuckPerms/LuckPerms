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

package net.luckperms.api.track;

import net.luckperms.api.model.user.User;
import net.luckperms.api.util.Result;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Optional;

/**
 * Encapsulates the result of {@link User}s promotion along a {@link Track}.
 */
public interface PromotionResult extends Result {

    /**
     * Gets the status of the result.
     *
     * @return the status
     */
    @NonNull Status getStatus();

    @Override
    default boolean wasSuccessful() {
        return getStatus().wasSuccessful();
    }

    /**
     * Gets the name of the group the user was promoted from, if applicable.
     *
     * <p>Will only be present for results with a {@link #getStatus() status} of
     * {@link Status#SUCCESS}.</p>
     *
     * @return the group the user was promoted from.
     */
    @NonNull Optional<String> getGroupFrom();

    /**
     * Gets the name of the group the user was promoted from, if applicable.
     *
     * <p>Will only be present for results with a {@link #getStatus() status} of
     * {@link Status#SUCCESS} or {@link Status#ADDED_TO_FIRST_GROUP}.</p>
     *
     * <p>The value will also be set for results with the {@link Status#MALFORMED_TRACK} status,
     * with this value marking the group which no longer exists.</p>
     *
     * @return the group the user was promoted to.
     */
    @NonNull Optional<String> getGroupTo();

    /**
     * The result status
     */
    enum Status implements Result {

        /**
         * Indicates that the user was promoted normally.
         */
        SUCCESS(true),

        /**
         * Indicates that the user was added to the first group in the track.
         *
         * <p>This usually occurs when the user isn't already on any of the groups in the track.</p>
         */
        ADDED_TO_FIRST_GROUP(true),

        /**
         * Indicates that the next group in the track no longer exists.
         */
        MALFORMED_TRACK(false),

        /**
         * Indicates that the user is already a member of the group at the end of the track,
         * and as such cannot be promoted any further.
         */
        END_OF_TRACK(false),

        /**
         * Indicates that the implementation was unable to determine the users current position on
         * this track.
         *
         * <p>This usually occurs when the user is on more than one group on the track.</p>
         */
        AMBIGUOUS_CALL(false),

        /**
         * An undefined failure occurred.
         */
        UNDEFINED_FAILURE(false);

        private final boolean success;

        Status(boolean success) {
            this.success = success;
        }

        @Override
        public boolean wasSuccessful() {
            return this.success;
        }
    }

}
