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

import me.lucko.luckperms.common.filter.sql.FilterSqlBuilder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FilterSqlTest {

    private static Stream<Arguments> testQueries() {
        return Stream.of(
                Arguments.of(
                        FilterList.empty(),
                        "",
                        ""
                ),
                Arguments.of(
                        FilterList.and(
                                TestField.FOO.isEqualTo("hello", ConstraintFactory.STRINGS)
                        ),
                        " WHERE foo = hello",
                        " WHERE foo = ?"
                ),
                Arguments.of(
                        FilterList.and(
                                TestField.FOO.isEqualTo("hello", ConstraintFactory.STRINGS),
                                TestField.BAR.isEqualTo("world", ConstraintFactory.STRINGS)
                        ),
                        " WHERE foo = hello AND bar = world",
                        " WHERE foo = ? AND bar = ?"
                ),
                Arguments.of(
                        FilterList.or(
                                TestField.FOO.isEqualTo("hello", ConstraintFactory.STRINGS)
                        ),
                        " WHERE foo = hello",
                        " WHERE foo = ?"
                ),
                Arguments.of(
                        FilterList.or(
                                TestField.FOO.isEqualTo("hello", ConstraintFactory.STRINGS),
                                TestField.BAR.isEqualTo("world", ConstraintFactory.STRINGS)
                        ),
                        " WHERE foo = hello OR bar = world",
                        " WHERE foo = ? OR bar = ?"
                ),
                Arguments.of(
                        FilterList.or(
                                TestField.FOO.isEqualTo("hello", ConstraintFactory.STRINGS),
                                TestField.BAR.isNotEqualTo("world", ConstraintFactory.STRINGS),
                                TestField.BAZ.isSimilarTo("abc%xyz", ConstraintFactory.STRINGS),
                                TestField.BAZ.isNotSimilarTo("a_c", ConstraintFactory.STRINGS)
                        ),
                        " WHERE foo = hello OR bar != world OR baz LIKE abc%xyz OR baz NOT LIKE a_c",
                        " WHERE foo = ? OR bar != ? OR baz LIKE ? OR baz NOT LIKE ?"
                )
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource
    public void testQueries(FilterList<Object> filters, String expectedSql, String expectedSqlParams) {
        FilterSqlBuilder<Object> sqlBuilder = new TestFilterSqlBuilder();
        sqlBuilder.visit(filters);

        System.out.println(sqlBuilder.builder().toReadableString());
        System.out.println(sqlBuilder.builder().toQueryString());

        assertEquals(expectedSql, sqlBuilder.builder().toReadableString());
        assertEquals(expectedSqlParams, sqlBuilder.builder().toQueryString());
    }

    private enum TestField implements FilterField<Object, String> {
        FOO, BAR, BAZ;

        @Override
        public String getValue(Object object) {
            return "null";
        }

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private static final class TestFilterSqlBuilder extends FilterSqlBuilder<Object> {

        @Override
        public void visitFieldName(FilterField<Object, ?> field) {
            this.builder.append(field.toString());
        }
    }

}
