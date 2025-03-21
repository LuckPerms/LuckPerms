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

package me.lucko.luckperms.common.storage.implementation.rest;

import com.google.common.collect.ImmutableList;
import me.lucko.luckperms.common.actionlog.LogPage;
import me.lucko.luckperms.common.actionlog.LoggedAction;
import me.lucko.luckperms.common.actionlog.filter.ActionFields;
import me.lucko.luckperms.common.bulkupdate.BulkUpdate;
import me.lucko.luckperms.common.context.ImmutableContextSetImpl;
import me.lucko.luckperms.common.filter.Comparison;
import me.lucko.luckperms.common.filter.Constraint;
import me.lucko.luckperms.common.filter.Filter;
import me.lucko.luckperms.common.filter.FilterField;
import me.lucko.luckperms.common.filter.FilterList;
import me.lucko.luckperms.common.filter.PageParameters;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.factory.NodeBuilders;
import me.lucko.luckperms.common.node.matcher.ConstraintNodeMatcher;
import me.lucko.luckperms.common.node.matcher.StandardNodeMatchers;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.StorageMetadata;
import me.lucko.luckperms.common.storage.implementation.StorageImplementation;
import me.lucko.luckperms.common.storage.misc.NodeEntry;
import me.lucko.luckperms.common.storage.misc.PlayerSaveResultImpl;
import me.lucko.luckperms.common.util.Difference;
import me.lucko.luckperms.common.util.Iterators;
import net.luckperms.api.actionlog.Action;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.model.PlayerSaveResult;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeBuilder;
import net.luckperms.api.node.NodeType;
import net.luckperms.rest.LuckPermsRestClient;
import net.luckperms.rest.model.ActionPage;
import net.luckperms.rest.model.Context;
import net.luckperms.rest.model.CreateGroupRequest;
import net.luckperms.rest.model.CreateTrackRequest;
import net.luckperms.rest.model.CreateUserRequest;
import net.luckperms.rest.model.GroupSearchResult;
import net.luckperms.rest.model.Health;
import net.luckperms.rest.model.UpdateTrackRequest;
import net.luckperms.rest.model.UpdateUserRequest;
import net.luckperms.rest.model.UserLookupResult;
import net.luckperms.rest.model.UserSearchResult;
import org.checkerframework.checker.nullness.qual.Nullable;
import retrofit2.Response;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class RestStorage implements StorageImplementation {
    private final LuckPermsPlugin plugin;
    private final LuckPermsRestClient client;

    public RestStorage(LuckPermsPlugin plugin, String baseUrl, String apiKey) {
        this.plugin = plugin;
        this.client = LuckPermsRestClient.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
    }

    @Override
    public LuckPermsPlugin getPlugin() {
        return this.plugin;
    }

    @Override
    public String getImplementationName() {
        return "REST";
    }

    @Override
    public void init() throws IOException {
        Health health = this.client.misc().health().execute().body();
        if (health == null || !health.healthy()) {
            this.plugin.getLogger().warn("REST storage endpoint is unhealthy");
        }
    }

    @Override
    public void shutdown() {
        this.client.close();
    }

    @Override
    public StorageMetadata getMeta() {
        StorageMetadata metadata = new StorageMetadata();

        boolean success = true;
        long start = System.currentTimeMillis();
        try {
            Health health = this.client.misc().health().execute().body();
            if (health == null || !health.healthy()) {
                success = false;
            }
        } catch (IOException e) {
            success = false;
        }

        if (success) {
            int duration = (int) (System.currentTimeMillis() - start);
            metadata.ping(duration);
        }

        metadata.connected(success);
        return metadata;
    }

    @Override
    public void logAction(Action entry) throws IOException {
        this.client.actions().submit(convertAction(entry)).execute();
    }

    @Override
    public LogPage getLogPage(FilterList<Action> filters, @Nullable PageParameters page) throws Exception {
        Response<ActionPage> resp = null;

        if (filters.isEmpty()) {
            // ActionFilters.all()
            resp = page != null
                    ? this.client.actions().query(page.pageSize(), page.pageNumber()).execute()
                    : this.client.actions().query().execute();

        } else if (filters.size() == 1) {
            // ActionFilters.source(uniqueId)
            Filter<Action, ?> filter = filters.get(0);
            if (filter.field() == ActionFields.SOURCE_UNIQUE_ID && filter.constraint().comparison() == Comparison.EQUAL) {
                UUID uniqueId = (UUID) filter.constraint().value();
                resp = page != null
                        ? this.client.actions().querySource(uniqueId, page.pageSize(), page.pageNumber()).execute()
                        : this.client.actions().querySource(uniqueId).execute();
            }

        } else if (filters.operator() == FilterList.LogicalOperator.AND && filters.size() == 2) {
            Filter<Action, ?> filterA = filters.get(0);
            Filter<Action, ?> filterB = filters.get(1);

            if (filterA.field() == ActionFields.TARGET_TYPE && filterA.constraint().comparison() == Comparison.EQUAL) {
                Action.Target.Type type = (Action.Target.Type) filterA.constraint().value();

                if (type == Action.Target.Type.USER) {
                    // ActionFilters.user(uniqueId)
                    if (filterB.field() == ActionFields.TARGET_UNIQUE_ID && filterB.constraint().comparison() == Comparison.EQUAL) {
                        UUID uniqueId = (UUID) filterB.constraint().value();
                        resp = page != null
                                ? this.client.actions().queryTargetUser(uniqueId, page.pageSize(), page.pageNumber()).execute()
                                : this.client.actions().queryTargetUser(uniqueId).execute();
                    }
                } else if (type == Action.Target.Type.GROUP) {
                    // ActionFilters.group(name)
                    if (filterB.field() == ActionFields.TARGET_NAME && filterB.constraint().comparison() == Comparison.EQUAL) {
                        String name = (String) filterB.constraint().value();
                        resp = page != null
                                ? this.client.actions().queryTargetGroup(name, page.pageSize(), page.pageNumber()).execute()
                                : this.client.actions().queryTargetGroup(name).execute();
                    }
                } else if (type == Action.Target.Type.TRACK) {
                    // ActionFilters.track(name)
                    if (filterB.field() == ActionFields.TARGET_NAME && filterB.constraint().comparison() == Comparison.EQUAL) {
                        String name = (String) filterB.constraint().value();
                        resp = page != null
                                ? this.client.actions().queryTargetTrack(name, page.pageSize(), page.pageNumber()).execute()
                                : this.client.actions().queryTargetTrack(name).execute();
                    }
                }
            }

        } else if (filters.operator() == FilterList.LogicalOperator.OR && filters.size() == 3) {
            // ActionFilters.search(query)
            ImmutableList<FilterField<Action, String>> searchFields = ImmutableList.of(ActionFields.SOURCE_NAME, ActionFields.TARGET_NAME, ActionFields.DESCRIPTION);
            if (filters.stream().allMatch(filter -> filter.constraint().comparison() == Comparison.SIMILAR && searchFields.contains(filter.field()))) {
                String query = (String) filters.get(0).constraint().value();
                if (!query.startsWith("%") || !query.endsWith("%")) {
                    throw new IllegalArgumentException("Unsupported search query: " + query);
                }
                String searchTerm = query.substring(1, query.length() - 1);

                resp = page != null
                        ? this.client.actions().querySearch(searchTerm, page.pageSize(), page.pageNumber()).execute()
                        : this.client.actions().querySearch(searchTerm).execute();
            }
        }

        if (resp == null) {
            throw new UnsupportedOperationException("Unsupported filter: " + filters);
        }

        ActionPage body = Objects.requireNonNull(resp.body(), "resp.body()");
        return LogPage.of(
                body.entries().stream().map(RestStorage::convertAction).collect(Collectors.toList()),
                page,
                body.overallSize()
        );
    }

    @Override
    public void applyBulkUpdate(BulkUpdate bulkUpdate) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public User loadUser(UUID uniqueId, String username) throws Exception {
        net.luckperms.rest.model.User remoteUser = this.client.users().get(uniqueId).execute().body();
        if (remoteUser == null) {
            throw new IllegalStateException("Client did not return a user for " + uniqueId);
        }

        User user = this.plugin.getUserManager().getOrMake(uniqueId, username);
        user.setUsername(remoteUser.username(), true);
        user.loadNodesFromStorage(remoteUser.nodes().stream().map(RestStorage::convertNode).collect(Collectors.toList()));

        return user;
    }

    @Override
    public Map<UUID, User> loadUsers(Set<UUID> uniqueIds) throws Exception {
        return uniqueIds.parallelStream()
                .map(uniqueId -> {
                    try {
                        return loadUser(uniqueId, null);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toMap(User::getUniqueId, Function.identity()));
    }

    @Override
    public void saveUser(User user) throws Exception {
        Difference<Node> changes = user.normalData().exportChanges(results -> {
            if (this.plugin.getUserManager().isNonDefaultUser(user)) {
                return true;
            }

            // if the only change is adding the default node, we don't need to export
            if (results.getChanges().size() == 1) {
                Difference.Change<Node> onlyChange = results.getChanges().iterator().next();
                return !(onlyChange.type() == Difference.ChangeType.ADD && this.plugin.getUserManager().isDefaultNode(onlyChange.value()));
            }

            return true;
        });
        if (changes == null) {
            return;
        }

        String username = user.getUsername().orElse(null);
        if (username != null) {
            this.client.users().update(user.getUniqueId(), new UpdateUserRequest(username)).execute();
        }

        Set<Node> added = changes.getAdded();
        Set<Node> removed = changes.getRemoved();
        
        if (!removed.isEmpty()) {
            this.client.users().nodesDelete(user.getUniqueId(), removed.stream().map(RestStorage::convertNode).collect(Collectors.toList())).execute();
        }
        if (!added.isEmpty()) {
            this.client.users().nodesAdd(user.getUniqueId(), added.stream().map(RestStorage::convertNode).collect(Collectors.toList())).execute();
        }
    }

    @Override
    public Set<UUID> getUniqueUsers() throws Exception {
        return this.client.users().list().execute().body();
    }

    @Override
    public <N extends Node> List<NodeEntry<UUID, N>> searchUserNodes(ConstraintNodeMatcher<N> matcher) throws Exception {
        List<UserSearchResult> results;
        if (matcher instanceof StandardNodeMatchers.TypeEquals) {
            NodeType<? extends N> type = ((StandardNodeMatchers.TypeEquals<N>) matcher).getType();
            results = this.client.users().searchNodesByType(convertNodeType(type)).execute().body();
        } else {
            Constraint<String> constraint = matcher.getConstraint();
            Comparison comparison = constraint.comparison();
            String value = constraint.value();

            if (comparison == Comparison.EQUAL) {
                results = this.client.users().searchNodesByKey(value).execute().body();
            } else if (comparison == Comparison.SIMILAR) {
                long wildcards = value.chars().filter(ch -> ch == '%').count();
                if (wildcards == 0) {
                    results = this.client.users().searchNodesByKey(value).execute().body();
                } else if (wildcards == 1 && value.endsWith("%")) {
                    results = this.client.users().searchNodesByKeyStartsWith(value.substring(0, value.length() - 1)).execute().body();
                } else {
                    throw new UnsupportedOperationException("Unsupported constraint: " + constraint);
                }
            } else {
                throw new UnsupportedOperationException("Unsupported constraint: " + constraint);
            }
        }

        if (results == null) {
            throw new IllegalStateException("Client returned null results");
        }

        List<NodeEntry<UUID, N>> held = new ArrayList<>();
        for (UserSearchResult result : results) {
            UUID uniqueId = result.uniqueId();
            for (net.luckperms.rest.model.Node node : result.results()) {
                N match = matcher.filterConstraintMatch(convertNode(node));
                if (match != null) {
                    held.add(NodeEntry.of(uniqueId, match));
                }
            }
        }
        return held;
    }

    @Override
    public Group createAndLoadGroup(String name) throws Exception {
        net.luckperms.rest.model.Group remoteGroup = this.client.groups().create(new CreateGroupRequest(name)).execute().body();
        if (remoteGroup == null) {
            remoteGroup = this.client.groups().get(name).execute().body();
            if (remoteGroup == null) {
                throw new IllegalStateException("Unable to create group: " + name);
            }
        }

        Group group = this.plugin.getGroupManager().getOrMake(name);
        group.loadNodesFromStorage(remoteGroup.nodes().stream().map(RestStorage::convertNode).collect(Collectors.toList()));
        return group;
    }

    @Override
    public Optional<Group> loadGroup(String name) throws Exception {
        net.luckperms.rest.model.Group remoteGroup = this.client.groups().get(name).execute().body();
        if (remoteGroup == null) {
            return Optional.empty();
        }
        
        Group group = this.plugin.getGroupManager().getOrMake(name);
        group.loadNodesFromStorage(remoteGroup.nodes().stream().map(RestStorage::convertNode).collect(Collectors.toList()));
        return Optional.of(group);
    }

    @Override
    public void loadAllGroups() throws Exception {
        Set<String> groups = this.client.groups().list().execute().body();
        if (groups == null) {
            throw new IllegalStateException("Client returned a null list of groups");
        }

        if (!Iterators.tryIterate(groups, this::loadGroup)) {
            throw new RuntimeException("Exception occurred whilst loading a group");
        }

        this.plugin.getGroupManager().retainAll(groups);
    }

    @Override
    public void saveGroup(Group group) throws Exception {
        Difference<Node> changes = group.normalData().exportChanges(c -> true);

        Set<Node> added = changes.getAdded();
        Set<Node> removed = changes.getRemoved();

        if (!removed.isEmpty()) {
            this.client.groups().nodesDelete(group.getName(), removed.stream().map(RestStorage::convertNode).collect(Collectors.toList())).execute();
        }
        if (!added.isEmpty()) {
            this.client.groups().nodesAdd(group.getName(), added.stream().map(RestStorage::convertNode).collect(Collectors.toList())).execute();
        }
    }

    @Override
    public void deleteGroup(Group group) throws Exception {
        this.client.groups().delete(group.getName()).execute();
    }

    @Override
    public <N extends Node> List<NodeEntry<String, N>> searchGroupNodes(ConstraintNodeMatcher<N> matcher) throws Exception {
        List<GroupSearchResult> results;
        if (matcher instanceof StandardNodeMatchers.TypeEquals) {
            NodeType<? extends N> type = ((StandardNodeMatchers.TypeEquals<N>) matcher).getType();
            results = this.client.groups().searchNodesByType(convertNodeType(type)).execute().body();
        } else {
            Constraint<String> constraint = matcher.getConstraint();
            Comparison comparison = constraint.comparison();
            String value = constraint.value();

            if (comparison == Comparison.EQUAL) {
                results = this.client.groups().searchNodesByKey(value).execute().body();
            } else if (comparison == Comparison.SIMILAR) {
                long wildcards = value.chars().filter(ch -> ch == '%').count();
                if (wildcards == 0) {
                    results = this.client.groups().searchNodesByKey(value).execute().body();
                } else if (wildcards == 1 && value.endsWith("%")) {
                    results = this.client.groups().searchNodesByKeyStartsWith(value.substring(0, value.length() - 1)).execute().body();
                } else {
                    throw new UnsupportedOperationException("Unsupported constraint: " + constraint);
                }
            } else {
                throw new UnsupportedOperationException("Unsupported constraint: " + constraint);
            }
        }

        if (results == null) {
            throw new IllegalStateException("Client returned null results");
        }

        List<NodeEntry<String, N>> held = new ArrayList<>();
        for (GroupSearchResult result : results) {
            String name = result.name();
            for (net.luckperms.rest.model.Node node : result.results()) {
                N match = matcher.filterConstraintMatch(convertNode(node));
                if (match != null) {
                    held.add(NodeEntry.of(name, match));
                }
            }
        }
        return held;
    }

    @Override
    public Track createAndLoadTrack(String name) throws Exception {
        net.luckperms.rest.model.Track remoteTrack = this.client.tracks().create(new CreateTrackRequest(name)).execute().body();
        if (remoteTrack == null) {
            remoteTrack = this.client.tracks().get(name).execute().body();
            if (remoteTrack == null) {
                throw new IllegalStateException("Unable to create track: " + name);
            }
        }

        Track track = this.plugin.getTrackManager().getOrMake(name);
        track.setGroups(remoteTrack.groups());
        return track;
    }

    @Override
    public Optional<Track> loadTrack(String name) throws Exception {
        net.luckperms.rest.model.Track remoteTrack = this.client.tracks().get(name).execute().body();
        if (remoteTrack == null) {
            return Optional.empty();
        }

        Track track = this.plugin.getTrackManager().getOrMake(name);
        track.setGroups(remoteTrack.groups());
        return Optional.of(track);
    }

    @Override
    public void loadAllTracks() throws Exception {
        Set<String> tracks = this.client.tracks().list().execute().body();
        if (tracks == null) {
            throw new IllegalStateException("Client returned a null list of tracks");
        }

        if (!Iterators.tryIterate(tracks, this::loadTrack)) {
            throw new RuntimeException("Exception occurred whilst loading a track");
        }

        this.plugin.getTrackManager().retainAll(tracks);
    }

    @Override
    public void saveTrack(Track track) throws Exception {
        this.client.tracks().update(track.getName(), new UpdateTrackRequest(track.getGroups())).execute();
    }

    @Override
    public void deleteTrack(Track track) throws Exception {
        this.client.tracks().delete(track.getName()).execute();
    }

    @Override
    public PlayerSaveResult savePlayerData(UUID uniqueId, String username) throws Exception {
        net.luckperms.rest.model.PlayerSaveResult remoteResult = this.client.users().create(new CreateUserRequest(uniqueId, username)).execute().body();

        Set<PlayerSaveResult.Outcome> outcomes = remoteResult.outcomes().stream().map(outcome -> {
            switch (outcome) {
                case CLEAN_INSERT:
                    return PlayerSaveResult.Outcome.CLEAN_INSERT;
                case NO_CHANGE:
                    return PlayerSaveResult.Outcome.NO_CHANGE;
                case USERNAME_UPDATED:
                    return PlayerSaveResult.Outcome.USERNAME_UPDATED;
                case OTHER_UNIQUE_IDS_PRESENT_FOR_USERNAME:
                    return PlayerSaveResult.Outcome.OTHER_UNIQUE_IDS_PRESENT_FOR_USERNAME;
                default:
                    throw new AssertionError(outcome);
            }
        }).collect(Collectors.toSet());

        if (outcomes.isEmpty()) {
            throw new IllegalStateException("No outcomes returned");
        }

        PlayerSaveResultImpl result;
        if (outcomes.contains(PlayerSaveResult.Outcome.CLEAN_INSERT)) {
            result = PlayerSaveResultImpl.cleanInsert();
        } else if (outcomes.contains(PlayerSaveResult.Outcome.NO_CHANGE)) {
            result = PlayerSaveResultImpl.noChange();
        } else if (outcomes.contains(PlayerSaveResult.Outcome.USERNAME_UPDATED)) {
            result = PlayerSaveResultImpl.usernameUpdated(remoteResult.previousUsername());
        } else {
            throw new IllegalStateException("No base outcome returned");
        }

        if (outcomes.contains(PlayerSaveResult.Outcome.OTHER_UNIQUE_IDS_PRESENT_FOR_USERNAME)) {
            result = result.withOtherUuidsPresent(remoteResult.otherUniqueIds());
        }

        return result;
    }

    @Override
    public void deletePlayerData(UUID uniqueId) throws Exception {
        this.client.users().delete(uniqueId, true).execute();
    }

    @Override
    public @Nullable UUID getPlayerUniqueId(String username) throws Exception {
        UserLookupResult result = this.client.users().lookup(username).execute().body();
        return result == null ? null : result.uniqueId();
    }

    @Override
    public @Nullable String getPlayerName(UUID uniqueId) throws Exception {
        UserLookupResult result = this.client.users().lookup(uniqueId).execute().body();
        return result == null ? null : result.username();
    }

    private static net.luckperms.rest.model.Action convertAction(Action action) {
        return new net.luckperms.rest.model.Action(
                action.getTimestamp().getEpochSecond(),
                new net.luckperms.rest.model.Action.Source(
                        action.getSource().getUniqueId(),
                        action.getSource().getName()
                ),
                new net.luckperms.rest.model.Action.Target(
                        action.getTarget().getUniqueId().orElse(null),
                        action.getTarget().getName(),
                        convertActionTargetType(action.getTarget().getType())
                ),
                action.getDescription()
        );
    }

    private static net.luckperms.rest.model.Action.Target.Type convertActionTargetType(Action.Target.Type type) {
        switch (type) {
            case USER:
                return net.luckperms.rest.model.Action.Target.Type.USER;
            case GROUP:
                return net.luckperms.rest.model.Action.Target.Type.GROUP;
            case TRACK:
                return net.luckperms.rest.model.Action.Target.Type.TRACK;
            default:
                throw new AssertionError(type);
        }
    }

    private static LoggedAction convertAction(net.luckperms.rest.model.Action action) {
        return LoggedAction.build()
                .timestamp(Instant.ofEpochSecond(action.timestamp()))
                .source(action.source().uniqueId())
                .sourceName(action.source().name())
                .target(action.target().uniqueId())
                .targetName(action.target().name())
                .targetType(convertActionTargetType(action.target().type()))
                .description(action.description())
                .build();
    }

    private static Action.Target.Type convertActionTargetType(net.luckperms.rest.model.Action.Target.Type type) {
        switch (type) {
            case USER:
                return Action.Target.Type.USER;
            case GROUP:
                return Action.Target.Type.GROUP;
            case TRACK:
                return Action.Target.Type.TRACK;
            default:
                throw new AssertionError(type);
        }
    }

    private static Node convertNode(net.luckperms.rest.model.Node node) {
        NodeBuilder<?, ?> builder = NodeBuilders.determineMostApplicable(node.key());
        if (node.value() != null) {
            builder.value(node.value());
        }
        if (node.context() != null) {
            builder.context(convertContexts(node.context()));
        }
        if (node.expiry() != null) {
            builder.expiry(node.expiry());
        }
        return builder.build();
    }

    private static net.luckperms.rest.model.Node convertNode(Node node) {
        return new net.luckperms.rest.model.Node(
                node.getKey(),
                node.getValue(),
                convertContexts(node.getContexts()),
                node.getExpiry() == null ? null : node.getExpiry().getEpochSecond()
        );
    }

    public static ContextSet convertContexts(Set<Context> contexts) {
        ImmutableContextSetImpl.BuilderImpl builder = new ImmutableContextSetImpl.BuilderImpl();
        for (Context context : contexts) {
            builder.add(context.key(), context.value());
        }
        return builder.build();
    }

    public static Set<Context> convertContexts(ContextSet contexts) {
        return StreamSupport.stream(contexts.spliterator(), false)
                .map(c -> new Context(c.getKey(), c.getValue()))
                .collect(Collectors.toSet());
    }

    public static net.luckperms.rest.model.NodeType convertNodeType(NodeType<?> type) {
        if (type == NodeType.REGEX_PERMISSION) return net.luckperms.rest.model.NodeType.REGEX_PERMISSION;
        if (type == NodeType.INHERITANCE) return net.luckperms.rest.model.NodeType.INHERITANCE;
        if (type == NodeType.PREFIX) return net.luckperms.rest.model.NodeType.PREFIX;
        if (type == NodeType.SUFFIX) return net.luckperms.rest.model.NodeType.SUFFIX;
        if (type == NodeType.META) return net.luckperms.rest.model.NodeType.META;
        if (type == NodeType.WEIGHT) return net.luckperms.rest.model.NodeType.WEIGHT;
        if (type == NodeType.DISPLAY_NAME) return net.luckperms.rest.model.NodeType.DISPLAY_NAME;
        throw new IllegalArgumentException("Invalid type: " + type.name());
    }
}
