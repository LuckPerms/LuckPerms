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

import me.lucko.luckperms.common.model.HolderType;

import net.luckperms.api.cacheddata.CachedData;
import net.luckperms.api.query.QueryOptions;

/**
 * Metadata about a given {@link CachedData}.
 */
public class CacheMetadata {

    /**
     * The cached data instance which creates this container
     */
    private final AbstractCachedDataManager parentContainer;

    /**
     * The type of the object which owns the cache
     */
    private final HolderType holderType;

    /**
     * The name of the object which owns the cache
     */
    private final String objectName;

    /**
     * The query options
     */
    private final QueryOptions queryOptions;

    public CacheMetadata(AbstractCachedDataManager parentContainer, HolderType holderType, String objectName, QueryOptions queryOptions) {
        this.parentContainer = parentContainer;
        this.holderType = holderType;
        this.objectName = objectName;
        this.queryOptions = queryOptions;
    }

    public AbstractCachedDataManager getParentContainer() {
        return this.parentContainer;
    }

    public HolderType getHolderType() {
        return this.holderType;
    }

    public String getObjectName() {
        return this.objectName;
    }

    public QueryOptions getQueryOptions() {
        return this.queryOptions;
    }
}
