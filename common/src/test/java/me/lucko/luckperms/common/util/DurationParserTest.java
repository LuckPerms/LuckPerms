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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DurationParserTest {

    private static Stream<Arguments> testSimple() {
        Duration years = ChronoUnit.YEARS.getDuration();
        Duration months = ChronoUnit.MONTHS.getDuration();

        return Stream.of(
                Arguments.of("2y", years.multipliedBy(2)),
                Arguments.of("3year", years.multipliedBy(3)),
                Arguments.of("4years", years.multipliedBy(4)),
                Arguments.of("2 y", years.multipliedBy(2)),
                Arguments.of("3 year", years.multipliedBy(3)),
                Arguments.of("4 years", years.multipliedBy(4)),
        
                Arguments.of("2mo", months.multipliedBy(2)),
                Arguments.of("3month", months.multipliedBy(3)),
                Arguments.of("4months", months.multipliedBy(4)),
                Arguments.of("2 mo", months.multipliedBy(2)),
                Arguments.of("3 month", months.multipliedBy(3)),
                Arguments.of("4 months", months.multipliedBy(4)),
        
                Arguments.of("2w", Duration.ofDays(7 * 2)),
                Arguments.of("3week", Duration.ofDays(7 * 3)),
                Arguments.of("4weeks", Duration.ofDays(7 * 4)),
                Arguments.of("2 w", Duration.ofDays(7 * 2)),
                Arguments.of("3 week", Duration.ofDays(7 * 3)),
                Arguments.of("4 weeks", Duration.ofDays(7 * 4)),
        
                Arguments.of("2d", Duration.ofDays(2)),
                Arguments.of("3day", Duration.ofDays(3)),
                Arguments.of("4days", Duration.ofDays(4)),
                Arguments.of("2 d", Duration.ofDays(2)),
                Arguments.of("3 day", Duration.ofDays(3)),
                Arguments.of("4 days", Duration.ofDays(4)),
        
                Arguments.of("2h", Duration.ofHours(2)),
                Arguments.of("3hour", Duration.ofHours(3)),
                Arguments.of("4hours", Duration.ofHours(4)),
                Arguments.of("2 h", Duration.ofHours(2)),
                Arguments.of("3 hour", Duration.ofHours(3)),
                Arguments.of("4 hours", Duration.ofHours(4)),
        
                Arguments.of("2m", Duration.ofMinutes(2)),
                Arguments.of("3min", Duration.ofMinutes(3)),
                Arguments.of("4mins", Duration.ofMinutes(4)),
                Arguments.of("5minute", Duration.ofMinutes(5)),
                Arguments.of("6minutes", Duration.ofMinutes(6)),
                Arguments.of("2 m", Duration.ofMinutes(2)),
                Arguments.of("3 min", Duration.ofMinutes(3)),
                Arguments.of("4 mins", Duration.ofMinutes(4)),
                Arguments.of("5 minute", Duration.ofMinutes(5)),
                Arguments.of("6 minutes", Duration.ofMinutes(6)),
        
                Arguments.of("2s", Duration.ofSeconds(2)),
                Arguments.of("3sec", Duration.ofSeconds(3)),
                Arguments.of("4secs", Duration.ofSeconds(4)),
                Arguments.of("5second", Duration.ofSeconds(5)),
                Arguments.of("6seconds", Duration.ofSeconds(6)),
                Arguments.of("2 s", Duration.ofSeconds(2)),
                Arguments.of("3 sec", Duration.ofSeconds(3)),
                Arguments.of("4 secs", Duration.ofSeconds(4)),
                Arguments.of("5 second", Duration.ofSeconds(5)),
                Arguments.of("6 seconds", Duration.ofSeconds(6))
        );
    }
    
    @ParameterizedTest
    @MethodSource
    public void testSimple(String input, Duration expected) {
        assertEquals(expected, DurationParser.parseDuration(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "5y 4mo 3w 2d 1h 6m 7s",
            "5y4mo3w2d1h6m7s",
            "5 years 4 months 3 weeks 2 days 1 hour 6 minutes 7 seconds",
            "5y, 4mo, 3w, 2d, 1h, 6m, 7s",
            "5y,4mo,3w,2d,1h,6m,7s",
            "5 years, 4 months, 3 weeks, 2 days, 1 hour, 6 minutes, 7 seconds"
    })
    public void testCombined(String input) {
        Duration expected = ChronoUnit.YEARS.getDuration().multipliedBy(5)
                .plus(ChronoUnit.MONTHS.getDuration().multipliedBy(4))
                .plus(ChronoUnit.WEEKS.getDuration().multipliedBy(3))
                .plusDays(2)
                .plusHours(1)
                .plusMinutes(6)
                .plusSeconds(7);

        assertEquals(expected, DurationParser.parseDuration(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "definitely not a duration",
            "still 1 not a duration",
            "still 1s not a duration"
    })
    public void testFail(String input) {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parseDuration(input));
    }

}
