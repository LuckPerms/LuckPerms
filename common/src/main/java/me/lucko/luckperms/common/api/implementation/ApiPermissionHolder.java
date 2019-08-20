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

package me.lucko.luckperms.common.api.implementation;

import com.google.common.collect.ImmutableSortedSet;

import me.lucko.luckperms.api.cacheddata.CachedDataManager;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.api.model.DataMutateResult;
import me.lucko.luckperms.api.model.DataType;
import me.lucko.luckperms.api.model.TemporaryDataMutateResult;
import me.lucko.luckperms.api.model.TemporaryMergeBehaviour;
import me.lucko.luckperms.api.node.Node;
import me.lucko.luckperms.api.node.NodeEqualityPredicate;
import me.lucko.luckperms.api.node.NodeType;
import me.lucko.luckperms.api.node.Tristate;
import me.lucko.luckperms.api.query.QueryOptions;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.node.comparator.NodeWithContextComparator;
import me.lucko.luckperms.common.node.utils.NodeTools;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class ApiPermissionHolder implements me.lucko.luckperms.api.model.PermissionHolder {
    private final PermissionHolder handle;

    private final Normal normalData;
    private final Transient transientData;

    ApiPermissionHolder(PermissionHolder handle) {
        this.handle = Objects.requireNonNull(handle, "handle");
        this.normalData = new Normal();
        this.transientData = new Transient();
    }

    PermissionHolder getHandle() {
        return this.handle;
    }

    @Override
    public @NonNull Identifier getIdentifier() {
        return this.handle.getIdentifier();
    }

    @Override
    public @NonNull String getFriendlyName() {
        return this.handle.getPlainDisplayName();
    }

    @Override
    public @NonNull CachedDataManager getCachedData() {
        return this.handle.getCachedData();
    }

    @Override
    public @NonNull CompletableFuture<Void> refreshCachedData() {
        return CompletableFuture.runAsync(() -> this.handle.getCachedData().invalidate());
    }

    @Override
    public Data getData(@NonNull DataType dataType) {
        switch (dataType) {
            case NORMAL:
                return this.normalData;
            case TRANSIENT:
                return this.transientData;
            default:
                throw new AssertionError();
        }
    }

    @Override
    public @NonNull Data data() {
        return this.normalData;
    }

    @Override
    public @NonNull Data transientData() {
        return this.transientData;
    }

    @Override
    public @NonNull List<Node> getNodes() {
        return this.handle.getOwnNodes(QueryOptions.nonContextual());
    }

    @Override
    public @NonNull SortedSet<Node> getDistinctNodes() {
        return this.handle.getOwnNodesSorted(QueryOptions.nonContextual());
    }

    @Override
    public @NonNull List<Node> resolveInheritedNodes(@NonNull QueryOptions queryOptions) {
        return this.handle.resolveInheritances(queryOptions);
    }

    @Override
    public @NonNull SortedSet<Node> resolveDistinctInheritedNodes(@NonNull QueryOptions queryOptions) {
        List<Node> entries = this.handle.getAllEntries(queryOptions);

        NodeTools.removeSamePermission(entries.iterator());
        SortedSet<Node> ret = new TreeSet<>(NodeWithContextComparator.reverse());
        ret.addAll(entries);

        return ImmutableSortedSet.copyOfSorted(ret);
    }

    @Override
    public void auditTemporaryPermissions() {
        this.handle.auditTemporaryPermissions();
    }

    @Override
    public @NonNull Tristate inheritsNode(@NonNull Node node, @NonNull NodeEqualityPredicate equalityPredicate) {
        return this.handle.inheritsPermission(node, equalityPredicate);
    }

    private abstract class AbstractData implements Data {
        private final DataType dataType;

        private AbstractData(DataType dataType) {
            this.dataType = dataType;
        }

        @Override
        public @NonNull Map<ImmutableContextSet, Collection<Node>> getNodes() {
            return ApiPermissionHolder.this.handle.getData(this.dataType).immutable().asMap();
        }

        @Override
        public @NonNull Set<Node> getFlattenedNodes() {
            return ApiPermissionHolder.this.handle.getData(this.dataType).asSet();
        }

        @Override
        public @NonNull Tristate hasNode(@NonNull Node node, @NonNull NodeEqualityPredicate equalityPredicate) {
            return ApiPermissionHolder.this.handle.hasPermission(this.dataType, node, equalityPredicate);
        }

        @Override
        public @NonNull DataMutateResult addNode(@NonNull Node node) {
            return ApiPermissionHolder.this.handle.setPermission(this.dataType, node, true);
        }

        @Override
        public @NonNull TemporaryDataMutateResult addNode(@NonNull Node node, @NonNull TemporaryMergeBehaviour temporaryMergeBehaviour) {
            return ApiPermissionHolder.this.handle.setPermission(this.dataType, node, temporaryMergeBehaviour);
        }

        @Override
        public @NonNull DataMutateResult removeNode(@NonNull Node node) {
            return ApiPermissionHolder.this.handle.unsetPermission(this.dataType, node);
        }

        @Override
        public void clearMatching(@NonNull Predicate<? super Node> test) {
            ApiPermissionHolder.this.handle.removeIf(this.dataType, null, test, null);
        }

        @Override
        public void clearNodes() {
            ApiPermissionHolder.this.handle.clearNodes(this.dataType, null);
        }

        @Override
        public void clearNodes(@NonNull ContextSet contextSet) {
            ApiPermissionHolder.this.handle.clearNodes(this.dataType, contextSet);
        }

        @Override
        public void clearMeta() {
            ApiPermissionHolder.this.handle.removeIf(this.dataType, null, NodeType.META_OR_CHAT_META::matches, null);
        }

        @Override
        public void clearMeta(@NonNull ContextSet contextSet) {
            ApiPermissionHolder.this.handle.removeIf(this.dataType, contextSet, NodeType.META_OR_CHAT_META::matches, null);
        }
    }

    private final class Normal extends AbstractData implements Data {
        private Normal() {
            super(DataType.NORMAL);
        }

        @Override
        public void clearParents() {
            ApiPermissionHolder.this.handle.clearNormalParents(null, true);
        }

        @Override
        public void clearParents(@NonNull ContextSet contextSet) {
            ApiPermissionHolder.this.handle.clearNormalParents(contextSet, true);
        }

    }

    private final class Transient extends AbstractData implements Data {
        private Transient() {
            super(DataType.TRANSIENT);
        }

        @Override
        public void clearParents() {
            ApiPermissionHolder.this.handle.removeIf(DataType.TRANSIENT, null, NodeType.INHERITANCE::matches, null);
        }

        @Override
        public void clearParents(@NonNull ContextSet contextSet) {
            ApiPermissionHolder.this.handle.removeIf(DataType.TRANSIENT, contextSet, NodeType.INHERITANCE::matches, null);
        }
    }

}
