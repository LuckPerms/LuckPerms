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
import me.lucko.luckperms.common.verbose.VerboseCheckTarget;
import net.luckperms.api.cacheddata.CachedData;
import net.luckperms.api.query.QueryOptions;

/**
 * Metadata about a given {@link CachedData}.
 */
public class CacheMetadata {

    /**
     * The type of the object which owns the cache
     */
    private final HolderType holderType;

    /**
     * The information about the holder for verbose checks
     */
    private final VerboseCheckTarget verboseCheckInfo;

    /**
     * The query options
     */
    private final QueryOptions queryOptions;

    public CacheMetadata(HolderType holderType, VerboseCheckTarget verboseCheckInfo, QueryOptions queryOptions) {
        this.holderType = holderType;
        this.verboseCheckInfo = verboseCheckInfo;
        this.queryOptions = queryOptions;
    }

    public HolderType getHolderType() {
        return this.holderType;
    }

    public VerboseCheckTarget getVerboseCheckInfo() {
        return this.verboseCheckInfo;
    }

    public QueryOptions getQueryOptions() {
        return this.queryOptions;
    }
}
