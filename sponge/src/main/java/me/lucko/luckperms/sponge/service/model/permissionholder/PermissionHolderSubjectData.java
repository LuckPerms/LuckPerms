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
import me.lucko.luckperms.common.query.QueryOptionsImpl;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.service.ProxyFactory;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectData;
import me.lucko.luckperms.sponge.service.model.LPSubjectReference;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.ChatMetaType;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import net.luckperms.api.util.Tristate;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.SubjectData;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

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
        ImmutableMap.Builder<ImmutableContextSet, ImmutableMap<String, Boolean>> permissions = ImmutableMap.builder();
        for (Map.Entry<ImmutableContextSet, Collection<Node>> entry : this.holder.getData(this.type).asMap().entrySet()) {
            ImmutableMap.Builder<String, Boolean> builder = ImmutableMap.builder();
            for (Node n : entry.getValue()) {
                builder.put(n.getKey(), n.getValue());
            }
            permissions.put(entry.getKey(), builder.build());
        }
        return permissions.build();
    }

    @Override
    public ImmutableMap<String, Boolean> getPermissions(ImmutableContextSet contexts) {
        ImmutableMap.Builder<String, Boolean> builder = ImmutableMap.builder();
        for (Node n : this.holder.getData(this.type).nodesInContext(contexts)) {
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
            this.holder.unsetNode(this.type, node);
            return save(this.holder).thenApply(v -> true);
        }

        Node node = NodeBuilders.determineMostApplicable(permission).value(tristate.asBoolean()).withContext(contexts).build();
        // unset the inverse, to allow false -> true, true -> false overrides.
        this.holder.unsetNode(this.type, node);
        this.holder.setNode(this.type, node, true);
        return save(this.holder).thenApply(v -> true);
    }

    @Override
    public CompletableFuture<Boolean> clearPermissions() {
        if (!this.holder.clearNodes(this.type, null, true)) {
            return CompletableFuture.completedFuture(false);
        }

        return save(this.holder).thenApply(v -> true);
    }

    @Override
    public CompletableFuture<Boolean> clearPermissions(ImmutableContextSet contexts) {
        Objects.requireNonNull(contexts, "contexts");
        if (!this.holder.clearNodes(this.type, contexts, true)) {
            return CompletableFuture.completedFuture(false);
        }

        return save(this.holder).thenApply(v -> true);
    }

    @Override
    public ImmutableMap<ImmutableContextSet, ImmutableList<LPSubjectReference>> getAllParents() {
        ImmutableMap.Builder<ImmutableContextSet, ImmutableList<LPSubjectReference>> parents = ImmutableMap.builder();
        for (Map.Entry<ImmutableContextSet, Collection<InheritanceNode>> entry : this.holder.getData(this.type).inheritanceAsMap().entrySet()) {
            ImmutableList.Builder<LPSubjectReference> builder = ImmutableList.builder();
            for (InheritanceNode n : entry.getValue()) {
                builder.add(this.service.getGroupSubjects().loadSubject(n.getGroupName()).join().toReference());
            }
            parents.put(entry.getKey(), builder.build());
        }
        return parents.build();
    }

    @Override
    public ImmutableList<LPSubjectReference> getParents(ImmutableContextSet contexts) {
        ImmutableList.Builder<LPSubjectReference> builder = ImmutableList.builder();
        for (InheritanceNode n : this.holder.getData(this.type).inheritanceNodesInContext(contexts)) {
            builder.add(this.service.getGroupSubjects().loadSubject(n.getGroupName()).join().toReference());
        }
        return builder.build();
    }

    @Override
    public CompletableFuture<Boolean> addParent(ImmutableContextSet contexts, LPSubjectReference subject) {
        Objects.requireNonNull(contexts, "contexts");
        Objects.requireNonNull(subject, "subject");

        if (!subject.collectionIdentifier().equals(PermissionService.SUBJECTS_GROUP)) {
            return CompletableFuture.completedFuture(false);
        }

        Node node = Inheritance.builder(subject.subjectIdentifier())
                .withContext(contexts)
                .build();

        if (!this.holder.setNode(this.type, node, true).wasSuccessful()) {
            return CompletableFuture.completedFuture(false);
        }

        return save(this.holder).thenApply(v -> true);
    }

    @Override
    public CompletableFuture<Boolean> removeParent(ImmutableContextSet contexts, LPSubjectReference subject) {
        Objects.requireNonNull(contexts, "contexts");
        Objects.requireNonNull(subject, "subject");

        if (!subject.collectionIdentifier().equals(PermissionService.SUBJECTS_GROUP)) {
            return CompletableFuture.completedFuture(false);
        }

        Node node = Inheritance.builder(subject.subjectIdentifier())
                .withContext(contexts)
                .build();

        if (!this.holder.unsetNode(this.type, node).wasSuccessful()) {
            return CompletableFuture.completedFuture(false);
        }

        return save(this.holder).thenApply(v -> true);
    }

    @Override
    public CompletableFuture<Boolean> clearParents() {
        if (!this.holder.removeIf(this.type, null, NodeType.INHERITANCE::matches, true)) {
            return CompletableFuture.completedFuture(false);
        }

        return save(this.holder).thenApply(v -> true);
    }

    @Override
    public CompletableFuture<Boolean> clearParents(ImmutableContextSet contexts) {
        Objects.requireNonNull(contexts, "contexts");

        if (!this.holder.removeIf(this.type, contexts, NodeType.INHERITANCE::matches, true)) {
            return CompletableFuture.completedFuture(false);
        }

        return save(this.holder).thenApply(v -> true);
    }

    @Override
    public ImmutableMap<ImmutableContextSet, ImmutableMap<String, String>> getAllOptions() {
        ImmutableMap.Builder<ImmutableContextSet, ImmutableMap<String, String>> options = ImmutableMap.builder();
        for (Map.Entry<ImmutableContextSet, Collection<Node>> entry : this.holder.getData(this.type).asMap().entrySet()) {
            options.put(entry.getKey(), nodesToOptions(entry.getValue()));
        }
        return options.build();
    }

    @Override
    public ImmutableMap<String, String> getOptions(ImmutableContextSet contexts) {
        return nodesToOptions(this.holder.getData(this.type).nodesInContext(contexts));
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
            ChatMetaType type = ChatMetaType.valueOf(key.toUpperCase(Locale.ROOT));

            // remove all prefixes/suffixes from the user
            this.holder.removeIf(this.type, contexts, type.nodeType()::matches, false);

            MetaAccumulator metaAccumulator = this.holder.accumulateMeta(QueryOptionsImpl.DEFAULT_CONTEXTUAL.toBuilder().context(contexts).build());
            int priority = metaAccumulator.getChatMeta(type).keySet().stream().mapToInt(e -> e).max().orElse(0);
            priority += 10;

            node = type.builder(value, priority).withContext(contexts).build();
        } else {
            // standard remove
            this.holder.removeIf(this.type, contexts, NodeType.META.predicate(n -> n.getMetaKey().equals(key)), false);
            node = Meta.builder(key, value).withContext(contexts).build();
        }

        this.holder.setNode(this.type, node, true);
        return save(this.holder).thenApply(v -> true);
    }

    @Override
    public CompletableFuture<Boolean> unsetOption(ImmutableContextSet contexts, String key) {
        Objects.requireNonNull(contexts, "contexts");
        Objects.requireNonNull(key, "key");

        Predicate<? super Node> test;
        if (key.equalsIgnoreCase(Prefix.NODE_KEY)) {
            test = NodeType.PREFIX::matches;
        } else if (key.equalsIgnoreCase(Suffix.NODE_KEY)) {
            test = NodeType.SUFFIX::matches;
        } else {
            test = NodeType.META.predicate(mn -> mn.getMetaKey().equals(key));
        }

        if (!this.holder.removeIf(this.type, contexts, test, false)) {
            return CompletableFuture.completedFuture(false);
        }

        return save(this.holder).thenApply(v -> true);
    }

    @Override
    public CompletableFuture<Boolean> clearOptions(ImmutableContextSet contexts) {
        Objects.requireNonNull(contexts, "contexts");

        if (!this.holder.removeIf(this.type, contexts, NodeType.META_OR_CHAT_META::matches, false)) {
            return CompletableFuture.completedFuture(false);
        }

        return save(this.holder).thenApply(v -> true);
    }

    @Override
    public CompletableFuture<Boolean> clearOptions() {
        if (!this.holder.removeIf(this.type, null, NodeType.META_OR_CHAT_META::matches, false)) {
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
            User user = (User) t;
            return this.service.getPlugin().getStorage().saveUser(user);
        } else {
            Group group = (Group) t;
            return this.service.getPlugin().getStorage().saveGroup(group);
        }
    }
}
