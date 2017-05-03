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

package me.lucko.luckperms.sponge.service.storage;

import lombok.Getter;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import me.lucko.luckperms.sponge.service.model.LPPermissionService;
import me.lucko.luckperms.sponge.service.persisted.PersistedSubject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles persisted Subject I/O and (de)serialization
 */
public class SubjectStorage {

    private final LPPermissionService service;

    @Getter
    private final Gson gson;

    private final File container;

    public SubjectStorage(LPPermissionService service, File container) {
        this.service = service;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.container = container;
        checkContainer();
    }

    private void checkContainer() {
        this.container.getParentFile().mkdirs();
    }

    public Set<String> getSavedCollections() {
        checkContainer();

        File[] dirs = container.listFiles(File::isDirectory);
        if (dirs == null) {
            return Collections.emptySet();
        }

        return ImmutableSet.copyOf(dirs).stream().map(File::getName).collect(Collectors.toSet());
    }

    public File resolveFile(String collectionName, String subjectName) {
        checkContainer();
        File collection = new File(container, collectionName);
        if (!collection.exists()) {
            collection.mkdirs();
        }

        return new File(collection, subjectName + ".json");
    }

    public void saveToFile(PersistedSubject subject) throws IOException {
        File subjectFile = resolveFile(subject.getParentCollection().getIdentifier(), subject.getIdentifier());
        saveToFile(new SubjectStorageModel(subject.getSubjectData()), subjectFile);
    }

    public void saveToFile(SubjectStorageModel model, File file) throws IOException {
        file.getParentFile().mkdirs();
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();

        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            gson.toJson(model.toJson(), writer);
            writer.flush();
        }
    }

    public Map<String, SubjectStorageModel> loadAllFromFile(String collectionName) {
        checkContainer();
        File collection = new File(container, collectionName);
        if (!collection.exists()) {
            return Collections.emptyMap();
        }

        String[] fileNames = collection.list((dir, name) -> name.endsWith(".json"));
        if (fileNames == null) return Collections.emptyMap();

        Map<String, SubjectStorageModel> holders = new HashMap<>();
        for (String name : fileNames) {
            File subject = new File(collection, name);

            try {
                Map.Entry<String, SubjectStorageModel> s = loadFromFile(subject);
                if (s != null) {
                    holders.put(s.getKey(), s.getValue());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return holders;
    }

    public Map.Entry<String, SubjectStorageModel> loadFromFile(String collectionName, String subjectName) throws IOException {
        checkContainer();
        File collection = new File(container, collectionName);
        if (!collection.exists()) {
            return null;
        }

        File subject = new File(collection, subjectName + ".json");
        return Maps.immutableEntry(subjectName, loadFromFile(subject).getValue());
    }

    public Map.Entry<String, SubjectStorageModel> loadFromFile(File file) throws IOException {
        if (!file.exists()) {
            return null;
        }

        String subjectName = file.getName().substring(0, file.getName().length() - ".json".length());

        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            JsonObject data = gson.fromJson(reader, JsonObject.class);
            SubjectStorageModel model = new SubjectStorageModel(service, data);
            return Maps.immutableEntry(subjectName, model);
        }
    }

}
