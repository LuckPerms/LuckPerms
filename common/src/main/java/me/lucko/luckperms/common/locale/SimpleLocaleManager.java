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

package me.lucko.luckperms.common.locale;

import com.google.common.collect.ImmutableMap;

import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

public class SimpleLocaleManager implements LocaleManager {

    private Map<Message, String> messages = ImmutableMap.of();
    private Map<CommandSpec, CommandSpecData> commands = ImmutableMap.of();

    public void tryLoad(LuckPermsPlugin plugin, File file) {
        if (file.exists()) {
            plugin.getLog().info("Found lang.yml - loading messages...");
            try {
                loadFromFile(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void loadFromFile(File file) throws Exception {
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            ImmutableMap.Builder<Message, String> messages = ImmutableMap.builder();
            ImmutableMap.Builder<CommandSpec, CommandSpecData> commands = ImmutableMap.builder();

            Map<String, Object> data = (Map<String, Object>) new Yaml().load(reader);
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isEmpty() || entry.getValue() == null) {
                    continue;
                }

                // might be a message
                if (entry.getValue() instanceof String) {
                    String key = entry.getKey().toUpperCase().replace('-', '_');
                    String value = (String) entry.getValue();

                    try {
                        messages.put(Message.valueOf(key), value);
                    } catch (IllegalArgumentException e) {
                        // ignore
                    }
                }

                // might be the entries for command specifications - take care for malformed entries of differing types.
                if (entry.getKey().equals("command-specs") && entry.getValue() instanceof Map) {
                    Map<?, ?> commandKeys = (Map) entry.getValue();

                    // key is the command id, value is a map of the commands attributes
                    for (Map.Entry commandKey : commandKeys.entrySet()) {

                        // just try catch, can't be bothered with safe casting every single value.
                        try {
                            String id = (String) commandKey.getKey();
                            Map<String, Object> attributes = (Map<String, Object>) commandKey.getValue();
                            CommandSpec spec = CommandSpec.valueOf(id.toUpperCase().replace('-', '_'));

                            String description = (String) attributes.get("description");
                            String usage = (String) attributes.get("usage");
                            Map<String, String> args = (Map<String, String>) attributes.get("args");
                            if (args != null && args.isEmpty()) {
                                args = null;
                            }

                            CommandSpecData specData = new CommandSpecData(description, usage, args == null ? null : ImmutableMap.copyOf(args));
                            commands.put(spec, specData);

                        } catch (IllegalArgumentException e) {
                            // ignore
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            this.messages = messages.build();
            this.commands = commands.build();
        }
    }

    @Override
    public int getSize() {
        return messages.size() + commands.size();
    }

    @Override
    public String getTranslation(Message key) {
        return messages.get(key);
    }

    @Override
    public CommandSpecData getTranslation(CommandSpec key) {
        return commands.get(key);
    }

}
