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

package me.lucko.luckperms.common.cacheddata.metastack;

import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.util.ImmutableCollectors;
import net.luckperms.api.metastacking.MetaStackElement;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.node.ChatMetaType;
import net.luckperms.api.node.metadata.types.InheritanceOriginMetadata;
import net.luckperms.api.node.types.ChatMetaNode;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Contains the standard {@link MetaStackElement}s provided by LuckPerms.
 */
public final class StandardStackElements {
    private StandardStackElements() {}

    public static MetaStackElement parseFromString(LuckPermsPlugin plugin, String s) {
        s = s.toLowerCase(Locale.ROOT);

        // static
        if (s.equals("highest")) return HIGHEST;
        if (s.equals("lowest")) return LOWEST;
        if (s.equals("highest_own")) return HIGHEST_OWN;
        if (s.equals("lowest_own")) return LOWEST_OWN;
        if (s.equals("highest_inherited")) return HIGHEST_INHERITED;
        if (s.equals("lowest_inherited")) return LOWEST_INHERITED;

        // dynamic
        String p;
        if ((p = parseParam(s, "highest_on_track_")) != null) return highestFromGroupOnTrack(plugin, p);
        if ((p = parseParam(s, "lowest_on_track_")) != null) return lowestFromGroupOnTrack(plugin, p);
        if ((p = parseParam(s, "highest_not_on_track_")) != null) return highestNotFromGroupOnTrack(plugin, p);
        if ((p = parseParam(s, "lowest_not_on_track_")) != null) return lowestNotFromGroupOnTrack(plugin, p);
        if ((p = parseParam(s, "highest_from_group_")) != null) return highestFromGroup(p);
        if ((p = parseParam(s, "lowest_from_group_")) != null) return lowestFromGroup(p);
        if ((p = parseParam(s, "highest_not_from_group_")) != null) return highestNotFromGroup(p);
        if ((p = parseParam(s, "lowest_not_from_group_")) != null) return lowestNotFromGroup(p);

        return null;
    }

    private static String parseParam(String s, String prefix) {
        if (s.startsWith(prefix) && s.length() > prefix.length()) {
            return s.substring(prefix.length());
        }
        return null;
    }

    public static List<MetaStackElement> parseList(LuckPermsPlugin plugin, List<String> strings) {
        return strings.stream()
                .map(s -> {
                    MetaStackElement parsed = parseFromString(plugin, s);
                    if (parsed == null) {
                        plugin.getLogger().warn("Unable to parse from: " + s, new IllegalArgumentException());
                    }
                    return parsed;
                })
                .filter(Objects::nonNull)
                .collect(ImmutableCollectors.toList());
    }

    // utility functions, used in combination with FluentMetaStackElement for form full MetaStackElements

    private static final MetaStackElement TYPE_CHECK = (type, node, current) -> type.nodeType().matches(node);
    private static final MetaStackElement HIGHEST_CHECK = (type, node, current) -> current == null || node.getPriority() > current.getPriority();
    private static final MetaStackElement LOWEST_CHECK = (type, node, current) -> current == null || node.getPriority() < current.getPriority();
    private static final MetaStackElement OWN_CHECK = (type, node, current) -> node.metadata(InheritanceOriginMetadata.KEY).getOrigin().getType().equals(PermissionHolder.Identifier.USER_TYPE);
    private static final MetaStackElement INHERITED_CHECK = (type, node, current) -> node.metadata(InheritanceOriginMetadata.KEY).getOrigin().getType().equals(PermissionHolder.Identifier.GROUP_TYPE);


    // implementations

    public static final MetaStackElement HIGHEST = FluentMetaStackElement.builder("HighestPriority")
            .with(TYPE_CHECK)
            .with(HIGHEST_CHECK)
            .build();

    public static final MetaStackElement HIGHEST_OWN = FluentMetaStackElement.builder("HighestPriorityOwn")
            .with(TYPE_CHECK)
            .with(OWN_CHECK)
            .with(HIGHEST_CHECK)
            .build();

    public static final MetaStackElement HIGHEST_INHERITED = FluentMetaStackElement.builder("HighestPriorityInherited")
            .with(TYPE_CHECK)
            .with(INHERITED_CHECK)
            .with(HIGHEST_CHECK)
            .build();

    public static MetaStackElement highestFromGroupOnTrack(LuckPermsPlugin plugin, String trackName) {
        return FluentMetaStackElement.builder("HighestPriorityOnTrack")
                .param("trackName", trackName)
                .with(TYPE_CHECK)
                .with(HIGHEST_CHECK)
                .with(new FromGroupOnTrackCheck(plugin, trackName))
                .build();
    }

    public static MetaStackElement highestNotFromGroupOnTrack(LuckPermsPlugin plugin, String trackName) {
        return FluentMetaStackElement.builder("HighestPriorityNotOnTrack")
                .param("trackName", trackName)
                .with(TYPE_CHECK)
                .with(HIGHEST_CHECK)
                .with(new NotFromGroupOnTrackCheck(plugin, trackName))
                .build();
    }

    public static MetaStackElement highestFromGroup(String groupName) {
        return FluentMetaStackElement.builder("HighestPriorityFromGroup")
                .param("groupName", groupName)
                .with(TYPE_CHECK)
                .with(HIGHEST_CHECK)
                .with(new FromGroupCheck(groupName))
                .build();
    }

    public static MetaStackElement highestNotFromGroup(String groupName) {
        return FluentMetaStackElement.builder("HighestPriorityNotFromGroup")
                .param("groupName", groupName)
                .with(TYPE_CHECK)
                .with(HIGHEST_CHECK)
                .with(new NotFromGroupCheck(groupName))
                .build();
    }

    public static final MetaStackElement LOWEST = FluentMetaStackElement.builder("LowestPriority")
            .with(TYPE_CHECK)
            .with(LOWEST_CHECK)
            .build();

    public static final MetaStackElement LOWEST_OWN = FluentMetaStackElement.builder("LowestPriorityOwn")
            .with(TYPE_CHECK)
            .with(OWN_CHECK)
            .with(LOWEST_CHECK)
            .build();

    public static final MetaStackElement LOWEST_INHERITED = FluentMetaStackElement.builder("LowestPriorityInherited")
            .with(TYPE_CHECK)
            .with(INHERITED_CHECK)
            .with(LOWEST_CHECK)
            .build();

    public static MetaStackElement lowestFromGroupOnTrack(LuckPermsPlugin plugin, String trackName) {
        return FluentMetaStackElement.builder("LowestPriorityOnTrack")
                .param("trackName", trackName)
                .with(TYPE_CHECK)
                .with(LOWEST_CHECK)
                .with(new FromGroupOnTrackCheck(plugin, trackName))
                .build();
    }

    public static MetaStackElement lowestNotFromGroupOnTrack(LuckPermsPlugin plugin, String trackName) {
        return FluentMetaStackElement.builder("LowestPriorityNotOnTrack")
                .param("trackName", trackName)
                .with(TYPE_CHECK)
                .with(LOWEST_CHECK)
                .with(new NotFromGroupOnTrackCheck(plugin, trackName))
                .build();
    }

    public static MetaStackElement lowestFromGroup(String groupName) {
        return FluentMetaStackElement.builder("LowestPriorityFromGroup")
                .param("groupName", groupName)
                .with(TYPE_CHECK)
                .with(LOWEST_CHECK)
                .with(new FromGroupCheck(groupName))
                .build();
    }

    public static MetaStackElement lowestNotFromGroup(String groupName) {
        return FluentMetaStackElement.builder("LowestPriorityNotFromGroup")
                .param("groupName", groupName)
                .with(TYPE_CHECK)
                .with(LOWEST_CHECK)
                .with(new NotFromGroupCheck(groupName))
                .build();
    }

    private static final class FromGroupOnTrackCheck implements MetaStackElement {
        private final LuckPermsPlugin plugin;
        private final String trackName;

        FromGroupOnTrackCheck(LuckPermsPlugin plugin, String trackName) {
            this.plugin = plugin;
            this.trackName = trackName;
        }

        @Override
        public boolean shouldAccumulate(@NonNull ChatMetaType type, @NonNull ChatMetaNode<?, ?> node, @Nullable ChatMetaNode<?, ?> current) {
            Track track = this.plugin.getTrackManager().getIfLoaded(this.trackName);
            if (track == null) {
                return false;
            }
            PermissionHolder.Identifier origin = node.metadata(InheritanceOriginMetadata.KEY).getOrigin();
            return origin.getType().equals(PermissionHolder.Identifier.GROUP_TYPE) && track.containsGroup(origin.getName());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FromGroupOnTrackCheck that = (FromGroupOnTrackCheck) o;
            return this.trackName.equals(that.trackName);
        }

        @Override
        public int hashCode() {
            return this.trackName.hashCode();
        }
    }

    private static final class NotFromGroupOnTrackCheck implements MetaStackElement {
        private final LuckPermsPlugin plugin;
        private final String trackName;

        NotFromGroupOnTrackCheck(LuckPermsPlugin plugin, String trackName) {
            this.plugin = plugin;
            this.trackName = trackName;
        }

        @Override
        public boolean shouldAccumulate(@NonNull ChatMetaType type, @NonNull ChatMetaNode<?, ?> node, @Nullable ChatMetaNode<?, ?> current) {
            Track track = this.plugin.getTrackManager().getIfLoaded(this.trackName);
            if (track == null) {
                return false;
            }
            PermissionHolder.Identifier origin = node.metadata(InheritanceOriginMetadata.KEY).getOrigin();
            return !(origin.getType().equals(PermissionHolder.Identifier.GROUP_TYPE) && track.containsGroup(origin.getName()));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NotFromGroupOnTrackCheck that = (NotFromGroupOnTrackCheck) o;
            return this.trackName.equals(that.trackName);
        }

        @Override
        public int hashCode() {
            return this.trackName.hashCode();
        }
    }

    private static final class FromGroupCheck implements MetaStackElement {
        private final String groupName;

        FromGroupCheck(String groupName) {
            this.groupName = groupName;
        }

        @Override
        public boolean shouldAccumulate(@NonNull ChatMetaType type, @NonNull ChatMetaNode<?, ?> node, @Nullable ChatMetaNode<?, ?> current) {
            PermissionHolder.Identifier origin = node.metadata(InheritanceOriginMetadata.KEY).getOrigin();
            return origin.getType().equals(PermissionHolder.Identifier.GROUP_TYPE) && this.groupName.equals(origin.getName());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FromGroupCheck that = (FromGroupCheck) o;
            return this.groupName.equals(that.groupName);
        }

        @Override
        public int hashCode() {
            return this.groupName.hashCode();
        }
    }

    private static final class NotFromGroupCheck implements MetaStackElement {
        private final String groupName;

        NotFromGroupCheck(String groupName) {
            this.groupName = groupName;
        }

        @Override
        public boolean shouldAccumulate(@NonNull ChatMetaType type, @NonNull ChatMetaNode<?, ?> node, @Nullable ChatMetaNode<?, ?> current) {
            PermissionHolder.Identifier origin = node.metadata(InheritanceOriginMetadata.KEY).getOrigin();
            return !(origin.getType().equals(PermissionHolder.Identifier.GROUP_TYPE) && this.groupName.equals(origin.getName()));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NotFromGroupCheck that = (NotFromGroupCheck) o;
            return this.groupName.equals(that.groupName);
        }

        @Override
        public int hashCode() {
            return this.groupName.hashCode();
        }
    }

}
