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

package me.lucko.luckperms.common.model;

import me.lucko.luckperms.common.model.nodemap.NodeMapMutable;
import me.lucko.luckperms.common.model.nodemap.RecordedNodeMap;
import me.lucko.luckperms.common.node.factory.NodeBuilders;
import me.lucko.luckperms.common.util.Difference;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RecordedNodeMapTest {

    private static final PermissionHolderIdentifier ORIGIN = new PermissionHolderIdentifier(HolderType.GROUP, "test");

    @Mock private PermissionHolder mockHolder;

    private NodeMapMutable delegate;
    private RecordedNodeMap map;

    @BeforeEach
    public void setup() {
        when(this.mockHolder.getIdentifier()).thenReturn(ORIGIN);
        this.delegate = new NodeMapMutable(this.mockHolder, DataType.NORMAL);
        this.map = new RecordedNodeMap(this.delegate);
    }

    private static Node makeNode(String key) {
        return NodeBuilders.determineMostApplicable(key).build();
    }

    @Test
    public void testMutationsAreRecorded() {
        Node a = makeNode("a");
        Node b = makeNode("b");

        this.map.add(a);
        this.map.add(b);
        this.map.remove(a);

        Difference<Node> exported = this.map.exportChanges(diff -> true);
        assertEquals(Set.of(b), exported.getAdded());
        assertEquals(Set.of(), exported.getRemoved());
    }

    @Test
    public void testExportChangesClearsTheLog() {
        this.map.add(makeNode("a"));

        Difference<Node> first = this.map.exportChanges(diff -> true);
        assertFalse(first.isEmpty());

        // the log should have been reset, so a second export is empty
        Difference<Node> second = this.map.exportChanges(diff -> true);
        assertTrue(second.isEmpty());
    }

    @Test
    public void testExportChangesRespectsPredicate() {
        this.map.add(makeNode("a"));

        // predicate rejects the export,  nothing should be returned and the log should be untouched
        Difference<Node> rejected = this.map.exportChanges(diff -> false);
        assertNull(rejected);

        Difference<Node> accepted = this.map.exportChanges(diff -> true);
        assertFalse(accepted.isEmpty());
    }

    @Test
    public void testDiscardChanges() {
        this.map.add(makeNode("a"));
        this.map.discardChanges();

        Difference<Node> exported = this.map.exportChanges(diff -> true);
        assertTrue(exported.isEmpty());

        // the underlying delegate should be unaffected, only the change log is discarded
        assertEquals(1, this.map.size());
    }

    @Test
    public void testAddDefaultNodeToChangeSet() {
        Difference<Node> result = this.map.addDefaultNodeToChangeSet();
        assertEquals(1, result.getAdded().size());
        Node added = result.getAdded().iterator().next();
        assertInstanceOf(InheritanceNode.class, added);
        assertEquals("default", ((InheritanceNode) added).getGroupName());

        // it should also have been recorded in the change log
        Difference<Node> exported = this.map.exportChanges(diff -> true);
        assertEquals(result.getAdded(), exported.getAdded());
    }

    @Test
    public void testBypassMutatesWithoutRecording() {
        assertSame(this.delegate, this.map.bypass());

        this.map.bypass().add(makeNode("a"));
        assertEquals(1, this.map.size());

        // the mutation happened directly on the delegate, so it shouldn't show up in the change log
        Difference<Node> exported = this.map.exportChanges(diff -> true);
        assertTrue(exported == null || exported.isEmpty());
    }

}
