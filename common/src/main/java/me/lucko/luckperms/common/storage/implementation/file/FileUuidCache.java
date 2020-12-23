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

package me.lucko.luckperms.common.storage.implementation.file;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

import me.lucko.luckperms.common.storage.misc.PlayerSaveResultImpl;
import me.lucko.luckperms.common.util.Uuids;

import net.luckperms.api.model.PlayerSaveResult;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FileUuidCache {
    private static final Splitter KV_SPLIT = Splitter.on(':').omitEmptyStrings();
    private static final Splitter LEGACY_KV_SPLIT = Splitter.on('=').omitEmptyStrings();
    private static final Splitter LEGACY_TIME_SPLIT = Splitter.on('|').omitEmptyStrings();

    // the lookup map
    private final LookupMap lookupMap = new LookupMap();

    /**
     * Adds a mapping to the cache
     *
     * @param uuid the uuid of the player
     * @param username the username of the player
     */
    public PlayerSaveResult addMapping(UUID uuid, String username) {
        // perform the insert
        String oldUsername = this.lookupMap.put(uuid, username);

        PlayerSaveResultImpl result = PlayerSaveResultImpl.determineBaseResult(username, oldUsername);

        Set<UUID> conflicting = new HashSet<>(this.lookupMap.lookupUuid(username));
        conflicting.remove(uuid);

        if (!conflicting.isEmpty()) {
            // remove the mappings for conflicting uuids
            for (UUID conflict : conflicting) {
                this.lookupMap.remove(conflict);
            }

            result = result.withOtherUuidsPresent(conflicting);
        }

        return result;
    }

    /**
     * Removes a mapping from the cache
     *
     * @param uuid the uuid of the player to remove
     */
    public void removeMapping(UUID uuid) {
        this.lookupMap.remove(uuid);
    }

    /**
     * Gets the most recent uuid which connected with the given username, or null
     *
     * @param username the username to lookup with
     * @return a uuid, or null
     */
    public @Nullable UUID lookupUuid(String username) {
        Set<UUID> uuids = this.lookupMap.lookupUuid(username);
        return Iterables.getFirst(uuids, null);
    }

    /**
     * Gets the most recent username used by a given uuid
     *
     * @param uuid the uuid to lookup with
     * @return a username, or null
     */
    public String lookupUsername(UUID uuid) {
        return this.lookupMap.lookupUsername(uuid);
    }

    public void load(Path file) {
        if (!Files.exists(file)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String entry;
            while ((entry = reader.readLine()) != null) {
                entry = entry.trim();
                if (entry.isEmpty() || entry.startsWith("#")) {
                    continue;
                }
                loadEntry(entry);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save(Path file) {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write("# LuckPerms UUID lookup cache");
            writer.newLine();
            for (Map.Entry<UUID, String> ent : this.lookupMap.entrySet()) {
                String out = ent.getKey() + ":" + ent.getValue();
                writer.write(out);
                writer.newLine();
            }
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadEntry(String entry) {
        if (entry.contains(":")) {
            // new format
            Iterator<String> parts = KV_SPLIT.split(entry).iterator();

            if (!parts.hasNext()) return;
            String uuidPart = parts.next();

            if (!parts.hasNext()) return;
            String usernamePart = parts.next();

            UUID uuid = Uuids.fromString(uuidPart);
            if (uuid == null) return;

            this.lookupMap.put(uuid, usernamePart);
        } else if (entry.contains("=")) {
            // old format
            Iterator<String> parts = LEGACY_KV_SPLIT.split(entry).iterator();

            if (!parts.hasNext()) return;
            String usernamePart = parts.next();

            if (!parts.hasNext()) return;
            String uuidPart = parts.next();

            // contains a time
            if (uuidPart.contains("|")) {
                Iterator<String> valueParts = LEGACY_TIME_SPLIT.split(uuidPart).iterator();
                if (!valueParts.hasNext()) return;
                uuidPart = valueParts.next();
            }

            UUID uuid = Uuids.fromString(uuidPart);
            if (uuid == null) return;

            this.lookupMap.put(uuid, usernamePart);
        }
    }

    private static final class LookupMap extends ConcurrentHashMap<UUID, String> {
        private final SetMultimap<String, UUID> reverse = Multimaps.newSetMultimap(new ConcurrentHashMap<>(), ConcurrentHashMap::newKeySet);

        @Override
        public String put(@NonNull UUID key, @NonNull String value) {
            String existing = super.put(key, value);

            // check if we need to remove a reverse entry which has been replaced
            // existing might be null
            if (!value.equalsIgnoreCase(existing)) {
                if (existing != null) {
                    this.reverse.remove(existing.toLowerCase(), key);
                }
            }

            this.reverse.put(value.toLowerCase(), key);
            return existing;
        }

        @Override
        public String remove(@NonNull Object k) {
            UUID key = (UUID) k;
            String username = super.remove(key);
            if (username != null) {
                this.reverse.remove(username.toLowerCase(), key);
            }
            return username;
        }

        public String lookupUsername(UUID uuid) {
            return super.get(uuid);
        }

        public Set<UUID> lookupUuid(String name) {
            return this.reverse.get(name.toLowerCase());
        }
    }

}
