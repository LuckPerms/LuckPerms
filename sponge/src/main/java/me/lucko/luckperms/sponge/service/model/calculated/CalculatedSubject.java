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

package me.lucko.luckperms.sponge.service.model.calculated;

import com.google.common.collect.ImmutableList;
import me.lucko.luckperms.common.cacheddata.type.MetaAccumulator;
import me.lucko.luckperms.common.graph.TraversalAlgorithm;
import me.lucko.luckperms.common.query.QueryOptionsImpl;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;
import me.lucko.luckperms.sponge.LPSpongePlugin;
import me.lucko.luckperms.sponge.service.inheritance.SubjectInheritanceGraph;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectReference;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public abstract class CalculatedSubject implements LPSubject {
    private final LPSpongePlugin plugin;
    private final CalculatedSubjectCachedDataManager cachedData;

    protected CalculatedSubject(LPSpongePlugin plugin) {
        this.plugin = plugin;
        this.cachedData = new CalculatedSubjectCachedDataManager(this, plugin);
    }

    @Override
    public LPSubject getDefaults() {
        return this.plugin.getService().getDefaultSubjects().getTypeDefaults(getParentCollection().getIdentifier());
    }

    @Override
    public abstract CalculatedSubjectData getSubjectData();

    @Override
    public abstract CalculatedSubjectData getTransientSubjectData();

    public Map<String, Node> getCombinedPermissions(QueryOptions filter) {
        Map<String, Node> permissions;
        Map<String, Node> merging;
        switch (getParentCollection().getResolutionOrder()) {
            case TRANSIENT_FIRST:
                permissions = getTransientSubjectData().resolvePermissions(filter);
                merging = getSubjectData().resolvePermissions(filter);
                break;
            case TRANSIENT_LAST:
                permissions = getSubjectData().resolvePermissions(filter);
                merging = getTransientSubjectData().resolvePermissions(filter);
                break;
            default:
                throw new AssertionError();
        }

        for (Map.Entry<String, Node> entry : merging.entrySet()) {
            permissions.putIfAbsent(entry.getKey(), entry.getValue());
        }
        return permissions;
    }

    public void resolveAllPermissions(Map<String, Node> accumulator, QueryOptions filter) {
        SubjectInheritanceGraph graph = new SubjectInheritanceGraph(filter);

        Iterable<CalculatedSubject> traversal = graph.traverse(TraversalAlgorithm.DEPTH_FIRST_PRE_ORDER, this);
        for (CalculatedSubject subject : traversal) {
            for (Map.Entry<String, Node> entry : subject.getCombinedPermissions(filter).entrySet()) {
                accumulator.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
    }

    public Set<LPSubjectReference> getCombinedParents(QueryOptions filter) {
        Set<LPSubjectReference> parents;
        Set<LPSubjectReference> merging;
        switch (getParentCollection().getResolutionOrder()) {
            case TRANSIENT_FIRST:
                parents = getTransientSubjectData().resolveParents(filter);
                merging = getSubjectData().resolveParents(filter);
                break;
            case TRANSIENT_LAST:
                parents = getSubjectData().resolveParents(filter);
                merging = getTransientSubjectData().resolveParents(filter);
                break;
            default:
                throw new AssertionError();
        }

        parents.addAll(merging);
        return parents;
    }

    public Set<LPSubjectReference> resolveAllParents(QueryOptions filter) {
        SubjectInheritanceGraph graph = new SubjectInheritanceGraph(filter);
        Set<LPSubjectReference> result = new LinkedHashSet<>();

        Iterable<CalculatedSubject> traversal = graph.traverse(TraversalAlgorithm.DEPTH_FIRST_PRE_ORDER, this);
        for (CalculatedSubject subject : traversal) {
            result.addAll(subject.getCombinedParents(filter));
        }

        return result;
    }

    public Map<String, MetaNode> getCombinedOptions(QueryOptions filter) {
        Map<String, MetaNode> options;
        Map<String, MetaNode> merging;
        switch (getParentCollection().getResolutionOrder()) {
            case TRANSIENT_FIRST:
                options = getTransientSubjectData().resolveOptions(filter);
                merging = getSubjectData().resolveOptions(filter);
                break;
            case TRANSIENT_LAST:
                options = getSubjectData().resolveOptions(filter);
                merging = getTransientSubjectData().resolveOptions(filter);
                break;
            default:
                throw new AssertionError();
        }

        for (Map.Entry<String, MetaNode> entry : merging.entrySet()) {
            options.putIfAbsent(entry.getKey(), entry.getValue());
        }
        return options;
    }

    public Map<String, String> resolveAllOptions(QueryOptions filter) {
        SubjectInheritanceGraph graph = new SubjectInheritanceGraph(filter);
        Map<String, String> result = new HashMap<>();

        Iterable<CalculatedSubject> traversal = graph.traverse(TraversalAlgorithm.DEPTH_FIRST_PRE_ORDER, this);
        for (CalculatedSubject subject : traversal) {
            for (MetaNode entry : subject.getCombinedOptions(filter).values()) {
                result.putIfAbsent(entry.getMetaKey(), entry.getMetaValue());
            }
        }

        return result;
    }

    public void resolveAllOptions(MetaAccumulator accumulator, QueryOptions filter) {
        SubjectInheritanceGraph graph = new SubjectInheritanceGraph(filter);

        Iterable<CalculatedSubject> traversal = graph.traverse(TraversalAlgorithm.DEPTH_FIRST_PRE_ORDER, this);
        for (CalculatedSubject subject : traversal) {
            for (MetaNode entry : subject.getCombinedOptions(filter).values()) {
                accumulator.accumulateNode(entry);
            }
        }

        accumulator.complete();
    }

    @Override
    public Tristate getPermissionValue(QueryOptions options, String permission) {
        return this.cachedData.getPermissionData(options).checkPermission(permission, CheckOrigin.INTERNAL).result();
    }

    @Override
    public Tristate getPermissionValue(ImmutableContextSet contexts, String permission) {
        return getPermissionValue(QueryOptionsImpl.DEFAULT_CONTEXTUAL.toBuilder().context(contexts).build(), permission);
    }

    @Override
    public boolean isChildOf(ImmutableContextSet contexts, LPSubjectReference parent) {
        return resolveAllParents(QueryOptionsImpl.DEFAULT_CONTEXTUAL.toBuilder().context(contexts).build()).contains(parent);
    }

    @Override
    public ImmutableList<LPSubjectReference> getParents(ImmutableContextSet contexts) {
        return ImmutableList.copyOf(resolveAllParents(QueryOptionsImpl.DEFAULT_CONTEXTUAL.toBuilder().context(contexts).build()));
    }

    @Override
    public Optional<String> getOption(ImmutableContextSet contexts, String key) {
        return Optional.ofNullable(this.cachedData.getMetaData(QueryOptionsImpl.DEFAULT_CONTEXTUAL.toBuilder().context(contexts).build()).getMetaValue(key, CheckOrigin.PLATFORM_API).result());
    }

    @Override
    public void performCacheCleanup() {
        this.cachedData.performCacheCleanup();
    }

    @Override
    public void invalidateCaches() {
        this.cachedData.invalidate();
    }

}
