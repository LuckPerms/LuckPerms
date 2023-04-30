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

package net.luckperms.api.query.meta;

import net.luckperms.api.cacheddata.Result;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.query.OptionKey;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;

/**
 * A function that selects the {@link MetaNode#getMetaValue() meta value} to be used in queries
 * against a given {@link MetaNode#getMetaKey() meta key}.
 *
 * @since 5.1
 */
public interface MetaValueSelector {

    /**
     * The {@link OptionKey} for {@link MetaValueSelector}.
     */
    OptionKey<MetaValueSelector> KEY = OptionKey.of("metavalueselector", MetaValueSelector.class);

    /**
     * Selects the meta value to map to the given key.
     *
     * <p>The {@code values} list is guaranteed to contain at least 1 element.</p>
     *
     * @param key the key
     * @param values the values, in the order in which they were accumulated.
     * @return the selected value
     */
    @NonNull Result<String, MetaNode> selectValue(@NonNull String key, @NonNull List<? extends Result<String, MetaNode>> values);

}
