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

import net.luckperms.api.track.DemotionResult;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;
import java.util.Optional;

/**
 * Utility class for creating instances of {@link DemotionResult}.
 */
public final class DemotionResults {
    private DemotionResults() {}

    public static DemotionResult success(String groupFrom, String groupTo) {
        return new Impl(DemotionResult.Status.SUCCESS, groupFrom, groupTo);
    }

    public static DemotionResult removedFromFirst(String groupFrom) {
        return new Impl(DemotionResult.Status.REMOVED_FROM_FIRST_GROUP, groupFrom, null);
    }

    public static DemotionResult malformedTrack(String groupTo) {
        return new Impl(DemotionResult.Status.MALFORMED_TRACK, null, groupTo);
    }

    public static DemotionResult notOnTrack() {
        return new Impl(DemotionResult.Status.NOT_ON_TRACK);
    }

    public static DemotionResult ambiguousCall() {
        return new Impl(DemotionResult.Status.AMBIGUOUS_CALL);
    }

    public static DemotionResult undefinedFailure() {
        return new Impl(DemotionResult.Status.UNDEFINED_FAILURE);
    }

    private static final class Impl implements DemotionResult {
        private final Status status;
        private final String groupFrom;
        private final String groupTo;

        private Impl(Status status, String groupFrom, String groupTo) {
            this.status = status;
            this.groupFrom = groupFrom;
            this.groupTo = groupTo;
        }

        private Impl(Status status) {
            this(status, null, null);
        }

        @Override
        public @NonNull Status getStatus() {
            return this.status;
        }

        @Override
        public @NonNull Optional<String> getGroupFrom() {
            return Optional.ofNullable(this.groupFrom);
        }

        @Override
        public @NonNull Optional<String> getGroupTo() {
            return Optional.ofNullable(this.groupTo);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Impl that = (Impl) o;
            return this.status == that.status &&
                    Objects.equals(this.groupFrom, that.groupFrom) &&
                    Objects.equals(this.groupTo, that.groupTo);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.status, this.groupFrom, this.groupTo);
        }

        @Override
        public String toString() {
            return "DemotionResult(" +
                    "status=" + this.status + ", " +
                    "groupFrom='" + this.groupFrom + "', " +
                    "groupTo='" + this.groupTo + "')";
        }
    }

}
