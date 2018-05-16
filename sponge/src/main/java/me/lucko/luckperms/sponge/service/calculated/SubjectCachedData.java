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
import me.lucko.luckperms.api.caching.MetaContexts;
import me.lucko.luckperms.api.metastacking.MetaStackDefinition;
import me.lucko.luckperms.common.caching.AbstractCachedData;
import me.lucko.luckperms.common.caching.type.MetaAccumulator;
import me.lucko.luckperms.common.calculators.CalculatorFactory;
import me.lucko.luckperms.common.calculators.PermissionCalculator;
import me.lucko.luckperms.common.calculators.PermissionCalculatorMetadata;
import me.lucko.luckperms.common.metastacking.SimpleMetaStackDefinition;
import me.lucko.luckperms.common.metastacking.StandardStackElements;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.processors.MapProcessor;
import me.lucko.luckperms.common.processors.PermissionProcessor;
import me.lucko.luckperms.common.processors.WildcardProcessor;
import me.lucko.luckperms.sponge.processors.FixedDefaultsProcessor;
import me.lucko.luckperms.sponge.processors.SpongeWildcardProcessor;

import java.util.Map;

public class SubjectCachedData extends AbstractCachedData implements CalculatorFactory {
    private static final MetaStackDefinition DEFAULT_META_STACK = new SimpleMetaStackDefinition(
            ImmutableList.of(StandardStackElements.HIGHEST),
            "", "", ""
    );

    private final CalculatedSubject subject;

    SubjectCachedData(CalculatedSubject subject, LuckPermsPlugin plugin) {
        super(plugin);
        this.subject = subject;
    }

    @Override
    protected PermissionCalculatorMetadata getMetadataForContexts(Contexts contexts) {
        return PermissionCalculatorMetadata.of(null, this.subject.getParentCollection().getIdentifier() + "/" + this.subject.getIdentifier(), contexts.getContexts());
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
        return this.subject.resolveAllPermissions();
    }

    @Override
    protected Map<String, Boolean> resolvePermissions(Contexts contexts) {
        return this.subject.resolveAllPermissions(contexts.getContexts().makeImmutable());
    }

    @Override
    protected void resolveMeta(MetaAccumulator accumulator) {
        this.subject.resolveAllOptions(accumulator);
    }

    @Override
    protected void resolveMeta(MetaAccumulator accumulator, MetaContexts contexts) {
        this.subject.resolveAllOptions(accumulator, contexts.getContexts().getContexts().makeImmutable());
    }

    @Override
    public PermissionCalculator build(Contexts contexts, PermissionCalculatorMetadata metadata) {
        ImmutableList.Builder<PermissionProcessor> processors = ImmutableList.builder();
        processors.add(new MapProcessor());
        processors.add(new SpongeWildcardProcessor());
        processors.add(new WildcardProcessor());

        if (!this.subject.getParentCollection().isDefaultsCollection()) {
            processors.add(new FixedDefaultsProcessor(this.subject.getService(), contexts.getContexts().makeImmutable(), this.subject.getDefaults()));
        }

        return new PermissionCalculator(this.plugin, metadata, processors.build());
    }
}
