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

package me.lucko.luckperms.common.filter;

import java.util.function.Function;

/**
 * Represents a field that can be filtered on.
 *
 * @param <T> the type the field is on
 */
public interface FilterField<T, FT> {

    static <T, FT> FilterField<T, FT> named(String name, Function<T, FT> func) {
        return new FilterField<T, FT>() {
            @Override
            public FT getValue(T object) {
                return func.apply(object);
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }

    /**
     * Gets the value of this field on the given object.
     *
     * @param object the object
     * @return the field value as a string
     */
    FT getValue(T object);

    default Filter<T, FT> isEqualTo(FT value, ConstraintFactory<FT> factory) {
        return new Filter<>(this, factory.build(Comparison.EQUAL, value));
    }

    default Filter<T, FT> isNotEqualTo(FT value, ConstraintFactory<FT> factory) {
        return new Filter<>(this, factory.build(Comparison.NOT_EQUAL, value));
    }

    default Filter<T, FT> isSimilarTo(FT value, ConstraintFactory<FT> factory) {
        return new Filter<>(this, factory.build(Comparison.SIMILAR, value));
    }

    default Filter<T, FT> isNotSimilarTo(FT value, ConstraintFactory<FT> factory) {
        return new Filter<>(this, factory.build(Comparison.NOT_SIMILAR, value));
    }
}
