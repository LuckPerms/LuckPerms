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

import co.aikar.timings.Timing;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.core.NodeBuilder;
import me.lucko.luckperms.common.core.NodeFactory;
import me.lucko.luckperms.common.core.PermissionHolder;
import me.lucko.luckperms.common.groups.Group;
import me.lucko.luckperms.common.users.User;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.sponge.timings.LPTiming;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.util.Tristate;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"OptionalGetWithoutIsPresent", "unused"})
@AllArgsConstructor
public class LuckPermsSubjectData implements SubjectData {
    private final boolean enduring;
    private final LuckPermsService service;

    @Getter
    private final PermissionHolder holder;

    private void objectSave(PermissionHolder t) {
        if (t instanceof User) {
            service.getPlugin().getStorage().saveUser(((User) t))
                    .thenRunAsync(() -> ((User) t).getRefreshBuffer().request(), service.getPlugin().getAsyncExecutor());
        }
        if (t instanceof Group) {
            service.getPlugin().getStorage().saveGroup((Group) t)
                    .thenRunAsync(() -> service.getPlugin().getUpdateTaskBuffer().request(), service.getPlugin().getAsyncExecutor());
        }
    }

    @Override
    public Map<Set<Context>, Map<String, Boolean>> getAllPermissions() {
        try (Timing ignored = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_GET_PERMISSIONS)) {
            Map<Set<Context>, Map<String, Boolean>> perms = new HashMap<>();

            for (Node n : enduring ? holder.getNodes() : holder.getTransientNodes()) {
                Set<Context> contexts = LuckPermsService.convertContexts(n.getContexts());

                if (n.isServerSpecific()) {
                    contexts.add(new Context(LuckPermsService.SERVER_CONTEXT, n.getServer().get()));
                }

                if (n.isWorldSpecific()) {
                    contexts.add(new Context(Context.WORLD_KEY, n.getWorld().get()));
                }

                if (!perms.containsKey(contexts)) {
                    perms.put(contexts, new HashMap<>());
                }

                perms.get(contexts).put(n.getPermission(), n.getValue());
            }

            ImmutableMap.Builder<Set<Context>, Map<String, Boolean>> map = ImmutableMap.builder();
            for (Map.Entry<Set<Context>, Map<String, Boolean>> e : perms.entrySet()) {
                map.put(ImmutableSet.copyOf(e.getKey()), ImmutableMap.copyOf(e.getValue()));
            }
            return map.build();
        }
    }

    @Override
    public Map<String, Boolean> getPermissions(@NonNull Set<Context> contexts) {
        return getAllPermissions().getOrDefault(contexts, ImmutableMap.of());
    }

    @Override
    public boolean setPermission(@NonNull Set<Context> contexts, @NonNull String permission, @NonNull Tristate tristate) {
        try (Timing i = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_SET_PERMISSION)) {
            if (tristate == Tristate.UNDEFINED) {
                // Unset
                Node.Builder builder = new NodeBuilder(permission);

                for (Context ct : contexts) {
                    builder.withExtraContext(ct.getKey(), ct.getValue());
                }

                try {
                    if (enduring) {
                        holder.unsetPermission(builder.build());
                    } else {
                        holder.unsetTransientPermission(builder.build());
                    }
                } catch (ObjectLacksException ignored) {}

                objectSave(holder);
                return true;
            }

            Node.Builder builder = new NodeBuilder(permission)
                    .setValue(tristate.asBoolean());

            for (Context ct : contexts) {
                builder.withExtraContext(ct.getKey(), ct.getValue());
            }

            try {
                if (enduring) {
                    holder.setPermission(builder.build());
                } else {
                    holder.setTransientPermission(builder.build());
                }
            } catch (ObjectAlreadyHasException ignored) {}

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
    public boolean clearPermissions(@NonNull Set<Context> c) {
        try (Timing i = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_CLEAR_PERMISSIONS)) {
            List<Node> toRemove = new ArrayList<>();
            for (Node n : enduring ? holder.getNodes() : holder.getTransientNodes()) {
                Set<Context> contexts = LuckPermsService.convertContexts(n.getContexts());

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
                } catch (ObjectLacksException ignored) {}
            });

            if (holder instanceof User) {
                service.getPlugin().getUserManager().giveDefaultIfNeeded(((User) holder), false);
            }

            objectSave(holder);
            return !toRemove.isEmpty();
        }
    }

    @Override
    public Map<Set<Context>, List<Subject>> getAllParents() {
        try (Timing ignored = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_GET_PARENTS)) {
            Map<Set<Context>, List<Subject>> parents = new HashMap<>();

            for (Node n : enduring ? holder.getNodes() : holder.getTransientNodes()) {
                if (!n.isGroupNode()) {
                    continue;
                }

                Set<Context> contexts = LuckPermsService.convertContexts(n.getContexts());

                if (n.isServerSpecific()) {
                    contexts.add(new Context(LuckPermsService.SERVER_CONTEXT, n.getServer().get()));
                }

                if (n.isWorldSpecific()) {
                    contexts.add(new Context(Context.WORLD_KEY, n.getWorld().get()));
                }

                if (!parents.containsKey(contexts)) {
                    parents.put(contexts, new ArrayList<>());
                }

                parents.get(contexts).add(service.getGroupSubjects().get(n.getGroupName()));
            }

            ImmutableMap.Builder<Set<Context>, List<Subject>> map = ImmutableMap.builder();
            for (Map.Entry<Set<Context>, List<Subject>> e : parents.entrySet()) {
                map.put(ImmutableSet.copyOf(e.getKey()), ImmutableList.copyOf(e.getValue()));
            }
            return map.build();
        }
    }

    @Override
    public List<Subject> getParents(@NonNull Set<Context> contexts) {
        return getAllParents().getOrDefault(contexts, ImmutableList.of());
    }

    @Override
    public boolean addParent(@NonNull Set<Context> set, @NonNull Subject subject) {
        try (Timing i = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_ADD_PARENT)) {
            if (subject instanceof LuckPermsGroupSubject) {
                LuckPermsGroupSubject permsSubject = ((LuckPermsGroupSubject) subject);
                ContextSet contexts = LuckPermsService.convertContexts(set);

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
                } catch (ObjectAlreadyHasException ignored) {}

                objectSave(holder);
                return true;
            }
            return false;
        }
    }

    @Override
    public boolean removeParent(@NonNull Set<Context> set, @NonNull Subject subject) {
        try (Timing i = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_REMOVE_PARENT)) {
            if (subject instanceof LuckPermsGroupSubject) {
                LuckPermsGroupSubject permsSubject = ((LuckPermsGroupSubject) subject);
                ContextSet contexts = LuckPermsService.convertContexts(set);

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
                } catch (ObjectLacksException ignored) {}

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
                } catch (ObjectLacksException ignored) {}
            });

            if (holder instanceof User) {
                service.getPlugin().getUserManager().giveDefaultIfNeeded(((User) holder), false);
            }

            objectSave(holder);
            return !toRemove.isEmpty();
        }
    }

    @Override
    public boolean clearParents(@NonNull Set<Context> set) {
        try (Timing i = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_CLEAR_PARENTS)) {
            List<Node> toRemove = new ArrayList<>();
            for (Node n : enduring ? holder.getNodes() : holder.getTransientNodes()) {
                if (!n.isGroupNode()) {
                    continue;
                }

                Set<Context> contexts = LuckPermsService.convertContexts(n.getContexts());

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

            if (holder instanceof User) {
                service.getPlugin().getUserManager().giveDefaultIfNeeded(((User) holder), false);
            }

            objectSave(holder);
            return !toRemove.isEmpty();
        }
    }

    @Override
    public Map<Set<Context>, Map<String, String>> getAllOptions() {
        try (Timing ignored = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_GET_OPTIONS)) {
            Map<Set<Context>, Map<String, String>> options = new HashMap<>();
            Map<Set<Context>, Integer> minPrefixPriority = new HashMap<>();
            Map<Set<Context>, Integer> minSuffixPriority = new HashMap<>();

            for (Node n : enduring ? holder.getNodes() : holder.getTransientNodes()) {
                if (!n.getValue()) {
                    continue;
                }

                if (!n.isMeta() && !n.isPrefix() && !n.isSuffix()) {
                    continue;
                }

                Set<Context> contexts = LuckPermsService.convertContexts(n.getContexts());

                if (n.isServerSpecific()) {
                    contexts.add(new Context(LuckPermsService.SERVER_CONTEXT, n.getServer().get()));
                }

                if (n.isWorldSpecific()) {
                    contexts.add(new Context(Context.WORLD_KEY, n.getWorld().get()));
                }

                if (!options.containsKey(contexts)) {
                    options.put(contexts, new HashMap<>());
                    minPrefixPriority.put(contexts, Integer.MIN_VALUE);
                    minSuffixPriority.put(contexts, Integer.MIN_VALUE);
                }

                if (n.isPrefix()) {
                    Map.Entry<Integer, String> value = n.getPrefix();
                    if (value.getKey() > minPrefixPriority.get(contexts)) {
                        options.get(contexts).put("prefix", value.getValue());
                        minPrefixPriority.put(contexts, value.getKey());
                    }
                    continue;
                }

                if (n.isSuffix()) {
                    Map.Entry<Integer, String> value = n.getSuffix();
                    if (value.getKey() > minSuffixPriority.get(contexts)) {
                        options.get(contexts).put("suffix", value.getValue());
                        minSuffixPriority.put(contexts, value.getKey());
                    }
                    continue;
                }

                if (n.isMeta()) {
                    Map.Entry<String, String> meta = n.getMeta();
                    options.get(contexts).put(meta.getKey(), meta.getValue());
                }
            }

            ImmutableMap.Builder<Set<Context>, Map<String, String>> map = ImmutableMap.builder();
            for (Map.Entry<Set<Context>, Map<String, String>> e : options.entrySet()) {
                map.put(ImmutableSet.copyOf(e.getKey()), ImmutableMap.copyOf(e.getValue()));
            }
            return map.build();
        }
    }

    @Override
    public Map<String, String> getOptions(@NonNull Set<Context> set) {
        return getAllOptions().getOrDefault(set, ImmutableMap.of());
    }

    @Override
    public boolean setOption(@NonNull Set<Context> set, @NonNull String key, @NonNull String value) {
        try (Timing i = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_SET_OPTION)) {
            ContextSet context = LuckPermsService.convertContexts(set);

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
            objectSave(holder);
            return true;
        }
    }

    @Override
    public boolean clearOptions(@NonNull Set<Context> set) {
        try (Timing i = service.getPlugin().getTimings().time(LPTiming.LP_SUBJECT_CLEAR_OPTIONS)) {
            List<Node> toRemove = new ArrayList<>();
            for (Node n : enduring ? holder.getNodes() : holder.getTransientNodes()) {
                if (!n.isMeta() && !n.isPrefix() && !n.isSuffix()) {
                    continue;
                }

                Set<Context> contexts = LuckPermsService.convertContexts(n.getContexts());

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
