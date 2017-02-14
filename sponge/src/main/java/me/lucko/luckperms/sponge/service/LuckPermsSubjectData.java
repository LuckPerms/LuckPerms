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

import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.common.caching.MetaHolder;
import me.lucko.luckperms.common.core.NodeBuilder;
import me.lucko.luckperms.common.core.NodeFactory;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.core.model.PermissionHolder;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.utils.ExtractedContexts;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.sponge.service.base.LPSubject;
import me.lucko.luckperms.sponge.service.base.LPSubjectData;
import me.lucko.luckperms.sponge.service.references.SubjectReference;
import me.lucko.luckperms.sponge.timings.LPTiming;

import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.PermissionService;

import co.aikar.timings.Timing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings({"OptionalGetWithoutIsPresent", "unused"})
@AllArgsConstructor
public class LuckPermsSubjectData implements LPSubjectData {
    private final boolean enduring;
    private final LuckPermsService service;

    @Getter
    private final PermissionHolder holder;

    @Getter
    LPSubject parentSubject;

    private void objectSave(PermissionHolder t) {
        if (t instanceof User) {
            service.getPlugin().getStorage().saveUser(((User) t))
                    .thenRunAsync(() -> ((User) t).getRefreshBuffer().request(), service.getPlugin().getScheduler().getAsyncExecutor());
        }
        if (t instanceof Group) {
            service.getPlugin().getStorage().saveGroup((Group) t)
                    .thenRunAsync(() -> service.getPlugin().getUpdateTaskBuffer().request(), service.getPlugin().getScheduler().getAsyncExecutor());
        }
    }

    @Override
    public Map<ImmutableContextSet, Map<String, Boolean>> getPermissions() {
        try (Timing ignored = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_GET_PERMISSIONS)) {
            Map<ImmutableContextSet, Map<String, Boolean>> perms = new HashMap<>();

            for (Node n : enduring ? holder.getNodes() : holder.getTransientNodes()) {
                MutableContextSet contexts = MutableContextSet.fromSet(n.getContexts());

                if (n.isServerSpecific()) {
                    contexts.add(new Context(LuckPermsService.SERVER_CONTEXT, n.getServer().get()));
                }

                if (n.isWorldSpecific()) {
                    contexts.add(new Context(Context.WORLD_KEY, n.getWorld().get()));
                }

                perms.computeIfAbsent(contexts.makeImmutable(), cs -> new HashMap<>()).put(n.getPermission(), n.getValue());
            }

            ImmutableMap.Builder<ImmutableContextSet, Map<String, Boolean>> map = ImmutableMap.builder();
            for (Map.Entry<ImmutableContextSet, Map<String, Boolean>> e : perms.entrySet()) {
                map.put(e.getKey(), ImmutableMap.copyOf(e.getValue()));
            }
            return map.build();
        }
    }

    @Override
    public boolean setPermission(@NonNull ContextSet contexts, @NonNull String permission, @NonNull Tristate tristate) {
        try (Timing i = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_SET_PERMISSION)) {
            if (tristate == Tristate.UNDEFINED) {
                // Unset
                Node node = new NodeBuilder(permission).withExtraContext(contexts).build();

                try {
                    if (enduring) {
                        holder.unsetPermission(node);
                    } else {
                        holder.unsetTransientPermission(node);
                    }
                } catch (ObjectLacksException ignored) {
                }

                objectSave(holder);
                return true;
            }

            Node node = new NodeBuilder(permission).setValue(tristate.asBoolean()).withExtraContext(contexts).build();

            // Workaround: unset the inverse, to allow false -> true, true -> false overrides.
            try {
                if (enduring) {
                    holder.unsetPermission(node);
                } else {
                    holder.unsetTransientPermission(node);
                }
            } catch (ObjectLacksException ignored) {
            }

            try {
                if (enduring) {
                    holder.setPermission(node);
                } else {
                    holder.setTransientPermission(node);
                }
            } catch (ObjectAlreadyHasException ignored) {
            }

            objectSave(holder);
            return true;
        }
    }

    @Override
    public boolean clearPermissions() {
        try (Timing ignored = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_CLEAR_PERMISSIONS)) {
            if (enduring) {
                holder.clearNodes();
            } else {
                holder.clearTransientNodes();
            }
            objectSave(holder);
            return true;
        }
    }

    @Override
    public boolean clearPermissions(@NonNull ContextSet c) {
        try (Timing i = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_CLEAR_PERMISSIONS)) {
            List<Node> toRemove = new ArrayList<>();
            for (Node n : enduring ? holder.getNodes() : holder.getTransientNodes()) {
                MutableContextSet contexts = MutableContextSet.fromSet(n.getContexts());

                if (n.isServerSpecific()) {
                    contexts.add(new Context(LuckPermsService.SERVER_CONTEXT, n.getServer().get()));
                }

                if (n.isWorldSpecific()) {
                    contexts.add(new Context(Context.WORLD_KEY, n.getWorld().get()));
                }

                if (contexts.equals(c)) {
                    toRemove.add(n);
                }
            }

            toRemove.forEach(n -> {
                try {
                    if (enduring) {
                        holder.unsetPermission(n);
                    } else {
                        holder.unsetTransientPermission(n);
                    }
                } catch (ObjectLacksException ignored) {
                }
            });

            if (holder instanceof User) {
                service.getPlugin().getUserManager().giveDefaultIfNeeded(((User) holder), false);
            }

            objectSave(holder);
            return !toRemove.isEmpty();
        }
    }

    @Override
    public Map<ImmutableContextSet, Set<SubjectReference>> getParents() {
        try (Timing ignored = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_GET_PARENTS)) {
            Map<ImmutableContextSet, Set<SubjectReference>> parents = new HashMap<>();

            for (Node n : enduring ? holder.getNodes() : holder.getTransientNodes()) {
                if (!n.isGroupNode()) {
                    continue;
                }

                MutableContextSet contexts = MutableContextSet.fromSet(n.getContexts());

                if (n.isServerSpecific()) {
                    contexts.add(new Context(LuckPermsService.SERVER_CONTEXT, n.getServer().get()));
                }

                if (n.isWorldSpecific()) {
                    contexts.add(new Context(Context.WORLD_KEY, n.getWorld().get()));
                }

                parents.computeIfAbsent(contexts.makeImmutable(), cs -> new HashSet<>()).add(service.getGroupSubjects().get(n.getGroupName()).toReference());
            }

            ImmutableMap.Builder<ImmutableContextSet, Set<SubjectReference>> map = ImmutableMap.builder();
            for (Map.Entry<ImmutableContextSet, Set<SubjectReference>> e : parents.entrySet()) {
                map.put(e.getKey(), ImmutableSet.copyOf(e.getValue()));
            }
            return map.build();
        }
    }

    @Override
    public boolean addParent(@NonNull ContextSet contexts, @NonNull SubjectReference subject) {
        try (Timing i = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_ADD_PARENT)) {
            if (subject.getCollection().equals(PermissionService.SUBJECTS_GROUP)) {
                LPSubject permsSubject = subject.resolve(service);

                try {
                    if (enduring) {
                        holder.setPermission(new NodeBuilder("group." + permsSubject.getIdentifier())
                                .withExtraContext(contexts)
                                .build());
                    } else {
                        holder.setTransientPermission(new NodeBuilder("group." + permsSubject.getIdentifier())
                                .withExtraContext(contexts)
                                .build());
                    }
                } catch (ObjectAlreadyHasException ignored) {
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

                try {
                    if (enduring) {
                        holder.unsetPermission(new NodeBuilder("group." + permsSubject.getIdentifier())
                                .withExtraContext(contexts)
                                .build());
                    } else {
                        holder.unsetTransientPermission(new NodeBuilder("group." + permsSubject.getIdentifier())
                                .withExtraContext(contexts)
                                .build());
                    }
                } catch (ObjectLacksException ignored) {
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
            List<Node> toRemove = (enduring ? holder.getNodes() : holder.getTransientNodes()).stream()
                    .filter(Node::isGroupNode)
                    .collect(Collectors.toList());

            toRemove.forEach(n -> {
                try {
                    if (enduring) {
                        holder.unsetPermission(n);
                    } else {
                        holder.unsetTransientPermission(n);
                    }
                } catch (ObjectLacksException ignored) {
                }
            });

            if (holder instanceof User) {
                service.getPlugin().getUserManager().giveDefaultIfNeeded(((User) holder), false);
            }

            objectSave(holder);
            return !toRemove.isEmpty();
        }
    }

    @Override
    public boolean clearParents(@NonNull ContextSet set) {
        try (Timing i = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_CLEAR_PARENTS)) {
            List<Node> toRemove = new ArrayList<>();
            for (Node n : enduring ? holder.getNodes() : holder.getTransientNodes()) {
                if (!n.isGroupNode()) {
                    continue;
                }

                MutableContextSet contexts = MutableContextSet.fromSet(n.getContexts());

                if (n.isServerSpecific()) {
                    contexts.add(new Context(LuckPermsService.SERVER_CONTEXT, n.getServer().get()));
                }

                if (n.isWorldSpecific()) {
                    contexts.add(new Context(Context.WORLD_KEY, n.getWorld().get()));
                }

                if (contexts.equals(set)) {
                    toRemove.add(n);
                }
            }

            toRemove.forEach(n -> {
                try {
                    if (enduring) {
                        holder.unsetPermission(n);
                    } else {
                        holder.unsetTransientPermission(n);
                    }
                } catch (ObjectLacksException ignored) {
                }
            });

            if (holder instanceof User) {
                service.getPlugin().getUserManager().giveDefaultIfNeeded(((User) holder), false);
            }

            objectSave(holder);
            return !toRemove.isEmpty();
        }
    }

    @Override
    public Map<ImmutableContextSet, Map<String, String>> getOptions() {
        try (Timing ignored = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_GET_OPTIONS)) {
            Map<ImmutableContextSet, Map<String, String>> options = new HashMap<>();
            Map<ImmutableContextSet, Integer> minPrefixPriority = new HashMap<>();
            Map<ImmutableContextSet, Integer> minSuffixPriority = new HashMap<>();

            for (Node n : enduring ? holder.getNodes() : holder.getTransientNodes()) {
                if (!n.getValue()) {
                    continue;
                }

                if (!n.isMeta() && !n.isPrefix() && !n.isSuffix()) {
                    continue;
                }

                MutableContextSet contexts = MutableContextSet.fromSet(n.getContexts());

                if (n.isServerSpecific()) {
                    contexts.add(new Context(LuckPermsService.SERVER_CONTEXT, n.getServer().get()));
                }

                if (n.isWorldSpecific()) {
                    contexts.add(new Context(Context.WORLD_KEY, n.getWorld().get()));
                }

                ImmutableContextSet immutableContexts = contexts.makeImmutable();

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

                // remove all prefixes/suffixes from the user
                List<Node> toRemove = holder.getNodes().stream()
                        .filter(n -> type.equals("prefix") ? n.isPrefix() : n.isSuffix())
                        .collect(Collectors.toList());

                toRemove.forEach(n -> {
                    try {
                        if (enduring) {
                            holder.unsetPermission(n);
                        } else {
                            holder.unsetTransientPermission(n);
                        }
                    } catch (ObjectLacksException ignored) {}
                });

                MetaHolder metaHolder = holder.accumulateMeta(null, null, ExtractedContexts.generate(context));
                int priority = (type.equals("prefix") ? metaHolder.getPrefixes() : metaHolder.getSuffixes()).keySet().stream()
                        .mapToInt(e -> e).max().orElse(0);
                priority += 10;

                try {
                    if (enduring) {
                        holder.setPermission(NodeFactory.makeChatMetaNode(type.equals("prefix"), priority, value).withExtraContext(context).build());
                    } else {
                        holder.setTransientPermission(NodeFactory.makeChatMetaNode(type.equals("prefix"), priority, value).withExtraContext(context).build());
                    }
                } catch (ObjectAlreadyHasException ignored) {}

            } else {
                // standard remove
                List<Node> toRemove = holder.getNodes().stream()
                        .filter(n -> n.isMeta() && n.getMeta().getKey().equals(key))
                        .collect(Collectors.toList());

                toRemove.forEach(n -> {
                    try {
                        holder.unsetPermission(n);
                    } catch (ObjectLacksException ignored) {}
                });

                try {
                    if (enduring) {
                        holder.setPermission(NodeFactory.makeMetaNode(key, value).withExtraContext(context).build());
                    } else {
                        holder.setTransientPermission(NodeFactory.makeMetaNode(key, value).withExtraContext(context).build());
                    }
                } catch (ObjectAlreadyHasException ignored) {}
            }

            objectSave(holder);
            return true;
        }
    }

    @Override
    public boolean unsetOption(ContextSet contexts, String key) {
        try (Timing i = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_SET_OPTION)) {
            List<Node> toRemove = (enduring ? holder.getNodes() : holder.getTransientNodes()).stream()
                    .filter(n -> n.isMeta() && n.getMeta().getKey().equals(key))
                    .collect(Collectors.toList());

            toRemove.forEach(n -> {
                try {
                    if (enduring) {
                        holder.unsetPermission(n);
                    } else {
                        holder.unsetTransientPermission(n);
                    }
                } catch (ObjectLacksException ignored) {}
            });

            objectSave(holder);
            return true;
        }
    }

    @Override
    public boolean clearOptions(@NonNull ContextSet set) {
        try (Timing i = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_CLEAR_OPTIONS)) {
            List<Node> toRemove = new ArrayList<>();
            for (Node n : enduring ? holder.getNodes() : holder.getTransientNodes()) {
                if (!n.isMeta() && !n.isPrefix() && !n.isSuffix()) {
                    continue;
                }

                MutableContextSet contexts = MutableContextSet.fromSet(n.getContexts());

                if (n.isServerSpecific()) {
                    contexts.add(new Context(LuckPermsService.SERVER_CONTEXT, n.getServer().get()));
                }

                if (n.isWorldSpecific()) {
                    contexts.add(new Context(Context.WORLD_KEY, n.getWorld().get()));
                }

                if (contexts.equals(set)) {
                    toRemove.add(n);
                }
            }

            toRemove.forEach(n -> {
                try {
                    if (enduring) {
                        holder.unsetPermission(n);
                    } else {
                        holder.unsetTransientPermission(n);
                    }
                } catch (ObjectLacksException ignored) {}
            });

            objectSave(holder);
            return !toRemove.isEmpty();
        }
    }

    @Override
    public boolean clearOptions() {
        try (Timing i = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_CLEAR_OPTIONS)) {
            List<Node> toRemove = (enduring ? holder.getNodes() : holder.getTransientNodes()).stream()
                    .filter(n -> n.isMeta() || n.isPrefix() || n.isSuffix())
                    .collect(Collectors.toList());

            toRemove.forEach(n -> {
                try {
                    if (enduring) {
                        holder.unsetPermission(n);
                    } else {
                        holder.unsetTransientPermission(n);
                    }
                } catch (ObjectLacksException ignored) {}
            });

            objectSave(holder);
            return !toRemove.isEmpty();
        }
    }
}
