package me.lucko.luckperms.common.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import me.lucko.luckperms.api.context.ImmutableContextSet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

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
            ImmutableSetMultimap.Builder<String, String> map = ImmutableSetMultimap.builder();

            for (Map.Entry<String, JsonElement> e : contexts.entrySet()) {
                JsonElement val = e.getValue();
                if (val.isJsonArray()) {
                    JsonArray vals = val.getAsJsonArray();
                    for (JsonElement element : vals) {
                        map.put(e.getKey(), element.getAsString());
                    }
                } else {
                    map.put(e.getKey(), val.getAsString());
                }
            }

            contextSet = ImmutableContextSet.fromMultimap(map.build());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
