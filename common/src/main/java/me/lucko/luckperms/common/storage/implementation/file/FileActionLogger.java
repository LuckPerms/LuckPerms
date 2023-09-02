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
import me.lucko.luckperms.common.actionlog.ActionJsonSerializer;
import me.lucko.luckperms.common.actionlog.Log;
import me.lucko.luckperms.common.cache.BufferedRequest;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.util.gson.GsonProvider;
import net.luckperms.api.actionlog.Action;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
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

    public void init(Path contentFile, Path legacyFile) {
        this.contentFile = contentFile;

        if (Files.exists(legacyFile)) {
            // migrate
            JsonArray array;

            try (JsonReader reader = new JsonReader(Files.newBufferedReader(legacyFile, StandardCharsets.UTF_8))) {
                array = GsonProvider.parser().parse(reader).getAsJsonArray();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            for (JsonElement element : array) {
                this.entryQueue.add(ActionJsonSerializer.deserialize(element));
            }

            flush();

            try {
                Files.delete(legacyFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

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
                List<String> toWrite = new ArrayList<>(this.entryQueue.size());

                // poll the queue for new entries
                for (Action e; (e = this.entryQueue.poll()) != null; ) {
                    toWrite.add(GsonProvider.normal().toJson(ActionJsonSerializer.serialize(e)));
                }

                Files.write(this.contentFile, toWrite, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            this.writeLock.unlock();
        }
    }

    public Log getLog() throws IOException {
        // if there is log content waiting to be written, flush immediately before trying to read
        if (this.saveBuffer.isEnqueued()) {
            this.saveBuffer.requestDirectly();
        }

        if (!Files.exists(this.contentFile)) {
            return Log.empty();
        }

        Log.Builder log = Log.builder();

        try (BufferedReader reader = Files.newBufferedReader(this.contentFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    JsonElement parsed = GsonProvider.parser().parse(line);
                    log.add(ActionJsonSerializer.deserialize(parsed));
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
