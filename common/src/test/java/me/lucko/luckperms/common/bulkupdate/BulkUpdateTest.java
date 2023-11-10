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

package me.lucko.luckperms.common.bulkupdate;

import com.google.common.collect.ImmutableSet;
import me.lucko.luckperms.common.bulkupdate.action.BulkUpdateAction;
import me.lucko.luckperms.common.bulkupdate.action.DeleteAction;
import me.lucko.luckperms.common.bulkupdate.action.UpdateAction;
import me.lucko.luckperms.common.filter.Comparison;
import me.lucko.luckperms.common.filter.Constraint;
import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.common.node.types.Permission;
import net.luckperms.api.node.Node;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BulkUpdateTest {

    @Test
    public void testUpdate() {
        BulkUpdate update = BulkUpdateBuilder.create()
                .action(UpdateAction.of(BulkUpdateField.SERVER, "foo"))
                .filter(BulkUpdateField.WORLD, Comparison.EQUAL.comparing("bar"))
                .filter(BulkUpdateField.PERMISSION, Comparison.SIMILAR.comparing("hello%"))
                .trackStatistics(true)
                .build();

        Instant time = Instant.now().plus(1, ChronoUnit.HOURS);

        Set<Node> nodes = ImmutableSet.of(
                Permission.builder().permission("test").build(),
                Permission.builder().permission("hello").build(),
                Permission.builder().permission("hello").withContext("world", "bar").build(),
                Permission.builder().permission("hello.world").value(false).expiry(time).withContext("world", "bar").build(),
                Permission.builder().permission("hello").withContext("world", "bar").withContext("server", "bar").build()
        );
        Set<Node> expected = ImmutableSet.of(
                Permission.builder().permission("test").build(),
                Permission.builder().permission("hello").build(),
                Permission.builder().permission("hello").withContext("world", "bar").withContext("server", "foo").build(),
                Permission.builder().permission("hello.world").value(false).expiry(time).withContext("world", "bar").withContext("server", "foo").build(),
                Permission.builder().permission("hello").withContext("world", "bar").withContext("server", "foo").build()
        );

        assertEquals(expected, update.apply(nodes, HolderType.USER));

        BulkUpdateStatistics statistics = update.getStatistics();
        assertEquals(3, statistics.getAffectedNodes());
        assertEquals(1, statistics.getAffectedUsers());
        assertEquals(0, statistics.getAffectedGroups());
    }

    @Test
    public void testDelete() {
        BulkUpdate update = BulkUpdateBuilder.create()
                .action(DeleteAction.create())
                .filter(BulkUpdateField.WORLD, Comparison.EQUAL.comparing("bar"))
                .filter(BulkUpdateField.PERMISSION, Comparison.SIMILAR.comparing("hello%"))
                .trackStatistics(true)
                .build();

        Instant time = Instant.now().plus(1, ChronoUnit.HOURS);

        Set<Node> nodes = ImmutableSet.of(
                Permission.builder().permission("test").build(),
                Permission.builder().permission("hello").build(),
                Permission.builder().permission("hello").withContext("world", "bar").build(),
                Permission.builder().permission("hello.world").value(false).expiry(time).withContext("world", "bar").build(),
                Permission.builder().permission("hello").withContext("world", "bar").withContext("server", "bar").build()
        );
        Set<Node> expected = ImmutableSet.of(
                Permission.builder().permission("test").build(),
                Permission.builder().permission("hello").build()
        );

        assertEquals(expected, update.apply(nodes, HolderType.USER));

        BulkUpdateStatistics statistics = update.getStatistics();
        assertEquals(3, statistics.getAffectedNodes());
        assertEquals(1, statistics.getAffectedUsers());
        assertEquals(0, statistics.getAffectedGroups());
    }

    private static Stream<Arguments> testSimpleActionSql() {
        return Stream.of(
                Arguments.of("DELETE FROM {table}", DeleteAction.create()),
                Arguments.of("UPDATE {table} SET permission=foo", UpdateAction.of(BulkUpdateField.PERMISSION, "foo")),
                Arguments.of("UPDATE {table} SET server=foo", UpdateAction.of(BulkUpdateField.SERVER, "foo")),
                Arguments.of("UPDATE {table} SET world=foo", UpdateAction.of(BulkUpdateField.WORLD, "foo"))
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource
    public void testSimpleActionSql(String expectedSql, BulkUpdateAction action) {
        BulkUpdate update = BulkUpdateBuilder.create()
                .action(action)
                .build();

        BulkUpdateSqlBuilder sqlBuilder = new BulkUpdateSqlBuilder();
        sqlBuilder.visit(update);
        String sql = sqlBuilder.builder().toReadableString();

        assertEquals(expectedSql, sql);
    }

    private static Stream<Arguments> testQueryFilterSql() {
        return Stream.of(
                Arguments.of(
                        "DELETE FROM {table} WHERE permission = foo",
                        DeleteAction.create(),
                        BulkUpdateField.PERMISSION,
                        Comparison.EQUAL.comparing("foo")
                ),
                Arguments.of(
                        "DELETE FROM {table} WHERE permission != foo",
                        DeleteAction.create(),
                        BulkUpdateField.PERMISSION,
                        Comparison.NOT_EQUAL.comparing("foo")
                ),
                Arguments.of(
                        "DELETE FROM {table} WHERE permission LIKE foo",
                        DeleteAction.create(),
                        BulkUpdateField.PERMISSION,
                        Comparison.SIMILAR.comparing("foo")
                ),
                Arguments.of(
                        "DELETE FROM {table} WHERE permission NOT LIKE foo",
                        DeleteAction.create(),
                        BulkUpdateField.PERMISSION,
                        Comparison.NOT_SIMILAR.comparing("foo")
                ),
                Arguments.of(
                        "UPDATE {table} SET server=foo WHERE world = bar",
                        UpdateAction.of(BulkUpdateField.SERVER, "foo"),
                        BulkUpdateField.WORLD,
                        Comparison.EQUAL.comparing("bar")
                )
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource
    public void testQueryFilterSql(String expectedSql, BulkUpdateAction action, BulkUpdateField field, Constraint constraint) {
        BulkUpdate update = BulkUpdateBuilder.create()
                .action(action)
                .filter(field, constraint)
                .build();

        BulkUpdateSqlBuilder sqlBuilder = new BulkUpdateSqlBuilder();
        sqlBuilder.visit(update);
        String sql = sqlBuilder.builder().toReadableString();

        assertEquals(expectedSql, sql);
    }

    @Test
    public void testQueryFilterMultipleSql() {
        BulkUpdate update = BulkUpdateBuilder.create()
                .action(UpdateAction.of(BulkUpdateField.SERVER, "foo"))
                .filter(BulkUpdateField.WORLD, Comparison.EQUAL.comparing("bar"))
                .filter(BulkUpdateField.PERMISSION, Comparison.SIMILAR.comparing("baz"))
                .filter(BulkUpdateField.SERVER, Comparison.NOT_EQUAL.comparing("aaa"))
                .filter(BulkUpdateField.WORLD, Comparison.NOT_SIMILAR.comparing("bbb"))
                .build();

        BulkUpdateSqlBuilder sqlBuilder = new BulkUpdateSqlBuilder();
        sqlBuilder.visit(update);
        assertEquals(
                "UPDATE {table} SET server=? WHERE world = ? AND permission LIKE ? AND server != ? AND world NOT LIKE ?",
                sqlBuilder.builder().toQueryString()
        );
        assertEquals(
                "UPDATE {table} SET server=foo WHERE world = bar AND permission LIKE baz AND server != aaa AND world NOT LIKE bbb",
                sqlBuilder.builder().toReadableString()
        );
    }

}
