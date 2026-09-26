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

package me.lucko.luckperms.common.filter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PageParametersTest {

    @ParameterizedTest(name = "[{index}] {0} {1}")
    @CsvSource({
            "5, 150, 30",
            "151, 150, 1",
            "150, 150, 1",
            "149, 150, 2",
            "1, 1, 1",
            "1, 0, 0",
            "10, 0, 0",
    })
    public void testMaxPage(int pageSize, int total, int expectedMaxPage) {
        int maxPage = new PageParameters(pageSize, 1).getMaxPage(total);
        assertEquals(expectedMaxPage, maxPage);
    }

    @Test
    public void testMaxPageOverflow() {
        int maxPage = new PageParameters(10, 1).getMaxPage(Integer.MAX_VALUE);
        assertTrue(maxPage > 0);
        assertEquals(214748365, maxPage);
    }

    @Test
    public void testPaginateOverflow() {
        PageParameters params = new PageParameters(10, Integer.MAX_VALUE);
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals(Collections.emptyList(), params.paginate(list));
    }

    @Test
    public void testPaginateStreamOverflow() {
        PageParameters params = new PageParameters(10, Integer.MAX_VALUE);
        Stream<String> stream = Stream.of("a", "b", "c");
        assertEquals(0, params.paginate(stream).count());
    }

}
