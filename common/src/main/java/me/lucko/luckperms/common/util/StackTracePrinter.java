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

package me.lucko.luckperms.common.util;

import java.util.function.Consumer;
import java.util.function.Predicate;

public final class StackTracePrinter {
    public static Consumer<StackTraceElement> elementToString(Consumer<String> consumer) {
        return e -> consumer.accept(e.getClassName() + "." + e.getMethodName() + (e.getLineNumber() >= 0 ? ":" + e.getLineNumber() : ""));
    }

    public static Builder builder() {
        return new Builder();
    }

    private final int truncateLength;
    private final Predicate<StackTraceElement> shouldPrintPredicate;

    private StackTracePrinter(int truncateLength, Predicate<StackTraceElement> shouldPrintPredicate) {
        this.truncateLength = truncateLength;
        this.shouldPrintPredicate = shouldPrintPredicate;
    }

    public int process(StackTraceElement[] stackTrace, Consumer<StackTraceElement> consumer) {
        // how many lines have been printed
        int count = 0;
        // if we're printing elements yet
        boolean printing = false;

        for (StackTraceElement e : stackTrace) {
            // start printing when the predicate passes
            if (!printing && this.shouldPrintPredicate.test(e)) {
                printing = true;
            }

            if (!printing) {
                continue;
            }
            if (count >= this.truncateLength) {
                break;
            }

            consumer.accept(e);
            count++;
        }
        if (stackTrace.length > this.truncateLength) {
            return stackTrace.length - this.truncateLength;
        }
        return 0;
    }

    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.truncateLength = this.truncateLength;
        builder.shouldPrintPredicate = this.shouldPrintPredicate;
        return builder;
    }

    public static final class Builder {
        private int truncateLength = Integer.MAX_VALUE;
        private Predicate<StackTraceElement> shouldPrintPredicate = Predicates.alwaysTrue();

        private Builder() {

        }

        public Builder truncateLength(int truncateLength) {
            this.truncateLength = truncateLength;
            return this;
        }

        public Builder ignoreElementsMatching(Predicate<? super StackTraceElement> predicate) {
            this.shouldPrintPredicate = this.shouldPrintPredicate.and(predicate.negate());
            return this;
        }

        public Builder ignoreClass(String className) {
            return ignoreElementsMatching(e -> e.getClassName().equals(className));
        }

        public Builder ignoreClassStartingWith(String className) {
            return ignoreElementsMatching(e -> e.getClassName().startsWith(className));
        }

        public StackTracePrinter build() {
            return new StackTracePrinter(this.truncateLength, this.shouldPrintPredicate);
        }
    }

}
