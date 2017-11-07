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

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents a logged action.
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
    UUID getActor();

    /**
     * Gets the name describing the actor.
     *
     * @return the name of the actor
     */
    String getActorName();

    /**
     * Gets the type of action.
     *
     * @return the action type
     */
    Type getType();

    /**
     * Gets the uuid of the object which was acted upon.
     *
     * <p>Will only return a value for {@link Type#USER} entries.</p>
     *
     * @return the uuid of acted object
     */
    Optional<UUID> getActed();

    /**
     * Gets the name describing the object which was acted upon
     *
     * @return the name of the acted object
     */
    String getActedName();

    /**
     * Returns a string describing the action which took place.
     *
     * <p>In most instances, this returns a variation of the command string which caused
     * the change.</p>
     *
     * @return the action
     */
    String getAction();

    /**
     * Represents the type of a {@link LogEntry}.
     *
     * @since 3.3
     */
    enum Type {
        USER('U'),
        GROUP('G'),
        TRACK('T');

        private final char code;

        Type(char code) {
            this.code = code;
        }

        public char getCode() {
            return this.code;
        }

        public static Type valueOf(char code) {
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
    }

    /**
     * Builds a LogEntry instance
     */
    interface Builder {

        @Nonnull
        Builder setTimestamp(long timestamp);

        @Nonnull
        Builder setActor(@Nonnull UUID actor);

        @Nonnull
        Builder setActorName(@Nonnull String actorName);

        @Nonnull
        Builder setType(@Nonnull Type type);

        @Nonnull
        Builder setActed(@Nullable UUID acted);

        @Nonnull
        Builder setActedName(@Nonnull String actedName);

        @Nonnull
        Builder setAction(@Nonnull String action);

        @Nonnull
        LogEntry build();

    }

}
