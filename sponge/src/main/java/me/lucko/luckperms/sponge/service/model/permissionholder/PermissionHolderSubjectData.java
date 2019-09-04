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

package me.lucko.luckperms.sponge.service.model.permissionholder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import me.lucko.luckperms.common.cacheddata.type.MetaAccumulator;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.factory.NodeBuilders;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.node.types.Meta;
import me.lucko.luckperms.common.node.types.Prefix;
import me.lucko.luckperms.common.node.types.Suffix;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.service.ProxyFactory;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectData;
import me.lucko.luckperms.sponge.service.model.LPSubjectReference;

import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.DataType;
import net.luckperms.api.node.ChatMetaType;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.Tristate;
import net.luckperms.api.node.types.ChatMetaNode;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import net.luckperms.api.query.QueryOptions;

import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.SubjectData;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class PermissionHolderSubjectData implements LPSubjectData {
    private final LuckPermsService service;

    private final DataType type;
    private final PermissionHolder holder;
    private final LPSubject parentSubject;

    public PermissionHolderSubjectData(LuckPermsService service, DataType type, PermissionHolder holder, LPSubject parentSubject) {
        this.type = type;
        this.service = service;
        this.holder = holder;
        this.parentSubject = parentSubject;
    }

    private Stream<? extends Node> streamNodes() {
        return this.holder.getData(this.type).immutable().values().stream();
    }

    @Override
    public SubjectData sponge() {
        return ProxyFactory.toSponge(this);
    }

    @Override
    public LPSubject getParentSubject() {
        return this.parentSubject;
    }

    @Override
    public DataType getType() {
        return this.type;
    }

    @Override
    public ImmutableMap<ImmutableContextSet, ImmutableMap<String, Boolean>> getAllPermissions() {
        ImmutableMap.Builder<ImmutableContextSet, ImmutableMap<String, Boolean>> ret = ImmutableMap.builder();
        for (Map.Entry<ImmutableContextSet, ? extends Collection<? extends Node>> entry : this.holder.getData(this.type).immutable().asMap().entrySet()) {
            ImmutableMap.Builder<String, Boolean> builder = ImmutableMap.builder();
            for (Node n : entry.getValue()) {
                builder.put(n.getKey(), n.getValue());
            }
            ret.put(entry.getKey(), builder.build());
        }
        return ret.build();
    }

    @Override
    public ImmutableMap<String, Boolean> getPermissions(ImmutableContextSet contexts) {
        ImmutableMap.Builder<String, Boolean> builder = ImmutableMap.builder();
        for (Node n : this.holder.getData(this.type).immutable().get(contexts)) {
            builder.put(n.getKey(), n.getValue());
        }
        return builder.build();
    }

    @Override
    public CompletableFuture<Boolean> setPermission(ImmutableContextSet contexts, String permission, Tristate tristate) {
        Objects.requireNonNull(contexts, "contexts");
        Objects.requireNonNull(permission, "permission");
        Objects.requireNonNull(tristate, "tristate");

        if (tristate == Tristate.UNDEFINED) {
            // Unset
            Node node = NodeBuilders.determineMostApplicable(permission).withContext(contexts).build();
            this.holder.unsetPermission(this.type, node);
            return save(this.holder).thenApply(v -> true);
        }

        Node node = NodeBuilders.determineMostApplicable(permission).value(tristate.asBoolean()).withContext(contexts).build();
        // unset the inverse, to allow false -> true, true -> false overrides.
        this.holder.unsetPermission(this.type, node);
        this.holder.setPermission(this.type, node, true);
        return save(this.holder).thenApply(v -> true);
    }

    @Override
    public CompletableFuture<Boolean> clearPermissions() {
        if (!this.holder.clearNodes(this.type, null)) {
            return CompletableFuture.completedFuture(false);
        }

        if (this.holder.getType() == HolderType.USER) {
            this.service.getPlugin().getUserManager().giveDefaultIfNeeded(((User) this.holder), false);
        }

        return save(this.holder).thenApply(v -> true);
    }

    @Override
    public CompletableFuture<Boolean> clearPermissions(ImmutableContextSet contexts) {
        Objects.requireNonNull(contexts, "contexts");
        if (!this.holder.clearNodes(this.type, contexts)) {
            return CompletableFuture.completedFuture(false);
        }

        if (this.holder.getType() == HolderType.USER) {
            this.service.getPlugin().getUserManager().giveDefaultIfNeeded(((User) this.holder), false);
        }

        return save(this.holder).thenApply(v -> true);
    }

    @Override
    public ImmutableMap<ImmutableContextSet, ImmutableList<LPSubjectReference>> getAllParents() {
        ImmutableMap.Builder<ImmutableContextSet, ImmutableList<LPSubjectReference>> ret = ImmutableMap.builder();
        for (Map.Entry<ImmutableContextSet, ? extends Collection<? extends Node>> entry : this.holder.getData(this.type).immutable().asMap().entrySet()) {
            ImmutableList.Builder<LPSubjectReference> builder = ImmutableList.builder();
            for (Node n : entry.getValue()) {
                if (n instanceof InheritanceNode) {
                    builder.add(this.service.getGroupSubjects().loadSubject(((InheritanceNode) n).getGroupName()).join().toReference());
                }
            }
            ret.put(entry.getKey(), builder.build());
        }
        return ret.build();
    }

    @Override
    public ImmutableList<LPSubjectReference> getParents(ImmutableContextSet contexts) {
        ImmutableList.Builder<LPSubjectReference> builder = ImmutableList.builder();
        for (Node n : this.holder.getData(this.type).immutable().get(contexts)) {
            if (n instanceof InheritanceNode) {
                builder.add(this.service.getGroupSubjects().loadSubject(((InheritanceNode) n).getGroupName()).join().toReference());
            }
        }
        return builder.build();
    }

    @Override
    public CompletableFuture<Boolean> addParent(ImmutableContextSet contexts, LPSubjectReference subject) {
        Objects.requireNonNull(contexts, "contexts");
        Objects.requireNonNull(subject, "subject");

        if (!subject.getCollectionIdentifier().equals(PermissionService.SUBJECTS_GROUP)) {
            return CompletableFuture.completedFuture(false);
        }

        Node node = Inheritance.builder(subject.getSubjectIdentifier())
                .withContext(contexts)
                .build();

        if (!this.holder.setPermission(this.type, node, true).wasSuccessful()) {
            return CompletableFuture.completedFuture(false);
        }

        return save(this.holder).thenApply(v -> true);
    }

    @Override
    public CompletableFuture<Boolean> removeParent(ImmutableContextSet contexts, LPSubjectReference subject) {
        Objects.requireNonNull(contexts, "contexts");
        Objects.requireNonNull(subject, "subject");

        if (!subject.getCollectionIdentifier().equals(PermissionService.SUBJECTS_GROUP)) {
            return CompletableFuture.completedFuture(false);
        }

        Node node = Inheritance.builder(subject.getSubjectIdentifier())
                .withContext(contexts)
                .build();

        if (!this.holder.unsetPermission(this.type, node).wasSuccessful()) {
            return CompletableFuture.completedFuture(false);
        }

        return save(this.holder).thenApply(v -> true);
    }

    @Override
    public CompletableFuture<Boolean> clearParents() {
        boolean ret;
        switch (this.type) {
            case NORMAL:
                ret = this.holder.clearNormalParents(null, true);
                break;
            case TRANSIENT:
                ret = streamNodes()
                        .filter(n -> n instanceof InheritanceNode)
                        .peek(n -> this.holder.unsetPermission(DataType.TRANSIENT, n))
                        .findAny().isPresent();
                break;
            default:
                throw new AssertionError();
        }

        if (!ret) {
            return CompletableFuture.completedFuture(false);
        }

        return save(this.holder).thenApply(v -> true);
    }

    @Override
    public CompletableFuture<Boolean> clearParents(ImmutableContextSet contexts) {
        Objects.requireNonNull(contexts, "contexts");
        boolean ret;
        switch (this.type) {
            case NORMAL:
                ret = this.holder.clearNormalParents(contexts, true);
                break;
            case TRANSIENT:
                ret = streamNodes()
                        .filter(n -> n instanceof InheritanceNode)
                        .filter(n -> n.getContexts().equals(contexts))
                        .peek(n -> this.holder.unsetPermission(DataType.TRANSIENT, n))
                        .findAny().isPresent();
                break;
            default:
                throw new AssertionError();
        }

        if (!ret) {
            return CompletableFuture.completedFuture(false);
        }

        return save(this.holder).thenApply(v -> true);
    }

    @Override
    public ImmutableMap<ImmutableContextSet, ImmutableMap<String, String>> getAllOptions() {
        ImmutableMap.Builder<ImmutableContextSet, ImmutableMap<String, String>> ret = ImmutableMap.builder();
        for (Map.Entry<ImmutableContextSet, ? extends Collection<? extends Node>> entry : this.holder.getData(this.type).immutable().asMap().entrySet()) {
            ret.put(entry.getKey(), nodesToOptions(entry.getValue()));
        }
        return ret.build();
    }

    @Override
    public ImmutableMap<String, String> getOptions(ImmutableContextSet contexts) {
        return nodesToOptions(this.holder.getData(this.type).immutable().get(contexts));
    }

    private static ImmutableMap<String, String> nodesToOptions(Iterable<? extends Node> nodes) {
        Map<String, String> builder = new HashMap<>();
        int maxPrefixPriority = Integer.MIN_VALUE;
        int maxSuffixPriority = Integer.MIN_VALUE;

        for (Node n : nodes) {
            if (!n.getValue()) continue;
            if (!NodeType.META_OR_CHAT_META.matches(n)) continue;

            if (n instanceof PrefixNode) {
                PrefixNode pn = (PrefixNode) n;
                if (pn.getPriority() > maxPrefixPriority) {
                    builder.put(Prefix.NODE_KEY, pn.getMetaValue());
                    maxPrefixPriority = pn.getPriority();
                }
                continue;
            }

            if (n instanceof SuffixNode) {
                SuffixNode sn = (SuffixNode) n;
                if (sn.getPriority() > maxSuffixPriority) {
                    builder.put(Suffix.NODE_KEY, sn.getMetaValue());
                    maxSuffixPriority = sn.getPriority();
                }
                continue;
            }

            if (n instanceof MetaNode) {
                MetaNode mn = (MetaNode) n;
                builder.put(mn.getMetaKey(), mn.getMetaValue());
            }
        }

        return ImmutableMap.copyOf(builder);
    }

    @Override
    public CompletableFuture<Boolean> setOption(ImmutableContextSet contexts, String key, String value) {
        Objects.requireNonNull(contexts, "contexts");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");

        Node node;
        if (key.equalsIgnoreCase(Prefix.NODE_KEY) || key.equalsIgnoreCase(Suffix.NODE_KEY)) {
            // special handling.
            ChatMetaType type = ChatMetaType.valueOf(key.toUpperCase());

            // remove all prefixes/suffixes from the user
            streamNodes()
                    .filter(n -> type.nodeType().matches(n))
                    .filter(n -> n.getContexts().equals(contexts))
                    .forEach(n -> this.holder.unsetPermission(this.type, n));

            MetaAccumulator metaAccumulator = this.holder.accumulateMeta(null, QueryOptions.defaultContextualOptions().toBuilder().context(contexts).build());
            metaAccumulator.complete();
            int priority = metaAccumulator.getChatMeta(type).keySet().stream().mapToInt(e -> e).max().orElse(0);
            priority += 10;

            ChatMetaNode.Builder<?, ?> builder = type == ChatMetaType.PREFIX ? Prefix.builder(priority, value) : Suffix.builder(priority, value);
            node = builder.withContext(contexts).build();
        } else {
            // standard remove
            streamNodes()
                    .filter(n -> n instanceof MetaNode && ((MetaNode) n).getMetaKey().equals(key))
                    .filter(n -> n.getContexts().equals(contexts))
                    .forEach(n -> this.holder.unsetPermission(this.type, n));

            node = Meta.builder(key, value).withContext(contexts).build();
        }

        this.holder.setPermission(this.type, node, true);
        return save(this.holder).thenApply(v -> true);
    }

    @Override
    public CompletableFuture<Boolean> unsetOption(ImmutableContextSet contexts, String key) {
        Objects.requireNonNull(contexts, "contexts");
        Objects.requireNonNull(key, "key");

        streamNodes()
                .filter(n -> {
                    if (key.equalsIgnoreCase(Prefix.NODE_KEY)) {
                        return n instanceof PrefixNode;
                    } else if (key.equalsIgnoreCase(Suffix.NODE_KEY)) {
                        return n instanceof SuffixNode;
                    } else {
                        return n instanceof MetaNode && ((MetaNode) n).getMetaKey().equals(key);
                    }
                })
                .filter(n -> n.getContexts().equals(contexts))
                .forEach(n -> this.holder.unsetPermission(this.type, n));

        return save(this.holder).thenApply(v -> true);
    }

    @Override
    public CompletableFuture<Boolean> clearOptions(ImmutableContextSet contexts) {
        Objects.requireNonNull(contexts, "contexts");

        boolean success = streamNodes()
                .filter(NodeType.META_OR_CHAT_META::matches)
                .filter(n -> n.getContexts().equals(contexts))
                .peek(n -> this.holder.unsetPermission(this.type, n))
                .findAny().isPresent();

        if (!success) {
            return CompletableFuture.completedFuture(false);
        }

        return save(this.holder).thenApply(v -> true);
    }

    @Override
    public CompletableFuture<Boolean> clearOptions() {
        boolean success = streamNodes()
                .filter(NodeType.META_OR_CHAT_META::matches)
                .peek(n -> this.holder.unsetPermission(this.type, n))
                .findAny().isPresent();

        if (!success) {
            return CompletableFuture.completedFuture(false);
        }

        return save(this.holder).thenApply(v -> true);
    }

    private CompletableFuture<Void> save(PermissionHolder t) {
        // if the holder is a group, invalidate caches.
        if (t.getType() == HolderType.GROUP) {
            this.service.getPlugin().getGroupManager().invalidateAllGroupCaches();
            this.service.getPlugin().getUserManager().invalidateAllUserCaches();
        }

        // no further action required for transient types
        if (this.type == DataType.TRANSIENT) {
            return CompletableFuture.completedFuture(null);
        }

        if (t.getType() == HolderType.USER) {
            User user = ((User) t);
            return this.service.getPlugin().getStorage().saveUser(user);
        } else {
            Group group = ((Group) t);
            return this.service.getPlugin().getStorage().saveGroup(group);
        }
    }
}
