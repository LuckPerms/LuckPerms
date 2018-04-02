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

package me.lucko.luckperms.common.utils;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Formats durations to a readable form
 *
 * @author khobbits, drtshock, vemacs
 * see: https://github.com/drtshock/Essentials/blob/2.x/Essentials/src/com/earth2me/essentials/utils/DateUtil.java
 */
public enum DurationFormatter {

    CONCISE {
        private final String[] names = new String[]{"y", "y", "m", "m", "d", "d", "h", "h", "m", "m", "s", "s"};

        @Override
        public String format(Calendar from, Calendar to) {
            return dateDiff(from, to, 4, this.names);
        }
    },

    CONCISE_LOW_ACCURACY {
        private final String[] names = new String[]{"y", "y", "m", "m", "d", "d", "h", "h", "m", "m", "s", "s"};

        @Override
        public String format(Calendar from, Calendar to) {
            return dateDiff(from, to, 2, this.names);
        }
    },

    LONG {
        private final String[] names = new String[]{"year", "years", "month", "months", "day", "days", "hour", "hours", "minute", "minutes", "second", "seconds"};

        @Override
        public String format(Calendar from, Calendar to) {
            return dateDiff(from, to, 4, this.names);
        }
    };

    /**
     * The calender type magic numbers to use when formatting
     */
    private static final int[] CALENDAR_TYPES = new int[] {
            Calendar.YEAR,
            Calendar.MONTH,
            Calendar.DAY_OF_MONTH,
            Calendar.HOUR_OF_DAY,
            Calendar.MINUTE,
            Calendar.SECOND
    };

    private static final int MAX_YEARS = 100000;

    /**
     * Formats the difference between two dates
     *
     * @param from the start date
     * @param to the end date
     * @param maxAccuracy how accurate the output should be (how many sections it'll have)
     * @param names the names to use to format each of the corresponding {@link #CALENDAR_TYPES}
     * @return a formatted string
     */
    private static String dateDiff(Calendar from, Calendar to, int maxAccuracy, String[] names) {
        boolean future = false;
        if (to.equals(from)) {
            return "now";
        }
        if (to.after(from)) {
            future = true;
        }

        StringBuilder sb = new StringBuilder();
        int accuracy = 0;
        for (int i = 0; i < CALENDAR_TYPES.length; i++) {
            if (accuracy > maxAccuracy) {
                break;
            }

            int diff = dateDiff(CALENDAR_TYPES[i], from, to, future);
            if (diff > 0) {
                accuracy++;
                sb.append(" ").append(diff).append(" ").append(names[i * 2 + (diff > 1 ? 1 : 0)]);
            }
        }

        if (sb.length() == 0) {
            return "now";
        }

        return sb.toString().trim();
    }

    private static int dateDiff(int type, Calendar fromDate, Calendar toDate, boolean future) {
        int year = Calendar.YEAR;

        int fromYear = fromDate.get(year);
        int toYear = toDate.get(year);
        if (Math.abs(fromYear - toYear) > MAX_YEARS) {
            toDate.set(year, fromYear + (future ? MAX_YEARS : -MAX_YEARS));
        }

        int diff = 0;
        long savedDate = fromDate.getTimeInMillis();
        while ((future && !fromDate.after(toDate)) || (!future && !fromDate.before(toDate))) {
            savedDate = fromDate.getTimeInMillis();
            fromDate.add(type, future ? 1 : -1);
            diff++;
        }

        diff--;
        fromDate.setTimeInMillis(savedDate);
        return diff;
    }

    /**
     * Formats the time difference between two dates
     *
     * @param from the start date
     * @param to the end date
     * @return the formatted duration string
     */
    public abstract String format(Calendar from, Calendar to);

    /**
     * Formats a duration, in seconds
     *
     * @param seconds the duration
     * @return the formatted duration string
     */
    public String format(long seconds) {
        Calendar from = new GregorianCalendar();
        from.setTimeInMillis(0);

        Calendar to = new GregorianCalendar();
        to.setTimeInMillis(seconds * 1000L);

        return format(from, to);
    }

    /**
     * Formats the duration between the current time and the given unix timestamp
     *
     * @param unixTimestamp the timestamp, in seconds
     * @return the formatted duration string
     */
    public String formatDateDiff(long unixTimestamp) {
        long now = System.currentTimeMillis() / 1000L;
        return format(unixTimestamp - now);
    }
}
