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

package me.lucko.luckperms.common.command.utils;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ArgumentTokenizerTest {

    private static Stream<Arguments> testBasicTokenize() {
        return Stream.of(
                Arguments.of("", new String[]{}),
                Arguments.of("hello world", new String[]{"hello", "world"}),
                Arguments.of("hello  world", new String[]{"hello", "", "world"}),
                Arguments.of("hello   world", new String[]{"hello", "", "", "world"}),
                Arguments.of("\"hello world\"", new String[]{"hello world"}),
                Arguments.of("\"hello  world\"", new String[]{"hello  world"}),
                Arguments.of("\" hello world\"", new String[]{" hello world"}),
                Arguments.of("\"hello world \"", new String[]{"hello world "}),
                Arguments.of("\"hello\"\"world\"", new String[]{"hello", "world"}),
                Arguments.of("\"hello\" \"world\"", new String[]{"hello", "world"})
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testBasicTokenize(String input, String[] expectedTokens) {
        for (ArgumentTokenizer tokenizer : ArgumentTokenizer.values()) {
            List<String> tokens = tokenizer.tokenizeInput(input);
            assertEquals(ImmutableList.copyOf(expectedTokens), ImmutableList.copyOf(tokens), "tokenizer " + tokenizer + " produced tokens " + tokens);
        }
    }

    private static Stream<Arguments> testExecuteTokenize() {
        return Stream.of(
                Arguments.of("hello world ", new String[]{"hello", "world"}),
                Arguments.of("hello world  ", new String[]{"hello", "world", ""})
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testExecuteTokenize(String input, String[] expectedTokens) {
        List<String> tokens = ArgumentTokenizer.EXECUTE.tokenizeInput(input);
        assertEquals(ImmutableList.copyOf(expectedTokens), ImmutableList.copyOf(tokens));
    }

    private static Stream<Arguments> testTabCompleteTokenize() {
        return Stream.of(
                Arguments.of("hello world ", new String[]{"hello", "world", ""}),
                Arguments.of("hello world  ", new String[]{"hello", "world", "", ""})
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testTabCompleteTokenize(String input, String[] expectedTokens) {
        List<String> tokens = ArgumentTokenizer.TAB_COMPLETE.tokenizeInput(input);
        assertEquals(ImmutableList.copyOf(expectedTokens), ImmutableList.copyOf(tokens));
    }

}
