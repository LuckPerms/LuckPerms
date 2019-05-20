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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import me.lucko.luckperms.api.actionlog.Action;
import me.lucko.luckperms.common.actionlog.Log;
import me.lucko.luckperms.common.actionlog.LogEntryJsonSerializer;
import me.lucko.luckperms.common.cache.BufferedRequest;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.util.gson.GsonProvider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class FileActionLogger {

    /**
     * The path to save logger content to
     */
    private Path contentFile;

    /**
     * Lock to ensure the file isn't written to by multiple threads
     */
    private final ReentrantLock writeLock = new ReentrantLock();

    /**
     * The queue of entries pending save to the file
     */
    private final Queue<Action> entryQueue = new ConcurrentLinkedQueue<>();

    private final SaveBuffer saveBuffer;

    public FileActionLogger(LuckPermsPlugin plugin) {
        this.saveBuffer = new SaveBuffer(plugin);
    }

    public void init(Path contentFile) {
        this.contentFile = contentFile;
    }

    public void logAction(Action entry) {
        this.entryQueue.add(entry);
        this.saveBuffer.request();
    }

    public void flush() {
        this.writeLock.lock();
        try {
            // don't perform the i/o process if there's nothing to be written
            if (this.entryQueue.peek() == null) {
                return;
            }

            try {
                // read existing array data into memory
                JsonArray array;

                if (Files.exists(this.contentFile)) {
                    try (JsonReader reader = new JsonReader(Files.newBufferedReader(this.contentFile, StandardCharsets.UTF_8))) {
                        array = GsonProvider.parser().parse(reader).getAsJsonArray();
                    } catch (IOException e) {
                        e.printStackTrace();
                        array = new JsonArray();
                    }
                } else {
                    array = new JsonArray();
                }

                // poll the queue for new entries
                for (Action e; (e = this.entryQueue.poll()) != null; ) {
                    array.add(LogEntryJsonSerializer.serialize(e));
                }

                // write the full content back to the file
                try (JsonWriter writer = new JsonWriter(Files.newBufferedWriter(this.contentFile, StandardCharsets.UTF_8))) {
                    writer.setIndent("  ");
                    GsonProvider.normal().toJson(array, writer);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            this.writeLock.unlock();
        }
    }

    public Log getLog() throws IOException {
        Log.Builder log = Log.builder();
        try (JsonReader reader = new JsonReader(Files.newBufferedReader(this.contentFile, StandardCharsets.UTF_8))) {
            JsonArray array = GsonProvider.parser().parse(reader).getAsJsonArray();
            for (JsonElement element : array) {
                log.add(LogEntryJsonSerializer.deserialize(element));
            }
        }
        return log.build();
    }

    private final class SaveBuffer extends BufferedRequest<Void> {
        public SaveBuffer(LuckPermsPlugin plugin) {
            super(2, TimeUnit.SECONDS, plugin.getBootstrap().getScheduler());
        }

        @Override
        protected Void perform() {
            FileActionLogger.this.flush();
            return null;
        }
    }

}
