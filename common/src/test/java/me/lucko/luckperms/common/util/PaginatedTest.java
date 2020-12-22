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

import com.google.common.collect.ImmutableList;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PaginatedTest {

    @Test
    void testSimple() {
        Paginated<String> paginated = new Paginated<>(ImmutableList.of("one", "two", "three", "four", "five"));
        assertEquals(3, paginated.getMaxPages(2));
        assertEquals(1, paginated.getMaxPages(5));
        assertEquals(1, paginated.getMaxPages(6));

        List<Paginated.Entry<String>> page1 = paginated.getPage(1, 2);
        assertEquals(2, page1.size());
        assertEquals("one", page1.get(0).value());
        assertEquals(1, page1.get(0).position());
        assertEquals("two", page1.get(1).value());
        assertEquals(2, page1.get(1).position());

        List<Paginated.Entry<String>> page2 = paginated.getPage(2, 2);
        assertEquals(2, page2.size());
        assertEquals("three", page2.get(0).value());
        assertEquals(3, page2.get(0).position());
        assertEquals("four", page2.get(1).value());
        assertEquals(4, page2.get(1).position());

        List<Paginated.Entry<String>> page3 = paginated.getPage(3, 2);
        assertEquals(1, page3.size());
        assertEquals("five", page3.get(0).value());
        assertEquals(5, page3.get(0).position());

        assertThrows(IllegalStateException.class, () -> paginated.getPage(4, 2));
        assertThrows(IllegalArgumentException.class, () -> paginated.getPage(0, 2));
        assertThrows(IllegalArgumentException.class, () -> paginated.getPage(-1, 2));
    }

}
