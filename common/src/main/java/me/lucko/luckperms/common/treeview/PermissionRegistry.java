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

import com.google.common.base.Splitter;
import me.lucko.luckperms.common.util.ImmutableCollectors;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Stores a collection of all permissions known to the platform.
 */
public class PermissionRegistry {
    private static final Splitter DOT_SPLIT = Splitter.on('.').omitEmptyStrings();

    /** The root node in the tree */
    private final TreeNode rootNode = new TreeNode();

    public TreeNode getRootNode() {
        return this.rootNode;
    }

    public List<String> rootAsList() {
        return this.rootNode.makeImmutableCopy().getNodeEndings().stream()
                .map(Map.Entry::getValue)
                .collect(ImmutableCollectors.toList());
    }

    /**
     * Offer a permission to the registry (to be potentially inserted asynchronously).
     *
     * @param permission the permission
     */
    public void offer(String permission) {
        insert(permission);
    }

    /**
     * Insert a permission into the registry.
     *
     * @param permission the permission
     */
    public void insert(String permission) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }

        try {
            doInsert(permission);
        } catch (Exception ex) {
            // ignore
        }
    }

    protected void doInsert(String permission) {
        permission = permission.toLowerCase(Locale.ROOT);

        // split the permission up into parts
        Iterable<String> parts = DOT_SPLIT.split(permission);

        // insert the permission into the node structure
        TreeNode current = this.rootNode;
        for (String part : parts) {
            current = current.tryInsert(part);
            if (current == null) {
                return;
            }
        }
    }

}
