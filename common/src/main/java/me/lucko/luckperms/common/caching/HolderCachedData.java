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

package me.lucko.luckperms.common.caching;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.caching.MetaContexts;
import me.lucko.luckperms.common.caching.type.MetaAccumulator;
import me.lucko.luckperms.common.calculators.CalculatorFactory;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.model.PermissionHolder;

import java.util.Map;

/**
 * Holds an easily accessible cache of a holders data in a number of contexts
 */
public abstract class HolderCachedData<T extends PermissionHolder> extends AbstractCachedData {

    /**
     * The holder whom this data instance is representing
     */
    protected final T holder;

    public HolderCachedData(T holder) {
        super(holder.getPlugin());
        this.holder = holder;
    }

    @Override
    protected CalculatorFactory getCalculatorFactory() {
        return this.plugin.getCalculatorFactory();
    }

    @Override
    protected MetaContexts getDefaultMetaContexts(Contexts contexts) {
        return this.plugin.getContextManager().formMetaContexts(contexts);
    }

    @Override
    protected Map<String, Boolean> resolvePermissions() {
        return this.holder.exportPermissions(true, this.plugin.getConfiguration().get(ConfigKeys.APPLYING_SHORTHAND));
    }

    @Override
    protected Map<String, Boolean> resolvePermissions(Contexts contexts) {
        return this.holder.exportPermissions(contexts, true, this.plugin.getConfiguration().get(ConfigKeys.APPLYING_SHORTHAND));
    }

    @Override
    protected void resolveMeta(MetaAccumulator accumulator) {
        this.holder.accumulateMeta(accumulator);
    }

    @Override
    protected void resolveMeta(MetaAccumulator accumulator, MetaContexts contexts) {
        this.holder.accumulateMeta(accumulator, contexts.getContexts());
    }
}
