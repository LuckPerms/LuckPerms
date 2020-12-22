package me.lucko.luckperms.common.util;

import com.google.common.collect.ImmutableMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnumNamerTest {

    @Test
    void testSimple() {
        EnumNamer<TestEnum> namer = new EnumNamer<>(
                TestEnum.class,
                ImmutableMap.of(TestEnum.THING, "hi"),
                v -> v.name().toLowerCase().replace('_', '-')
        );

        assertEquals("test", namer.name(TestEnum.TEST));
        assertEquals("a-test", namer.name(TestEnum.A_TEST));
        assertEquals("hi", namer.name(TestEnum.THING));
    }

    enum TestEnum {
        TEST, A_TEST, THING
    }

}
