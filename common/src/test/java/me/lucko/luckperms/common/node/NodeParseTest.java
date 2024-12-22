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

package me.lucko.luckperms.common.node;

import me.lucko.luckperms.common.node.types.DisplayName;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.node.types.Meta;
import me.lucko.luckperms.common.node.types.Prefix;
import me.lucko.luckperms.common.node.types.RegexPermission;
import me.lucko.luckperms.common.node.types.Suffix;
import me.lucko.luckperms.common.node.types.Weight;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NodeParseTest {

    @ParameterizedTest
    @CsvSource({
            "group.test, test",
            "group.TEST, test",
    })
    public void testInheritance(String key, String expectedGroupName) {
        Inheritance.Builder builder = Inheritance.parse(key);
        assertNotNull(builder);

        Inheritance node = builder.build();
        assertEquals(expectedGroupName, node.getGroupName());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "aaaa"
    })
    public void testInheritanceFail(String key) {
        Inheritance.Builder builder = Inheritance.parse(key);
        assertNull(builder);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "group.hello world"
    })
    public void testInheritanceThrows(String key) {
        assertThrows(IllegalArgumentException.class, () -> Inheritance.parse(key));
    }

    @ParameterizedTest
    @CsvSource({
            "displayname.test, test",
            "displayname.TEST, TEST",
            "displayname.hello world, hello world"
    })
    public void testDisplayName(String key, String expectedDisplayName) {
        DisplayName.Builder builder = DisplayName.parse(key);
        assertNotNull(builder);

        DisplayName node = builder.build();
        assertEquals(expectedDisplayName, node.getDisplayName());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "aaaa"
    })
    public void testDisplayNameFail(String key) {
        DisplayName.Builder builder = DisplayName.parse(key);
        assertNull(builder);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "displayname."
    })
    public void testDisplayNameThrows(String key) {
        assertThrows(IllegalArgumentException.class, () -> DisplayName.parse(key));
    }

    @ParameterizedTest
    @CsvSource({
            "weight.100, 100",
            "weight.-100, -100",
            "weight.0, 0"
    })
    public void testWeight(String key, int expectedWeight) {
        Weight.Builder builder = Weight.parse(key);
        assertNotNull(builder);

        Weight node = builder.build();
        assertEquals(expectedWeight, node.getWeight());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "aaaa",
            "weight.",
            "weight.hello"
    })
    public void testWeightFail(String key) {
        Weight.Builder builder = Weight.parse(key);
        assertNull(builder);
    }

    @ParameterizedTest
    @CsvSource({
            "prefix.100.hello, 100, hello",
            "prefix.-100.hello, -100, hello",
            "prefix.0.hello, 0, hello",
            "prefix.100.hello world, 100, hello world",
            "prefix.100.HELLO world &123, 100, HELLO world &123",
            "prefix.100., 100, ''",
            "prefix.100.hello\\.world, 100, hello.world",
            "prefix.100.hello.world, 100, hello.world",
    })
    public void testPrefix(String key, int expectedPriority, String expectedValue) {
        Prefix.Builder builder = Prefix.parse(key);
        assertNotNull(builder);

        Prefix node = builder.build();
        assertEquals(expectedPriority, node.getPriority());
        assertEquals(expectedValue, node.getMetaValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "aaaa",
            "prefix.",
            "prefix.hello",
            "prefix.100",
            "prefix.hello.hello",
            "suffix.100.hello"
    })
    public void testPrefixFail(String key) {
        Prefix.Builder builder = Prefix.parse(key);
        assertNull(builder);
    }

    @ParameterizedTest
    @CsvSource({
            "suffix.100.hello, 100, hello",
            "suffix.-100.hello, -100, hello",
            "suffix.0.hello, 0, hello",
            "suffix.100.hello world, 100, hello world",
            "suffix.100.HELLO world &123, 100, HELLO world &123",
            "suffix.100., 100, ''",
            "suffix.100.hello\\.world, 100, hello.world",
            "suffix.100.hello.world, 100, hello.world",
    })
    public void testSuffix(String key, int expectedPriority, String expectedValue) {
        Suffix.Builder builder = Suffix.parse(key);
        assertNotNull(builder);

        Suffix node = builder.build();
        assertEquals(expectedPriority, node.getPriority());
        assertEquals(expectedValue, node.getMetaValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "aaaa",
            "suffix.",
            "suffix.hello",
            "suffix.100",
            "suffix.hello.hello",
            "prefix.100.hello"
    })
    public void testSuffixFail(String key) {
        Suffix.Builder builder = Suffix.parse(key);
        assertNull(builder);
    }

    @ParameterizedTest
    @CsvSource({
            "meta.k.v, k, v",
            "meta.hello.world, hello, world",
            "meta.hello., hello, ''",
            "meta.a\\.b.hel\\.lo, a.b, hel.lo",
            "meta.a\\\\.b.hel\\.lo, a\\.b, hel.lo",
            "meta.a.b.c, a, b.c"
    })
    public void testMeta(String key, String expectedKey, String expectedValue) {
        Meta.Builder builder = Meta.parse(key);
        assertNotNull(builder);

        Meta node = builder.build();
        assertEquals(expectedKey, node.getMetaKey());
        assertEquals(expectedValue, node.getMetaValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "aaaa",
            "meta.",
            "meta.hello",
    })
    public void testMetaFail(String key) {
        Meta.Builder builder = Meta.parse(key);
        assertNull(builder);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "meta.."
    })
    public void testMetaFailThrows(String key) {
        assertThrows(IllegalArgumentException.class, () -> Meta.parse(key));
    }

    @ParameterizedTest
    @CsvSource({
            "r=hello, hello",
            "R=hello, hello",
            "r=.*&^12 3[], .*&^12 3[]"
    })
    public void testRegexPermission(String key, String expectedPattern) {
        RegexPermission.Builder builder = RegexPermission.parse(key);
        assertNotNull(builder);

        RegexPermission node = builder.build();
        assertEquals(expectedPattern, node.getPatternString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "aaaa"
    })
    public void testRegexPermissionFail(String key) {
        RegexPermission.Builder builder = RegexPermission.parse(key);
        assertNull(builder);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "r=",
            "R="
    })
    public void testRegexPermissionFailThrows(String key) {
        assertThrows(IllegalArgumentException.class, () -> RegexPermission.parse(key));
    }

}
