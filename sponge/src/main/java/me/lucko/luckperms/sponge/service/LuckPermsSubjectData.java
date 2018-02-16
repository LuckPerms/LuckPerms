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
import me.lucko.luckperms.sponge.service.reference.LPSubjectReference;

import org.spongepowered.api.service.permission.PermissionService;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LuckPermsSubjectData implements LPSubjectData {
    private final boolean enduring;
    private final LuckPermsService service;

    private final PermissionHolder holder;

    private final LPSubject parentSubject;

    public LuckPermsSubjectData(boolean enduring, LuckPermsService service, PermissionHolder holder, LPSubject parentSubject) {
        this.enduring = enduring;
        this.service = service;
        this.holder = holder;
        this.parentSubject = parentSubject;
    }

    @Override
    public LPSubject getParentSubject() {
        return this.parentSubject;
    }

    @Override
    public ImmutableMap<ImmutableContextSet, ImmutableMap<String, Boolean>> getAllPermissions() {
        Map<ImmutableContextSet, ImmutableMap.Builder<String, Boolean>> perms = new HashMap<>();

        for (Map.Entry<ImmutableContextSet, Collection<Node>> e : (this.enduring ? this.holder.getEnduringNodes() : this.holder.getTransientNodes()).asMap().entrySet()) {
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
    public CompletableFuture<Boolean> setPermission(ImmutableContextSet contexts, String permission, Tristate tristate) {
        Objects.requireNonNull(contexts, "contexts");
        Objects.requireNonNull(permission, "permission");
        Objects.requireNonNull(tristate, "tristate");

        if (tristate == Tristate.UNDEFINED) {
            // Unset
            Node node = NodeFactory.builder(permission).withExtraContext(contexts).build();

            if (this.enduring) {
                this.holder.unsetPermission(node);
            } else {
                this.holder.unsetTransientPermission(node);
            }

            return objectSave(this.holder).thenApply(v -> true);
        }

        Node node = NodeFactory.builder(permission).setValue(tristate.asBoolean()).withExtraContext(contexts).build();

        // Workaround: unset the inverse, to allow false -> true, true -> false overrides.
        if (this.enduring) {
            this.holder.unsetPermission(node);
        } else {
            this.holder.unsetTransientPermission(node);
        }

        if (this.enduring) {
            this.holder.setPermission(node);
        } else {
            this.holder.setTransientPermission(node);
        }

        return objectSave(this.holder).thenApply(v -> true);
    }

    @Override
    public CompletableFuture<Boolean> clearPermissions() {
        boolean ret;
        if (this.enduring) {
            ret = this.holder.clearNodes();
        } else {
            ret = this.holder.clearTransientNodes();
        }

        if (!ret) {
            return CompletableFuture.completedFuture(false);
        }

        if (this.holder.getType().isUser()) {
            this.service.getPlugin().getUserManager().giveDefaultIfNeeded(((User) this.holder), false);
        }

        return objectSave(this.holder).thenApply(v -> true);
    }

    @Override
    public CompletableFuture<Boolean> clearPermissions(ImmutableContextSet contexts) {
        Objects.requireNonNull(contexts, "contexts");

        boolean ret;
        if (this.enduring) {
            ret = this.holder.clearNodes(contexts);
        } else {
            List<Node> toRemove = streamNodes(false)
                    .filter(n -> n.getFullContexts().equals(contexts))
                    .collect(Collectors.toList());

            toRemove.forEach(makeUnsetConsumer(false));
            ret = !toRemove.isEmpty();
        }

        if (!ret) {
            return CompletableFuture.completedFuture(false);
        }

        if (this.holder.getType().isUser()) {
            this.service.getPlugin().getUserManager().giveDefaultIfNeeded(((User) this.holder), false);
        }

        return objectSave(this.holder).thenApply(v -> true);
    }

    @Override
    public ImmutableMap<ImmutableContextSet, ImmutableList<LPSubjectReference>> getAllParents() {
        Map<ImmutableContextSet, ImmutableList.Builder<LPSubjectReference>> parents = new HashMap<>();

        for (Map.Entry<ImmutableContextSet, Collection<Node>> e : (this.enduring ? this.holder.getEnduringNodes() : this.holder.getTransientNodes()).asMap().entrySet()) {
            ImmutableList.Builder<LPSubjectReference> results = ImmutableList.builder();
            for (Node n : e.getValue()) {
                if (n.isGroupNode()) {
                    results.add(this.service.getGroupSubjects().loadSubject(n.getGroupName()).join().toReference());
                }
            }
            parents.put(e.getKey(), results);
        }

        ImmutableMap.Builder<ImmutableContextSet, ImmutableList<LPSubjectReference>> map = ImmutableMap.builder();
        for (Map.Entry<ImmutableContextSet, ImmutableList.Builder<LPSubjectReference>> e : parents.entrySet()) {
            map.put(e.getKey(), e.getValue().build());
        }
        return map.build();
    }

    @Override
    public CompletableFuture<Boolean> addParent(ImmutableContextSet contexts, LPSubjectReference subject) {
        Objects.requireNonNull(contexts, "contexts");
        Objects.requireNonNull(subject, "subject");

        if (subject.getCollectionIdentifier().equals(PermissionService.SUBJECTS_GROUP)) {
            return subject.resolveLp().thenCompose(sub -> {
                DataMutateResult result;

                if (this.enduring) {
                    result = this.holder.setPermission(NodeFactory.buildGroupNode(sub.getIdentifier())
                            .withExtraContext(contexts)
                            .build());
                } else {
                    result = this.holder.setTransientPermission(NodeFactory.buildGroupNode(sub.getIdentifier())
                            .withExtraContext(contexts)
                            .build());
                }

                if (!result.asBoolean()) {
                    return CompletableFuture.completedFuture(false);
                }

                return objectSave(this.holder).thenApply(v -> true);
            });
        }
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public CompletableFuture<Boolean> removeParent(ImmutableContextSet contexts, LPSubjectReference subject) {
        Objects.requireNonNull(contexts, "contexts");
        Objects.requireNonNull(subject, "subject");

        if (subject.getCollectionIdentifier().equals(PermissionService.SUBJECTS_GROUP)) {
            subject.resolveLp().thenCompose(sub -> {
                DataMutateResult result;

                if (this.enduring) {
                    result = this.holder.unsetPermission(NodeFactory.buildGroupNode(sub.getIdentifier())
                            .withExtraContext(contexts)
                            .build());
                } else {
                    result = this.holder.unsetTransientPermission(NodeFactory.buildGroupNode(sub.getIdentifier())
                            .withExtraContext(contexts)
                            .build());
                }

                if (!result.asBoolean()) {
                    return CompletableFuture.completedFuture(false);
                }

                return objectSave(this.holder).thenApply(v -> true);
            });
        }
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public CompletableFuture<Boolean> clearParents() {
        boolean ret;
        if (this.enduring) {
            ret = this.holder.clearParents(true);
        } else {
            List<Node> toRemove = streamNodes(false)
                    .filter(Node::isGroupNode)
                    .collect(Collectors.toList());

            toRemove.forEach(makeUnsetConsumer(false));
            ret = !toRemove.isEmpty();

            if (ret && this.holder.getType().isUser()) {
                this.service.getPlugin().getUserManager().giveDefaultIfNeeded(((User) this.holder), false);
            }
        }

        if (!ret) {
            return CompletableFuture.completedFuture(false);
        }

        return objectSave(this.holder).thenApply(v -> true);
    }

    @Override
    public CompletableFuture<Boolean> clearParents(ImmutableContextSet contexts) {
        Objects.requireNonNull(contexts, "contexts");

        boolean ret;
        if (this.enduring) {
            ret = this.holder.clearParents(contexts, true);
        } else {
            List<Node> toRemove = streamNodes(false)
                    .filter(Node::isGroupNode)
                    .filter(n -> n.getFullContexts().equals(contexts))
                    .collect(Collectors.toList());

            toRemove.forEach(makeUnsetConsumer(false));
            ret = !toRemove.isEmpty();

            if (ret && this.holder.getType().isUser()) {
                this.service.getPlugin().getUserManager().giveDefaultIfNeeded(((User) this.holder), false);
            }
        }

        if (!ret) {
            return CompletableFuture.completedFuture(false);
        }

        return objectSave(this.holder).thenApply(v -> true);
    }

    @Override
    public ImmutableMap<ImmutableContextSet, ImmutableMap<String, String>> getAllOptions() {
        Map<ImmutableContextSet, Map<String, String>> options = new HashMap<>();
        Map<ImmutableContextSet, Integer> minPrefixPriority = new HashMap<>();
        Map<ImmutableContextSet, Integer> minSuffixPriority = new HashMap<>();

        for (Node n : this.enduring ? this.holder.getEnduringNodes().values() : this.holder.getTransientNodes().values()) {
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
    public CompletableFuture<Boolean> setOption(ImmutableContextSet contexts, String key, String value) {
        Objects.requireNonNull(contexts, "contexts");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");

        if (key.equalsIgnoreCase(NodeFactory.PREFIX_KEY) || key.equalsIgnoreCase(NodeFactory.SUFFIX_KEY)) {
            // special handling.
            ChatMetaType type = ChatMetaType.valueOf(key.toUpperCase());

            // remove all prefixes/suffixes from the user
            List<Node> toRemove = streamNodes(this.enduring)
                    .filter(type::matches)
                    .filter(n -> n.getFullContexts().equals(contexts))
                    .collect(Collectors.toList());

            toRemove.forEach(makeUnsetConsumer(this.enduring));

            MetaAccumulator metaAccumulator = this.holder.accumulateMeta(null, this.service.getPlugin().getContextManager().formContexts(contexts));
            int priority = metaAccumulator.getChatMeta(type).keySet().stream().mapToInt(e -> e).max().orElse(0);
            priority += 10;

            if (this.enduring) {
                this.holder.setPermission(NodeFactory.buildChatMetaNode(type, priority, value).withExtraContext(contexts).build());
            } else {
                this.holder.setTransientPermission(NodeFactory.buildChatMetaNode(type, priority, value).withExtraContext(contexts).build());
            }

        } else {
            // standard remove
            List<Node> toRemove = streamNodes(this.enduring)
                    .filter(n -> n.isMeta() && n.getMeta().getKey().equals(key))
                    .filter(n -> n.getFullContexts().equals(contexts))
                    .collect(Collectors.toList());

            toRemove.forEach(makeUnsetConsumer(this.enduring));

            if (this.enduring) {
                this.holder.setPermission(NodeFactory.buildMetaNode(key, value).withExtraContext(contexts).build());
            } else {
                this.holder.setTransientPermission(NodeFactory.buildMetaNode(key, value).withExtraContext(contexts).build());
            }
        }

        return objectSave(this.holder).thenApply(v -> true);
    }

    @Override
    public CompletableFuture<Boolean> unsetOption(ImmutableContextSet contexts, String key) {
        Objects.requireNonNull(contexts, "contexts");
        Objects.requireNonNull(key, "key");

        List<Node> toRemove = streamNodes(this.enduring)
                .filter(n -> {
                    if (key.equalsIgnoreCase(NodeFactory.PREFIX_KEY)) {
                        return n.isPrefix();
                    } else if (key.equalsIgnoreCase(NodeFactory.SUFFIX_KEY)) {
                        return n.isSuffix();
                    } else {
                        return n.isMeta() && n.getMeta().getKey().equals(key);
                    }
                })
                .filter(n -> n.getFullContexts().equals(contexts))
                .collect(Collectors.toList());

        toRemove.forEach(makeUnsetConsumer(this.enduring));

        return objectSave(this.holder).thenApply(v -> true);
    }

    @Override
    public CompletableFuture<Boolean> clearOptions(ImmutableContextSet contexts) {
        Objects.requireNonNull(contexts, "contexts");

        List<Node> toRemove = streamNodes(this.enduring)
                .filter(n -> n.isMeta() || n.isPrefix() || n.isSuffix())
                .filter(n -> n.getFullContexts().equals(contexts))
                .collect(Collectors.toList());

        toRemove.forEach(makeUnsetConsumer(this.enduring));

        return objectSave(this.holder).thenApply(v -> !toRemove.isEmpty());
    }

    @Override
    public CompletableFuture<Boolean> clearOptions() {
        List<Node> toRemove = streamNodes(this.enduring)
                .filter(n -> n.isMeta() || n.isPrefix() || n.isSuffix())
                .collect(Collectors.toList());

        toRemove.forEach(makeUnsetConsumer(this.enduring));

        return objectSave(this.holder).thenApply(v -> !toRemove.isEmpty());
    }

    private Stream<Node> streamNodes(boolean enduring) {
        return (enduring ? this.holder.getEnduringNodes() : this.holder.getTransientNodes()).values().stream();
    }

    private Consumer<Node> makeUnsetConsumer(boolean enduring) {
        return n -> {
            if (enduring) {
                this.holder.unsetPermission(n);
            } else {
                this.holder.unsetTransientPermission(n);
            }
        };
    }

    private CompletableFuture<Void> objectSave(PermissionHolder t) {
        if (!this.enduring) {
            // don't bother saving to primary storage. just refresh
            if (t.getType().isUser()) {
                User user = ((User) t);
                return user.getRefreshBuffer().request();
            } else {
                return this.service.getPlugin().getUpdateTaskBuffer().request();
            }
        } else {
            if (t.getType().isUser()) {
                User user = ((User) t);
                CompletableFuture<Void> fut = new CompletableFuture<>();
                this.service.getPlugin().getStorage().saveUser(user).whenCompleteAsync((v, ex) -> {
                    if (ex != null) {
                        fut.complete(null);
                    }

                    user.getRefreshBuffer().request().thenAccept(fut::complete);
                }, this.service.getPlugin().getScheduler().async());
                return fut;
            } else {
                Group group = ((Group) t);
                CompletableFuture<Void> fut = new CompletableFuture<>();
                this.service.getPlugin().getStorage().saveGroup(group).whenCompleteAsync((v, ex) -> {
                    if (ex != null) {
                        fut.complete(null);
                    }

                    this.service.getPlugin().getUpdateTaskBuffer().request().thenAccept(fut::complete);
                }, this.service.getPlugin().getScheduler().async());
                return fut;
            }
        }
    }
}
