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

package me.lucko.luckperms.sponge.service.model.persisted;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import me.lucko.luckperms.common.util.ImmutableCollectors;
import me.lucko.luckperms.common.util.MoreFiles;
import me.lucko.luckperms.common.util.gson.GsonProvider;
import me.lucko.luckperms.sponge.service.model.LPPermissionService;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Handles persisted Subject I/O and (de)serialization
 */
public class SubjectStorage {

    /**
     * The permission service
     */
    private final LPPermissionService service;

    /**
     * The root directory used to store files
     */
    private final Path container;

    public SubjectStorage(LPPermissionService service, Path container) {
        this.service = service;
        this.container = container;
    }

    /**
     * Returns a set of all known collections
     *
     * @return the identifiers of all known collections
     */
    public Set<String> getSavedCollections() {
        if (!Files.exists(this.container)) {
            return ImmutableSet.of();
        }

        try (Stream<Path> s = Files.list(this.container)) {
            return s.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .collect(ImmutableCollectors.toSet());
        } catch (IOException e) {
            e.printStackTrace();
            return ImmutableSet.of();
        }
    }

    /**
     * Returns the file where a subjects data should be stored
     *
     * @param collectionIdentifier the identifier of the subjects collection
     * @param subjectIdentifier the identifier of the subject
     * @return a file
     */
    private Path resolveFile(String collectionIdentifier, String subjectIdentifier) {
        Path collection = this.container.resolve(collectionIdentifier);
        try {
            MoreFiles.createDirectoriesIfNotExists(collection);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return collection.resolve(subjectIdentifier + ".json");
    }

    /**
     * Saves a subject
     *
     * @param subject the subject to save
     * @throws IOException if the write fails
     */
    public void saveToFile(PersistedSubject subject) throws IOException {
        Path subjectFile = resolveFile(subject.getParentCollection().getIdentifier(), subject.getIdentifier().getName());
        saveToFile(SubjectDataContainer.copyOf(subject.getSubjectData()), subjectFile);
    }

    /**
     * Saves subject data to a specific file
     *
     * @param container the data
     * @param file the file
     * @throws IOException if the write fails
     */
    public void saveToFile(SubjectDataContainer container, Path file) throws IOException {
        MoreFiles.createDirectoriesIfNotExists(file.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GsonProvider.prettyPrinting().toJson(container.serialize(), writer);
            writer.flush();
        }
    }

    /**
     * Loads all known subjects for a given collection
     *
     * @param collectionIdentifier the collection identifier
     * @return a map of found subjects
     */
    public Map<String, SubjectDataContainer> loadAllFromFile(String collectionIdentifier) {
        Path collection = this.container.resolve(collectionIdentifier);
        if (!Files.exists(collection)) {
            return Collections.emptyMap();
        }

        Map<String, SubjectDataContainer> holders = new HashMap<>();
        try (Stream<Path> s = Files.list(collection)){
            s.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .forEach(subjectFile -> {
                        try {
                            LoadedSubject sub = loadFromFile(subjectFile);
                            if (sub != null) {
                                holders.put(sub.identifier, sub.data);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return holders;
    }

    /**
     * Loads a subject
     *
     * @param collectionIdentifier the collection id
     * @param subjectIdentifier the subject id
     * @return a loaded subject
     * @throws IOException if the read fails
     */
    public LoadedSubject loadFromFile(String collectionIdentifier, String subjectIdentifier) throws IOException {
        Path collection = this.container.resolve(collectionIdentifier);
        if (!Files.exists(collection)) {
            return null;
        }

        Path subject = collection.resolve(subjectIdentifier + ".json");
        return new LoadedSubject(subjectIdentifier, loadFromFile(subject).data);
    }

    /**
     * Loads a subject from a particular file
     *
     * @param file the file to load from
     * @return a loaded subject
     * @throws IOException if the read fails
     */
    public LoadedSubject loadFromFile(Path file) throws IOException {
        if (!Files.exists(file)) {
            return null;
        }

        String fileName = file.getFileName().toString();
        String subjectName = fileName.substring(0, fileName.length() - ".json".length());

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject data = GsonProvider.prettyPrinting().fromJson(reader, JsonObject.class);
            SubjectDataContainer model = SubjectDataContainer.deserialize(this.service, data);
            return new LoadedSubject(subjectName, model);
        } catch (Exception e) {
            throw new IOException("Exception occurred whilst loading from " + file.toString(), e);
        }
    }

    private static final class LoadedSubject {
        private final String identifier;
        private final SubjectDataContainer data;

        private LoadedSubject(String identifier, SubjectDataContainer data) {
            this.identifier = identifier;
            this.data = data;
        }
    }
}
