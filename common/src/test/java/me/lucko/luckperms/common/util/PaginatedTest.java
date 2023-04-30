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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PaginatedTest {

    private static final Paginated<String> EXAMPLE_PAGE = new Paginated<>(ImmutableList.of("one", "two", "three", "four", "five"));

    @ParameterizedTest
    @CsvSource({
            "3, 2",
            "1, 5",
            "1, 6"
    })
    public void testMaxPages(int expected, int entriesPerPage) {
        assertEquals(expected, EXAMPLE_PAGE.getMaxPages(entriesPerPage));
    }

    @ParameterizedTest
    @CsvSource({
            "1, 2",
            "2, 2",
            "3, 1"
    })
    public void testPageSize(int pageNo, int expectedSize) {
        List<Paginated.Entry<String>> page = EXAMPLE_PAGE.getPage(pageNo, 2);
        assertEquals(expectedSize, page.size());
    }

    private static Stream<Arguments> testPageContent() {
        return Stream.of(
                Arguments.of(1, ImmutableList.of(
                        new Paginated.Entry<>(1, "one"),
                        new Paginated.Entry<>(2, "two")
                )),
                Arguments.of(2, ImmutableList.of(
                        new Paginated.Entry<>(3, "three"),
                        new Paginated.Entry<>(4, "four")
                )),
                Arguments.of(3, ImmutableList.of(
                        new Paginated.Entry<>(5, "five")
                ))
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testPageContent(int pageNo, List<Paginated.Entry<String>> expectedContent) {
        assertEquals(expectedContent, EXAMPLE_PAGE.getPage(pageNo, 2));
    }

    @ParameterizedTest
    @CsvSource({
            "4, 2",
    })
    public void testFailState(int pageNo, int pageSize) {
        assertThrows(IllegalStateException.class, () -> EXAMPLE_PAGE.getPage(pageNo, pageSize));
    }

    @ParameterizedTest
    @CsvSource({
            "0, 2",
            "-1, 2"
    })
    public void testFailArgument(int pageNo, int pageSize) {
        assertThrows(IllegalArgumentException.class, () -> EXAMPLE_PAGE.getPage(pageNo, pageSize));
    }

}
