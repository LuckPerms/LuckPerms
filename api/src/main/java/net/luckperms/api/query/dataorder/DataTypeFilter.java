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

package net.luckperms.api.query.dataorder;

import net.luckperms.api.model.data.DataType;
import net.luckperms.api.query.QueryOptions;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Represents which different {@link DataType}s are used for a query.
 *
 * <p>The {@link DataTypeFilter} enum simply represents some default
 * implementations of the {@link Predicate} required by {@link QueryOptions}
 * and the {@link #values(Predicate)} method.</p>
 *
 * <p>Users are free to implement their own predicate. However, be aware that
 * it is possible that more {@link DataType}s may be added in the future.
 * Ideally the {@link Predicate} implementations should be able to handle these
 * smoothly.</p>
 *
 * @see DataTypeFilterFunction
 * @since 5.2
 */
public enum DataTypeFilter implements Predicate<DataType> {

    /**
     * A data type filter indicating that all {@link DataType}s should be used.
     */
    ALL {
        @Override
        public boolean test(DataType dataType) {
            return true;
        }
    },

    /**
     * A data type filter indicating that no {@link DataType}s should be used.
     */
    NONE {
        @Override
        public boolean test(DataType dataType) {
            return false;
        }
    },

    /**
     * A data type filter indicating that only {@link DataType#NORMAL} should be used.
     */
    NORMAL_ONLY {
        @Override
        public boolean test(DataType dataType) {
            return dataType == DataType.NORMAL;
        }
    },

    /**
     * A data type filter indicating that only {@link DataType#TRANSIENT} should be used.
     */
    TRANSIENT_ONLY {
        @Override
        public boolean test(DataType dataType) {
            return dataType == DataType.TRANSIENT;
        }
    };

    private static final List<DataType> ALL_LIST = Collections.unmodifiableList(Arrays.asList(DataType.NORMAL, DataType.TRANSIENT));
    private static final List<DataType> NORMAL_ONLY_LIST = Collections.singletonList(DataType.NORMAL);
    private static final List<DataType> TRANSIENT_ONLY_LIST = Collections.singletonList(DataType.TRANSIENT);

    /**
     * Gets a {@link List} of all {@link DataType}s, filtered by the {@code predicate}.
     *
     * @param predicate the predicate to filter with
     * @return the list of data types
     */
    public static @NonNull List<DataType> values(@NonNull Predicate<? super DataType> predicate) {
        boolean normal = predicate.test(DataType.NORMAL);
        boolean trans = predicate.test(DataType.TRANSIENT);

        if (normal && trans) {
            return ALL_LIST;
        } else if (normal) {
            return NORMAL_ONLY_LIST;
        } else if (trans) {
            return TRANSIENT_ONLY_LIST;
        } else {
            return Collections.emptyList();
        }
    }

}
