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

package me.lucko.luckperms.common.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.contexts.ContextSetJsonSerializer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * A wrapper for the 'contexts.json' file.
 */
@RequiredArgsConstructor
public class ContextsFile {
    private final LuckPermsConfiguration configuration;

    @Getter
    @Setter
    private ImmutableContextSet staticContexts = ImmutableContextSet.empty();

    @Getter
    @Setter
    private ImmutableContextSet defaultContexts = ImmutableContextSet.empty();

    public void load() {
        File file = new File(configuration.getPlugin().getConfigDirectory(), "contexts.json");
        File oldFile = new File(configuration.getPlugin().getConfigDirectory(), "static-contexts.json");
        if (oldFile.exists()) {
            oldFile.renameTo(file);
        }

        if (!file.exists()) {
            save();
            return;
        }

        boolean save = false;
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            JsonObject data = new Gson().fromJson(reader, JsonObject.class);

            if (data.has("context")) {
                staticContexts = ContextSetJsonSerializer.deserializeContextSet(data.get("context").getAsJsonObject()).makeImmutable();
                save = true;
            }

            if (data.has("static-contexts")) {
                staticContexts = ContextSetJsonSerializer.deserializeContextSet(data.get("static-contexts").getAsJsonObject()).makeImmutable();
            }

            if (data.has("default-contexts")) {
                defaultContexts = ContextSetJsonSerializer.deserializeContextSet(data.get("default-contexts").getAsJsonObject()).makeImmutable();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (save) {
            save();
        }
    }

    public void save() {
        File file = new File(configuration.getPlugin().getConfigDirectory(), "contexts.json");

        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {

            JsonObject data = new JsonObject();
            data.add("static-contexts", ContextSetJsonSerializer.serializeContextSet(staticContexts));
            data.add("default-contexts", ContextSetJsonSerializer.serializeContextSet(defaultContexts));

            new GsonBuilder().setPrettyPrinting().create().toJson(data, writer);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
