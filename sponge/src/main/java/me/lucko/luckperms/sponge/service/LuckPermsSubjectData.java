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

package me.lucko.luckperms.sponge.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import me.lucko.luckperms.api.ChatMetaType;
import me.lucko.luckperms.api.DataMutateResult;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.caching.MetaAccumulator;
import me.lucko.luckperms.common.contexts.ExtractedContexts;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.NodeFactory;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectData;
import me.lucko.luckperms.sponge.service.model.SubjectReference;
import me.lucko.luckperms.sponge.timings.LPTiming;

import org.spongepowered.api.service.permission.PermissionService;

import co.aikar.timings.Timing;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"OptionalGetWithoutIsPresent", "unused"})
@AllArgsConstructor
public class LuckPermsSubjectData implements LPSubjectData {
    private final boolean enduring;
    private final LuckPermsService service;

    @Getter
    private final PermissionHolder holder;

    @Getter
    LPSubject parentSubject;

    @Override
    public ImmutableMap<ImmutableContextSet, ImmutableMap<String, Boolean>> getAllPermissions() {
        try (Timing ignored = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_GET_PERMISSIONS)) {
            Map<ImmutableContextSet, ImmutableMap.Builder<String, Boolean>> perms = new HashMap<>();

            for (Map.Entry<ImmutableContextSet, Collection<Node>> e : (enduring ? holder.getEnduringNodes() : holder.getTransientNodes()).asMap().entrySet()) {
                ImmutableMap.Builder<String, Boolean> results = ImmutableMap.builder();
                for (Node n : e.getValue()) {
                    results.put(n.getPermission(), n.getValue());
                }
                perms.put(e.getKey(), results);
            }

            ImmutableMap.Builder<ImmutableContextSet, ImmutableMap<String, Boolean>> map = ImmutableMap.builder();
            for (Map.Entry<ImmutableContextSet, ImmutableMap.Builder<String, Boolean>> e : perms.entrySet()) {
                map.put(e.getKey(), e.getValue().build());
            }
            return map.build();
        }
    }

    @Override
    public CompletableFuture<Boolean> setPermission(@NonNull ImmutableContextSet contexts, @NonNull String permission, @NonNull Tristate tristate) {
        try (Timing i = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_SET_PERMISSION)) {
            if (tristate == Tristate.UNDEFINED) {
                // Unset
                Node node = NodeFactory.newBuilder(permission).withExtraContext(contexts).build();

                if (enduring) {
                    holder.unsetPermission(node);
                } else {
                    holder.unsetTransientPermission(node);
                }

                return objectSave(holder).thenApply(v -> true);
            }

            Node node = NodeFactory.newBuilder(permission).setValue(tristate.asBoolean()).withExtraContext(contexts).build();

            // Workaround: unset the inverse, to allow false -> true, true -> false overrides.
            if (enduring) {
                holder.unsetPermission(node);
            } else {
                holder.unsetTransientPermission(node);
            }

            if (enduring) {
                holder.setPermission(node);
            } else {
                holder.setTransientPermission(node);
            }

            return objectSave(holder).thenApply(v -> true);
        }
    }

    @Override
    public CompletableFuture<Boolean> clearPermissions() {
        try (Timing ignored = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_CLEAR_PERMISSIONS)) {
            boolean ret;
            if (enduring) {
                ret = holder.clearNodes();
            } else {
                ret = holder.clearTransientNodes();
            }

            if (!ret) {
                return CompletableFuture.completedFuture(false);
            }

            if (holder instanceof User) {
                service.getPlugin().getUserManager().giveDefaultIfNeeded(((User) holder), false);
            }

            return objectSave(holder).thenApply(v -> true);
        }
    }

    @Override
    public CompletableFuture<Boolean> clearPermissions(@NonNull ImmutableContextSet set) {
        try (Timing i = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_CLEAR_PERMISSIONS)) {
            boolean ret;

            if (enduring) {
                ret = holder.clearNodes(set);
            } else {
                List<Node> toRemove = streamNodes(false)
                        .filter(n -> n.getFullContexts().equals(set))
                        .collect(Collectors.toList());

                toRemove.forEach(makeUnsetConsumer(false));
                ret = !toRemove.isEmpty();
            }

            if (!ret) {
                return CompletableFuture.completedFuture(false);
            }

            if (holder instanceof User) {
                service.getPlugin().getUserManager().giveDefaultIfNeeded(((User) holder), false);
            }

            return objectSave(holder).thenApply(v -> true);
        }
    }

    @Override
    public ImmutableMap<ImmutableContextSet, ImmutableList<SubjectReference>> getAllParents() {
        try (Timing ignored = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_GET_PARENTS)) {
            Map<ImmutableContextSet, ImmutableList.Builder<SubjectReference>> parents = new HashMap<>();

            for (Map.Entry<ImmutableContextSet, Collection<Node>> e : (enduring ? holder.getEnduringNodes() : holder.getTransientNodes()).asMap().entrySet()) {
                ImmutableList.Builder<SubjectReference> results = ImmutableList.builder();
                for (Node n : e.getValue()) {
                    if (n.isGroupNode()) {
                        results.add(service.getGroupSubjects().loadSubject(n.getGroupName()).join().toReference());
                    }
                }
                parents.put(e.getKey(), results);
            }

            ImmutableMap.Builder<ImmutableContextSet, ImmutableList<SubjectReference>> map = ImmutableMap.builder();
            for (Map.Entry<ImmutableContextSet, ImmutableList.Builder<SubjectReference>> e : parents.entrySet()) {
                map.put(e.getKey(), e.getValue().build());
            }
            return map.build();
        }
    }

    @Override
    public CompletableFuture<Boolean> addParent(@NonNull ImmutableContextSet contexts, @NonNull SubjectReference subject) {
        try (Timing i = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_ADD_PARENT)) {
            if (subject.getCollectionIdentifier().equals(PermissionService.SUBJECTS_GROUP)) {
                return subject.resolveLp().thenCompose(sub -> {
                    DataMutateResult result;

                    if (enduring) {
                        result = holder.setPermission(NodeFactory.newBuilder("group." + sub.getIdentifier())
                                .withExtraContext(contexts)
                                .build());
                    } else {
                        result = holder.setTransientPermission(NodeFactory.newBuilder("group." + sub.getIdentifier())
                                .withExtraContext(contexts)
                                .build());
                    }

                    if (!result.asBoolean()) {
                        return CompletableFuture.completedFuture(false);
                    }

                    return objectSave(holder).thenApply(v -> true);
                });
            }
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    public CompletableFuture<Boolean> removeParent(@NonNull ImmutableContextSet contexts, @NonNull SubjectReference subject) {
        try (Timing i = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_REMOVE_PARENT)) {
            if (subject.getCollectionIdentifier().equals(PermissionService.SUBJECTS_GROUP)) {
                subject.resolveLp().thenCompose(sub -> {
                    DataMutateResult result;

                    if (enduring) {
                        result = holder.unsetPermission(NodeFactory.newBuilder("group." + sub.getIdentifier())
                                .withExtraContext(contexts)
                                .build());
                    } else {
                        result = holder.unsetTransientPermission(NodeFactory.newBuilder("group." + sub.getIdentifier())
                                .withExtraContext(contexts)
                                .build());
                    }

                    if (!result.asBoolean()) {
                        return CompletableFuture.completedFuture(false);
                    }

                    return objectSave(holder).thenApply(v -> true);
                });
            }
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    public CompletableFuture<Boolean> clearParents() {
        try (Timing i = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_CLEAR_PARENTS)) {
            boolean ret;

            if (enduring) {
                ret = holder.clearParents(true);
            } else {
                List<Node> toRemove = streamNodes(false)
                        .filter(Node::isGroupNode)
                        .collect(Collectors.toList());

                toRemove.forEach(makeUnsetConsumer(false));
                ret = !toRemove.isEmpty();

                if (ret && holder instanceof User) {
                    service.getPlugin().getUserManager().giveDefaultIfNeeded(((User) holder), false);
                }
            }

            if (!ret) {
                return CompletableFuture.completedFuture(false);
            }

            return objectSave(holder).thenApply(v -> true);
        }
    }

    @Override
    public CompletableFuture<Boolean> clearParents(@NonNull ImmutableContextSet set) {
        try (Timing i = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_CLEAR_PARENTS)) {
            boolean ret;
            if (enduring) {
                ret = holder.clearParents(set, true);
            } else {
                List<Node> toRemove = streamNodes(false)
                        .filter(Node::isGroupNode)
                        .filter(n -> n.getFullContexts().equals(set))
                        .collect(Collectors.toList());

                toRemove.forEach(makeUnsetConsumer(false));
                ret = !toRemove.isEmpty();

                if (ret && holder instanceof User) {
                    service.getPlugin().getUserManager().giveDefaultIfNeeded(((User) holder), false);
                }
            }

            if (!ret) {
                return CompletableFuture.completedFuture(false);
            }

            return objectSave(holder).thenApply(v -> true);
        }
    }

    @Override
    public ImmutableMap<ImmutableContextSet, ImmutableMap<String, String>> getAllOptions() {
        try (Timing ignored = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_GET_OPTIONS)) {
            Map<ImmutableContextSet, Map<String, String>> options = new HashMap<>();
            Map<ImmutableContextSet, Integer> minPrefixPriority = new HashMap<>();
            Map<ImmutableContextSet, Integer> minSuffixPriority = new HashMap<>();

            for (Node n : enduring ? holder.getEnduringNodes().values() : holder.getTransientNodes().values()) {
                if (!n.getValue()) continue;
                if (!n.isMeta() && !n.isPrefix() && !n.isSuffix()) continue;

                ImmutableContextSet immutableContexts = n.getFullContexts().makeImmutable();

                if (!options.containsKey(immutableContexts)) {
                    options.put(immutableContexts, new HashMap<>());
                    minPrefixPriority.put(immutableContexts, Integer.MIN_VALUE);
                    minSuffixPriority.put(immutableContexts, Integer.MIN_VALUE);
                }

                if (n.isPrefix()) {
                    Map.Entry<Integer, String> value = n.getPrefix();
                    if (value.getKey() > minPrefixPriority.get(immutableContexts)) {
                        options.get(immutableContexts).put("prefix", value.getValue());
                        minPrefixPriority.put(immutableContexts, value.getKey());
                    }
                    continue;
                }

                if (n.isSuffix()) {
                    Map.Entry<Integer, String> value = n.getSuffix();
                    if (value.getKey() > minSuffixPriority.get(immutableContexts)) {
                        options.get(immutableContexts).put("suffix", value.getValue());
                        minSuffixPriority.put(immutableContexts, value.getKey());
                    }
                    continue;
                }

                if (n.isMeta()) {
                    Map.Entry<String, String> meta = n.getMeta();
                    options.get(immutableContexts).put(meta.getKey(), meta.getValue());
                }
            }

            ImmutableMap.Builder<ImmutableContextSet, ImmutableMap<String, String>> map = ImmutableMap.builder();
            for (Map.Entry<ImmutableContextSet, Map<String, String>> e : options.entrySet()) {
                map.put(e.getKey(), ImmutableMap.copyOf(e.getValue()));
            }
            return map.build();
        }
    }

    @Override
    public CompletableFuture<Boolean> setOption(@NonNull ImmutableContextSet context, @NonNull String key, @NonNull String value) {
        try (Timing i = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_SET_OPTION)) {
            if (key.equalsIgnoreCase("prefix") || key.equalsIgnoreCase("suffix")) {
                // special handling.
                ChatMetaType type = ChatMetaType.valueOf(key.toUpperCase());

                // remove all prefixes/suffixes from the user
                List<Node> toRemove = streamNodes(enduring)
                        .filter(type::matches)
                        .filter(n -> n.getFullContexts().equals(context))
                        .collect(Collectors.toList());

                toRemove.forEach(makeUnsetConsumer(enduring));

                MetaAccumulator metaAccumulator = holder.accumulateMeta(null, null, ExtractedContexts.generate(service.calculateContexts(context)));
                int priority = metaAccumulator.getChatMeta(type).keySet().stream().mapToInt(e -> e).max().orElse(0);
                priority += 10;

                if (enduring) {
                    holder.setPermission(NodeFactory.makeChatMetaNode(type, priority, value).withExtraContext(context).build());
                } else {
                    holder.setTransientPermission(NodeFactory.makeChatMetaNode(type, priority, value).withExtraContext(context).build());
                }

            } else {
                // standard remove
                List<Node> toRemove = streamNodes(enduring)
                        .filter(n -> n.isMeta() && n.getMeta().getKey().equals(key))
                        .filter(n -> n.getFullContexts().equals(context))
                        .collect(Collectors.toList());

                toRemove.forEach(makeUnsetConsumer(enduring));

                if (enduring) {
                    holder.setPermission(NodeFactory.makeMetaNode(key, value).withExtraContext(context).build());
                } else {
                    holder.setTransientPermission(NodeFactory.makeMetaNode(key, value).withExtraContext(context).build());
                }
            }

            return objectSave(holder).thenApply(v -> true);
        }
    }

    @Override
    public CompletableFuture<Boolean> unsetOption(ImmutableContextSet set, String key) {
        try (Timing i = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_SET_OPTION)) {
            List<Node> toRemove = streamNodes(enduring)
                    .filter(n -> {
                        if (key.equalsIgnoreCase("prefix")) {
                            return n.isPrefix();
                        } else if (key.equalsIgnoreCase("suffix")) {
                            return n.isSuffix();
                        } else {
                            return n.isMeta() && n.getMeta().getKey().equals(key);
                        }
                    })
                    .filter(n -> n.getFullContexts().equals(set))
                    .collect(Collectors.toList());

            toRemove.forEach(makeUnsetConsumer(enduring));

            return objectSave(holder).thenApply(v -> true);
        }
    }

    @Override
    public CompletableFuture<Boolean> clearOptions(@NonNull ImmutableContextSet set) {
        try (Timing i = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_CLEAR_OPTIONS)) {
            List<Node> toRemove = streamNodes(enduring)
                    .filter(n -> n.isMeta() || n.isPrefix() || n.isSuffix())
                    .filter(n -> n.getFullContexts().equals(set))
                    .collect(Collectors.toList());

            toRemove.forEach(makeUnsetConsumer(enduring));

            return objectSave(holder).thenApply(v -> !toRemove.isEmpty());
        }
    }

    @Override
    public CompletableFuture<Boolean> clearOptions() {
        try (Timing i = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_CLEAR_OPTIONS)) {
            List<Node> toRemove = streamNodes(enduring)
                    .filter(n -> n.isMeta() || n.isPrefix() || n.isSuffix())
                    .collect(Collectors.toList());

            toRemove.forEach(makeUnsetConsumer(enduring));

            return objectSave(holder).thenApply(v -> !toRemove.isEmpty());
        }
    }

    private Stream<Node> streamNodes(boolean enduring) {
        return (enduring ? holder.getEnduringNodes() : holder.getTransientNodes()).values().stream();
    }

    private Consumer<Node> makeUnsetConsumer(boolean enduring) {
        return n -> {
            if (enduring) {
                holder.unsetPermission(n);
            } else {
                holder.unsetTransientPermission(n);
            }
        };
    }

    private CompletableFuture<Void> objectSave(PermissionHolder t) {
        if (!enduring) {
            // don't bother saving to primary storage. just refresh
            if (t instanceof User) {
                User user = ((User) t);
                return user.getRefreshBuffer().request();
            } else {
                return service.getPlugin().getUpdateTaskBuffer().request();
            }
        } else {
            if (t instanceof User) {
                User user = ((User) t);
                return service.getPlugin().getStorage().saveUser(user).thenCombineAsync(user.getRefreshBuffer().request(), (b, v) -> v, service.getPlugin().getScheduler().async());
            } else {
                Group group = ((Group) t);
                return service.getPlugin().getStorage().saveGroup(group).thenCombineAsync(service.getPlugin().getUpdateTaskBuffer().request(), (b, v) -> v, service.getPlugin().getScheduler().async());
            }
        }
    }
}
