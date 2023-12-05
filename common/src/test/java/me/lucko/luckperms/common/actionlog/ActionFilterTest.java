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

import me.lucko.luckperms.common.actionlog.filter.ActionFilters;
import me.lucko.luckperms.common.filter.FilterList;
import net.luckperms.api.actionlog.Action;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ActionFilterTest {

    @Test
    public void testSource() {
        UUID uuid = UUID.randomUUID();
        FilterList<Action> filter = ActionFilters.source(uuid);

        assertFalse(filter.evaluate(LoggedAction.build()
                .source(UUID.randomUUID())
                .sourceName("Test Source")
                .targetType(Action.Target.Type.USER)
                .target(UUID.randomUUID())
                .targetName("Test Target")
                .description("test 123")
                .build())
        );
        assertTrue(filter.evaluate(LoggedAction.build()
                .source(uuid)
                .sourceName("Test Source")
                .targetType(Action.Target.Type.USER)
                .target(UUID.randomUUID())
                .targetName("Test Target")
                .description("test 123")
                .build())
        );
    }

    @Test
    public void testUser() {
        UUID uuid = UUID.randomUUID();
        FilterList<Action> filter = ActionFilters.user(uuid);

        assertFalse(filter.evaluate(LoggedAction.build()
                .source(UUID.randomUUID())
                .sourceName("Test Source")
                .targetType(Action.Target.Type.USER)
                .target(UUID.randomUUID())
                .targetName("Test Target")
                .description("test 123")
                .build())
        );
        assertFalse(filter.evaluate(LoggedAction.build()
                .source(UUID.randomUUID())
                .sourceName("Test Source")
                .targetType(Action.Target.Type.GROUP)
                .target(uuid)
                .targetName("Test Target")
                .description("test 123")
                .build())
        );
        assertTrue(filter.evaluate(LoggedAction.build()
                .source(UUID.randomUUID())
                .sourceName("Test Source")
                .targetType(Action.Target.Type.USER)
                .target(uuid)
                .targetName("Test Target")
                .description("test 123")
                .build())
        );
    }

    @Test
    public void testGroup() {
        String name = "test";
        FilterList<Action> filter = ActionFilters.group(name);

        assertFalse(filter.evaluate(LoggedAction.build()
                .source(UUID.randomUUID())
                .sourceName("Test Source")
                .targetType(Action.Target.Type.USER)
                .target(UUID.randomUUID())
                .targetName("Test Target")
                .description("test 123")
                .build())
        );
        assertFalse(filter.evaluate(LoggedAction.build()
                .source(UUID.randomUUID())
                .sourceName("Test Source")
                .targetType(Action.Target.Type.GROUP)
                .targetName("aaaaa")
                .description("test 123")
                .build())
        );
        assertFalse(filter.evaluate(LoggedAction.build()
                .source(UUID.randomUUID())
                .sourceName("Test Source")
                .targetType(Action.Target.Type.TRACK)
                .targetName(name)
                .description("test 123")
                .build())
        );
        assertTrue(filter.evaluate(LoggedAction.build()
                .source(UUID.randomUUID())
                .sourceName("Test Source")
                .targetType(Action.Target.Type.GROUP)
                .targetName(name)
                .description("test 123")
                .build())
        );
    }

    @Test
    public void testTrack() {
        String name = "test";
        FilterList<Action> filter = ActionFilters.track(name);

        assertFalse(filter.evaluate(LoggedAction.build()
                .source(UUID.randomUUID())
                .sourceName("Test Source")
                .targetType(Action.Target.Type.USER)
                .target(UUID.randomUUID())
                .targetName("Test Target")
                .description("test 123")
                .build())
        );
        assertFalse(filter.evaluate(LoggedAction.build()
                .source(UUID.randomUUID())
                .sourceName("Test Source")
                .targetType(Action.Target.Type.TRACK)
                .targetName("aaaaa")
                .description("test 123")
                .build())
        );
        assertFalse(filter.evaluate(LoggedAction.build()
                .source(UUID.randomUUID())
                .sourceName("Test Source")
                .targetType(Action.Target.Type.GROUP)
                .targetName(name)
                .description("test 123")
                .build())
        );
        assertTrue(filter.evaluate(LoggedAction.build()
                .source(UUID.randomUUID())
                .sourceName("Test Source")
                .targetType(Action.Target.Type.TRACK)
                .targetName(name)
                .description("test 123")
                .build())
        );
    }

    @Test
    public void testSearch() {
        FilterList<Action> filter = ActionFilters.search("bar");

        assertFalse(filter.evaluate(LoggedAction.build()
                .source(UUID.randomUUID())
                .sourceName("Test Source")
                .targetType(Action.Target.Type.GROUP)
                .targetName("Test Target")
                .description("test 123")
                .build())
        );

        assertTrue(filter.evaluate(LoggedAction.build()
                .source(UUID.randomUUID())
                .sourceName("foobarbaz")
                .targetType(Action.Target.Type.GROUP)
                .targetName("Test Target")
                .description("test 123")
                .build())
        );
        assertTrue(filter.evaluate(LoggedAction.build()
                .source(UUID.randomUUID())
                .sourceName("Test Source")
                .targetType(Action.Target.Type.GROUP)
                .targetName("foobarbaz")
                .description("test 123")
                .build())
        );
        assertTrue(filter.evaluate(LoggedAction.build()
                .source(UUID.randomUUID())
                .sourceName("Test Source")
                .targetType(Action.Target.Type.GROUP)
                .targetName("Test Target")
                .description("foo bar baz")
                .build())
        );
        assertTrue(filter.evaluate(LoggedAction.build()
                .source(UUID.randomUUID())
                .sourceName("bar")
                .targetType(Action.Target.Type.GROUP)
                .targetName("bar")
                .description("bar")
                .build())
        );
    }

}
