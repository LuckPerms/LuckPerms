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

import com.google.common.collect.ImmutableSet;
import me.lucko.luckperms.common.util.Difference.ChangeType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DifferenceTest {

    @Test
    public void testSimple() {
        Difference<String> diff = new Difference<>();
        assertTrue(diff.isEmpty());

        diff.recordChange(ChangeType.ADD, "test1");
        diff.recordChange(ChangeType.REMOVE, "test2");
        diff.recordChange(ChangeType.ADD, "test3");

        assertEquals(ImmutableSet.of("test1", "test3"), diff.getAdded());
        assertEquals(ImmutableSet.of("test2"), diff.getRemoved());

        assertFalse(diff.isEmpty());
        assertEquals(3, diff.getChanges().size());
    }

    @Test
    public void testOverride() {
        Difference<String> diff = new Difference<>();
        assertTrue(diff.isEmpty());

        diff.recordChange(ChangeType.ADD, "test1");
        diff.recordChange(ChangeType.REMOVE, "test1");

        assertTrue(diff.isEmpty());
    }

    @Test
    public void testMerge() {
        Difference<String> diff = new Difference<>();
        diff.recordChange(ChangeType.ADD, "test1");

        Difference<String> diff2 = new Difference<>();
        diff2.recordChange(ChangeType.REMOVE, "test1");

        assertEquals(1, diff.getChanges().size());
        assertEquals(1, diff2.getChanges().size());

        Difference<String> returnedDiff = diff.mergeFrom(diff2);
        assertSame(diff, returnedDiff);

        assertEquals(0, diff.getChanges().size());
        assertEquals(1, diff2.getChanges().size());
    }

}
