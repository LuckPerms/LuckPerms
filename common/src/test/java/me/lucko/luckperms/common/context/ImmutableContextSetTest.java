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

import com.google.common.collect.ImmutableSet;
import net.luckperms.api.context.Context;
import net.luckperms.api.context.ContextSatisfyMode;
import net.luckperms.api.context.ImmutableContextSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImmutableContextSetTest {

    private static Stream<Consumer<ImmutableContextSet.Builder>> testBuilder() {
        return Stream.of(
                builder -> {
                    builder.add("test", "a");
                    builder.add("test", "b");
                    builder.add("test", "c");
                },
                builder -> {
                    builder.add("test", "c");
                    builder.add("test", "b");
                    builder.add("test", "a");
                },
                builder -> {
                    builder.add("test", "b");
                    builder.add("test", "a");
                    builder.add("test", "c");
                },
                builder -> {
                    builder.add("test", "b");
                    builder.add("test", "c");
                    builder.add("test", "a");
                },
                builder -> {
                    builder.add("test", "a");
                    builder.add("test", "a");
                    builder.add("test", "b");
                    builder.add("test", "c");
                }
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testBuilder(Consumer<ImmutableContextSet.Builder> action) {
        ImmutableContextSet.Builder builder = new ImmutableContextSetImpl.BuilderImpl();
        action.accept(builder);
        ImmutableContextSet set = builder.build();

        ImmutableSet<Context> expected = ImmutableSet.of(
                new ContextImpl("test", "a"),
                new ContextImpl("test", "b"),
                new ContextImpl("test", "c")
        );

        assertEquals(expected, set.toSet());
        assertEquals(3, set.size());

        assertTrue(set.contains("test", "a"));
        assertTrue(set.contains("test", "b"));
        assertTrue(set.contains("test", "c"));
    }

    @Test
    public void testContains() {
        ImmutableContextSet set = new ImmutableContextSetImpl.BuilderImpl()
                .add("test", "a")
                .add("test", "a")
                .add("test", "b")
                .add("test", "c")
                .build();

        assertTrue(set.contains("test", "a"));
        assertFalse(set.contains("test", "z"));
        assertFalse(set.contains("aaa", "a"));

        assertTrue(set.containsKey("test"));
        assertFalse(set.containsKey("aaa"));
    }

    private static Stream<Consumer<ImmutableContextSet.Builder>> testContainsAllTrue() {
        return Stream.of(
                builder -> builder.add("aaa", "a").add("bbb", "a"),
                builder -> builder.add("aaa", "b").add("bbb", "a"),
                builder -> builder.add("aaa", "c").add("bbb", "a"),
                builder -> builder.add("aaa", "c").add("bbb", "b")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testContainsAllTrue(Consumer<ImmutableContextSet.Builder> setup) {
        ImmutableContextSetImpl set = (ImmutableContextSetImpl) new ImmutableContextSetImpl.BuilderImpl()
                .add("aaa", "a")
                .add("aaa", "b")
                .add("aaa", "c")
                .add("bbb", "a")
                .add("bbb", "b")
                .build();

        ImmutableContextSet.Builder builder = new ImmutableContextSetImpl.BuilderImpl();
        setup.accept(builder);

        assertTrue(set.otherContainsAll(
                builder.build(),
                ContextSatisfyMode.AT_LEAST_ONE_VALUE_PER_KEY)
        );
    }

    private static Stream<Consumer<ImmutableContextSet.Builder>> testContainsAllFalse() {
        return Stream.of(
                builder -> builder.add("aaa", "a").add("bbb", "z"),
                builder -> builder.add("aaa", "b").add("bbb", "z"),
                builder -> builder.add("aaa", "b"),
                builder -> builder.add("aaa", "c"),
                builder -> {}
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testContainsAllFalse(Consumer<ImmutableContextSet.Builder> setup) {
        ImmutableContextSetImpl set = (ImmutableContextSetImpl) new ImmutableContextSetImpl.BuilderImpl()
                .add("aaa", "a")
                .add("aaa", "b")
                .add("aaa", "c")
                .add("bbb", "a")
                .add("bbb", "b")
                .build();

        ImmutableContextSet.Builder builder = new ImmutableContextSetImpl.BuilderImpl();
        setup.accept(builder);

        assertFalse(set.otherContainsAll(
                builder.build(),
                ContextSatisfyMode.AT_LEAST_ONE_VALUE_PER_KEY)
        );
    }

}
