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

package me.lucko.luckperms.common.metastacking;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.UtilityClass;

import me.lucko.luckperms.api.ChatMetaType;
import me.lucko.luckperms.api.LocalizedNode;
import me.lucko.luckperms.api.metastacking.MetaStackElement;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.ImmutableCollectors;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Contains the standard {@link MetaStackElement}s provided by LuckPerms.
 */
@UtilityClass
public class StandardStackElements {
    private static final HighestPriority HIGHEST_PRIORITY = new HighestPriority();
    private static final LowestPriority LOWEST_PRIORITY = new LowestPriority();
    private static final HighestPriorityOwn HIGHEST_PRIORITY_OWN = new HighestPriorityOwn();
    private static final LowestPriorityOwn LOWEST_PRIORITY_OWN = new LowestPriorityOwn();
    private static final HighestPriorityInherited HIGHEST_PRIORITY_INHERITED = new HighestPriorityInherited();
    private static final LowestPriorityInherited LOWEST_PRIORITY_INHERITED = new LowestPriorityInherited();

    public static Optional<MetaStackElement> parseFromString(LuckPermsPlugin plugin, String s) {
        s = s.toLowerCase();

        if (s.equals("highest")) {
            return Optional.of(HIGHEST_PRIORITY);
        }

        if (s.equals("lowest")) {
            return Optional.of(LOWEST_PRIORITY);
        }

        if (s.equals("highest_own")) {
            return Optional.of(HIGHEST_PRIORITY_OWN);
        }

        if (s.equals("lowest_own")) {
            return Optional.of(LOWEST_PRIORITY_OWN);
        }

        if (s.equals("highest_inherited")) {
            return Optional.of(HIGHEST_PRIORITY_INHERITED);
        }

        if (s.equals("lowest_inherited")) {
            return Optional.of(LOWEST_PRIORITY_INHERITED);
        }

        if (s.startsWith("highest_on_track_") && s.length() > "highest_on_track_".length()) {
            String track = s.substring("highest_on_track_".length());
            return Optional.of(new HighestPriorityTrack(plugin, track));
        }

        if (s.startsWith("lowest_on_track_") && s.length() > "lowest_on_track_".length()) {
            String track = s.substring("lowest_on_track_".length());
            return Optional.of(new LowestPriorityTrack(plugin, track));
        }

        if (s.startsWith("highest_not_on_track_") && s.length() > "highest_not_on_track_".length()) {
            String track = s.substring("highest_not_on_track_".length());
            return Optional.of(new HighestPriorityNotOnTrack(plugin, track));
        }

        if (s.startsWith("lowest_not_on_track_") && s.length() > "lowest_not_on_track_".length()) {
            String track = s.substring("lowest_not_on_track_".length());
            return Optional.of(new LowestPriorityNotOnTrack(plugin, track));
        }

        new IllegalArgumentException("Cannot parse MetaStackElement: " + s).printStackTrace();
        return Optional.empty();
    }

    public static List<MetaStackElement> parseList(LuckPermsPlugin plugin, List<String> strings) {
        return strings.stream()
                .map(s -> parseFromString(plugin, s))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(ImmutableCollectors.toList());
    }

    /**
     * Returns true if the current node has the greater priority
     * @param current the current entry
     * @param newEntry the new entry
     * @return true if the accumulation should return
     */
    private static boolean compareEntriesHighest(Map.Entry<Integer, String> current, Map.Entry<Integer, String> newEntry) {
        return current != null && current.getKey() >= newEntry.getKey();
    }

    /**
     * Returns true if the current node has the lesser priority
     * @param current the current entry
     * @param newEntry the new entry
     * @return true if the accumulation should return
     */
    private static boolean compareEntriesLowest(Map.Entry<Integer, String> current, Map.Entry<Integer, String> newEntry) {
        return current != null && current.getKey() <= newEntry.getKey();
    }

    /**
     * Returns true if the node is not held by a user
     * @param node the node to check
     * @return true if the accumulation should return
     */
    private static boolean checkOwnElement(LocalizedNode node) {
        try {
            UUID.fromString(node.getLocation());
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    /**
     * Returns true if the node is not held by a group on the track
     * @param node the node to check
     * @param track the track
     * @return true if the accumulation should return
     */
    private static boolean checkTrackElement(LuckPermsPlugin plugin, LocalizedNode node, String track) {
        if (node.getLocation() == null || node.getLocation().equals("")) {
            return true;
        }

        Track t = plugin.getTrackManager().getIfLoaded(track);
        return t == null || !t.containsGroup(node.getLocation());
    }

    /**
     * Returns true if the node is held by a group on the track
     * @param node the node to check
     * @param track the track
     * @return true if the accumulation should return
     */
    private static boolean checkNotTrackElement(LuckPermsPlugin plugin, LocalizedNode node, String track) {
        // it's not come from a group on this track (from the user directly)
        if (node.getLocation() == null || node.getLocation().equals("")) {
            return false;
        }

        Track t = plugin.getTrackManager().getIfLoaded(track);
        return t == null || t.containsGroup(node.getLocation());
    }

    @ToString
    private static final class HighestPriority implements MetaStackElement {
        @Override
        public boolean shouldAccumulate(LocalizedNode node, ChatMetaType type, Map.Entry<Integer, String> current) {
            if (type.shouldIgnore(node)) {
                return false;
            }

            Map.Entry<Integer, String> newEntry = type.getEntry(node);
            return !compareEntriesHighest(current, newEntry);
        }
    }

    @ToString
    private static final class HighestPriorityOwn implements MetaStackElement {
        @Override
        public boolean shouldAccumulate(LocalizedNode node, ChatMetaType type, Map.Entry<Integer, String> current) {
            if (type.shouldIgnore(node)) {
                return false;
            }

            if (checkOwnElement(node)) {
                return false;
            }

            Map.Entry<Integer, String> newEntry = type.getEntry(node);
            return !compareEntriesHighest(current, newEntry);
        }
    }

    @ToString
    private static final class HighestPriorityInherited implements MetaStackElement {
        @Override
        public boolean shouldAccumulate(LocalizedNode node, ChatMetaType type, Map.Entry<Integer, String> current) {
            if (type.shouldIgnore(node)) {
                return false;
            }

            if (!checkOwnElement(node)) {
                return false;
            }

            Map.Entry<Integer, String> newEntry = type.getEntry(node);
            return !compareEntriesHighest(current, newEntry);
        }
    }

    @ToString(of = "trackName")
    @RequiredArgsConstructor
    @EqualsAndHashCode(of = "trackName")
    private static final class HighestPriorityTrack implements MetaStackElement {
        private final LuckPermsPlugin plugin;
        private final String trackName;

        @Override
        public boolean shouldAccumulate(LocalizedNode node, ChatMetaType type, Map.Entry<Integer, String> current) {
            if (type.shouldIgnore(node)) {
                return false;
            }

            Map.Entry<Integer, String> newEntry = type.getEntry(node);
            return !compareEntriesHighest(current, newEntry) && !checkTrackElement(plugin, node, trackName);
        }
    }

    @ToString(of = "trackName")
    @RequiredArgsConstructor
    @EqualsAndHashCode(of = "trackName")
    private static final class HighestPriorityNotOnTrack implements MetaStackElement {
        private final LuckPermsPlugin plugin;
        private final String trackName;

        @Override
        public boolean shouldAccumulate(LocalizedNode node, ChatMetaType type, Map.Entry<Integer, String> current) {
            if (type.shouldIgnore(node)) {
                return false;
            }

            Map.Entry<Integer, String> newEntry = type.getEntry(node);
            return !compareEntriesHighest(current, newEntry) && !checkNotTrackElement(plugin, node, trackName);
        }
    }

    @ToString
    private static final class LowestPriority implements MetaStackElement {
        @Override
        public boolean shouldAccumulate(LocalizedNode node, ChatMetaType type, Map.Entry<Integer, String> current) {
            if (type.shouldIgnore(node)) {
                return false;
            }

            Map.Entry<Integer, String> newEntry = type.getEntry(node);
            return !compareEntriesLowest(current, newEntry);
        }
    }

    @ToString
    private static final class LowestPriorityOwn implements MetaStackElement {
        @Override
        public boolean shouldAccumulate(LocalizedNode node, ChatMetaType type, Map.Entry<Integer, String> current) {
            if (type.shouldIgnore(node)) {
                return false;
            }

            if (checkOwnElement(node)) {
                return false;
            }

            Map.Entry<Integer, String> newEntry = type.getEntry(node);
            return !compareEntriesLowest(current, newEntry);
        }
    }

    @ToString
    private static final class LowestPriorityInherited implements MetaStackElement {
        @Override
        public boolean shouldAccumulate(LocalizedNode node, ChatMetaType type, Map.Entry<Integer, String> current) {
            if (type.shouldIgnore(node)) {
                return false;
            }

            if (!checkOwnElement(node)) {
                return false;
            }

            Map.Entry<Integer, String> newEntry = type.getEntry(node);
            return !compareEntriesLowest(current, newEntry);
        }
    }

    @ToString(of = "trackName")
    @RequiredArgsConstructor
    @EqualsAndHashCode(of = "trackName")
    private static final class LowestPriorityTrack implements MetaStackElement {
        private final LuckPermsPlugin plugin;
        private final String trackName;

        @Override
        public boolean shouldAccumulate(LocalizedNode node, ChatMetaType type, Map.Entry<Integer, String> current) {
            if (type.shouldIgnore(node)) {
                return false;
            }

            Map.Entry<Integer, String> entry = type.getEntry(node);
            return !compareEntriesLowest(current, entry) && !checkTrackElement(plugin, node, trackName);
        }
    }

    @ToString(of = "trackName")
    @RequiredArgsConstructor
    @EqualsAndHashCode(of = "trackName")
    private static final class LowestPriorityNotOnTrack implements MetaStackElement {
        private final LuckPermsPlugin plugin;
        private final String trackName;

        @Override
        public boolean shouldAccumulate(LocalizedNode node, ChatMetaType type, Map.Entry<Integer, String> current) {
            if (type.shouldIgnore(node)) {
                return false;
            }

            Map.Entry<Integer, String> newEntry = type.getEntry(node);
            return !compareEntriesLowest(current, newEntry) && !checkNotTrackElement(plugin, node, trackName);
        }
    }
}
