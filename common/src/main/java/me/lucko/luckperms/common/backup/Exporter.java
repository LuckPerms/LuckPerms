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

package me.lucko.luckperms.common.backup;

import com.google.gson.JsonObject;

import me.lucko.luckperms.common.http.AbstractHttpClient;
import me.lucko.luckperms.common.http.UnsuccessfulRequestException;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.model.manager.group.GroupManager;
import me.lucko.luckperms.common.node.utils.NodeJsonSerializer;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.storage.Storage;
import me.lucko.luckperms.common.util.gson.GsonProvider;
import me.lucko.luckperms.common.util.gson.JArray;
import me.lucko.luckperms.common.util.gson.JObject;

import net.kyori.adventure.text.Component;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

/**
 * Handles export operations
 */
public abstract class Exporter implements Runnable {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    protected final LuckPermsPlugin plugin;
    private final Sender executor;
    private final boolean includeUsers;
    private final boolean includeGroups;
    protected final ProgressLogger log;

    protected Exporter(LuckPermsPlugin plugin, Sender executor, boolean includeUsers, boolean includeGroups) {
        this.plugin = plugin;
        this.executor = executor;
        this.includeUsers = includeUsers;
        this.includeGroups = includeGroups;

        this.log = new ProgressLogger();
        this.log.addListener(plugin.getConsoleSender());
        this.log.addListener(executor);
    }

    @Override
    public void run() {
        JsonObject json = new JsonObject();
        json.add("metadata", new JObject()
                .add("generatedBy", this.executor.getNameWithLocation())
                .add("generatedAt", DATE_FORMAT.format(new Date(System.currentTimeMillis())))
                .toJson());

        if (this.includeGroups) {
            this.log.log("Gathering group data...");
            json.add("groups", exportGroups());

            this.log.log("Gathering track data...");
            json.add("tracks", exportTracks());
        }

        if (this.includeUsers) {
            this.log.log("Gathering user data...");
            json.add("users", exportUsers());
        }

        processOutput(json);
    }

    protected abstract void processOutput(JsonObject json);

    private JsonObject exportGroups() {
        JsonObject out = new JsonObject();
        List<Group> groups = this.plugin.getGroupManager().getAll().values().stream()
                .sorted(Comparator.<Group>comparingInt(o -> o.getWeight().orElse(0)).reversed()
                        .thenComparing(Group::getName)
                )
                .collect(Collectors.toList());

        for (Group group : groups) {
            out.add(group.getName(), new JObject()
                    .add("nodes", NodeJsonSerializer.serializeNodes(group.normalData().asSet()))
                    .toJson());
        }
        return out;
    }

    private JsonObject exportTracks() {
        JsonObject out = new JsonObject();
        Collection<Track> tracks = this.plugin.getTrackManager().getAll().values().stream()
                .sorted(Comparator.comparing(Track::getName))
                .collect(Collectors.toList());

        for (Track track : tracks) {
            out.add(track.getName(), new JObject()
                    .add("groups", new JArray().consume(arr -> track.getGroups().forEach(arr::add)))
                    .toJson());
        }
        return out;
    }

    private JsonObject exportUsers() {
        // Users are migrated in separate threads.
        // This is because there are likely to be a lot of them, and because we can.
        // It's a big speed improvement, since the database/files are split up and can handle concurrent reads.

        this.log.log("Finding a list of unique users to export.");

        // Find all of the unique users we need to export
        Storage ds = this.plugin.getStorage();
        Set<UUID> users = ds.getUniqueUsers().join();
        this.log.log("Found " + users.size() + " unique users to export.");

        // create a threadpool to process the users concurrently
        ExecutorService executor = Executors.newFixedThreadPool(32);

        // A set of futures, which are really just the processes we need to wait for.
        Set<CompletableFuture<Void>> futures = new HashSet<>();

        AtomicInteger userCount = new AtomicInteger(0);
        Map<UUID, JsonObject> out = Collections.synchronizedMap(new TreeMap<>());

        // iterate through each user.
        for (UUID uuid : users) {
            // register a task for the user, and schedule it's execution with the pool
            futures.add(CompletableFuture.runAsync(() -> {
                User user = this.plugin.getStorage().loadUser(uuid, null).join();
                out.put(user.getUniqueId(), new JObject()
                        .consume(obj -> {
                            user.getUsername().ifPresent(username -> obj.add("username", username));
                            if (!user.getPrimaryGroup().getStoredValue().orElse(GroupManager.DEFAULT_GROUP_NAME).equalsIgnoreCase(GroupManager.DEFAULT_GROUP_NAME)) {
                                obj.add("primaryGroup", user.getPrimaryGroup().getStoredValue().get());
                            }
                        })
                        .add("nodes", NodeJsonSerializer.serializeNodes(user.normalData().asSet()))
                        .toJson());
                this.plugin.getUserManager().getHouseKeeper().cleanup(user.getUniqueId());
                userCount.incrementAndGet();
            }, executor));
        }

        // all of the threads have been scheduled now and are running. we just need to wait for them all to complete
        CompletableFuture<Void> overallFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        while (true) {
            try {
                overallFuture.get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException e) {
                // abnormal error - just break
                e.printStackTrace();
                break;
            } catch (TimeoutException e) {
                // still executing - send a progress report and continue waiting
                this.log.logProgress("Exported " + userCount.get() + " users so far.");
                continue;
            }

            // process is complete
            break;
        }

        executor.shutdown();

        JsonObject outJson = new JsonObject();
        for (Map.Entry<UUID, JsonObject> entry : out.entrySet()) {
            outJson.add(entry.getKey().toString(), entry.getValue());
        }
        return outJson;
    }

    public static final class SaveFile extends Exporter {
        private final Path filePath;

        public SaveFile(LuckPermsPlugin plugin, Sender executor, Path filePath, boolean includeUsers, boolean includeGroups) {
            super(plugin, executor, includeUsers, includeGroups);
            this.filePath = filePath;
        }

        @Override
        protected void processOutput(JsonObject json) {
            this.log.log("Finished gathering data, writing file...");

            try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(this.filePath)), StandardCharsets.UTF_8))) {
                GsonProvider.prettyPrinting().toJson(json, out);
            } catch (IOException e) {
                e.printStackTrace();
            }

            this.log.getListeners().forEach(l -> Message.EXPORT_FILE_SUCCESS.send(l, this.filePath.toFile().getAbsolutePath()));
        }
    }

    public static final class WebUpload extends Exporter {
        private final String label;

        public WebUpload(LuckPermsPlugin plugin, Sender executor, boolean includeUsers, boolean includeGroups, String label) {
            super(plugin, executor, includeUsers, includeGroups);
            this.label = label;
        }

        @Override
        protected void processOutput(JsonObject json) {
            this.log.log("Finished gathering data, uploading data...");

            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            try (Writer writer = new OutputStreamWriter(new GZIPOutputStream(bytesOut), StandardCharsets.UTF_8)) {
                GsonProvider.prettyPrinting().toJson(json, writer);
            } catch (IOException e) {
                this.plugin.getLogger().severe("Error compressing data", e);
            }

            try {
                String pasteId = this.plugin.getBytebin().postContent(bytesOut.toByteArray(), AbstractHttpClient.JSON_TYPE).key();
                this.log.getListeners().forEach(l -> Message.EXPORT_WEB_SUCCESS.send(l, pasteId, this.label));
            } catch (UnsuccessfulRequestException e) {
                this.log.getListeners().forEach(l -> Message.HTTP_REQUEST_FAILURE.send(l, e.getResponse().code(), e.getResponse().message()));
            } catch (IOException e) {
                this.plugin.getLogger().severe("Error uploading data to bytebin", e);
                this.log.getListeners().forEach(Message.HTTP_UNKNOWN_FAILURE::send);
            }
        }
    }

    private static final class ProgressLogger {
        private final Set<Sender> listeners = new HashSet<>();

        public void addListener(Sender sender) {
            this.listeners.add(sender);
        }

        public Set<Sender> getListeners() {
            return this.listeners;
        }

        public void log(String msg) {
            dispatchMessage(Message.EXPORT_LOG, msg);
        }

        public void logProgress(String msg) {
            dispatchMessage(Message.EXPORT_LOG_PROGRESS, msg);
        }

        private void dispatchMessage(Message.Args1<String> messageType, String content) {
            final Component message = messageType.build(content);
            for (Sender s : this.listeners) {
                s.sendMessage(message);
            }
        }
    }
}
