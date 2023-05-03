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
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents the order in which to query different {@link DataType}s.
 *
 * <p>The {@link DataQueryOrder} enum simply represents some default
 * implementations of the {@link Comparator} required by {@link QueryOptions}
 * and the {@link #queryInOrder(Comparator, Consumer)} or
 * {@link #order(Comparator)} methods.</p>
 *
 * <p>Users are free to implement their own comparator. However, be aware that
 * it is possible that more {@link DataType}s may be added in the future.
 * Ideally the {@link Comparator} implementations should be able to handle these
 * smoothly.</p>
 *
 * @see DataQueryOrderFunction
 */
public enum DataQueryOrder implements Comparator<DataType> {

    /**
     * A data query order indicating that {@link DataType#TRANSIENT} should be queried first.
     */
    TRANSIENT_FIRST {
        @Override
        public int compare(DataType o1, DataType o2) {
            if (o1 == o2) {
                return 0;
            }
            return o1 == DataType.TRANSIENT ? 1 : -1;
        }
    },

    /**
     * A data query order indicating that {@link DataType#TRANSIENT} should be queried last.
     */
    TRANSIENT_LAST {
        @Override
        public int compare(DataType o1, DataType o2) {
            if (o1 == o2) {
                return 0;
            }
            return o1 == DataType.TRANSIENT ? -1 : 1;
        }
    };

    private static final List<DataType> TRANSIENT_FIRST_LIST = Collections.unmodifiableList(Arrays.asList(DataType.TRANSIENT, DataType.NORMAL));
    private static final List<DataType> TRANSIENT_LAST_LIST = Collections.unmodifiableList(Arrays.asList(DataType.NORMAL, DataType.TRANSIENT));

    /**
     * Gets a {@link List} of all {@link DataType}s, in order of greatest to least, as defined by
     * the {@code comparator}.
     *
     * <p>Equivalent to calling {@link Arrays#sort(Object[], Comparator)} on
     * {@link DataType#values()}, but with the comparator
     * {@link Comparator#reversed() reversed}.</p>
     *
     * @param comparator the comparator
     * @return the ordered data types
     */
    public static @NonNull List<DataType> order(@NonNull Comparator<? super DataType> comparator) {
        int compare = comparator.compare(DataType.TRANSIENT, DataType.NORMAL);
        if (compare > 0) {
            // transient first
            return TRANSIENT_FIRST_LIST;
        } else if (compare < 0) {
            // transient last
            return TRANSIENT_LAST_LIST;
        } else {
            // ??? - no defined order
            throw new IllegalStateException("Comparator " + comparator + " does not define an order between DataType.NORMAL and DataType.TRANSIENT!");
        }
    }

    /**
     * Calls the {@code action} {@link Consumer} for each {@link DataType}, in
     * the order defined by the {@code comparator}.
     *
     * @param comparator the comparator
     * @param action the action
     */
    public static void queryInOrder(@NonNull Comparator<? super DataType> comparator, @NonNull Consumer<? super DataType> action) {
        order(comparator).forEach(action);
    }

}
