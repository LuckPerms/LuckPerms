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

package me.lucko.luckperms.common.storage.dao.file;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;

import me.lucko.luckperms.common.utils.DateUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FileUuidCache {
    private static final Splitter KV_SPLIT = Splitter.on('=').omitEmptyStrings();
    private static final Splitter TIME_SPLIT = Splitter.on('|').omitEmptyStrings();

    // the map for lookups
    private final Map<String, Map.Entry<UUID, Long>> lookupMap = new ConcurrentHashMap<>();

    /**
     * Adds a mapping to the cache
     *
     * @param uuid the uuid of the player
     * @param username the username of the player
     */
    public void addMapping(UUID uuid, String username) {
        lookupMap.put(username.toLowerCase(), Maps.immutableEntry(uuid, DateUtil.unixSecondsNow()));
    }

    /**
     * Gets the most recent uuid which connected with the given username, or null
     *
     * @param username the username to lookup with
     * @return a uuid, or null
     */
    public UUID lookupUUID(String username) {
        Map.Entry<UUID, Long> ret = lookupMap.get(username.toLowerCase());
        return ret == null ? null : ret.getKey();
    }

    /**
     * Gets the most recent username used by a given uuid
     *
     * @param uuid the uuid to lookup with
     * @return a username, or null
     */
    public String lookupUsername(UUID uuid) {
        String username = null;
        Long time = Long.MIN_VALUE;

        for (Map.Entry<String, Map.Entry<UUID, Long>> ent : lookupMap.entrySet()) {
            if (!ent.getValue().getKey().equals(uuid)) {
                continue;
            }

            Long t = ent.getValue().getValue();

            if (t > time) {
                time = t;
                username = ent.getKey();
            }
        }

        return username;
    }

    public void load(File file) {
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {

            String entry;
            while ((entry = reader.readLine()) != null) {
                entry = entry.trim();
                if (entry.isEmpty() || entry.startsWith("#")) {
                    continue;
                }

                Iterator<String> parts = KV_SPLIT.split(entry).iterator();

                if (!parts.hasNext()) continue;
                String key = parts.next();

                if (!parts.hasNext()) continue;
                String value = parts.next();

                UUID uid;
                Long t;

                // contains a time (backwards compat)
                if (value.contains("|")) {
                    // try to split and extract the time element from the end.
                    Iterator<String> valueParts = TIME_SPLIT.split(value).iterator();

                    if (!valueParts.hasNext()) continue;
                    String uuid = valueParts.next();

                    if (!valueParts.hasNext()) continue;
                    String time = valueParts.next();

                    try {
                        uid = UUID.fromString(uuid);
                    } catch (IllegalArgumentException e) {
                        continue;
                    }

                    try {
                        t = Long.parseLong(time);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                } else {
                    // just parse from the value
                    try {
                        uid = UUID.fromString(value);
                    } catch (IllegalArgumentException e) {
                        continue;
                    }

                    t = 0L;
                }

                lookupMap.put(key, Maps.immutableEntry(uid, t));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save(File file) {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write("# LuckPerms UUID lookup cache");
            writer.newLine();

            for (Map.Entry<String, Map.Entry<UUID, Long>> ent : lookupMap.entrySet()) {
                String out = ent.getKey() + "=" + ent.getValue().getKey().toString() + "|" + ent.getValue().getValue().toString();
                writer.write(out);
                writer.newLine();
            }

            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
