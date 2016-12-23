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

package me.lucko.luckperms.common.utils;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import com.google.common.base.Splitter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

public class PermissionCache {

    @Getter
    private final Node rootNode;
    private final Queue<String> queue;

    @Setter
    private boolean shutdown = false;

    public PermissionCache(Executor executor) {
        rootNode = new Node();
        queue = new ConcurrentLinkedQueue<>();

        executor.execute(() -> {
            while (true) {
                for (String e; (e = queue.poll()) != null; ) {
                    insert(e.toLowerCase());
                }

                if (shutdown) {
                    return;
                }

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {}
            }
        });
    }

    public void offer(@NonNull String permission) {
        queue.offer(permission);
    }

    private void insert(String permission) {
        List<String> parts = Splitter.on('.').splitToList(permission);

        Node current = rootNode;
        for (String part : parts) {
            current = current.getChildMap().computeIfAbsent(part, s -> new Node());
        }
    }

    public static class Node {
        private Map<String, Node> children = null;

        // lazy init
        private synchronized Map<String, Node> getChildMap() {
            if (children == null) {
                children = new ConcurrentHashMap<>();
            }
            return children;
        }

        public Optional<Map<String, Node>> getChildren() {
            return Optional.ofNullable(children);
        }
    }

}
