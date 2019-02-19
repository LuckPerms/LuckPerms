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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Represents a logged action.
 *
 * @see ActionLogger#newEntryBuilder() for creating an instance
 */
public interface LogEntry extends Comparable<LogEntry> {

    /**
     * Gets the time in unix seconds when the action occurred.
     *
     * @return the timestamp
     */
    long getTimestamp();

    /**
     * Gets the id of the object which performed the action.
     *
     * <p>This is the players uuid in most cases.</p>
     *
     * @return the actor id
     */
    @NonNull UUID getActor();

    /**
     * Gets the name describing the actor.
     *
     * @return the name of the actor
     */
    @NonNull String getActorName();

    /**
     * Gets the type of action.
     *
     * @return the action type
     */
    @NonNull Type getType();

    /**
     * Gets the uuid of the object which was acted upon.
     *
     * <p>Will only return a value for {@link Type#USER} entries.</p>
     *
     * @return the uuid of acted object
     */
    @NonNull Optional<UUID> getActed();

    /**
     * Gets the name describing the object which was acted upon
     *
     * @return the name of the acted object
     */
    @NonNull String getActedName();

    /**
     * Returns a string describing the action which took place.
     *
     * <p>In most instances, this returns a variation of the command string which caused
     * the change.</p>
     *
     * @return the action
     */
    @NonNull String getAction();

    /**
     * Represents the type of a {@link LogEntry}.
     *
     * @since 3.3
     */
    enum Type {
        USER('U'), GROUP('G'), TRACK('T');

        /**
         * Parses a {@link Type} from a string.
         *
         * @param type the string
         * @return a type
         * @throws IllegalArgumentException if a type could not be parsed
         * @since 4.4
         */
        public static @NonNull Type parse(String type) {
            try {
                return valueOf(type);
            } catch (IllegalArgumentException e) {
                // ignore
            }
            try {
                return valueOf(type.charAt(0));
            } catch (IllegalArgumentException e) {
                // ignore
            }
            throw new IllegalArgumentException("Unknown type: " + type);
        }

        /**
         * Returns a {@link Type} by its code.
         *
         * @param code the code - see {@link Type#getCode()}.
         * @return a type
         * @throws IllegalArgumentException if a type could not be resolved
         */
        public static @NonNull Type valueOf(char code) {
            switch (code) {
                case 'U':
                case 'u':
                    return USER;
                case 'G':
                case 'g':
                    return GROUP;
                case 'T':
                case 't':
                    return TRACK;
                default:
                    throw new IllegalArgumentException("Unknown code: " + code);
            }
        }

        private final char code;

        Type(char code) {
            this.code = code;
        }

        public char getCode() {
            return this.code;
        }
    }

    /**
     * Builds a LogEntry instance
     */
    interface Builder {

        /**
         * Sets the timestamp of the entry.
         *
         * @param timestamp the timestamp
         * @return the builder
         * @see LogEntry#getTimestamp()
         */
        @NonNull Builder setTimestamp(long timestamp);

        /**
         * Sets the actor of the entry.
         *
         * @param actor the actor
         * @return the builder
         * @see LogEntry#getActor()
         */
        @NonNull Builder setActor(@NonNull UUID actor);

        /**
         * Sets the actor name of the entry.
         *
         * @param actorName the actor name
         * @return the builder
         * @see LogEntry#getActorName()
         */
        @NonNull Builder setActorName(@NonNull String actorName);

        /**
         * Sets the type of the entry.
         *
         * @param type the type
         * @return the builder
         * @see LogEntry#getType()
         */
        @NonNull Builder setType(@NonNull Type type);

        /**
         * Sets the acted object for the entry.
         *
         * @param acted the acted object
         * @return the builder
         * @see LogEntry#getActed()
         */
        @NonNull Builder setActed(@Nullable UUID acted);

        /**
         * Sets the acted name for the entry.
         *
         * @param actedName the acted name
         * @return the builder
         * @see LogEntry#getActedName()
         */
        @NonNull Builder setActedName(@NonNull String actedName);

        /**
         * Sets the action of the entry.
         *
         * @param action the action
         * @return the builder
         * @see LogEntry#getAction()
         */
        @NonNull Builder setAction(@NonNull String action);

        /**
         * Creates a {@link LogEntry} instance from the builder.
         *
         * @return a new log entry instance
         */
        @NonNull LogEntry build();

    }

}
