package me.lucko.luckperms.common.util;

import com.google.common.collect.ImmutableList;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IteratorsTest {

    @Test
    void testDivideEmpty() {
        assertEquals(ImmutableList.of(), Iterators.divideIterable(ImmutableList.of(), 2));
    }

    @Test
    void testDivideSimple() {
        List<List<String>> expected = ImmutableList.of(
                ImmutableList.of("one", "two"),
                ImmutableList.of("three", "four"),
                ImmutableList.of("five")

        );
        List<List<String>> actual = Iterators.divideIterable(
                ImmutableList.of("one", "two", "three", "four", "five"),
                2
        );

        assertEquals(expected, actual);
    }

    @Test
    void testDivideBoundary() {
        List<List<String>> expected = ImmutableList.of(
                ImmutableList.of("one", "two"),
                ImmutableList.of("three", "four")

        );
        List<List<String>> actual = Iterators.divideIterable(
                ImmutableList.of("one", "two", "three", "four"),
                2
        );

        assertEquals(expected, actual);
    }

}
