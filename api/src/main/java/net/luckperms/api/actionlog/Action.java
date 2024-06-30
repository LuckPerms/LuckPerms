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

package net.luckperms.api.actionlog;

import net.luckperms.api.LuckPermsProvider;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a logged action.
 *
 * <p>API users should not implement this interface directly.</p>
 */
@NonExtendable
public interface Action extends Comparable<Action> {

    /**
     * Gets a {@link Action.Builder}
     *
     * @return a new builder
     */
    static @NonNull Builder builder() {
        return LuckPermsProvider.get().getActionLogger().actionBuilder();
    }

    /**
     * Gets the time when the action occurred.
     *
     * @return the timestamp
     */
    @NonNull Instant getTimestamp();

    /**
     * Gets the source of the action.
     *
     * @return the source
     */
    @NonNull Source getSource();

    /**
     * Gets the target of the action.
     *
     * @return the target
     */
    @NonNull Target getTarget();

    /**
     * Returns a string describing the action which took place.
     *
     * <p>In most instances, this returns a variation of the command string which caused
     * the change.</p>
     *
     * @return the action
     */
    @NonNull String getDescription();

    /**
     * Represents the source of an action.
     */
    @NonExtendable
    interface Source {

        /**
         * Gets the source unique id.
         *
         * @return the source unique id
         */
        @NonNull UUID getUniqueId();

        /**
         * Gets the source name.
         *
         * @return the source name
         */
        @NonNull String getName();

    }

    /**
     * Represents the target of an action.
     */
    @NonExtendable
    interface Target {

        /**
         * Gets the target unique id.
         *
         * @return the target unique id
         */
        @NonNull Optional<UUID> getUniqueId();

        /**
         * Gets the target name.
         *
         * @return the target name
         */
        @NonNull String getName();

        /**
         * Gets the target type.
         *
         * @return the target type
         */
        @NonNull Type getType();

        /**
         * Represents the type of {@link Target}.
         */
        enum Type {
            USER, GROUP, TRACK
        }
    }

    /**
     * Builds an {@link Action} instance
     */
    interface Builder {

        /**
         * Sets the timestamp of the entry.
         *
         * @param timestamp the timestamp
         * @return the builder
         * @see Action#getTimestamp()
         */
        @NonNull Builder timestamp(@NonNull Instant timestamp);

        /**
         * Sets the actor of the entry.
         *
         * @param actor the actor
         * @return the builder
         */
        @NonNull Builder source(@NonNull UUID actor);

        /**
         * Sets the actor name of the entry.
         *
         * @param actorName the actor name
         * @return the builder
         */
        @NonNull Builder sourceName(@NonNull String actorName);

        /**
         * Sets the type of the entry.
         *
         * @param type the type
         * @return the builder
         */
        @NonNull Builder targetType(Target.@NonNull Type type);

        /**
         * Sets the acted object for the entry.
         *
         * @param acted the acted object
         * @return the builder
         */
        @NonNull Builder target(@Nullable UUID acted);

        /**
         * Sets the acted name for the entry.
         *
         * @param actedName the acted name
         * @return the builder
         */
        @NonNull Builder targetName(@NonNull String actedName);

        /**
         * Sets the action of the entry.
         *
         * @param action the action
         * @return the builder
         */
        @NonNull Builder description(@NonNull String action);

        /**
         * Creates a {@link Action} instance from the builder.
         *
         * @return a new log entry instance
         */
        @NonNull Action build();

    }

}
