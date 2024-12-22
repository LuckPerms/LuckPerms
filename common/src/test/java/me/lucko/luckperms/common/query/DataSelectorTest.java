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

import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.common.model.PermissionHolderIdentifier;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.query.QueryMode;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.query.dataorder.DataQueryOrder;
import net.luckperms.api.query.dataorder.DataQueryOrderFunction;
import net.luckperms.api.query.dataorder.DataTypeFilter;
import net.luckperms.api.query.dataorder.DataTypeFilterFunction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class DataSelectorTest {
    private static final PermissionHolderIdentifier IDENTIFIER = new PermissionHolderIdentifier(HolderType.USER, "Notch");

    @Test
    public void testDefault() {
        DataType[] types = DataSelector.selectOrder(QueryOptionsImpl.DEFAULT_CONTEXTUAL, IDENTIFIER);
        assertArrayEquals(new DataType[]{DataType.TRANSIENT, DataType.NORMAL}, types);
    }

    @Test
    public void testOrdering() {
        QueryOptions transientFirst = new QueryOptionsBuilderImpl(QueryMode.CONTEXTUAL)
                .option(DataQueryOrderFunction.KEY, DataQueryOrderFunction.always(DataQueryOrder.TRANSIENT_FIRST))
                .build();

        QueryOptions transientLast = new QueryOptionsBuilderImpl(QueryMode.CONTEXTUAL)
                .option(DataQueryOrderFunction.KEY, DataQueryOrderFunction.always(DataQueryOrder.TRANSIENT_LAST))
                .build();

        DataType[] types = DataSelector.selectOrder(transientFirst, IDENTIFIER);
        assertArrayEquals(new DataType[]{DataType.TRANSIENT, DataType.NORMAL}, types);

        types = DataSelector.selectOrder(transientLast, IDENTIFIER);
        assertArrayEquals(new DataType[]{DataType.NORMAL, DataType.TRANSIENT}, types);
    }

    @Test
    public void testSelection() {
        QueryOptions normalOnly = new QueryOptionsBuilderImpl(QueryMode.CONTEXTUAL)
                .option(DataTypeFilterFunction.KEY, DataTypeFilterFunction.always(DataTypeFilter.NORMAL_ONLY))
                .build();

        QueryOptions transientOnly = new QueryOptionsBuilderImpl(QueryMode.CONTEXTUAL)
                .option(DataTypeFilterFunction.KEY, DataTypeFilterFunction.always(DataTypeFilter.TRANSIENT_ONLY))
                .build();

        DataType[] types = DataSelector.selectOrder(normalOnly, IDENTIFIER);
        assertArrayEquals(new DataType[]{DataType.NORMAL}, types);

        types = DataSelector.selectOrder(transientOnly, IDENTIFIER);
        assertArrayEquals(new DataType[]{DataType.TRANSIENT}, types);
    }

}
