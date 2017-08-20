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

package me.lucko.luckperms.sponge.service.legacy;

import lombok.RequiredArgsConstructor;

import me.lucko.luckperms.sponge.LPSpongePlugin;
import me.lucko.luckperms.sponge.service.storage.SubjectStorage;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@SuppressWarnings("deprecation")
@RequiredArgsConstructor
public class LegacyDataMigrator implements Runnable {
    private final LPSpongePlugin plugin;

    private final File oldDirectory;
    private final SubjectStorage storage;

    @Override
    public void run() {
        if (!oldDirectory.exists() || !oldDirectory.isDirectory()) {
            return;
        }

        plugin.getLog().warn("Migrating old sponge data... Please wait.");

        File[] collections = oldDirectory.listFiles(File::isDirectory);
        if (collections == null) {
            return;
        }

        for (File collectionDir : collections) {

            File[] subjects = collectionDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (subjects == null) {
                continue;
            }

            for (File subjectFile : subjects) {
                String subjectName = subjectFile.getName().substring(0, subjectFile.getName().length() - ".json".length());

                try (BufferedReader reader = Files.newBufferedReader(subjectFile.toPath(), StandardCharsets.UTF_8)) {
                    SubjectDataHolder holder = storage.getGson().fromJson(reader, SubjectDataHolder.class);
                    storage.saveToFile(holder.asSubjectModel(plugin.getService()), storage.resolveFile(collectionDir.getName(), subjectName));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                subjectFile.delete();
            }

            collectionDir.delete();
        }

        oldDirectory.delete();
    }
}
