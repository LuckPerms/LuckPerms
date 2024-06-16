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
