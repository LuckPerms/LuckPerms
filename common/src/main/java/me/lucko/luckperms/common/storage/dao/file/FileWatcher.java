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

package me.lucko.luckperms.common.storage.dao.file;

import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

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
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class FileWatcher implements Runnable {
    private static final WatchEvent.Kind[] KINDS = new WatchEvent.Kind[]{
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY
    };

    private final LuckPermsPlugin plugin;

    private final Map<String, WatchedLocation> keyMap;
    private final Map<String, Long> internalChanges;
    private WatchService watchService = null;

    public FileWatcher(LuckPermsPlugin plugin) {
        this.plugin = plugin;
        this.keyMap = Collections.synchronizedMap(new HashMap<>());
        this.internalChanges = Collections.synchronizedMap(new HashMap<>());
        try {
            this.watchService = plugin.getDataDirectory().toPath().getFileSystem().newWatchService();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void subscribe(String id, Path path, Consumer<String> consumer) {
        if (this.watchService == null) {
            return;
        }

        // Register with a delay to ignore changes made at startup
        this.plugin.getScheduler().asyncLater(() -> {
            this.keyMap.computeIfAbsent(id, s -> {
                WatchKey key;
                try {
                    key = path.register(this.watchService, KINDS);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return new WatchedLocation(path, key, consumer);
            });
        }, 40L);
    }

    public void registerChange(StorageLocation location, String fileName) {
        this.internalChanges.put(location.name().toLowerCase() + "/" + fileName, System.currentTimeMillis());
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

    @Override
    public void run() {
        long expireTime = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(4);
        // was either processed last time, or recently modified by the system.
        this.internalChanges.values().removeIf(lastChange -> lastChange < expireTime);

        List<String> expired = new ArrayList<>();

        for (Map.Entry<String, WatchedLocation> ent : this.keyMap.entrySet()) {
            String id = ent.getKey();
            Path path = ent.getValue().getPath();
            WatchKey key = ent.getValue().getKey();

            List<WatchEvent<?>> watchEvents = key.pollEvents();

            for (WatchEvent<?> event : watchEvents) {
                Path context = (Path) event.context();

                if (context == null) {
                    continue;
                }

                Path file = path.resolve(context);
                String fileName = context.toString();

                // ignore temporary changes
                if (fileName.endsWith(".tmp")) {
                    continue;
                }

                if (this.internalChanges.containsKey(id + "/" + fileName)) {
                    // This file was modified by the system.
                    continue;
                }

                this.internalChanges.put(id + "/" + fileName, System.currentTimeMillis());

                this.plugin.getLog().info("[FileWatcher] Detected change in file: " + file.toString());

                // Process the change
                ent.getValue().getFileConsumer().accept(fileName);
            }

            boolean valid = key.reset();
            if (!valid) {
                new RuntimeException("WatchKey no longer valid: " + key.toString()).printStackTrace();
                expired.add(id);
            }
        }

        expired.forEach(this.keyMap::remove);
    }

    private static class WatchedLocation {
        private final Path path;
        private final WatchKey key;
        private final Consumer<String> fileConsumer;

        public WatchedLocation(Path path, WatchKey key, Consumer<String> fileConsumer) {
            this.path = path;
            this.key = key;
            this.fileConsumer = fileConsumer;
        }

        public Path getPath() {
            return this.path;
        }

        public WatchKey getKey() {
            return this.key;
        }

        public Consumer<String> getFileConsumer() {
            return this.fileConsumer;
        }
    }

}
