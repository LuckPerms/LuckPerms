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

import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;
import me.lucko.luckperms.common.util.ImmutableCollectors;
import me.lucko.luckperms.common.util.RepeatingTask;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * Stores a collection of all permissions known to the platform.
 */
public class PermissionRegistry extends RepeatingTask {
    private static final Splitter DOT_SPLIT = Splitter.on('.').omitEmptyStrings();

    // the root node in the tree
    private final TreeNode rootNode;

    // a queue of permission strings to be processed by the tree
    private final Queue<String> queue;

    public PermissionRegistry(SchedulerAdapter scheduler) {
        super(scheduler, 1, TimeUnit.SECONDS);
        this.rootNode = new TreeNode();
        this.queue = new ConcurrentLinkedQueue<>();
    }

    public TreeNode getRootNode() {
        return this.rootNode;
    }

    public List<String> rootAsList() {
        return this.rootNode.makeImmutableCopy().getNodeEndings().stream().map(Map.Entry::getValue).collect(ImmutableCollectors.toList());
    }

    public void offer(String permission) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }
        this.queue.offer(permission);
    }

    @Override
    protected void tick() {
        for (String e; (e = this.queue.poll()) != null; ) {
            insert(e);
        }
    }

    public void insert(String permission) {
        try {
            doInsert(permission);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void doInsert(String permission) {
        // split the permission up into parts
        List<String> parts = DOT_SPLIT.splitToList(permission);

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
