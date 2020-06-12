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

package me.lucko.luckperms.common.query;

import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.query.dataorder.DataQueryOrder;
import net.luckperms.api.query.dataorder.DataQueryOrderFunction;
import net.luckperms.api.query.dataorder.DataTypeFilter;
import net.luckperms.api.query.dataorder.DataTypeFilterFunction;

import java.util.Comparator;
import java.util.function.Predicate;

public final class DataSelector {
    private DataSelector() {}

    // These arrays are only ever exposed internally - we ensure they don't get modified
    private static final DataType[] TRANSIENT_NORMAL = new DataType[]{DataType.TRANSIENT, DataType.NORMAL};
    private static final DataType[] NORMAL_TRANSIENT = new DataType[]{DataType.NORMAL, DataType.TRANSIENT};
    private static final DataType[] NORMAL = new DataType[]{DataType.NORMAL};
    private static final DataType[] TRANSIENT = new DataType[]{DataType.TRANSIENT};
    private static final DataType[] NONE = new DataType[0];

    public static DataType[] select(QueryOptions queryOptions, PermissionHolder.Identifier identifier) {
        final DataQueryOrderFunction orderFunc = queryOptions.option(DataQueryOrderFunction.KEY).orElse(null);
        final DataTypeFilterFunction filterFunc = queryOptions.option(DataTypeFilterFunction.KEY).orElse(null);

        if (orderFunc == null && filterFunc == null) {
            return TRANSIENT_NORMAL;
        }

        Predicate<DataType> predicate = filterFunc != null ? filterFunc.getTypeFilter(identifier) : DataTypeFilter.ALL;

        boolean normal = predicate.test(DataType.NORMAL);
        boolean trans = predicate.test(DataType.TRANSIENT);

        // if both are included - we need to consult the order comparator
        if (normal && trans) {
            Comparator<DataType> comparator = orderFunc != null ? orderFunc.getOrderComparator(identifier) : DataQueryOrder.TRANSIENT_FIRST;

            int compare = comparator.compare(DataType.TRANSIENT, DataType.NORMAL);
            if (compare == 0) {
                throw new IllegalStateException("Comparator " + comparator + " does not define an order between DataType.NORMAL and DataType.TRANSIENT!");
            }

            return compare > 0 ? TRANSIENT_NORMAL : NORMAL_TRANSIENT;
        }

        // otherwise, no need to worry about order
        if (normal) {
            return NORMAL;
        } else if (trans) {
            return TRANSIENT;
        } else {
            return NONE;
        }
    }

}
