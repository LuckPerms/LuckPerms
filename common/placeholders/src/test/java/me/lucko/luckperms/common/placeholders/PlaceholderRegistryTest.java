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

package me.lucko.luckperms.common.placeholders;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class PlaceholderRegistryTest {

    @Test
    public void testAllPlaceholdersAreRegistered() {
        List<Placeholder> returnedByAllMethod = PlaceholderRegistry.getAll();
        Map<String, Placeholder> inClass = Arrays.stream(Placeholders.class.getDeclaredFields())
                .filter(f -> Placeholder.class.isAssignableFrom(f.getType()))
                .collect(Collectors.toMap(Field::getName, f -> {
                    try {
                        return (Placeholder) f.get(null);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }, (a, b) -> { throw new UnsupportedOperationException(); }, LinkedHashMap::new));

        assertEquals(new ArrayList<>(inClass.values()), returnedByAllMethod);
        inClass.forEach((fieldName, placeholder) ->
                assertEquals(fieldName.toLowerCase(Locale.ROOT), placeholder.id(), "Placeholder " + fieldName + " has an id that doesn't match its field name")
        );
    }

    @Test
    public void testPlaceholdersDontOverlap() {
        for (Placeholder placeholder : PlaceholderRegistry.getAll()) {
            for (Placeholder other : PlaceholderRegistry.getAll()) {
                if (placeholder == other) {
                    continue;
                }

                assertNotEquals(placeholder.id(), other.id(), "Placeholder " + placeholder + " has the same id as " + other);
                assertFalse(other instanceof Placeholder.UsingArgument && placeholder.id().startsWith(other.id()), "Placeholder " + placeholder.id() + " has an id that overlaps with " + other.id());
            }
        }
    }

    @Test
    public void testRegistryLookup() {
        Placeholder value = PlaceholderRegistry.lookup("prefix_element");
        assertSame(Placeholders.PREFIX_ELEMENT, value);

        assertNull(PlaceholderRegistry.lookup("non_existent"));
    }

}
