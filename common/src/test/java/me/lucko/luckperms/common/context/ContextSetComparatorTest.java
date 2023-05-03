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

package me.lucko.luckperms.common.context;

import com.google.common.collect.Lists;
import me.lucko.luckperms.common.context.comparator.ContextSetComparator;
import net.luckperms.api.context.ImmutableContextSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ContextSetComparatorTest {

    private static final ImmutableContextSet EMPTY = ImmutableContextSetImpl.EMPTY;
    private static final ImmutableContextSet JUST_SERVER = ImmutableContextSetImpl.of("server", "foo");
    private static final ImmutableContextSet JUST_WORLD = ImmutableContextSetImpl.of("world", "foo");
    private static final ImmutableContextSet SERVER_AND_WORLD = new ImmutableContextSetImpl.BuilderImpl()
            .add("server", "foo")
            .add("world", "foo")
            .build();
    private static final ImmutableContextSet MISC = new ImmutableContextSetImpl.BuilderImpl()
            .add("foo", "foo")
            .add("foo", "bar")
            .build();
    private static final ImmutableContextSet SERVER_AND_MISC = new ImmutableContextSetImpl.BuilderImpl()
            .add("server", "foo")
            .add("foo", "foo")
            .add("foo", "bar")
            .build();
    private static final ImmutableContextSet WORLD_AND_MISC = new ImmutableContextSetImpl.BuilderImpl()
            .add("world", "foo")
            .add("foo", "foo")
            .add("foo", "bar")
            .build();
    private static final ImmutableContextSet SERVER_AND_WORLD_AND_MISC_1 = new ImmutableContextSetImpl.BuilderImpl()
            .add("server", "foo")
            .add("world", "foo")
            .add("foo", "foo")
            .build();
    private static final ImmutableContextSet SERVER_AND_WORLD_AND_MISC_2 = new ImmutableContextSetImpl.BuilderImpl()
            .add("server", "foo")
            .add("world", "foo")
            .add("foo", "foo")
            .add("foo", "bar")
            .build();

    private static Stream<ImmutableContextSet> all() {
        return Stream.of(EMPTY, JUST_SERVER, JUST_WORLD, SERVER_AND_WORLD, MISC, SERVER_AND_MISC, WORLD_AND_MISC, SERVER_AND_WORLD_AND_MISC_1, SERVER_AND_WORLD_AND_MISC_2);
    }

    private static final Comparator<ImmutableContextSet> INSTANCE = ContextSetComparator.normal();

    @ParameterizedTest
    @MethodSource("all")
    @SuppressWarnings("EqualsWithItself")
    public void testEquals(ImmutableContextSet set) {
        assertEquals(0, INSTANCE.compare(set, set));
    }

    @Test
    public void testEmpty() {
        assertTrue(INSTANCE.compare(JUST_SERVER, EMPTY) > 0);
        assertTrue(INSTANCE.compare(EMPTY, JUST_SERVER) < 0);
    }

    @Test
    public void testServerPresence() {
        assertTrue(INSTANCE.compare(JUST_SERVER, MISC) > 0);
        assertTrue(INSTANCE.compare(JUST_SERVER, WORLD_AND_MISC) > 0);
    }

    @Test
    public void testWorldPresence() {
        assertTrue(INSTANCE.compare(JUST_WORLD, MISC) > 0);
    }

    @Test
    public void testOverallSize() {
        assertTrue(INSTANCE.compare(SERVER_AND_MISC, JUST_SERVER) > 0);
        assertTrue(INSTANCE.compare(WORLD_AND_MISC, JUST_WORLD) > 0);
    }

    @ParameterizedTest
    @MethodSource("all")
    public void testOverallSizeAll(ImmutableContextSet other) {
        if (other == SERVER_AND_WORLD_AND_MISC_2) return;
        assertTrue(INSTANCE.compare(SERVER_AND_WORLD_AND_MISC_2, other) > 0);
    }

    private static Stream<Arguments> testTransitivity() {
        return Stream.of(
                Arguments.of(
                        ImmutableContextSetImpl.of("a", "-"),
                        ImmutableContextSetImpl.of("b", "-"),
                        ImmutableContextSetImpl.of("c", "-")
                ),
                Arguments.of(
                        ImmutableContextSetImpl.of("-", "a"),
                        ImmutableContextSetImpl.of("-", "b"),
                        ImmutableContextSetImpl.of("-", "c")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testTransitivity(ImmutableContextSet a, ImmutableContextSet b, ImmutableContextSet c) {
        List<ImmutableContextSet> list = new ArrayList<>();
        list.add(a);
        list.add(b);
        list.add(c);
        list.sort(INSTANCE);

        List<ImmutableContextSet> reversed = new ArrayList<>(list);
        reversed.sort(INSTANCE.reversed());

        assertEquals(Lists.reverse(list), reversed);
    }

}
