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

import lombok.Getter;
import lombok.NonNull;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Stores a collection of all permissions known to the platform.
 */
public class PermissionVault implements Runnable {
    private static final Splitter DOT_SPLIT = Splitter.on('.').omitEmptyStrings();

    // the root node in the tree
    @Getter
    private final TreeNode rootNode;

    // the known permissions already in the vault
    private final Set<String> knownPermissions;

    // a queue of permission strings to be processed by the tree
    private final Queue<String> queue;

    // if the handler should shutdown
    private boolean shutdown = false;

    public PermissionVault(Executor executor) {
        rootNode = new TreeNode();
        knownPermissions = ConcurrentHashMap.newKeySet(3000);
        queue = new ConcurrentLinkedQueue<>();

        executor.execute(this);
    }

    @Override
    public void run() {
        while (true) {
            for (String e; (e = queue.poll()) != null; ) {
                try {
                    String s = e.toLowerCase();
                    // only attempt an insert if we're not seen this permission before
                    if (knownPermissions.add(s)) {
                        insert(s);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            if (shutdown) {
                return;
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {}
        }
    }

    public void offer(@NonNull String permission) {
        queue.offer(permission);
    }

    public Set<String> getKnownPermissions() {
        return ImmutableSet.copyOf(knownPermissions);
    }

    public List<String> rootAsList() {
        return rootNode.makeImmutableCopy().getNodeEndings().stream().map(Map.Entry::getValue).collect(Collectors.toList());
    }

    public int getSize() {
        return rootNode.getDeepSize();
    }

    private void insert(String permission) {
        // split the permission up into parts
        List<String> parts = DOT_SPLIT.splitToList(permission);

        // insert the permission into the node structure
        TreeNode current = rootNode;
        for (String part : parts) {
            current = current.getChildMap().computeIfAbsent(part, s -> new TreeNode());
        }
    }

    public void shutdown() {
        shutdown = true;
    }

}
