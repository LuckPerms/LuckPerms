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

package me.lucko.luckperms.common.placeholders;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

public class PlaceholderResolverTest {

    private final List<Placeholder> placeholders = Arrays.asList(
            Placeholder.basic("test_simple", ctx -> "hello world"),
            Placeholder.usingArgument("test_arg", ctx -> "hello " + ctx.argument())
    );
    private final PlaceholderContext ctx = new PlaceholderContext(mock(LuckPerms.class), mock(User.class), mock(QueryOptions.class));
    private final PlaceholderResolver resolver = new PlaceholderResolver(this.placeholders);

    @Test
    public void testNullResolve() {
        assertNull(this.resolver.resolve(this.ctx, "non_existent"));
        assertNull(this.resolver.resolve(this.ctx, ""));

        assertNull(this.resolver.resolve(this.ctx, "test_simple_hello")); // reject basic with arg provided
        assertNull(this.resolver.resolve(this.ctx, "test_arg")); // reject usingArgument without arg provided
        assertNull(this.resolver.resolve(this.ctx, "test_arg_")); // reject usingArgument without arg provided
    }

    @Test
    public void testBasicResolve() {
        assertEquals("hello world", this.resolver.resolve(this.ctx, "test_simple"));
    }

    @Test
    public void testUsingArgumentResolve() {
        assertEquals("hello there", this.resolver.resolve(this.ctx, "test_arg_there"));
    }

}
