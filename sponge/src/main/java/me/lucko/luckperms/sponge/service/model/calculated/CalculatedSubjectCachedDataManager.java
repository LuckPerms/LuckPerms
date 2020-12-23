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

import me.lucko.luckperms.common.cacheddata.AbstractCachedDataManager;
import me.lucko.luckperms.common.cacheddata.CacheMetadata;
import me.lucko.luckperms.common.cacheddata.type.MetaAccumulator;
import me.lucko.luckperms.common.calculator.CalculatorFactory;
import me.lucko.luckperms.common.calculator.PermissionCalculator;
import me.lucko.luckperms.common.calculator.processor.MapProcessor;
import me.lucko.luckperms.common.calculator.processor.PermissionProcessor;
import me.lucko.luckperms.common.calculator.processor.SpongeWildcardProcessor;
import me.lucko.luckperms.common.calculator.processor.WildcardProcessor;
import me.lucko.luckperms.common.metastacking.SimpleMetaStackDefinition;
import me.lucko.luckperms.common.metastacking.StandardStackElements;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.sponge.calculator.FixedDefaultsProcessor;

import net.luckperms.api.metastacking.DuplicateRemovalFunction;
import net.luckperms.api.metastacking.MetaStackDefinition;
import net.luckperms.api.node.ChatMetaType;
import net.luckperms.api.query.QueryOptions;

import java.util.Map;
import java.util.function.IntFunction;

public class CalculatedSubjectCachedDataManager extends AbstractCachedDataManager implements CalculatorFactory {
    private static final MetaStackDefinition DEFAULT_META_STACK = new SimpleMetaStackDefinition(
            ImmutableList.of(StandardStackElements.HIGHEST),
            DuplicateRemovalFunction.RETAIN_ALL,
            "", "", ""
    );

    private final CalculatedSubject subject;

    CalculatedSubjectCachedDataManager(CalculatedSubject subject, LuckPermsPlugin plugin) {
        super(plugin);
        this.subject = subject;
    }

    @Override
    protected CacheMetadata getMetadataForQueryOptions(QueryOptions queryOptions) {
        return new CacheMetadata(null, this.subject.getParentCollection().getIdentifier() + "/" + this.subject.getIdentifier(), queryOptions);
    }

    @Override
    protected QueryOptions getQueryOptions() {
        return this.subject.sponge().getQueryOptions();
    }

    @Override
    protected CalculatorFactory getCalculatorFactory() {
        return this;
    }

    @Override
    protected MetaStackDefinition getDefaultMetaStackDefinition(ChatMetaType type) {
        return DEFAULT_META_STACK;
    }

    @Override
    protected <M extends Map<String, Boolean>> M resolvePermissions(IntFunction<M> mapFactory, QueryOptions queryOptions) {
        M map = mapFactory.apply(16);
        this.subject.resolveAllPermissions(map, queryOptions);
        return map;
    }

    @Override
    protected void resolveMeta(MetaAccumulator accumulator, QueryOptions queryOptions) {
        this.subject.resolveAllOptions(accumulator, queryOptions);
    }

    @Override
    public PermissionCalculator build(QueryOptions queryOptions, CacheMetadata metadata) {
        ImmutableList.Builder<PermissionProcessor> processors = ImmutableList.builder();
        processors.add(new MapProcessor());
        processors.add(new SpongeWildcardProcessor());
        processors.add(new WildcardProcessor());

        if (!this.subject.getParentCollection().isDefaultsCollection()) {
            processors.add(new FixedDefaultsProcessor(this.subject.getService(), queryOptions, this.subject.getDefaults(), true));
        }

        return new PermissionCalculator(getPlugin(), metadata, processors.build());
    }
}
