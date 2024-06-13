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

import com.google.common.collect.ForwardingList;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class FilterList<T> extends ForwardingList<Filter<T, ?>> {

    public static <T> FilterList<T> empty() {
        return new FilterList<>(LogicalOperator.AND, ImmutableList.of());
    }

    @SafeVarargs
    public static <T> FilterList<T> and(Filter<T, ?>... filters) {
        return new FilterList<>(LogicalOperator.AND, ImmutableList.copyOf(filters));
    }

    @SafeVarargs
    public static <T> FilterList<T> or(Filter<T, ?>... filters) {
        return new FilterList<>(LogicalOperator.OR, ImmutableList.copyOf(filters));
    }

    private final LogicalOperator operator;
    private final List<Filter<T, ?>> filters;

    public FilterList(LogicalOperator operator, List<Filter<T, ?>> filters) {
        this.operator = operator;
        this.filters = filters;
    }

    public LogicalOperator operator() {
        return this.operator;
    }

    @Override
    protected List<Filter<T, ?>> delegate() {
        return this.filters;
    }

    /**
     * Check to see if a value satisfies all (AND) or any (OR) filters in the list
     *
     * @param value the value to check
     * @return true if satisfied
     */
    public boolean evaluate(T value) {
        return this.operator.match(this.filters, value);
    }

    @Override
    public String toString() {
        String operator = this.operator.name().toLowerCase(Locale.ROOT);
        return this.filters.stream().map(Filter::toString).collect(Collectors.joining(" " + operator + " "));
    }

    public enum LogicalOperator {
        AND {
            @Override
            public <T> boolean match(List<? extends Filter<T, ?>> filters, T value) {
                return filters.stream().allMatch(filter -> filter.evaluate(value)); // true if empty
            }
        },
        OR {
            @Override
            public <T> boolean match(List<? extends Filter<T, ?>> filters, T value) {
                return filters.stream().anyMatch(filter -> filter.evaluate(value)); // false if empty
            }
        };

        public abstract <T> boolean match(List<? extends Filter<T, ?>> filters, T value);
    }

}
