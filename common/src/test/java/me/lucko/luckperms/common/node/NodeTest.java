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

import me.lucko.luckperms.common.context.ImmutableContextSetImpl;
import me.lucko.luckperms.common.node.types.Meta;
import me.lucko.luckperms.common.node.types.Permission;
import me.lucko.luckperms.common.node.types.Prefix;
import me.lucko.luckperms.common.node.types.Suffix;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.metadata.NodeMetadataKey;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NodeTest {

    @Test
    public void testBasic() {
        Node node = Permission.builder()
                .permission("hello.world")
                .build();

        assertEquals("hello.world", node.getKey());
        assertTrue(node.getValue());
        assertNull(node.getExpiry());
        assertNull(node.getExpiryDuration());
        assertFalse(node.hasExpired());
        assertFalse(node.hasExpiry());
    }

    @Test
    public void testEscaping() {
        Node node = Meta.builder("hel.lo", "wo.rld").build();
        assertEquals("meta.hel\\.lo.wo\\.rld", node.getKey());

        node = Prefix.builder("hel.lo", 100).build();
        assertEquals("prefix.100.hel\\.lo", node.getKey());

        node = Suffix.builder("hel.lo", 100).build();
        assertEquals("suffix.100.hel\\.lo", node.getKey());
    }

    @Test
    public void testExpiry() {
        Instant instant = Instant.now()
                .plusSeconds(60)
                .truncatedTo(ChronoUnit.SECONDS);

        Node node = Permission.builder()
                .permission("hello.world")
                .expiry(instant)
                .build();

        assertEquals("hello.world", node.getKey());
        assertTrue(node.getValue());
        assertEquals(instant, node.getExpiry());
        assertNotNull(node.getExpiryDuration());
        assertFalse(node.hasExpired());
        assertTrue(node.hasExpiry());
    }

    @Test
    public void testContext() {
        Node node = Permission.builder()
                .permission("hello.world")
                .withContext("hello", "123")
                .withContext("world", "456")
                .build();

        ImmutableContextSet contexts = node.getContexts();
        ImmutableContextSet expected = new ImmutableContextSetImpl.BuilderImpl()
                .add("hello", "123")
                .add("world", "456")
                .build();

        assertEquals(expected, contexts);
    }

    @Test
    public void testMetadata() {
        NodeMetadataKey<UUID> key = NodeMetadataKey.of("test2", UUID.class);
        UUID randomUniqueId = UUID.randomUUID();

        Node node = Permission.builder()
                .permission("hello.world")
                .withMetadata(key, randomUniqueId)
                .build();

        assertEquals(Optional.of(randomUniqueId), node.getMetadata(key));
        assertEquals(randomUniqueId, node.metadata(key));
    }

    @Test
    public void testMetadataFails() {
        NodeMetadataKey<String> key1 = NodeMetadataKey.of("test1", String.class);
        NodeMetadataKey<UUID> key2 = NodeMetadataKey.of("test2", UUID.class);
        NodeMetadataKey<UUID> key3 = NodeMetadataKey.of("test2", UUID.class);
        NodeMetadataKey<String> key4 = NodeMetadataKey.of("test2", String.class);
        UUID randomUniqueId = UUID.randomUUID();

        Node node = Permission.builder()
                .permission("hello.world")
                .withMetadata(key2, randomUniqueId)
                .build();

        assertEquals(Optional.of(randomUniqueId), node.getMetadata(key2));
        assertEquals(Optional.of(randomUniqueId), node.getMetadata(key3));
        assertEquals(randomUniqueId, node.metadata(key2));
        assertEquals(randomUniqueId, node.metadata(key3));

        assertEquals(Optional.empty(), node.getMetadata(key1));
        assertEquals(Optional.empty(), node.getMetadata(key4));
        assertThrows(IllegalStateException.class, () -> node.metadata(key1));
        assertThrows(IllegalStateException.class, () -> node.metadata(key4));
    }

}
