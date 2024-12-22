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

import java.util.Objects;
import java.util.function.Predicate;

/**
 * A function that generates a {@link DataTypeFilter} predicate for
 * {@link PermissionHolder}s as required during inheritance.
 *
 * @since 5.2
 */
public interface DataTypeFilterFunction {

    /**
     * The {@link OptionKey} for {@link DataTypeFilterFunction}.
     */
    OptionKey<DataTypeFilterFunction> KEY = OptionKey.of("datatypefilterfunction", DataTypeFilterFunction.class);

    /**
     * Creates a {@link DataTypeFilterFunction} that always returns the given
     * {@code predicate} (commonly one of the values in {@link DataTypeFilter}).
     *
     * @param predicate the predicate
     * @return the data type filter function
     */
    static @NonNull DataTypeFilterFunction always(@NonNull Predicate<DataType> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return id -> predicate;
    }

    /**
     * Gets the {@link DataTypeFilter} predicate for the given
     * {@link PermissionHolder.Identifier holder identifier}.
     *
     * @param holderIdentifier the holder identifier
     * @return the predicate to use
     */
    @NonNull Predicate<DataType> getTypeFilter(PermissionHolder.@NonNull Identifier holderIdentifier);

}
