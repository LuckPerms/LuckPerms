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

package me.lucko.luckperms.common.storage.implementation.file;

import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.util.Iterators;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class FileWatcher {
    private static final WatchEvent.Kind[] KINDS = new WatchEvent.Kind[]{
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY
    };

    private final Path basePath;
    private final Map<Path, WatchedLocation> watchedLocations;

    // the watchservice instance
    private final WatchService watchService;

    private boolean initialised = false;

    public FileWatcher(LuckPermsPlugin plugin, Path basePath) throws IOException {
        this.watchedLocations = Collections.synchronizedMap(new HashMap<>());
        this.basePath = basePath;
        this.watchService = basePath.getFileSystem().newWatchService();

        plugin.getBootstrap().getScheduler().asyncLater(this::initLocations, 5, TimeUnit.SECONDS);
        plugin.getBootstrap().getScheduler().asyncRepeating(this::tick, 1, TimeUnit.SECONDS);
    }

    public WatchedLocation getWatcher(Path path) {
        Path relativePath = this.basePath.relativize(path);
        return this.watchedLocations.computeIfAbsent(relativePath, p -> new WatchedLocation(this, p));
    }

    public void close() {
        if (this.watchService == null) {
            return;
        }

        try {
            this.watchService.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initLocations() {
        for (WatchedLocation loc : this.watchedLocations.values()) {
            try {
                loc.setup();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.initialised = true;
    }

    private void tick() {
        List<Path> expired = new ArrayList<>();
        for (Map.Entry<Path, WatchedLocation> ent : this.watchedLocations.entrySet()) {
            boolean valid = ent.getValue().tick();
            if (!valid) {
                new RuntimeException("WatchKey no longer valid: " + ent.getKey().toString()).printStackTrace();
                expired.add(ent.getKey());
            }
        }
        expired.forEach(this.watchedLocations::remove);
    }

    private static boolean isFileTemporary(String fileName) {
        return fileName.endsWith(".tmp") || fileName.endsWith(".swp") || fileName.endsWith(".swx") || fileName.endsWith(".swpz");
    }

    /**
     * Encapsulates a "watcher" in a specific directory.
     */
    public final class WatchedLocation {
        // the parent watcher
        private final FileWatcher watcher;

        // the absolute path to the directory being watched
        private final Path absolutePath;

        // the times of recent changes
        private final Map<String, Long> lastChange = Collections.synchronizedMap(new HashMap<>());

        // if the key is registered
        private boolean ready = false;

        // the watch key
        private WatchKey key = null;

        // the callback functions
        private final List<Consumer<Path>> callbacks = new CopyOnWriteArrayList<>();

        private WatchedLocation(FileWatcher watcher, Path relativePath) {
            this.watcher = watcher;
            this.absolutePath = this.watcher.basePath.resolve(relativePath);
        }

        private synchronized void setup() throws IOException {
            if (this.ready) {
                return;
            }

            this.key = this.absolutePath.register(this.watcher.watchService, KINDS);
            this.ready = true;
        }

        private boolean tick() {
            if (!this.ready) {
                // await init
                if (!FileWatcher.this.initialised) {
                    return true;
                }

                try {
                    setup();
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            // remove old change entries.
            long expireTime = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(4);
            this.lastChange.values().removeIf(lastChange -> lastChange < expireTime);

            List<WatchEvent<?>> watchEvents = this.key.pollEvents();
            for (WatchEvent<?> event : watchEvents) {
                Path context = (Path) event.context();

                if (context == null) {
                    continue;
                }

                String fileName = context.toString();

                // ignore temporary changes
                if (isFileTemporary(fileName)) {
                    continue;
                }

                // ignore changes already registered to the system
                if (this.lastChange.containsKey(fileName)) {
                    continue;
                }
                this.lastChange.put(fileName, System.currentTimeMillis());

                // process the change
                Iterators.tryIterate(this.callbacks, cb -> cb.accept(context));
            }

            // reset the watch key.
            return this.key.reset();
        }

        public void recordChange(String fileName) {
            this.lastChange.put(fileName, System.currentTimeMillis());
        }

        public void addListener(Consumer<Path> updateConsumer) {
            this.callbacks.add(updateConsumer);
        }
    }

}
