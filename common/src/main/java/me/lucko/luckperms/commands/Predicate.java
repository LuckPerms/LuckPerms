package me.lucko.luckperms.commands;

import lombok.experimental.UtilityClass;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@UtilityClass
public class Predicate {
    public static <T> java.util.function.Predicate<T> alwaysFalse() {
        return t -> false;
    }

    public static <T> java.util.function.Predicate<T> alwaysTrue() {
        return t -> true;
    }

    public static java.util.function.Predicate<Integer> notInRange(Integer start, Integer end) {
        return inverse(inRange(start, end));
    }

    public static java.util.function.Predicate<Integer> inRange(Integer start, Integer end) {
        return isOneOf(IntStream.rangeClosed(start, end).boxed().collect(Collectors.toSet()));
    }

    public static <T> java.util.function.Predicate<T> notOneOf(Set<T> ts) {
        return inverse(isOneOf(ts));
    }

    public static <T> java.util.function.Predicate<T> isOneOf(Set<T> ta) {
        return t -> {
            for (T i : ta) {
                if (i == t) {
                    return true;
                }
            }
            return false;
        };
    }

    public static <T> java.util.function.Predicate<T> not(T t) {
        return inverse(is(t));
    }

    public static <T> java.util.function.Predicate<T> is(T t) {
        return t2 -> t == t2;
    }

    public static <T> java.util.function.Predicate<T> inverse(java.util.function.Predicate<T> t) {
        return t2 -> !t.test(t2);
    }
}
