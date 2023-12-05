package me.lucko.luckperms.common.filter;

import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public interface ConstraintFactory<T> {

    Predicate<T> equal(T value);
    Predicate<T> notEqual(T value);
    Predicate<T> similar(T value);
    Predicate<T> notSimilar(T value);

    default Constraint<T> build(Comparison comparison, T value) {
        switch (comparison) {
            case EQUAL:
                return new Constraint<>(comparison, value, equal(value));
            case NOT_EQUAL:
                return new Constraint<>(comparison, value, notEqual(value));
            case SIMILAR:
                return new Constraint<>(comparison, value, similar(value));
            case NOT_SIMILAR:
                return new Constraint<>(comparison, value, notSimilar(value));
            default:
                throw new AssertionError(comparison);
        }
    }

    ConstraintFactory<String> STRINGS = new ConstraintFactory<String>() {
        @Override
        public Predicate<String> equal(String value) {
            return value::equalsIgnoreCase;
        }

        @Override
        public Predicate<String> notEqual(String value) {
            return string -> !value.equalsIgnoreCase(string);
        }

        @Override
        public Predicate<String> similar(String value) {
            Pattern pattern = Comparison.compilePatternForLikeSyntax(value);
            return string -> pattern.matcher(string).matches();
        }

        @Override
        public Predicate<String> notSimilar(String value) {
            Pattern pattern = Comparison.compilePatternForLikeSyntax(value);
            return string -> !pattern.matcher(string).matches();
        }
    };

    ConstraintFactory<UUID> UUIDS = new ConstraintFactory<UUID>() {
        @Override
        public Predicate<UUID> equal(UUID value) {
            return value::equals;
        }

        @Override
        public Predicate<UUID> notEqual(UUID value) {
            return string -> !value.equals(string);
        }

        @Override
        public Predicate<UUID> similar(UUID value) {
            Pattern pattern = Comparison.compilePatternForLikeSyntax(value.toString());
            return uuid -> pattern.matcher(uuid.toString()).matches();
        }

        @Override
        public Predicate<UUID> notSimilar(UUID value) {
            Pattern pattern = Comparison.compilePatternForLikeSyntax(value.toString());
            return uuid -> !pattern.matcher(uuid.toString()).matches();
        }
    };

}
