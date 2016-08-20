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

package me.lucko.luckperms.commands;

import lombok.Getter;
import me.lucko.luckperms.constants.Permission;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Factory class to make a thread-safe sender instance
 * @param <T> the command sender type
 */
public abstract class SenderFactory<T> implements Runnable {
    private final Map<T, List<String>> messages = new HashMap<>();
    //private final List<Map.Entry<T, String>> messages = new ArrayList<>();
    private final SenderFactory<T> factory = this;

    protected abstract String getName(T t);
    protected abstract UUID getUuid(T t);
    protected abstract void sendMessage(T t, String s);
    protected abstract boolean hasPermission(T t, String node);

    public final Sender wrap(T t) {
        return new Sender() {
            final WeakReference<T> tRef = new WeakReference<>(t);

            // Cache these permissions, so they can be accessed async
            final Map<Permission, Boolean> perms = Arrays.stream(Permission.values())
                    .map(p -> new AbstractMap.SimpleEntry<>(p, factory.hasPermission(t, p.getNode())))
                    .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

            @Getter
            final String name = factory.getName(t);

            @Getter
            final UUID uuid = factory.getUuid(t);

            @Override
            public void sendMessage(String s) {
                final T t = tRef.get();
                if (t != null) {
                    synchronized (messages) {
                        if (!messages.containsKey(t)) {
                            messages.put(t, new ArrayList<>());
                        }

                        messages.get(t).add(s);
                    }
                }
            }

            @Override
            public boolean hasPermission(Permission permission) {
                synchronized (perms) {
                    return perms.get(permission);
                }
            }
        };
    }

    @Override
    public final void run() {
        synchronized (messages) {
            if (!messages.isEmpty()) {
                messages.entrySet().forEach(e -> e.getValue().forEach(s -> factory.sendMessage(e.getKey(), s)));
                messages.clear();
            }
        }
    }
}
