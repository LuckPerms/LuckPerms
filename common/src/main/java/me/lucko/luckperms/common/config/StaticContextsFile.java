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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.core.NodeModel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@RequiredArgsConstructor
public class StaticContextsFile {
    private final LuckPermsConfiguration configuration;

    @Getter
    private ImmutableContextSet contextSet = ImmutableContextSet.empty();

    public void reload() {
        File file = new File(configuration.getPlugin().getConfigDirectory(), "static-contexts.json");
        if (!file.exists()) {
            try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                JsonObject template = new JsonObject();
                template.add("context", new JsonObject());
                new GsonBuilder().setPrettyPrinting().create().toJson(template, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            contextSet = ImmutableContextSet.empty();
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            JsonObject data = new Gson().fromJson(reader, JsonObject.class);

            if (!data.has("context") || !data.get("context").isJsonObject()) {
                return;
            }

            JsonObject contexts = data.get("context").getAsJsonObject();
            contextSet = NodeModel.deserializeContextSet(contexts).makeImmutable();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
