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

package me.lucko.luckperms.sponge.service.calculated;

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.caching.MetaContexts;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.api.metastacking.MetaStackDefinition;
import me.lucko.luckperms.common.caching.AbstractCachedData;
import me.lucko.luckperms.common.caching.type.MetaAccumulator;
import me.lucko.luckperms.common.calculators.CalculatorFactory;
import me.lucko.luckperms.common.calculators.PermissionCalculator;
import me.lucko.luckperms.common.calculators.PermissionCalculatorMetadata;
import me.lucko.luckperms.common.graph.TraversalAlgorithm;
import me.lucko.luckperms.common.metastacking.SimpleMetaStackDefinition;
import me.lucko.luckperms.common.metastacking.StandardStackElements;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.processors.MapProcessor;
import me.lucko.luckperms.common.processors.PermissionProcessor;
import me.lucko.luckperms.common.processors.WildcardProcessor;
import me.lucko.luckperms.common.verbose.CheckOrigin;
import me.lucko.luckperms.sponge.processors.FixedDefaultsProcessor;
import me.lucko.luckperms.sponge.processors.SpongeWildcardProcessor;
import me.lucko.luckperms.sponge.service.inheritance.SubjectInheritanceGraph;
import me.lucko.luckperms.sponge.service.inheritance.SubjectInheritanceGraphs;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.reference.LPSubjectReference;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public abstract class CalculatedSubject implements LPSubject {
    private static final MetaStackDefinition DEFAULT_META_STACK = new SimpleMetaStackDefinition(
            ImmutableList.of(StandardStackElements.HIGHEST_PRIORITY),
            "", "", ""
    );

    private final LuckPermsPlugin plugin;
    private final SubjectCachedData cachedData;

    protected CalculatedSubject(LuckPermsPlugin plugin) {
        this.plugin = plugin;
        this.cachedData = new SubjectCachedData(plugin);
    }

    public abstract CalculatedSubjectData getSubjectData();
    public abstract CalculatedSubjectData getTransientSubjectData();

    public Map<String, Boolean> getCombinedPermissions(ContextSet filter) {
        Map<String, Boolean> permissions;
        Map<String, Boolean> merging;
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

        for (Map.Entry<String, Boolean> entry : merging.entrySet()) {
            permissions.putIfAbsent(entry.getKey(), entry.getValue());
        }
        return permissions;
    }

    public Map<String, Boolean> getCombinedPermissions() {
        Map<String, Boolean> permissions;
        Map<String, Boolean> merging;
        switch (getParentCollection().getResolutionOrder()) {
            case TRANSIENT_FIRST:
                permissions = getTransientSubjectData().resolvePermissions();
                merging = getSubjectData().resolvePermissions();
                break;
            case TRANSIENT_LAST:
                permissions = getSubjectData().resolvePermissions();
                merging = getTransientSubjectData().resolvePermissions();
                break;
            default:
                throw new AssertionError();
        }

        for (Map.Entry<String, Boolean> entry : merging.entrySet()) {
            permissions.putIfAbsent(entry.getKey(), entry.getValue());
        }
        return permissions;
    }

    public Map<String, Boolean> resolveAllPermissions(ImmutableContextSet filter) {
        SubjectInheritanceGraph graph = SubjectInheritanceGraphs.getGraph(filter);
        Map<String, Boolean> result = new HashMap<>();

        Iterable<CalculatedSubject> traversal = graph.traverse(TraversalAlgorithm.DEPTH_FIRST_PRE_ORDER, this);
        for (CalculatedSubject subject : traversal) {
            for (Map.Entry<String, Boolean> entry : subject.getCombinedPermissions(filter).entrySet()) {
                result.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    public Map<String, Boolean> resolveAllPermissions() {
        SubjectInheritanceGraph graph = SubjectInheritanceGraphs.getGraph();
        Map<String, Boolean> result = new HashMap<>();

        Iterable<CalculatedSubject> traversal = graph.traverse(TraversalAlgorithm.DEPTH_FIRST_PRE_ORDER, this);
        for (CalculatedSubject subject : traversal) {
            for (Map.Entry<String, Boolean> entry : subject.getCombinedPermissions().entrySet()) {
                result.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    public Set<LPSubjectReference> getCombinedParents(ContextSet filter) {
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

        for (LPSubjectReference entry : merging) {
            if (!parents.contains(entry)) {
                parents.add(entry);
            }
        }
        return parents;
    }

    public Set<LPSubjectReference> getCombinedParents() {
        Set<LPSubjectReference> parents;
        Set<LPSubjectReference> merging;
        switch (getParentCollection().getResolutionOrder()) {
            case TRANSIENT_FIRST:
                parents = getTransientSubjectData().resolveParents();
                merging = getSubjectData().resolveParents();
                break;
            case TRANSIENT_LAST:
                parents = getSubjectData().resolveParents();
                merging = getTransientSubjectData().resolveParents();
                break;
            default:
                throw new AssertionError();
        }

        for (LPSubjectReference entry : merging) {
            if (!parents.contains(entry)) {
                parents.add(entry);
            }
        }
        return parents;
    }

    public Set<LPSubjectReference> resolveAllParents(ImmutableContextSet filter) {
        SubjectInheritanceGraph graph = SubjectInheritanceGraphs.getGraph(filter);
        Set<LPSubjectReference> result = new LinkedHashSet<>();

        Iterable<CalculatedSubject> traversal = graph.traverse(TraversalAlgorithm.DEPTH_FIRST_PRE_ORDER, this);
        for (CalculatedSubject subject : traversal) {
            for (LPSubjectReference entry : subject.getCombinedParents(filter)) {
                if (!result.contains(entry)) {
                    result.add(entry);
                }
            }
        }

        return result;
    }

    public Set<LPSubjectReference> resolveAllParents() {
        SubjectInheritanceGraph graph = SubjectInheritanceGraphs.getGraph();
        Set<LPSubjectReference> result = new LinkedHashSet<>();

        Iterable<CalculatedSubject> traversal = graph.traverse(TraversalAlgorithm.DEPTH_FIRST_PRE_ORDER, this);
        for (CalculatedSubject subject : traversal) {
            for (LPSubjectReference entry : subject.getCombinedParents()) {
                if (!result.contains(entry)) {
                    result.add(entry);
                }
            }
        }

        return result;
    }

    public Map<String, String> getCombinedOptions(ContextSet filter) {
        Map<String, String> options;
        Map<String, String> merging;
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

        for (Map.Entry<String, String> entry : merging.entrySet()) {
            options.putIfAbsent(entry.getKey(), entry.getValue());
        }
        return options;
    }

    public Map<String, String> getCombinedOptions() {
        Map<String, String> options;
        Map<String, String> merging;
        switch (getParentCollection().getResolutionOrder()) {
            case TRANSIENT_FIRST:
                options = getTransientSubjectData().resolveOptions();
                merging = getSubjectData().resolveOptions();
                break;
            case TRANSIENT_LAST:
                options = getSubjectData().resolveOptions();
                merging = getTransientSubjectData().resolveOptions();
                break;
            default:
                throw new AssertionError();
        }

        for (Map.Entry<String, String> entry : merging.entrySet()) {
            options.putIfAbsent(entry.getKey(), entry.getValue());
        }
        return options;
    }

    public Map<String, String> resolveAllOptions(ImmutableContextSet filter) {
        SubjectInheritanceGraph graph = SubjectInheritanceGraphs.getGraph(filter);
        Map<String, String> result = new HashMap<>();

        Iterable<CalculatedSubject> traversal = graph.traverse(TraversalAlgorithm.DEPTH_FIRST_PRE_ORDER, this);
        for (CalculatedSubject subject : traversal) {
            for (Map.Entry<String, String> entry : subject.getCombinedOptions(filter).entrySet()) {
                result.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    public Map<String, String> resolveAllOptions() {
        SubjectInheritanceGraph graph = SubjectInheritanceGraphs.getGraph();
        Map<String, String> result = new HashMap<>();

        Iterable<CalculatedSubject> traversal = graph.traverse(TraversalAlgorithm.DEPTH_FIRST_PRE_ORDER, this);
        for (CalculatedSubject subject : traversal) {
            for (Map.Entry<String, String> entry : subject.getCombinedOptions().entrySet()) {
                result.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    public void resolveAllOptions(MetaAccumulator accumulator, ImmutableContextSet filter) {
        SubjectInheritanceGraph graph = SubjectInheritanceGraphs.getGraph(filter);
        Iterable<CalculatedSubject> traversal = graph.traverse(TraversalAlgorithm.DEPTH_FIRST_PRE_ORDER, this);
        for (CalculatedSubject subject : traversal) {
            for (Map.Entry<String, String> entry : subject.getCombinedOptions(filter).entrySet()) {
                accumulator.accumulateMeta(entry.getKey(), entry.getValue());
            }
        }
    }

    public void resolveAllOptions(MetaAccumulator accumulator) {
        SubjectInheritanceGraph graph = SubjectInheritanceGraphs.getGraph();
        Iterable<CalculatedSubject> traversal = graph.traverse(TraversalAlgorithm.DEPTH_FIRST_PRE_ORDER, this);
        for (CalculatedSubject subject : traversal) {
            for (Map.Entry<String, String> entry : subject.getCombinedOptions().entrySet()) {
                accumulator.accumulateMeta(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public Tristate getPermissionValue(ImmutableContextSet contexts, String permission) {
        Contexts lookupContexts = Contexts.of(contexts, Contexts.global().getSettings());
        return this.cachedData.getPermissionData(lookupContexts).getPermissionValue(permission, CheckOrigin.INTERNAL);
    }

    @Override
    public boolean isChildOf(ImmutableContextSet contexts, LPSubjectReference parent) {
        return resolveAllParents(contexts).contains(parent);
    }

    @Override
    public ImmutableList<LPSubjectReference> getParents(ImmutableContextSet contexts) {
        return ImmutableList.copyOf(resolveAllParents(contexts));
    }

    @Override
    public Optional<String> getOption(ImmutableContextSet contexts, String key) {
        Contexts lookupContexts = Contexts.of(contexts, Contexts.global().getSettings());
        Map<String, String> meta = this.cachedData.getMetaData(lookupContexts).getMeta();
        return Optional.ofNullable(meta.get(key));
    }

    @Override
    public void performCleanup() {
        this.cachedData.doCacheCleanup();
    }

    @Override
    public void invalidateCaches() {
        this.cachedData.invalidateCaches();
    }

    private final class SubjectCachedData extends AbstractCachedData implements CalculatorFactory {
        private SubjectCachedData(LuckPermsPlugin plugin) {
            super(plugin);
        }

        @Override
        protected PermissionCalculatorMetadata getMetadataForContexts(Contexts contexts) {
            return PermissionCalculatorMetadata.of(null, getParentCollection().getIdentifier() + "/" + getIdentifier(), contexts.getContexts());
        }

        @Override
        protected CalculatorFactory getCalculatorFactory() {
            return this;
        }

        @Override
        protected MetaContexts getDefaultMetaContexts(Contexts contexts) {
            return MetaContexts.of(contexts, DEFAULT_META_STACK, DEFAULT_META_STACK);
        }

        @Override
        protected Map<String, Boolean> resolvePermissions() {
            return resolveAllPermissions();
        }

        @Override
        protected Map<String, Boolean> resolvePermissions(Contexts contexts) {
            return resolveAllPermissions(contexts.getContexts().makeImmutable());
        }

        @Override
        protected void resolveMeta(MetaAccumulator accumulator) {
            resolveAllOptions(accumulator);
        }

        @Override
        protected void resolveMeta(MetaAccumulator accumulator, MetaContexts contexts) {
            resolveAllOptions(accumulator, contexts.getContexts().getContexts().makeImmutable());
        }

        @Override
        public PermissionCalculator build(Contexts contexts, PermissionCalculatorMetadata metadata) {
            ImmutableList.Builder<PermissionProcessor> processors = ImmutableList.builder();
            processors.add(new MapProcessor());
            processors.add(new SpongeWildcardProcessor());
            processors.add(new WildcardProcessor());

            if (!getParentCollection().isDefaultsCollection()) {
                processors.add(new FixedDefaultsProcessor(getService(), contexts.getContexts().makeImmutable(), getDefaults()));
            }

            return new PermissionCalculator(this.plugin, metadata, processors.build());
        }
    }
}
