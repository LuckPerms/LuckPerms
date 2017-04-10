/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import me.lucko.luckperms.api.DataMutateResult;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.caching.MetaAccumulator;
import me.lucko.luckperms.common.core.NodeFactory;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.core.model.PermissionHolder;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.utils.ExtractedContexts;
import me.lucko.luckperms.sponge.service.proxy.LPSubject;
import me.lucko.luckperms.sponge.service.proxy.LPSubjectData;
import me.lucko.luckperms.sponge.service.references.SubjectReference;
import me.lucko.luckperms.sponge.timings.LPTiming;

import org.spongepowered.api.service.permission.PermissionService;

import co.aikar.timings.Timing;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    public Map<ImmutableContextSet, Map<String, Boolean>> getPermissions() {
        try (Timing ignored = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_GET_PERMISSIONS)) {
            Map<ImmutableContextSet, ImmutableMap.Builder<String, Boolean>> perms = new HashMap<>();

            for (Map.Entry<ImmutableContextSet, Collection<Node>> e : (enduring ? holder.getNodes() : holder.getTransientNodes()).asMap().entrySet()) {
                ImmutableMap.Builder<String, Boolean> results = ImmutableMap.builder();
                for (Node n : e.getValue()) {
                    results.put(n.getPermission(), n.getValue());
                }
                perms.put(e.getKey(), results);
            }

            ImmutableMap.Builder<ImmutableContextSet, Map<String, Boolean>> map = ImmutableMap.builder();
            for (Map.Entry<ImmutableContextSet, ImmutableMap.Builder<String, Boolean>> e : perms.entrySet()) {
                map.put(e.getKey(), e.getValue().build());
            }
            return map.build();
        }
    }

    @Override
    public boolean setPermission(@NonNull ContextSet contexts, @NonNull String permission, @NonNull Tristate tristate) {
        try (Timing i = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_SET_PERMISSION)) {
            if (tristate == Tristate.UNDEFINED) {
                // Unset
                Node node = NodeFactory.newBuilder(permission).withExtraContext(contexts).build();

                if (enduring) {
                    holder.unsetPermission(node);
                } else {
                    holder.unsetTransientPermission(node);
                }

                objectSave(holder);
                return true;
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

            objectSave(holder);
            return true;
        }
    }

    @Override
    public boolean clearPermissions() {
        try (Timing ignored = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_CLEAR_PERMISSIONS)) {
            boolean ret;
            if (enduring) {
                ret = holder.clearNodes();
            } else {
                ret = holder.clearTransientNodes();
            }

            if (!ret) {
                return false;
            }

            if (holder instanceof User) {
                service.getPlugin().getUserManager().giveDefaultIfNeeded(((User) holder), false);
            }

            objectSave(holder);
            return true;
        }
    }

    @Override
    public boolean clearPermissions(@NonNull ContextSet set) {
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
                return false;
            }

            if (holder instanceof User) {
                service.getPlugin().getUserManager().giveDefaultIfNeeded(((User) holder), false);
            }

            objectSave(holder);
            return true;
        }
    }

    @Override
    public Map<ImmutableContextSet, Set<SubjectReference>> getParents() {
        try (Timing ignored = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_GET_PARENTS)) {
            Map<ImmutableContextSet, ImmutableSet.Builder<SubjectReference>> parents = new HashMap<>();

            for (Map.Entry<ImmutableContextSet, Collection<Node>> e : (enduring ? holder.getNodes() : holder.getTransientNodes()).asMap().entrySet()) {
                ImmutableSet.Builder<SubjectReference> results = ImmutableSet.builder();
                for (Node n : e.getValue()) {
                    if (n.isGroupNode()) {
                        results.add(service.getGroupSubjects().get(n.getGroupName()).toReference());
                    }
                }
                parents.put(e.getKey(), results);
            }

            ImmutableMap.Builder<ImmutableContextSet, Set<SubjectReference>> map = ImmutableMap.builder();
            for (Map.Entry<ImmutableContextSet, ImmutableSet.Builder<SubjectReference>> e : parents.entrySet()) {
                map.put(e.getKey(), e.getValue().build());
            }
            return map.build();
        }
    }

    @Override
    public boolean addParent(@NonNull ContextSet contexts, @NonNull SubjectReference subject) {
        try (Timing i = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_ADD_PARENT)) {
            if (subject.getCollection().equals(PermissionService.SUBJECTS_GROUP)) {
                LPSubject permsSubject = subject.resolve(service);
                DataMutateResult result;

                if (enduring) {
                    result = holder.setPermission(NodeFactory.newBuilder("group." + permsSubject.getIdentifier())
                            .withExtraContext(contexts)
                            .build());
                } else {
                    result = holder.setTransientPermission(NodeFactory.newBuilder("group." + permsSubject.getIdentifier())
                            .withExtraContext(contexts)
                            .build());
                }

                if (!result.asBoolean()) {
                    return false;
                }

                objectSave(holder);
                return true;
            }
            return false;
        }
    }

    @Override
    public boolean removeParent(@NonNull ContextSet contexts, @NonNull SubjectReference subject) {
        try (Timing i = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_REMOVE_PARENT)) {
            if (subject.getCollection().equals(PermissionService.SUBJECTS_GROUP)) {
                LPSubject permsSubject = subject.resolve(service);
                DataMutateResult result;

                if (enduring) {
                    result = holder.unsetPermission(NodeFactory.newBuilder("group." + permsSubject.getIdentifier())
                            .withExtraContext(contexts)
                            .build());
                } else {
                    result = holder.unsetTransientPermission(NodeFactory.newBuilder("group." + permsSubject.getIdentifier())
                            .withExtraContext(contexts)
                            .build());
                }

                if (!result.asBoolean()) {
                    return false;
                }

                objectSave(holder);
                return true;
            }
            return false;
        }
    }

    @Override
    public boolean clearParents() {
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
                return false;
            }

            objectSave(holder);
            return true;
        }
    }

    @Override
    public boolean clearParents(@NonNull ContextSet set) {
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
                return false;
            }

            objectSave(holder);
            return true;
        }
    }

    @Override
    public Map<ImmutableContextSet, Map<String, String>> getOptions() {
        try (Timing ignored = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_GET_OPTIONS)) {
            Map<ImmutableContextSet, Map<String, String>> options = new HashMap<>();
            Map<ImmutableContextSet, Integer> minPrefixPriority = new HashMap<>();
            Map<ImmutableContextSet, Integer> minSuffixPriority = new HashMap<>();

            for (Node n : enduring ? holder.getNodes().values() : holder.getTransientNodes().values()) {
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

            ImmutableMap.Builder<ImmutableContextSet, Map<String, String>> map = ImmutableMap.builder();
            for (Map.Entry<ImmutableContextSet, Map<String, String>> e : options.entrySet()) {
                map.put(e.getKey(), ImmutableMap.copyOf(e.getValue()));
            }
            return map.build();
        }
    }

    @Override
    public boolean setOption(@NonNull ContextSet context, @NonNull String key, @NonNull String value) {
        try (Timing i = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_SET_OPTION)) {
            if (key.equalsIgnoreCase("prefix") || key.equalsIgnoreCase("suffix")) {
                // special handling.
                String type = key.toLowerCase();
                boolean prefix = type.equals("prefix");

                // remove all prefixes/suffixes from the user
                List<Node> toRemove = streamNodes(enduring)
                        .filter(n -> prefix ? n.isPrefix() : n.isSuffix())
                        .filter(n -> n.getFullContexts().equals(context))
                        .collect(Collectors.toList());

                toRemove.forEach(makeUnsetConsumer(enduring));

                MetaAccumulator metaAccumulator = holder.accumulateMeta(null, null, ExtractedContexts.generate(service.calculateContexts(context)));
                int priority = (type.equals("prefix") ? metaAccumulator.getPrefixes() : metaAccumulator.getSuffixes()).keySet().stream()
                        .mapToInt(e -> e).max().orElse(0);
                priority += 10;

                if (enduring) {
                    holder.setPermission(NodeFactory.makeChatMetaNode(type.equals("prefix"), priority, value).withExtraContext(context).build());
                } else {
                    holder.setTransientPermission(NodeFactory.makeChatMetaNode(type.equals("prefix"), priority, value).withExtraContext(context).build());
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

            objectSave(holder);
            return true;
        }
    }

    @Override
    public boolean unsetOption(ContextSet set, String key) {
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

            objectSave(holder);
            return true;
        }
    }

    @Override
    public boolean clearOptions(@NonNull ContextSet set) {
        try (Timing i = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_CLEAR_OPTIONS)) {
            List<Node> toRemove = streamNodes(enduring)
                    .filter(n -> n.isMeta() || n.isPrefix() || n.isSuffix())
                    .filter(n -> n.getFullContexts().equals(set))
                    .collect(Collectors.toList());

            toRemove.forEach(makeUnsetConsumer(enduring));

            objectSave(holder);
            return !toRemove.isEmpty();
        }
    }

    @Override
    public boolean clearOptions() {
        try (Timing i = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_CLEAR_OPTIONS)) {
            List<Node> toRemove = streamNodes(enduring)
                    .filter(n -> n.isMeta() || n.isPrefix() || n.isSuffix())
                    .collect(Collectors.toList());

            toRemove.forEach(makeUnsetConsumer(enduring));

            objectSave(holder);
            return !toRemove.isEmpty();
        }
    }

    private Stream<Node> streamNodes(boolean enduring) {
        return (enduring ? holder.getNodes() : holder.getTransientNodes()).values().stream();
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

    private void objectSave(PermissionHolder t) {
        if (!enduring) {
            // don't bother saving to primary storage. just refresh
            if (t instanceof User) {
                ((User) t).getRefreshBuffer().request();
            } else {
                service.getPlugin().getUpdateTaskBuffer().request();
            }
        } else {
            if (t instanceof User) {
                service.getPlugin().getStorage().saveUser(((User) t))
                        .thenRunAsync(() -> ((User) t).getRefreshBuffer().request(), service.getPlugin().getScheduler().getAsyncExecutor());
            } else {
                service.getPlugin().getStorage().saveGroup((Group) t)
                        .thenRunAsync(() -> service.getPlugin().getUpdateTaskBuffer().request(), service.getPlugin().getScheduler().getAsyncExecutor());
            }
        }
    }
}
