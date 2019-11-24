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
import java.util.function.Function;

/**
 * Calculates the contexts applicable for a contextual subject.
 *
 * <p>Implementations of this interface should satisfy the following
 * requirements:</p>
 * <ul>
 *     <li>Context lookups should be <i>fast</i>: lookup methods are likely to
 *     be invoked frequently, and should therefore be fast to execute. If
 *     determining the current contexts involves a particularly time consuming
 *     lookup (database queries, network requests, etc), then such results
 *     should be cached ahead of time.</li>
 *
 *     <li>Context lookups should be <i>thread-safe</i>: lookups will sometimes
 *     be performed from "async" threads, and therefore should not access any
 *     part of the server only safe for access from a sync context. If
 *     necessary, such results should be determined ahead of time and stored in
 *     a thread-safe collection for retrieval later.</li>
 *
 *     <li>Context lookups should <i>not query active contexts</i>: doing so is
 *     likely to result in a stack overflow, or thread deadlock. Care should be
 *     taken to avoid (indirect) calls to the same calculator.</li>
 * </ul>
 * <p></p>
 *
 * <p>Calculators should be registered using
 * {@link ContextManager#registerCalculator(ContextCalculator)}.</p>
 */
@FunctionalInterface
public interface ContextCalculator<T> {

    /**
     * Creates a new {@link ContextCalculator} that provides a single context.
     *
     * @param key the key of the context provided by the calculator
     * @param valueFunction the function used to compute the corresponding value
     *                      for each query. A context will not be "accumulated"
     *                      if the value returned is null.
     * @param <T> the contextual type
     * @return the resultant calculator
     */
    static <T> @NonNull ContextCalculator<T> forSingleContext(@NonNull String key, @NonNull Function<T, String> valueFunction) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(valueFunction, "valueFunction");
        return (target, consumer) -> {
            String value = valueFunction.apply(target);
            if (value != null) {
                consumer.accept(key, value);
            }
        };
    }

    /**
     * Submits any contexts this calculator determines to be applicable to
     * the {@code target} contextual subject.
     *
     * <p>Care should be taken to ensure implementations of this method meet the
     * general requirements for {@link ContextCalculator}, defined in the class
     * doc.</p>
     *
     * @param target the target contextual subject for this operation
     * @param consumer the {@link ContextConsumer} to submit contexts to
     */
    void calculate(@NonNull T target, @NonNull ContextConsumer consumer);

    /**
     * Gets a {@link ContextSet}, containing some/all of the contexts this
     * calculator could potentially submit.
     *
     * <p>The result of this call is primarily intended on providing
     * suggestions to end users when defining permissions.</p>
     *
     * @return a set of potential contexts
     */
    default ContextSet estimatePotentialContexts() {
        return ImmutableContextSet.empty();
    }

}
