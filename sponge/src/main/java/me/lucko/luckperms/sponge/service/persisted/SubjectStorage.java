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

package me.lucko.luckperms.sponge.service.persisted;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles persisted Subject I/O and (de)serialization
 */
public class SubjectStorage {
    private final Gson gson;
    private final File container;

    public SubjectStorage(File container) {
        this.gson = new GsonBuilder().setPrettyPrinting().enableComplexMapKeySerialization().create();
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

    public void saveToFile(PersistedSubject subject) throws IOException {
        checkContainer();
        File collection = new File(container, subject.getContainingCollection().getIdentifier());
        if (!collection.exists()) {
            collection.mkdirs();
        }

        File subjectFile = new File(collection, subject.getIdentifier() + ".json");
        saveToFile(subject, subjectFile);
    }

    public void saveToFile(PersistedSubject subject, File file) throws IOException {
        file.getParentFile().mkdirs();
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();

        Files.write(saveToString(new SubjectDataHolder(subject.getSubjectData())), file, Charset.defaultCharset());
    }

    public String saveToString(SubjectDataHolder subject) {
        return gson.toJson(subject);
    }

    public Map<String, SubjectDataHolder> loadAllFromFile(String collectionName) {
        checkContainer();
        File collection = new File(container, collectionName);
        if (!collection.exists()) {
            return Collections.emptyMap();
        }

        String[] fileNames = collection.list((dir, name) -> name.endsWith(".json"));
        if (fileNames == null) return Collections.emptyMap();

        Map<String, SubjectDataHolder> holders = new HashMap<>();
        for (String name : fileNames) {
            File subject = new File(collection, name);

            try {
                Map.Entry<String, SubjectDataHolder> s = loadFromFile(subject);
                if (s != null) {
                    holders.put(s.getKey(), s.getValue());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return holders;
    }

    public Map.Entry<String, SubjectDataHolder> loadFromFile(String collectionName, String subjectName) throws IOException {
        checkContainer();
        File collection = new File(container, collectionName);
        if (!collection.exists()) {
            return null;
        }

        File subject = new File(collection, subjectName + ".json");
        return new AbstractMap.SimpleEntry<>(subjectName, loadFromFile(subject).getValue());
    }

    public Map.Entry<String, SubjectDataHolder> loadFromFile(File file) throws IOException {
        if (!file.exists()) {
            return null;
        }

        String s = Files.toString(file, Charset.defaultCharset());
        return new AbstractMap.SimpleEntry<>(file.getName().substring(0, file.getName().length() - 5), loadFromString(s));
    }

    public SubjectDataHolder loadFromString(String s) {
        return gson.fromJson(s, SubjectDataHolder.class);
    }

}
