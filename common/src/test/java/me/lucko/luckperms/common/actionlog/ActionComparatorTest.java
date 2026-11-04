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

import net.luckperms.api.actionlog.Action;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ActionComparatorTest {

    private static final UUID SOURCE_UUID = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");
    private static final Instant TIMESTAMP = Instant.ofEpochSecond(1_000_000_000L);

    private static LoggedAction actionForGroup(String group) {
        return LoggedAction.build()
                .timestamp(TIMESTAMP)
                .source(SOURCE_UUID)
                .sourceName("admin")
                .targetType(Action.Target.Type.GROUP)
                .targetName(group)
                .description("permission set example.permission true")
                .build();
    }

    @Test
    public void testDifferentTargetNamesDoNotCompareEqual() {
        LoggedAction alpha = actionForGroup("alpha");
        LoggedAction beta = actionForGroup("beta");

        assertNotEquals(alpha, beta);
        assertTrue(alpha.compareTo(beta) < 0);
        assertTrue(beta.compareTo(alpha) > 0);

        SortedSet<Action> actions = new TreeSet<>();
        actions.add(alpha);
        actions.add(beta);
        assertEquals(2, actions.size());
    }

    @Test
    public void testIdenticalActionsCompareEqual() {
        assertEquals(0, actionForGroup("alpha").compareTo(actionForGroup("alpha")));
    }

}
