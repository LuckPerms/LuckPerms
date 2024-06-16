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

public class Filter<T, FT> {
    private final FilterField<T, FT> field;
    private final Constraint<FT> constraint;

    public Filter(FilterField<T, FT> field, Constraint<FT> constraint) {
        this.field = field;
        this.constraint = constraint;
    }

    public final FilterField<T, FT> field() {
        return this.field;
    }

    public final Constraint<FT> constraint() {
        return this.constraint;
    }

    /**
     * Returns if the given value satisfies this filter
     *
     * @param value the value
     * @return true if satisfied
     */
    public boolean evaluate(T value) {
        return this.constraint.evaluate(this.field.getValue(value));
    }

    @Override
    public String toString() {
        return this.field + " " + this.constraint;
    }
}
