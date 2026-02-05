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

package me.lucko.luckperms.common.calculator;

import me.lucko.luckperms.common.cacheddata.result.TristateResult;
import me.lucko.luckperms.common.calculator.processor.DirectProcessor;
import me.lucko.luckperms.common.calculator.processor.PermissionProcessor;
import me.lucko.luckperms.common.calculator.processor.RegexProcessor;
import me.lucko.luckperms.common.calculator.processor.SpongeWildcardProcessor;
import me.lucko.luckperms.common.calculator.processor.WildcardProcessor;
import me.lucko.luckperms.common.node.factory.NodeBuilders;
import net.luckperms.api.node.Node;
import net.luckperms.api.util.Tristate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class PermissionProcessorTest {

    @ParameterizedTest
    @CsvSource({
            "test, UNDEFINED",
            "test.node1, TRUE",
            "test.node2, FALSE"
    })
    public void testDirect(String node, Tristate expected) {
        PermissionProcessor processor = new DirectProcessor(createNodeMap(Map.of(
                "test.node1", true,
                "test.node2", false
        )));

        TristateResult result = processor.hasPermission(TristateResult.UNDEFINED, node);
        assertEquals(expected, result.result());
        assertNull(result.overriddenResult());

        if (expected != Tristate.UNDEFINED) {
            assertNotNull(result.node());
            assertSame(DirectProcessor.class, result.processorClass());
        } else {
            assertNull(result.node());
            assertNull(result.processorClass());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "one.two.three.four, TRUE",
            "one.two.test, TRUE",
            "one.two, FALSE",
            "one.test, FALSE",
            "one.*, FALSE",
            "one, TRUE",
            "test, TRUE",
            "*, TRUE",
    })
    public void testWildcard(String node, Tristate expected) {
        PermissionProcessor processor = new WildcardProcessor(createNodeMap(Map.of(
                "one.two.*", true,
                "one.*", false,
                "*", true
        )));

        TristateResult result = processor.hasPermission(TristateResult.UNDEFINED, node);
        assertEquals(expected, result.result());
        assertNull(result.overriddenResult());
        assertNotNull(result.node());
        assertSame(WildcardProcessor.class, result.processorClass());
    }

    @ParameterizedTest
    @CsvSource({
            "one.two.three.test, FALSE",
            "one.two.three, TRUE",
            "one.two.test, TRUE",
            "one.two, FALSE",
            "one.test, FALSE",
            "one, UNDEFINED",
    })
    public void testSpongeWildcard(String node, Tristate expected) {
        PermissionProcessor processor = new SpongeWildcardProcessor(createNodeMap(Map.of(
                "one.two.three", false,
                "one.two", true,
                "one", false
        )));

        TristateResult result = processor.hasPermission(TristateResult.UNDEFINED, node);
        assertEquals(expected, result.result());

        if (expected != Tristate.UNDEFINED) {
            assertNotNull(result.node());
            assertSame(SpongeWildcardProcessor.class, result.processorClass());
        } else {
            assertNull(result.node());
            assertNull(result.processorClass());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "hello, UNDEFINED",
            "hello1, TRUE",
            "hello123, TRUE",
            "helloo, UNDEFINED",
            "regex1, FALSE",
            "regexes2, FALSE",
            "regexp3, FALSE",
            "regexps4, FALSE",
    })
    public void testRegex(String node, Tristate expected) {
        PermissionProcessor processor = new RegexProcessor(createNodeMap(Map.of(
                "r=hello\\d+", true,
                "R=rege(x(es)?|xps?)[1-5]", false
        )));

        TristateResult result = processor.hasPermission(TristateResult.UNDEFINED, node);
        assertEquals(expected, result.result());
        assertNull(result.overriddenResult());

        if (expected != Tristate.UNDEFINED) {
            assertNotNull(result.node());
            assertSame(RegexProcessor.class, result.processorClass());
        } else {
            assertNull(result.node());
            assertNull(result.processorClass());
        }
    }

    private static Map<String, Node> createNodeMap(Map<String, Boolean> nodes) {
        return nodes.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> NodeBuilders.determineMostApplicable(e.getKey()).value(e.getValue()).build()
        ));
    }

}
