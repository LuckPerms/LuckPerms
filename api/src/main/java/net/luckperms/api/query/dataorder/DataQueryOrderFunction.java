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

import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.query.OptionKey;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Comparator;
import java.util.Objects;

/**
 * A function that generates a {@link DataQueryOrder} comparator for
 * {@link PermissionHolder}s as required during inheritance.
 */
public interface DataQueryOrderFunction {

    /**
     * The {@link OptionKey} for {@link DataQueryOrderFunction}.
     */
    OptionKey<DataQueryOrderFunction> KEY = OptionKey.of("dataqueryorderfunction", DataQueryOrderFunction.class);

    /**
     * Creates a {@link DataQueryOrderFunction} that always returns the given
     * {@code comparator}.
     *
     * @param comparator the comparator
     * @return the data query order function
     * @since 5.2
     */
    static @NonNull DataQueryOrderFunction always(@NonNull Comparator<DataType> comparator) {
        Objects.requireNonNull(comparator, "comparator");
        return id -> comparator;
    }

    /**
     * Gets the {@link DataQueryOrder} comparator for the given
     * {@link PermissionHolder.Identifier holder identifier}.
     *
     * @param holderIdentifier the holder identifier
     * @return the comparator to use
     */
    @NonNull Comparator<DataType> getOrderComparator(PermissionHolder.@NonNull Identifier holderIdentifier);

}
