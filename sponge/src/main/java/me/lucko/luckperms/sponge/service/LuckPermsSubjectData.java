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
import me.lucko.luckperms.common.caching.type.MetaAccumulator;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.NodeFactory;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectData;
import me.lucko.luckperms.sponge.service.model.SubjectReference;

import org.spongepowered.api.service.permission.PermissionService;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
public class LuckPermsSubjectData implements LPSubjectData {
    private final boolean enduring;
    private final LuckPermsService service;

    @Getter
    private final PermissionHolder holder;

    @Getter
    private final LPSubject parentSubject;

    @Override
    public ImmutableMap<ImmutableContextSet, ImmutableMap<String, Boolean>> getAllPermissions() {
        Map<ImmutableContextSet, ImmutableMap.Builder<String, Boolean>> perms = new HashMap<>();

        for (Map.Entry<ImmutableContextSet, Collection<Node>> e : (enduring ? holder.getEnduringNodes() : holder.getTransientNodes()).asMap().entrySet()) {
            ImmutableMap.Builder<String, Boolean> results = ImmutableMap.builder();
            for (Node n : e.getValue()) {
                results.put(n.getPermission(), n.getValuePrimitive());
            }
            perms.put(e.getKey(), results);
        }

        ImmutableMap.Builder<ImmutableContextSet, ImmutableMap<String, Boolean>> map = ImmutableMap.builder();
        for (Map.Entry<ImmutableContextSet, ImmutableMap.Builder<String, Boolean>> e : perms.entrySet()) {
            map.put(e.getKey(), e.getValue().build());
        }
        return map.build();
    }

    @Override
    public CompletableFuture<Boolean> setPermission(@NonNull ImmutableContextSet contexts, @NonNull String permission, @NonNull Tristate tristate) {
        if (tristate == Tristate.UNDEFINED) {
            // Unset
            Node node = NodeFactory.builder(permission).withExtraContext(contexts).build();

            if (enduring) {
                holder.unsetPermission(node);
            } else {
                holder.unsetTransientPermission(node);
            }

            return objectSave(holder).thenApply(v -> true);
        }

        Node node = NodeFactory.builder(permission).setValue(tristate.asBoolean()).withExtraContext(contexts).build();

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

    @Override
    public CompletableFuture<Boolean> clearPermissions() {
        boolean ret;
        if (enduring) {
            ret = holder.clearNodes();
        } else {
            ret = holder.clearTransientNodes();
        }

        if (!ret) {
            return CompletableFuture.completedFuture(false);
        }

        if (holder.getType().isUser()) {
            service.getPlugin().getUserManager().giveDefaultIfNeeded(((User) holder), false);
        }

        return objectSave(holder).thenApply(v -> true);
    }

    @Override
    public CompletableFuture<Boolean> clearPermissions(@NonNull ImmutableContextSet set) {
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

        if (holder.getType().isUser()) {
            service.getPlugin().getUserManager().giveDefaultIfNeeded(((User) holder), false);
        }

        return objectSave(holder).thenApply(v -> true);
    }

    @Override
    public ImmutableMap<ImmutableContextSet, ImmutableList<SubjectReference>> getAllParents() {
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

    @Override
    public CompletableFuture<Boolean> addParent(@NonNull ImmutableContextSet contexts, @NonNull SubjectReference subject) {
        if (subject.getCollectionIdentifier().equals(PermissionService.SUBJECTS_GROUP)) {
            return subject.resolveLp().thenCompose(sub -> {
                DataMutateResult result;

                if (enduring) {
                    result = holder.setPermission(NodeFactory.buildGroupNode(sub.getIdentifier())
                            .withExtraContext(contexts)
                            .build());
                } else {
                    result = holder.setTransientPermission(NodeFactory.buildGroupNode(sub.getIdentifier())
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

    @Override
    public CompletableFuture<Boolean> removeParent(@NonNull ImmutableContextSet contexts, @NonNull SubjectReference subject) {
        if (subject.getCollectionIdentifier().equals(PermissionService.SUBJECTS_GROUP)) {
            subject.resolveLp().thenCompose(sub -> {
                DataMutateResult result;

                if (enduring) {
                    result = holder.unsetPermission(NodeFactory.buildGroupNode(sub.getIdentifier())
                            .withExtraContext(contexts)
                            .build());
                } else {
                    result = holder.unsetTransientPermission(NodeFactory.buildGroupNode(sub.getIdentifier())
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

    @Override
    public CompletableFuture<Boolean> clearParents() {
        boolean ret;
        if (enduring) {
            ret = holder.clearParents(true);
        } else {
            List<Node> toRemove = streamNodes(false)
                    .filter(Node::isGroupNode)
                    .collect(Collectors.toList());

            toRemove.forEach(makeUnsetConsumer(false));
            ret = !toRemove.isEmpty();

            if (ret && holder.getType().isUser()) {
                service.getPlugin().getUserManager().giveDefaultIfNeeded(((User) holder), false);
            }
        }

        if (!ret) {
            return CompletableFuture.completedFuture(false);
        }

        return objectSave(holder).thenApply(v -> true);
    }

    @Override
    public CompletableFuture<Boolean> clearParents(@NonNull ImmutableContextSet set) {
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

            if (ret && holder.getType().isUser()) {
                service.getPlugin().getUserManager().giveDefaultIfNeeded(((User) holder), false);
            }
        }

        if (!ret) {
            return CompletableFuture.completedFuture(false);
        }

        return objectSave(holder).thenApply(v -> true);
    }

    @Override
    public ImmutableMap<ImmutableContextSet, ImmutableMap<String, String>> getAllOptions() {
        Map<ImmutableContextSet, Map<String, String>> options = new HashMap<>();
        Map<ImmutableContextSet, Integer> minPrefixPriority = new HashMap<>();
        Map<ImmutableContextSet, Integer> minSuffixPriority = new HashMap<>();

        for (Node n : enduring ? holder.getEnduringNodes().values() : holder.getTransientNodes().values()) {
            if (!n.getValuePrimitive()) continue;
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
                    options.get(immutableContexts).put(NodeFactory.PREFIX_KEY, value.getValue());
                    minPrefixPriority.put(immutableContexts, value.getKey());
                }
                continue;
            }

            if (n.isSuffix()) {
                Map.Entry<Integer, String> value = n.getSuffix();
                if (value.getKey() > minSuffixPriority.get(immutableContexts)) {
                    options.get(immutableContexts).put(NodeFactory.SUFFIX_KEY, value.getValue());
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

    @Override
    public CompletableFuture<Boolean> setOption(@NonNull ImmutableContextSet context, @NonNull String key, @NonNull String value) {
        if (key.equalsIgnoreCase(NodeFactory.PREFIX_KEY) || key.equalsIgnoreCase(NodeFactory.SUFFIX_KEY)) {
            // special handling.
            ChatMetaType type = ChatMetaType.valueOf(key.toUpperCase());

            // remove all prefixes/suffixes from the user
            List<Node> toRemove = streamNodes(enduring)
                    .filter(type::matches)
                    .filter(n -> n.getFullContexts().equals(context))
                    .collect(Collectors.toList());

            toRemove.forEach(makeUnsetConsumer(enduring));

            MetaAccumulator metaAccumulator = holder.accumulateMeta(null, null, service.getPlugin().getContextManager().formContexts(context));
            int priority = metaAccumulator.getChatMeta(type).keySet().stream().mapToInt(e -> e).max().orElse(0);
            priority += 10;

            if (enduring) {
                holder.setPermission(NodeFactory.buildChatMetaNode(type, priority, value).withExtraContext(context).build());
            } else {
                holder.setTransientPermission(NodeFactory.buildChatMetaNode(type, priority, value).withExtraContext(context).build());
            }

        } else {
            // standard remove
            List<Node> toRemove = streamNodes(enduring)
                    .filter(n -> n.isMeta() && n.getMeta().getKey().equals(key))
                    .filter(n -> n.getFullContexts().equals(context))
                    .collect(Collectors.toList());

            toRemove.forEach(makeUnsetConsumer(enduring));

            if (enduring) {
                holder.setPermission(NodeFactory.buildMetaNode(key, value).withExtraContext(context).build());
            } else {
                holder.setTransientPermission(NodeFactory.buildMetaNode(key, value).withExtraContext(context).build());
            }
        }

        return objectSave(holder).thenApply(v -> true);
    }

    @Override
    public CompletableFuture<Boolean> unsetOption(ImmutableContextSet set, String key) {
        List<Node> toRemove = streamNodes(enduring)
                .filter(n -> {
                    if (key.equalsIgnoreCase(NodeFactory.PREFIX_KEY)) {
                        return n.isPrefix();
                    } else if (key.equalsIgnoreCase(NodeFactory.SUFFIX_KEY)) {
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

    @Override
    public CompletableFuture<Boolean> clearOptions(@NonNull ImmutableContextSet set) {
        List<Node> toRemove = streamNodes(enduring)
                .filter(n -> n.isMeta() || n.isPrefix() || n.isSuffix())
                .filter(n -> n.getFullContexts().equals(set))
                .collect(Collectors.toList());

        toRemove.forEach(makeUnsetConsumer(enduring));

        return objectSave(holder).thenApply(v -> !toRemove.isEmpty());
    }

    @Override
    public CompletableFuture<Boolean> clearOptions() {
        List<Node> toRemove = streamNodes(enduring)
                .filter(n -> n.isMeta() || n.isPrefix() || n.isSuffix())
                .collect(Collectors.toList());

        toRemove.forEach(makeUnsetConsumer(enduring));

        return objectSave(holder).thenApply(v -> !toRemove.isEmpty());
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
            if (t.getType().isUser()) {
                User user = ((User) t);
                return user.getRefreshBuffer().request();
            } else {
                return service.getPlugin().getUpdateTaskBuffer().request();
            }
        } else {
            if (t.getType().isUser()) {
                User user = ((User) t);
                CompletableFuture<Void> fut = new CompletableFuture<>();
                service.getPlugin().getStorage().saveUser(user).whenCompleteAsync((v, ex) -> {
                    if (ex != null) {
                        fut.complete(null);
                    }

                    user.getRefreshBuffer().request().thenAccept(fut::complete);
                }, service.getPlugin().getScheduler().async());
                return fut;
            } else {
                Group group = ((Group) t);
                CompletableFuture<Void> fut = new CompletableFuture<>();
                service.getPlugin().getStorage().saveGroup(group).whenCompleteAsync((v, ex) -> {
                    if (ex != null) {
                        fut.complete(null);
                    }

                    service.getPlugin().getUpdateTaskBuffer().request().thenAccept(fut::complete);
                }, service.getPlugin().getScheduler().async());
                return fut;
            }
        }
    }
}
