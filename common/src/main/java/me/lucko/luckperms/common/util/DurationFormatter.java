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
import java.time.temporal.ChronoUnit;

/**
 * Formats durations to a readable form
 */
public enum DurationFormatter {
    LONG,
    CONCISE {
        @Override
        protected String formatUnitPlural(ChronoUnit unit) {
            return String.valueOf(Character.toLowerCase(unit.name().charAt(0)));
        }

        @Override
        protected String formatUnitSingular(ChronoUnit unit) {
            return formatUnitPlural(unit);
        }
    },
    CONCISE_LOW_ACCURACY(3) {
        @Override
        protected String formatUnitPlural(ChronoUnit unit) {
            return String.valueOf(Character.toLowerCase(unit.name().charAt(0)));
        }

        @Override
        protected String formatUnitSingular(ChronoUnit unit) {
            return formatUnitPlural(unit);
        }
    };

    private final Unit[] units = new Unit[]{
            new Unit(ChronoUnit.YEARS),
            new Unit(ChronoUnit.MONTHS),
            new Unit(ChronoUnit.WEEKS),
            new Unit(ChronoUnit.DAYS),
            new Unit(ChronoUnit.HOURS),
            new Unit(ChronoUnit.MINUTES),
            new Unit(ChronoUnit.SECONDS)
    };

    private final int accuracy;

    DurationFormatter() {
        this(Integer.MAX_VALUE);
    }

    DurationFormatter(int accuracy) {
        this.accuracy = accuracy;
    }

    /**
     * Formats {@code duration} as a string.
     *
     * @param duration the duration
     * @return the formatted string
     */
    public String format(Duration duration) {
        long seconds = duration.getSeconds();
        StringBuilder output = new StringBuilder();
        int outputSize = 0;

        for (Unit unit : this.units) {
            long n = seconds / unit.duration;
            if (n > 0) {
                seconds -= unit.duration * n;
                output.append(' ').append(n).append(unit.toString(n));
                outputSize++;
            }
            if (seconds <= 0 || outputSize >= this.accuracy) {
                break;
            }
        }

        if (output.length() == 0) {
            return "0" + this.units[this.units.length - 1].stringPlural;
        }
        return output.substring(1);
    }

    protected String formatUnitPlural(ChronoUnit unit) {
        return " " + unit.name().toLowerCase();
    }

    protected String formatUnitSingular(ChronoUnit unit) {
        String s = unit.name().toLowerCase();
        return " " + s.substring(0, s.length() - 1);
    }

    private final class Unit {
        private final long duration;
        private final String stringPlural;
        private final String stringSingular;

        Unit(ChronoUnit unit) {
            this.duration = unit.getDuration().getSeconds();
            this.stringPlural = formatUnitPlural(unit);
            this.stringSingular = formatUnitSingular(unit);
        }

        public String toString(long n) {
            return n == 1 ? this.stringSingular : this.stringPlural;
        }
    }

}
