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

package net.luckperms.api.context;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Extension of {@link ContextCalculator} which provides the same context
 * regardless of the subject.
 */
@FunctionalInterface
public interface StaticContextCalculator extends ContextCalculator<Object> {

    /**
     * Creates a new {@link StaticContextCalculator} that provides a single context.
     *
     * @param key the key of the context provided by the calculator
     * @param valueFunction the function used to compute the corresponding value
     *                      for each query. A context will not be "accumulated"
     *                      if the value returned is null.
     * @return the resultant calculator
     */
    static StaticContextCalculator forSingleContext(String key, Supplier<String> valueFunction) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(valueFunction, "valueFunction");
        return consumer -> {
            String value = valueFunction.get();
            if (value != null) {
                consumer.accept(key, value);
            }
        };
    }

    /**
     * Submits any contexts this calculator determines to be applicable.
     *
     * <p>Care should be taken to ensure implementations of this method meet the
     * general requirements for {@link ContextCalculator}, defined in the class
     * doc.</p>
     *
     * @param consumer the {@link ContextConsumer} to submit contexts to
     */
    void calculate(@NonNull ContextConsumer consumer);

    @Override
    default void calculate(@NonNull Object target, @NonNull ContextConsumer consumer) {
        calculate(consumer);
    }
}
