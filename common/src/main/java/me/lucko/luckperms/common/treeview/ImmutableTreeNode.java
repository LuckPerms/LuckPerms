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

package me.lucko.luckperms.common.treeview;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An immutable and sorted version of TreeNode
 *
 * Entries in the children map are sorted first by whether they have
 * any children, and then alphabetically
 */
public class ImmutableTreeNode implements Comparable<ImmutableTreeNode> {
    private Map<String, ImmutableTreeNode> children = null;

    public ImmutableTreeNode(Stream<Map.Entry<String, ImmutableTreeNode>> children) {
        if (children != null) {
            LinkedHashMap<String, ImmutableTreeNode> sortedMap = children
                    .sorted((o1, o2) -> {
                        // sort first by if the node has any children
                        int childStatus = o1.getValue().compareTo(o2.getValue());
                        if (childStatus != 0) {
                            return childStatus;
                        }

                        // then alphabetically
                        return String.CASE_INSENSITIVE_ORDER.compare(o1.getKey(), o2.getKey());
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

            this.children = ImmutableMap.copyOf(sortedMap);
        }
    }

    public Optional<Map<String, ImmutableTreeNode>> getChildren() {
        return Optional.ofNullable(this.children);
    }

    /**
     * Gets the node endings of each branch of the tree at this stage
     *
     * The key represents the depth of the node.
     *
     * @return the node endings
     */
    public List<Map.Entry<Integer, String>> getNodeEndings() {
        if (this.children == null) {
            return Collections.emptyList();
        }

        List<Map.Entry<Integer, String>> results = new ArrayList<>();
        for (Map.Entry<String, ImmutableTreeNode> node : this.children.entrySet()) {
            String value = node.getKey();

            // add self
            results.add(Maps.immutableEntry(0, value));

            // add child nodes, incrementing their level & appending their prefix node
            results.addAll(node.getValue().getNodeEndings().stream()
                    .map(e -> Maps.immutableEntry(
                            e.getKey() + 1, // increment level
                            // add this node's key infront of the child value
                            value + "." + e.getValue())
                    )
                    .collect(Collectors.toList()));
        }
        return results;
    }

    public JsonObject toJson(String prefix) {
        if (this.children == null) {
            return new JsonObject();
        }

        JsonObject object = new JsonObject();
        for (Map.Entry<String, ImmutableTreeNode> entry : this.children.entrySet()) {
            String name = prefix + entry.getKey();
            object.add(name, entry.getValue().toJson(name + "."));
        }
        return object;
    }

    @Override
    public int compareTo(@NonNull ImmutableTreeNode o) {
        return (this.children != null) == o.getChildren().isPresent() ? 0 : this.children != null ? 1 : -1;
    }
}
