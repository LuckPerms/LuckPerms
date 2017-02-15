/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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
import lombok.Setter;

import com.google.common.base.Splitter;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

/**
 * Stores a collection of all permissions known to the platform.
 */
public class PermissionVault implements Runnable {
    private static final Splitter DOT_SPLIT = Splitter.on('.').omitEmptyStrings();

    @Getter
    private final TreeNode rootNode;
    private final Queue<String> queue;

    @Setter
    private boolean shutdown = false;

    public PermissionVault(Executor executor) {
        rootNode = new TreeNode();
        queue = new ConcurrentLinkedQueue<>();

        executor.execute(this);
    }

    @Override
    public void run() {
        while (true) {
            for (String e; (e = queue.poll()) != null; ) {
                try {
                    insert(e.toLowerCase());
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

    public int getSize() {
        return rootNode.getDeepSize();
    }

    private void insert(String permission) {
        List<String> parts = DOT_SPLIT.splitToList(permission);

        TreeNode current = rootNode;
        for (String part : parts) {
            current = current.getChildMap().computeIfAbsent(part, s -> new TreeNode());
        }
    }

}
