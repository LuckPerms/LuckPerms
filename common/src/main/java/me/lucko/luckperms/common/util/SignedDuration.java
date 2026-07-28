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

package me.lucko.luckperms.common.util;

import java.time.Duration;

/**
 * A {@link Duration} paired with a {@link Sign}, indicating how it should be applied in
 * relation to any duration that already exists.
 *
 * <p>Parsed from arguments of the form {@code 1h}, {@code +1h} and {@code -1h}.</p>
 */
public final class SignedDuration {

    /**
     * How a {@link SignedDuration} relates to an already existing duration.
     */
    public enum Sign {
        /**
         * No sign was given - the duration stands on its own.
         */
        ABSOLUTE,

        /**
         * The duration was prefixed with {@code +} - it should be added on top of the
         * existing duration.
         */
        ADD,

        /**
         * The duration was prefixed with {@code -} - it should be taken away from the
         * existing duration.
         */
        SUBTRACT
    }

    public static SignedDuration of(Sign sign, Duration duration) {
        return new SignedDuration(sign, duration);
    }

    private final Sign sign;
    private final Duration duration;

    private SignedDuration(Sign sign, Duration duration) {
        this.sign = sign;
        this.duration = duration;
    }

    public Sign sign() {
        return this.sign;
    }

    /**
     * Gets the magnitude of the duration. Never negative - the direction is carried by
     * the {@link #sign()}.
     */
    public Duration duration() {
        return this.duration;
    }

    public boolean isSigned() {
        return this.sign != Sign.ABSOLUTE;
    }

}
