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

package me.lucko.luckperms.common.actionlog;

import me.lucko.luckperms.common.actionlog.filter.ActionFilterSqlBuilder;
import me.lucko.luckperms.common.actionlog.filter.ActionFilters;
import me.lucko.luckperms.common.filter.FilterList;
import net.luckperms.api.actionlog.Action;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ActionFilterSqlTest {

    private static Stream<Arguments> testFiltersSql() {
        return Stream.of(
                Arguments.of(
                        ActionFilters.source(UUID.fromString("725d585e-4ff1-4f18-acca-6ac538364080")),
                        "WHERE actor_uuid = 725d585e-4ff1-4f18-acca-6ac538364080",
                        "WHERE actor_uuid = ?"
                ),
                Arguments.of(
                        ActionFilters.user(UUID.fromString("725d585e-4ff1-4f18-acca-6ac538364080")),
                        "WHERE type = U AND acted_uuid = 725d585e-4ff1-4f18-acca-6ac538364080",
                        "WHERE type = ? AND acted_uuid = ?"
                ),
                Arguments.of(
                        ActionFilters.group("test"),
                        "WHERE type = G AND acted_name = test",
                        "WHERE type = ? AND acted_name = ?"
                ),
                Arguments.of(
                        ActionFilters.track("test"),
                        "WHERE type = T AND acted_name = test",
                        "WHERE type = ? AND acted_name = ?"
                ),
                Arguments.of(
                        ActionFilters.search("test"),
                        "WHERE actor_name LIKE %test% OR acted_name LIKE %test% OR action LIKE %test%",
                        "WHERE actor_name LIKE ? OR acted_name LIKE ? OR action LIKE ?"
                )
        );
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource
    public void testFiltersSql(FilterList<Action> filters, String expectedSql, String expectedSqlParams) {
        ActionFilterSqlBuilder sqlBuilder = new ActionFilterSqlBuilder();
        sqlBuilder.visit(filters);

        assertEquals(" " + expectedSql, sqlBuilder.builder().toReadableString());
        assertEquals(" " + expectedSqlParams, sqlBuilder.builder().toQueryString());
    }

}
