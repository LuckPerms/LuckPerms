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

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import me.lucko.luckperms.common.context.ImmutableContextSetImpl;
import me.lucko.luckperms.common.context.serializer.ContextSetJsonSerializer;
import me.lucko.luckperms.common.util.gson.GsonProvider;
import net.luckperms.api.context.ImmutableContextSet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A wrapper for the 'contexts.json' file.
 */
public class ContextsFile {
    private final LuckPermsConfiguration configuration;

    private ImmutableContextSet staticContexts = ImmutableContextSetImpl.EMPTY;
    private ImmutableContextSet defaultContexts = ImmutableContextSetImpl.EMPTY;

    public ContextsFile(LuckPermsConfiguration configuration) {
        this.configuration = configuration;
    }

    public void load() {
        Path file = this.configuration.getPlugin().getBootstrap().getDataDirectory().resolve("contexts.json");
        if (!Files.exists(file)) {
            save();
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject data = GsonProvider.normal().fromJson(reader, JsonObject.class);
            if (data == null) {
                return;
            }

            if (data.has("static-contexts")) {
                this.staticContexts = ContextSetJsonSerializer.deserialize(data.get("static-contexts").getAsJsonObject()).immutableCopy();
            }

            if (data.has("default-contexts")) {
                this.defaultContexts = ContextSetJsonSerializer.deserialize(data.get("default-contexts").getAsJsonObject()).immutableCopy();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save() {
        Path file = this.configuration.getPlugin().getBootstrap().getDataDirectory().resolve("contexts.json");

        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            JsonObject data = new JsonObject();
            data.add("static-contexts", ContextSetJsonSerializer.serialize(this.staticContexts));
            data.add("default-contexts", ContextSetJsonSerializer.serialize(this.defaultContexts));

            new GsonBuilder().setPrettyPrinting().create().toJson(data, writer);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ImmutableContextSet getStaticContexts() {
        return this.staticContexts;
    }

    public ImmutableContextSet getDefaultContexts() {
        return this.defaultContexts;
    }

}
