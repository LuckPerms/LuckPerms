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

import me.lucko.luckperms.sponge.LPSpongePlugin;
import me.lucko.luckperms.sponge.service.storage.SubjectStorage;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@SuppressWarnings("deprecation")
public class LegacyDataMigrator implements Runnable {
    private final LPSpongePlugin plugin;

    private final File oldDirectory;
    private final SubjectStorage storage;

    public LegacyDataMigrator(LPSpongePlugin plugin, File oldDirectory, SubjectStorage storage) {
        this.plugin = plugin;
        this.oldDirectory = oldDirectory;
        this.storage = storage;
    }

    @Override
    public void run() {
        if (!this.oldDirectory.exists() || !this.oldDirectory.isDirectory()) {
            return;
        }

        this.plugin.getLog().warn("Migrating old sponge data... Please wait.");

        File[] collections = this.oldDirectory.listFiles(File::isDirectory);
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
                    SubjectDataHolder holder = this.storage.getGson().fromJson(reader, SubjectDataHolder.class);
                    this.storage.saveToFile(holder.asSubjectModel(this.plugin.getService()), this.storage.resolveFile(collectionDir.getName(), subjectName));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                subjectFile.delete();
            }

            collectionDir.delete();
        }

        this.oldDirectory.delete();
    }
}
