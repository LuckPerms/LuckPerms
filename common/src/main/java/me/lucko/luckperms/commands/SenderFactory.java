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

import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import me.lucko.luckperms.constants.Constants;
import me.lucko.luckperms.constants.Permission;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Factory class to make a thread-safe sender instance
 * @param <T> the command sender type
 */
public abstract class SenderFactory<T> implements Runnable {
    private final Map<T, List<String>> messages = new HashMap<>();
    private final AtomicBoolean shouldSend = new AtomicBoolean(false);
    private final SenderFactory<T> factory = this;

    protected abstract String getName(T t);
    protected abstract UUID getUuid(T t);
    protected abstract void sendMessage(T t, String s);
    protected abstract boolean hasPermission(T t, String node);

    public final Sender wrap(T t) {
        return new SenderImp(t, null);
    }

    public final Sender wrap(T t, Set<Permission> toCheck) {
        return new SenderImp(t, toCheck);
    }

    @Override
    public final void run() {
        if (!shouldSend.getAndSet(false)) {
            return;
        }

        synchronized (messages) {
            for (Map.Entry<T, List<String>> e : messages.entrySet()) {
                for (String s : e.getValue()) {
                    factory.sendMessage(e.getKey(), s);
                }
            }

            messages.clear();
        }
    }

    private class SenderImp implements Sender {
        private final WeakReference<T> tRef;

        // Cache these permissions, so they can be accessed async
        private Map<Permission, Boolean> perms;

        @Getter
        private final String name;

        @Getter
        private final UUID uuid;

        private final boolean console;

        private SenderImp(T t, Set<Permission> toCheck) {
            this.tRef = new WeakReference<>(t);
            this.name = factory.getName(t);
            this.uuid = factory.getUuid(t);
            this.console = this.uuid.equals(Constants.getConsoleUUID()) || this.uuid.equals(Constants.getImporterUUID());

            if (!this.console) {
                if (toCheck == null || toCheck.isEmpty()) {
                    this.perms = ImmutableMap.copyOf(Arrays.stream(Permission.values())
                            .collect(Collectors.toMap(p -> p, p -> factory.hasPermission(t, p.getNode()))));
                } else {
                    this.perms = ImmutableMap.copyOf(toCheck.stream().collect(Collectors.toMap(p -> p, p -> factory.hasPermission(t, p.getNode()))));
                }
            }
        }

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
                shouldSend.set(true);
            }
        }

        @Override
        public boolean hasPermission(Permission permission) {
            return console || perms.get(permission);
        }
    }
}
