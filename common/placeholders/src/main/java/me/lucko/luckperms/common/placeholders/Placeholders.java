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

package me.lucko.luckperms.common.placeholders;

import net.luckperms.api.metastacking.DuplicateRemovalFunction;
import net.luckperms.api.metastacking.MetaStackDefinition;
import net.luckperms.api.metastacking.MetaStackElement;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.track.Track;
import org.jetbrains.annotations.VisibleForTesting;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A class containing the standard, built-in placeholders.
 */
public final class Placeholders {
    private Placeholders() {
        throw new AssertionError();
    }

    // Basic meta placeholders

    /** Outputs the user's prefix. */
    public static final Placeholder.Basic PREFIX = Placeholder.basic("prefix", (ctx) -> stringNullToEmpty(ctx.metaData().getPrefix()));

    /** Outputs the user's suffix. */
    public static final Placeholder.Basic SUFFIX = Placeholder.basic("suffix", (ctx) -> stringNullToEmpty(ctx.metaData().getSuffix()));

    /** Outputs all meta values for a given key, separated by commas. */
    public static final Placeholder.UsingArgument ALL_META = Placeholder.usingArgument("all_meta", (ctx) -> {
        List<String> values = ctx.metaData().getMeta().getOrDefault(ctx.argument(), Collections.emptyList());
        return values.isEmpty() ? "" : String.join(", ", values);
    });

    /** Outputs a specific meta value for a given key. */
    public static final Placeholder.UsingArgument META = Placeholder.usingArgument("meta", (ctx) -> stringNullToEmpty(ctx.metaData().getMetaValue(ctx.argument())));

    // Meta stack element placeholders
    /** Outputs the user's prefix from a specific meta stack element. */
    public static final Placeholder.UsingArgument PREFIX_ELEMENT = Placeholder.usingArgument("prefix_element", (ctx) -> {
        MetaStackElement stackElement = ctx.api().getMetaStackFactory().fromString(ctx.argument()).orElse(null);
        if (stackElement == null) {
            throw new IllegalArgumentException("Invalid meta stack element: " + ctx.argument());
        }

        MetaStackDefinition stackDefinition = ctx.api().getMetaStackFactory().createDefinition(
                Collections.singletonList(stackElement), DuplicateRemovalFunction.RETAIN_ALL, "", "", "");
        QueryOptions newOptions = ctx.queryOptions().toBuilder()
                .option(MetaStackDefinition.PREFIX_STACK_KEY, stackDefinition)
                .option(MetaStackDefinition.SUFFIX_STACK_KEY, stackDefinition)
                .build();

        return stringNullToEmpty(ctx.userData().getMetaData(newOptions).getPrefix());
    });

    /** Outputs the user's suffix from a specific meta stack element. */
    public static final Placeholder.UsingArgument SUFFIX_ELEMENT = Placeholder.usingArgument("suffix_element", (ctx) -> {
        MetaStackElement stackElement = ctx.api().getMetaStackFactory().fromString(ctx.argument()).orElse(null);
        if (stackElement == null) {
            throw new IllegalArgumentException("Invalid meta stack element: " + ctx.argument());
        }

        MetaStackDefinition stackDefinition = ctx.api().getMetaStackFactory().createDefinition(
                Collections.singletonList(stackElement), DuplicateRemovalFunction.RETAIN_ALL, "", "", "");
        QueryOptions newOptions = ctx.queryOptions().toBuilder()
                .option(MetaStackDefinition.PREFIX_STACK_KEY, stackDefinition)
                .option(MetaStackDefinition.SUFFIX_STACK_KEY, stackDefinition)
                .build();

        return Objects.toString(ctx.userData().getMetaData(newOptions).getSuffix(), "");
    });

    // Context placeholders

    /** Outputs all context key-value pairs, separated by commas. */
    public static final Placeholder.Basic ALL_CONTEXT = Placeholder.basic("all_context", (ctx) ->
            ctx.queryOptions().context().toSet().stream()
                    .map(c -> c.getKey() + "=" + c.getValue())
                    .collect(Collectors.joining(", "))
    );

    /** Outputs all values for a specific context key, separated by commas. */
    public static final Placeholder.UsingArgument CONTEXT = Placeholder.usingArgument("context", (ctx) -> String.join(", ", ctx.queryOptions().context().getValues(ctx.argument())));

    // Group placeholders

    /** Outputs the user's directly assigned groups, separated by commas. */
    public static final Placeholder.Basic GROUPS = Placeholder.basic("groups", (ctx) ->
            ctx.user().getNodes(NodeType.INHERITANCE).stream()
                    .filter(n -> ctx.queryOptions().satisfies(n.getContexts()))
                    .map(InheritanceNode::getGroupName)
                    .map(name -> convertGroupDisplayName(ctx, name))
                    .collect(Collectors.joining(", "))
    );

    /** Outputs all groups the user inherits from, separated by commas. */
    public static final Placeholder.Basic INHERITED_GROUPS = Placeholder.basic("inherited_groups", (ctx) ->
            ctx.user().getInheritedGroups(ctx.queryOptions()).stream()
                    .map(Group::getFriendlyName)
                    .collect(Collectors.joining(", "))
    );

    /** Outputs the user's primary group name. */
    public static final Placeholder.Basic PRIMARY_GROUP_NAME = Placeholder.basic("primary_group_name", (ctx) ->
            convertGroupDisplayName(ctx, ctx.user().getPrimaryGroup())
    );

    // Permission check placeholders
    /** Checks if the user has a specific permission node directly assigned (outputs "true" or "false"). */
    public static final Placeholder.UsingArgument HAS_PERMISSION = Placeholder.usingArgument("has_permission", (ctx) ->
            String.valueOf(ctx.user().getNodes().stream()
                    .filter(n -> ctx.queryOptions().satisfies(n.getContexts()))
                    .anyMatch(n -> n.getKey().equals(ctx.argument())))
    );

    /** Checks if the user inherits a specific permission node (outputs "true" or "false"). */
    public static final Placeholder.UsingArgument INHERITS_PERMISSION = Placeholder.usingArgument("inherits_permission", (ctx) ->
            String.valueOf(ctx.user().resolveInheritedNodes(ctx.queryOptions()).stream()
                    .filter(n -> n.getContexts().isSatisfiedBy(ctx.queryOptions().context()))
                    .anyMatch(n -> n.getKey().equals(ctx.argument())))
    );

    /** Checks the result of a permission check (outputs "true" or "false"). */
    public static final Placeholder.UsingArgument CHECK_PERMISSION = Placeholder.usingArgument("check_permission", (ctx) ->
            String.valueOf(ctx.permissionData().checkPermission(ctx.argument()).asBoolean())
    );

    /** Checks if the user is directly in a specific group (outputs "true" or "false"). */
    public static final Placeholder.UsingArgument IN_GROUP = Placeholder.usingArgument("in_group", (ctx) ->
            String.valueOf(ctx.user().getNodes(NodeType.INHERITANCE).stream()
                    .filter(n -> ctx.queryOptions().satisfies(n.getContexts()))
                    .map(InheritanceNode::getGroupName)
                    .anyMatch(s -> s.equalsIgnoreCase(ctx.argument())))
    );

    /** Checks if the user inherits from a specific group (outputs "true" or "false"). */
    public static final Placeholder.UsingArgument INHERITS_GROUP = Placeholder.usingArgument("inherits_group", (ctx) ->
            String.valueOf(ctx.user().getInheritedGroups(ctx.queryOptions()).stream()
                    .anyMatch(g -> g.getName().equalsIgnoreCase(ctx.argument())))
    );

    // Track placeholders
    /** Checks if the user's primary group is on a specific track (outputs "true" or "false"). */
    public static final Placeholder.UsingArgument ON_TRACK = Placeholder.usingArgument("on_track", (ctx) ->
            String.valueOf(Optional.ofNullable(ctx.api().getTrackManager().getTrack(ctx.argument()))
                    .map(t -> t.containsGroup(ctx.user().getPrimaryGroup()))
                    .orElse(false))
    );

    /** Checks if the user has any groups on a specific track (outputs "true" or "false"). */
    public static final Placeholder.UsingArgument HAS_GROUPS_ON_TRACK = Placeholder.usingArgument("has_groups_on_track", (ctx) ->
            String.valueOf(Optional.ofNullable(ctx.api().getTrackManager().getTrack(ctx.argument()))
                    .map(t -> ctx.user().getNodes(NodeType.INHERITANCE).stream()
                            .map(InheritanceNode::getGroupName)
                            .anyMatch(t::containsGroup)
                    )
                    .orElse(false))
    );

    // Group weight placeholders

    /** Outputs the name of the user's highest weighted directly assigned group. */
    public static final Placeholder.Basic HIGHEST_GROUP_BY_WEIGHT = Placeholder.basic("highest_group_by_weight", (ctx) ->
            ctx.user().getNodes(NodeType.INHERITANCE).stream()
                    .filter(n -> ctx.queryOptions().satisfies(n.getContexts()))
                    .map(InheritanceNode::getGroupName)
                    .map(n -> ctx.api().getGroupManager().getGroup(n))
                    .filter(Objects::nonNull)
                    .max(Comparator.comparingInt(g -> g.getWeight().orElse(0)))
                    .map(Group::getName)
                    .map(name -> convertGroupDisplayName(ctx, name))
                    .orElse("")
    );

    /** Outputs the name of the user's lowest weighted directly assigned group. */
    public static final Placeholder.Basic LOWEST_GROUP_BY_WEIGHT = Placeholder.basic("lowest_group_by_weight", (ctx) ->
            ctx.user().getNodes(NodeType.INHERITANCE).stream()
                    .filter(n -> ctx.queryOptions().satisfies(n.getContexts()))
                    .map(InheritanceNode::getGroupName)
                    .map(n -> ctx.api().getGroupManager().getGroup(n))
                    .filter(Objects::nonNull)
                    .min(Comparator.comparingInt(g -> g.getWeight().orElse(0)))
                    .map(Group::getName)
                    .map(name -> convertGroupDisplayName(ctx, name))
                    .orElse("")
    );

    /** Outputs the name of the user's highest weighted inherited group. */
    public static final Placeholder.Basic HIGHEST_INHERITED_GROUP_BY_WEIGHT = Placeholder.basic("highest_inherited_group_by_weight", (ctx) ->
            ctx.user().getInheritedGroups(ctx.queryOptions()).stream()
                    .max(Comparator.comparingInt(g -> g.getWeight().orElse(0)))
                    .map(Group::getName)
                    .map(name -> convertGroupDisplayName(ctx, name))
                    .orElse("")
    );

    /** Outputs the name of the user's lowest weighted inherited group. */
    public static final Placeholder.Basic LOWEST_INHERITED_GROUP_BY_WEIGHT = Placeholder.basic("lowest_inherited_group_by_weight", (ctx) ->
            ctx.user().getInheritedGroups(ctx.queryOptions()).stream()
                    .min(Comparator.comparingInt(g -> g.getWeight().orElse(0)))
                    .map(Group::getName)
                    .map(name -> convertGroupDisplayName(ctx, name))
                    .orElse("")
    );

    /** Outputs the weight value of the user's highest weighted directly assigned group. */
    public static final Placeholder.Basic HIGHEST_GROUP_WEIGHT = Placeholder.basic("highest_group_weight", (ctx) ->
            String.valueOf(ctx.user().getNodes(NodeType.INHERITANCE).stream()
                    .filter(n -> ctx.queryOptions().satisfies(n.getContexts()))
                    .map(InheritanceNode::getGroupName)
                    .map(n -> ctx.api().getGroupManager().getGroup(n))
                    .filter(Objects::nonNull)
                    .map(Group::getWeight)
                    .filter(OptionalInt::isPresent)
                    .mapToInt(OptionalInt::getAsInt)
                    .max()
                    .orElse(0))
    );

    // Track position placeholders

    /** Outputs the user's current group on a specific track. */
    public static final Placeholder.UsingArgument CURRENT_GROUP_ON_TRACK = Placeholder.usingArgument("current_group_on_track", (ctx) -> {
        Track track = ctx.api().getTrackManager().getTrack(ctx.argument());
        if (track == null) {
            return "";
        }

        List<Group> groups = ctx.user().getNodes(NodeType.INHERITANCE).stream()
                .filter(n -> track.containsGroup(n.getGroupName()))
                .filter(n -> ctx.queryOptions().satisfies(n.getContexts()))
                .distinct()
                .map(n -> ctx.api().getGroupManager().getGroup(n.getGroupName()))
                .collect(Collectors.toList());

        if (groups.size() != 1) {
            return "";
        }

        return groups.get(0).getFriendlyName();
    });

    /** Outputs the next group on a specific track. */
    public static final Placeholder.UsingArgument NEXT_GROUP_ON_TRACK = Placeholder.usingArgument("next_group_on_track", (ctx) -> {
        Track track = ctx.api().getTrackManager().getTrack(ctx.argument());
        if (track == null || track.getGroups().size() <= 1) {
            return "";
        }

        List<Group> groups = ctx.user().getNodes(NodeType.INHERITANCE).stream()
                .filter(n -> track.containsGroup(n.getGroupName()))
                .filter(n -> ctx.queryOptions().satisfies(n.getContexts()))
                .distinct()
                .map(n -> ctx.api().getGroupManager().getGroup(n.getGroupName()))
                .collect(Collectors.toList());

        if (groups.size() != 1) {
            return "";
        }

        return Objects.toString(convertGroupDisplayName(ctx, track.getNext(groups.get(0))), "");
    });

    /** Outputs the previous group on a specific track. */
    public static final Placeholder.UsingArgument PREVIOUS_GROUP_ON_TRACK = Placeholder.usingArgument("previous_group_on_track", (ctx) -> {
        Track track = ctx.api().getTrackManager().getTrack(ctx.argument());
        if (track == null || track.getGroups().size() <= 1) {
            return "";
        }

        List<Group> groups = ctx.user().getNodes(NodeType.INHERITANCE).stream()
                .filter(n -> track.containsGroup(n.getGroupName()))
                .filter(n -> ctx.queryOptions().satisfies(n.getContexts()))
                .distinct()
                .map(n -> ctx.api().getGroupManager().getGroup(n.getGroupName()))
                .collect(Collectors.toList());

        if (groups.size() != 1) {
            return "";
        }

        return Objects.toString(convertGroupDisplayName(ctx, track.getPrevious(groups.get(0))), "");
    });

    /** Outputs the first group the user has on a comma-separated list of tracks. */
    public static final Placeholder.UsingArgument FIRST_GROUP_ON_TRACKS = Placeholder.usingArgument("first_group_on_tracks", (ctx) -> {
        List<String> tracks = Arrays.stream(ctx.argument().split(",")).map(String::trim).collect(Collectors.toList());
        Set<String> groups = ctx.user().getInheritedGroups(ctx.queryOptions()).stream().map(Group::getName).collect(Collectors.toSet());

        return tracks.stream()
                .map(n -> ctx.api().getTrackManager().getTrack(n))
                .filter(Objects::nonNull)
                .map(Track::getGroups)
                .map(trackGroups -> trackGroups.stream().filter(groups::contains).findFirst())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .map(name -> convertGroupDisplayName(ctx, name))
                .orElse("");
    });

    /** Outputs the last group the user has on a comma-separated list of tracks. */
    public static final Placeholder.UsingArgument LAST_GROUP_ON_TRACKS = Placeholder.usingArgument("last_group_on_tracks", (ctx) -> {
        List<String> tracks = Arrays.stream(ctx.argument().split(",")).map(String::trim).collect(Collectors.toList());
        Set<String> groups = ctx.user().getInheritedGroups(ctx.queryOptions()).stream().map(Group::getName).collect(Collectors.toSet());

        return tracks.stream()
                .map(n -> ctx.api().getTrackManager().getTrack(n))
                .filter(Objects::nonNull)
                .map(Track::getGroups)
                .map(list -> {
                    List<String> copy = new ArrayList<>(list);
                    Collections.reverse(copy);
                    return copy;
                })
                .map(trackGroups -> trackGroups.stream().filter(groups::contains).findFirst())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .map(name -> convertGroupDisplayName(ctx, name))
                .orElse("");
    });

    // Expiry time placeholders

    /** Outputs the expiry time remaining for a specific permission node. */
    public static final Placeholder.UsingArgument EXPIRY_TIME = Placeholder.usingArgument("expiry_time", (ctx) ->
            ctx.user().getNodes().stream()
                    .filter(Node::hasExpiry)
                    .filter(n -> n.getKey().equals(ctx.argument()))
                    .filter(n -> ctx.queryOptions().satisfies(n.getContexts()))
                    .map(Node::getExpiryDuration)
                    .filter(Objects::nonNull)
                    .filter(d -> !d.isNegative())
                    .findFirst()
                    .map(Placeholders::formatDuration)
                    .orElse("")
    );

    /** Outputs the expiry time remaining for a specific inherited permission node. */
    public static final Placeholder.UsingArgument INHERITED_EXPIRY_TIME = Placeholder.usingArgument("inherited_expiry_time", (ctx) ->
            ctx.user().resolveInheritedNodes(ctx.queryOptions()).stream()
                    .filter(Node::hasExpiry)
                    .filter(n -> n.getKey().equals(ctx.argument()))
                    .map(Node::getExpiryDuration)
                    .filter(Objects::nonNull)
                    .filter(d -> !d.isNegative())
                    .findFirst()
                    .map(Placeholders::formatDuration)
                    .orElse("")
    );

    /** Outputs the expiry time remaining for a specific group membership. */
    public static final Placeholder.UsingArgument GROUP_EXPIRY_TIME = Placeholder.usingArgument("group_expiry_time", (ctx) ->
            ctx.user().getNodes(NodeType.INHERITANCE).stream()
                    .filter(Node::hasExpiry)
                    .filter(n -> n.getGroupName().equals(ctx.argument()))
                    .filter(n -> ctx.queryOptions().satisfies(n.getContexts()))
                    .map(Node::getExpiryDuration)
                    .filter(Objects::nonNull)
                    .filter(d -> !d.isNegative())
                    .findFirst()
                    .map(Placeholders::formatDuration)
                    .orElse("")
    );

    /** Outputs the expiry time remaining for a specific inherited group membership. */
    public static final Placeholder.UsingArgument INHERITED_GROUP_EXPIRY_TIME = Placeholder.usingArgument("inherited_group_expiry_time", (ctx) ->
            ctx.user().resolveInheritedNodes(ctx.queryOptions()).stream()
                    .filter(Node::hasExpiry)
                    .filter(NodeType.INHERITANCE::matches)
                    .map(NodeType.INHERITANCE::cast)
                    .filter(n -> n.getGroupName().equals(ctx.argument()))
                    .map(Node::getExpiryDuration)
                    .filter(Objects::nonNull)
                    .filter(d -> !d.isNegative())
                    .findFirst()
                    .map(Placeholders::formatDuration)
                    .orElse("")
    );

    private static String stringNullToEmpty(String string) {
        return string == null ? "" : string;
    }

    private static String convertGroupDisplayName(PlaceholderContext ctx, String groupName) {
        Group group = ctx.api().getGroupManager().getGroup(groupName);
        return group != null ? group.getFriendlyName() : groupName;
    }

    // simple version of me.lucko.luckperms.common.util.DurationFormatter
    @VisibleForTesting
    static String formatDuration(Duration duration) {
        if (duration == null || duration.isNegative()) {
            return "";
        }

        long seconds = duration.getSeconds();
        StringBuilder builder = new StringBuilder();

        ChronoUnit[] units = {ChronoUnit.YEARS, ChronoUnit.MONTHS, ChronoUnit.WEEKS,
                ChronoUnit.DAYS, ChronoUnit.HOURS, ChronoUnit.MINUTES, ChronoUnit.SECONDS};
        String[] labels = {"y", "mo", "w", "d", "h", "m", "s"};

        for (int i = 0; i < units.length; i++) {
            long unitSeconds = units[i].getDuration().getSeconds();
            long n = seconds / unitSeconds;
            if (n > 0) {
                seconds -= unitSeconds * n;
                if (builder.length() > 0) {
                    builder.append(" ");
                }
                builder.append(n).append(labels[i]);
            }
            if (seconds <= 0) {
                break;
            }
        }

        return builder.length() == 0 ? "0s" : builder.toString();
    }
}
