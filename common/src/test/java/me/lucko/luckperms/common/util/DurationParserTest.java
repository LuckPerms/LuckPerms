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

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DurationParserTest {

    private static void test(Duration expected, String input) {
        assertEquals(expected, DurationParser.parseDuration(input));
    }

    @Test
    void testSimple() {
        test(ChronoUnit.YEARS.getDuration().multipliedBy(2), "2y");
        test(ChronoUnit.YEARS.getDuration().multipliedBy(3), "3year");
        test(ChronoUnit.YEARS.getDuration().multipliedBy(4), "4years");
        test(ChronoUnit.YEARS.getDuration().multipliedBy(2), "2 y");
        test(ChronoUnit.YEARS.getDuration().multipliedBy(3), "3 year");
        test(ChronoUnit.YEARS.getDuration().multipliedBy(4), "4 years");

        test(ChronoUnit.MONTHS.getDuration().multipliedBy(2), "2mo");
        test(ChronoUnit.MONTHS.getDuration().multipliedBy(3), "3month");
        test(ChronoUnit.MONTHS.getDuration().multipliedBy(4), "4months");
        test(ChronoUnit.MONTHS.getDuration().multipliedBy(2), "2 mo");
        test(ChronoUnit.MONTHS.getDuration().multipliedBy(3), "3 month");
        test(ChronoUnit.MONTHS.getDuration().multipliedBy(4), "4 months");

        test(Duration.ofDays(7 * 2), "2w");
        test(Duration.ofDays(7 * 3), "3week");
        test(Duration.ofDays(7 * 4), "4weeks");
        test(Duration.ofDays(7 * 2), "2 w");
        test(Duration.ofDays(7 * 3), "3 week");
        test(Duration.ofDays(7 * 4), "4 weeks");

        test(Duration.ofDays(2), "2d");
        test(Duration.ofDays(3), "3day");
        test(Duration.ofDays(4), "4days");
        test(Duration.ofDays(2), "2 d");
        test(Duration.ofDays(3), "3 day");
        test(Duration.ofDays(4), "4 days");

        test(Duration.ofHours(2), "2h");
        test(Duration.ofHours(3), "3hour");
        test(Duration.ofHours(4), "4hours");
        test(Duration.ofHours(2), "2 h");
        test(Duration.ofHours(3), "3 hour");
        test(Duration.ofHours(4), "4 hours");

        test(Duration.ofMinutes(2), "2m");
        test(Duration.ofMinutes(3), "3min");
        test(Duration.ofMinutes(4), "4mins");
        test(Duration.ofMinutes(5), "5minute");
        test(Duration.ofMinutes(6), "6minutes");
        test(Duration.ofMinutes(2), "2 m");
        test(Duration.ofMinutes(3), "3 min");
        test(Duration.ofMinutes(4), "4 mins");
        test(Duration.ofMinutes(5), "5 minute");
        test(Duration.ofMinutes(6), "6 minutes");

        test(Duration.ofSeconds(2), "2s");
        test(Duration.ofSeconds(3), "3sec");
        test(Duration.ofSeconds(4), "4secs");
        test(Duration.ofSeconds(5), "5second");
        test(Duration.ofSeconds(6), "6seconds");
        test(Duration.ofSeconds(2), "2 s");
        test(Duration.ofSeconds(3), "3 sec");
        test(Duration.ofSeconds(4), "4 secs");
        test(Duration.ofSeconds(5), "5 second");
        test(Duration.ofSeconds(6), "6 seconds");
    }

    @Test
    void testCombined() {
        Duration expected = ChronoUnit.YEARS.getDuration().multipliedBy(5)
                .plus(ChronoUnit.MONTHS.getDuration().multipliedBy(4))
                .plus(ChronoUnit.WEEKS.getDuration().multipliedBy(3))
                .plusDays(2)
                .plusHours(1)
                .plusMinutes(6)
                .plusSeconds(7);

        test(expected, "5y 4mo 3w 2d 1h 6m 7s");
        test(expected, "5y4mo3w2d1h6m7s");
        test(expected, "5 years 4 months 3 weeks 2 days 1 hour 6 minutes 7 seconds");
    }

}
