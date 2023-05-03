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

package me.lucko.luckperms.common.cacheddata;

import me.lucko.luckperms.common.cacheddata.type.MetaAccumulator;
import me.lucko.luckperms.common.calculator.CalculatorFactory;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.model.PermissionHolder;
import net.luckperms.api.metastacking.MetaStackDefinition;
import net.luckperms.api.node.ChatMetaType;
import net.luckperms.api.node.Node;
import net.luckperms.api.query.QueryOptions;

import java.util.Map;
import java.util.function.IntFunction;

/**
 * Holds an easily accessible cache of a holders data in a number of contexts
 */
public abstract class HolderCachedDataManager<T extends PermissionHolder> extends AbstractCachedDataManager {

    /**
     * The holder whom this data instance is representing
     */
    protected final T holder;

    public HolderCachedDataManager(T holder) {
        super(holder.getPlugin());
        this.holder = holder;
    }

    @Override
    protected QueryOptions getQueryOptions() {
        return this.holder.getQueryOptions();
    }

    @Override
    protected CalculatorFactory getCalculatorFactory() {
        return getPlugin().getCalculatorFactory();
    }

    @Override
    protected MetaStackDefinition getDefaultMetaStackDefinition(ChatMetaType type) {
        switch (type) {
            case PREFIX:
                return getPlugin().getConfiguration().get(ConfigKeys.PREFIX_FORMATTING_OPTIONS);
            case SUFFIX:
                return getPlugin().getConfiguration().get(ConfigKeys.SUFFIX_FORMATTING_OPTIONS);
            default:
                throw new AssertionError();
        }
    }

    @Override
    protected <M extends Map<String, Node>> M resolvePermissions(IntFunction<M> mapFactory, QueryOptions queryOptions) {
        return this.holder.exportPermissions(mapFactory, queryOptions, true, getPlugin().getConfiguration().get(ConfigKeys.APPLYING_SHORTHAND));
    }

    @Override
    protected void resolveMeta(MetaAccumulator accumulator, QueryOptions queryOptions) {
        this.holder.accumulateMeta(accumulator, queryOptions);
    }
}
