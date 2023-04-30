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

import me.lucko.luckperms.common.node.factory.NodeBuilders;
import me.lucko.luckperms.common.node.types.Permission;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NodeBuildersTest {

    @ParameterizedTest
    @CsvSource({
            "luckperms.user.info, Permission$Builder, PERMISSION",
            "group.default, Inheritance$Builder, INHERITANCE",
            "meta.key.value, Meta$Builder, META",
            "prefix.100.hello, Prefix$Builder, PREFIX",
            "suffix.100.hello, Suffix$Builder, SUFFIX",
            "displayname.hello, DisplayName$Builder, DISPLAY_NAME",
            "weight.10, Weight$Builder, WEIGHT",
            "r=hello, RegexPermission$Builder, REGEX_PERMISSION",
            "R=hello, RegexPermission$Builder, REGEX_PERMISSION"
    })
    public void testDetermineMostApplicableType(String key, String expectedBuilderClass, String expectedType) {
        NodeBuilder<?, ?> builder = NodeBuilders.determineMostApplicable(key);
        assertTrue(builder.getClass().getName().endsWith(expectedBuilderClass));

        Node node = builder.build();
        assertEquals(expectedType, node.getType().name());
    }

    @Test
    public void testNonSpecificNodeBuild() {
        Permission.Builder builder = Permission.builder().permission("group.default");
        assertThrows(IllegalArgumentException.class, builder::build);
    }

}
