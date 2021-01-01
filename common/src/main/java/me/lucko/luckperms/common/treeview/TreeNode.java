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

import com.google.common.collect.Maps;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents one "branch" or "level" of the node tree
 */
public class TreeNode {

    /*
     * We enforce a limit of the size of the node tree to ensure memory
     * usage remains sane. some plugins check for "dynamic" permissions (cc: griefprevention)
     * which means this tree can grow to very large sizes and use tons of memory
     *
     * The rules for limiting the tree size are designed to ensure the system is
     * still useful, but that unnecessarily large amounts of data aren't stored
     */
    private static boolean allowInsert(TreeNode node) {
        // level 0    =>  no limit
        // level 1/2  =>  up to 500
        // level 3+   =>  up to 100

        if (node.level == 0) {
            return true;
        } else if (node.level <= 2) {
            return node.getChildrenSize() < 500;
        } else {
            return node.getChildrenSize() < 100;
        }
    }

    private Map<String, TreeNode> children = null;

    private final int level;

    public TreeNode() {
        this.level = 0;
    }

    @SuppressWarnings("CopyConstructorMissesField") // it's not a copy constructor
    TreeNode(TreeNode parent) {
        this.level = parent.level + 1;
    }

    // lazy init
    private synchronized Map<String, TreeNode> getChildMap() {
        if (this.children == null) {
            this.children = new ConcurrentHashMap<>();
        }
        return this.children;
    }

    public @Nullable TreeNode tryInsert(String s) {
        Map<String, TreeNode> childMap = getChildMap();
        if (!allowInsert(this)) {
            return null;
        }
        return childMap.computeIfAbsent(s, x -> new TreeNode(this));
    }

    public Optional<Map<String, TreeNode>> getChildren() {
        return Optional.ofNullable(this.children);
    }

    public int getChildrenSize() {
        if (this.children == null) {
            return 0;
        } else {
            return this.children.size();
        }
    }

    public ImmutableTreeNode makeImmutableCopy() {
        if (this.children == null) {
            return new ImmutableTreeNode(null);
        } else {
            return new ImmutableTreeNode(this.children.entrySet().stream()
                    .map(e -> Maps.immutableEntry(
                            e.getKey(),
                            e.getValue().makeImmutableCopy()
                    ))
            );
        }
    }
}
