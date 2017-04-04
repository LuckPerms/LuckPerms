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
