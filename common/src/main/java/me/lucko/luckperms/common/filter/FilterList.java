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
