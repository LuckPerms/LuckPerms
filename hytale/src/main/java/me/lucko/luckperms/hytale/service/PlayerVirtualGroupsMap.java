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

package me.lucko.luckperms.hytale.service;

import com.google.common.collect.ImmutableSet;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A thread-safe map of player UUIDs to their virtual groups.
 */
public class PlayerVirtualGroupsMap {
    private final Map<UUID, ImmutableSet<String>> uuidToGroups = new ConcurrentHashMap<>();

    public void addPlayerToGroup(UUID uuid, String group0) {
        String group = group0.toLowerCase(Locale.ROOT);

        this.uuidToGroups.compute(uuid, (key, existing)-> {
            if (existing == null) {
                return ImmutableSet.of(group);
            } else {
                if (existing.contains(group)) {
                    return existing;
                } else {
                    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
                    builder.addAll(existing);
                    builder.add(group);
                    return builder.build();
                }
            }
        });
    }

    public void removePlayerFromGroup(UUID uuid, String group0) {
        String group = group0.toLowerCase(Locale.ROOT);

        this.uuidToGroups.computeIfPresent(uuid, (key, existing) -> {
            if (!existing.contains(group)) {
                return existing;
            } else {
                if (existing.size() == 1) {
                    return null;
                }

                ImmutableSet.Builder<String> builder = ImmutableSet.builder();
                for (String g : existing) {
                    if (!g.equals(group)) {
                        builder.add(g);
                    }
                }
                return builder.build();
            }
        });
    }

    public ImmutableSet<String> getPlayerGroups(UUID uuid) {
        return this.uuidToGroups.getOrDefault(uuid, ImmutableSet.of());
    }

}
