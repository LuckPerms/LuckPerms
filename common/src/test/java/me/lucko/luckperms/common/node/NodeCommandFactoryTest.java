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

import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.common.node.factory.NodeCommandFactory;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.node.types.Meta;
import me.lucko.luckperms.common.node.types.Permission;
import me.lucko.luckperms.common.node.types.Prefix;
import net.luckperms.api.node.Node;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NodeCommandFactoryTest {

    private static Stream<Arguments> testUndoCommand() {
        return Stream.of(
                Arguments.of("group test permission unset test", Permission.builder().permission("test").build(), HolderType.GROUP, false),
                Arguments.of("user test permission unset test", Permission.builder().permission("test").build(), HolderType.USER, false),
                Arguments.of("user test permission unsettemp test", Permission.builder().permission("test").expiry(1, TimeUnit.HOURS).build(), HolderType.USER, false),
                Arguments.of("user test permission unset test server=foo world=bar", Permission.builder().permission("test").withContext("server", "foo").withContext("world", "bar").build(), HolderType.USER, false),
                Arguments.of("user test permission unset test global", Permission.builder().permission("test").build(), HolderType.USER, true),
                Arguments.of("user test parent remove test", Inheritance.builder().group("test").build(), HolderType.USER, false),
                Arguments.of("user test parent removetemp test", Inheritance.builder().group("test").expiry(1, TimeUnit.HOURS).build(), HolderType.USER, false),
                Arguments.of("user test meta removeprefix 100 test", Prefix.builder().priority(100).prefix("test").build(), HolderType.USER, false),
                Arguments.of("user test meta removetempprefix 100 test", Prefix.builder().priority(100).prefix("test").expiry(1, TimeUnit.HOURS).build(), HolderType.USER, false),
                Arguments.of("user test meta removeprefix 100 \"hello world\"", Prefix.builder().priority(100).prefix("hello world").build(), HolderType.USER, false),
                Arguments.of("user test meta unset foo", Meta.builder().key("foo").value("bar").build(), HolderType.USER, false),
                Arguments.of("user test meta unsettemp foo", Meta.builder().key("foo").value("bar").expiry(1, TimeUnit.HOURS).build(), HolderType.USER, false)
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource
    public void testUndoCommand(String expected, Node node, HolderType holderType, boolean explicitGlobalContext) {
        String result = NodeCommandFactory.undoCommand(node, "test", holderType, explicitGlobalContext);
        assertEquals(expected, result);

    }

}
