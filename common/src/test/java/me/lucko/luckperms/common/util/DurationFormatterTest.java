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

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.renderer.TranslatableComponentRenderer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.util.UTF8ResourceBundleControl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DurationFormatterTest {

    private static TranslatableComponentRenderer<Locale> renderer;

    @BeforeAll
    public static void setupRenderer() {
        TranslationRegistry registry = TranslationRegistry.create(Key.key("luckperms", "test"));

        ResourceBundle bundle = ResourceBundle.getBundle("luckperms", Locale.ENGLISH, UTF8ResourceBundleControl.get());
        registry.registerAll(Locale.ENGLISH, bundle, false);

        renderer = TranslatableComponentRenderer.usingTranslationSource(registry);
    }

    private static String render(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(renderer.render(component, Locale.ENGLISH));
    }

    private static Stream<Arguments> testSimple() {
        Duration years = ChronoUnit.YEARS.getDuration();
        Duration months = ChronoUnit.MONTHS.getDuration();

        return Stream.of(
                Arguments.of("1 year", years.multipliedBy(1)),
                Arguments.of("2 years", years.multipliedBy(2)),
                Arguments.of("3 years", years.multipliedBy(3)),
                Arguments.of("15 years", years.multipliedBy(15)),
                Arguments.of("1 month", months.multipliedBy(1)),
                Arguments.of("2 months", months.multipliedBy(2)),
                Arguments.of("1 week", Duration.ofDays(7)),
                Arguments.of("2 weeks", Duration.ofDays(7 * 2)),
                Arguments.of("1 day", Duration.ofDays(1)),
                Arguments.of("2 days", Duration.ofDays(2)),
                Arguments.of("1 hour", Duration.ofHours(1)),
                Arguments.of("2 hours", Duration.ofHours(2)),
                Arguments.of("15 hours", Duration.ofHours(15)),
                Arguments.of("1 minute", Duration.ofMinutes(1)),
                Arguments.of("2 minutes", Duration.ofMinutes(2)),
                Arguments.of("15 minutes", Duration.ofMinutes(15)),
                Arguments.of("1 second", Duration.ofSeconds(1)),
                Arguments.of("2 seconds", Duration.ofSeconds(2)),
                Arguments.of("15 seconds", Duration.ofSeconds(15)),
                Arguments.of("0 seconds", Duration.ZERO)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testSimple(String expected, Duration input) {
        assertEquals(expected, render(DurationFormatter.LONG.format(input)));
    }

    @Test
    public void testFormats() {
        Duration duration = ChronoUnit.YEARS.getDuration().multipliedBy(5)
                .plus(ChronoUnit.MONTHS.getDuration().multipliedBy(4))
                .plus(ChronoUnit.WEEKS.getDuration().multipliedBy(3))
                .plusDays(2)
                .plusHours(1)
                .plusMinutes(6)
                .plusSeconds(7);

        assertEquals("5y 4mo 3w 2d 1h 6m 7s", render(DurationFormatter.CONCISE.format(duration)));
        assertEquals("5y 4mo 3w", render(DurationFormatter.CONCISE_LOW_ACCURACY.format(duration)));
        assertEquals("5 years 4 months 3 weeks 2 days 1 hour 6 minutes 7 seconds", render(DurationFormatter.LONG.format(duration)));
    }

}
