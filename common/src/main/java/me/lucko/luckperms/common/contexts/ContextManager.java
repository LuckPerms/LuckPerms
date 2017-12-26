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

package me.lucko.luckperms.common.contexts;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.context.ContextCalculator;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.api.context.StaticContextCalculator;

import java.util.Optional;

/**
 * Manages {@link ContextCalculator}s, and calculates applicable contexts for a
 * given type.
 *
 * @param <T> the calculator type
 */
public interface ContextManager<T> {

    /**
     * Gets the class of the subject handled by this instance
     *
     * @return the subject class
     */
    Class<T> getSubjectClass();

    /**
     * Queries the ContextManager for current context values for the subject.
     *
     * @param subject the subject
     * @return the applicable context for the subject
     */
    ImmutableContextSet getApplicableContext(T subject);

    /**
     * Queries the ContextManager for current context values for the subject.
     *
     * @param subject the subject
     * @return the applicable context for the subject
     */
    Contexts getApplicableContexts(T subject);

    /**
     * Gets the contexts from the static calculators in this manager.
     *
     * @return the current active static contexts
     */
    ImmutableContextSet getStaticContext();

    /**
     * Gets the contexts from the static calculators in this manager.
     *
     * @return the current active static contexts
     */
    Contexts getStaticContexts();

    /**
     * Returns a string form of the managers static context
     *
     * <p>Returns an empty optional if the set is empty.</p>
     *
     * @return a string representation of {@link #getStaticContext()}
     */
    Optional<String> getStaticContextString();

    /**
     * Forms a {@link Contexts} instance from an {@link ImmutableContextSet}.
     *
     * @param subject the subject
     * @param contextSet the context set
     * @return a contexts instance
     */
    Contexts formContexts(T subject, ImmutableContextSet contextSet);

    /**
     * Forms a {@link Contexts} instance from an {@link ImmutableContextSet}.
     *
     * @param contextSet the context set
     * @return a contexts instance
     */
    Contexts formContexts(ImmutableContextSet contextSet);

    /**
     * Registers a context calculator with the manager.
     *
     * @param calculator the calculator
     */
    void registerCalculator(ContextCalculator<? super T> calculator);

    /**
     * Registers a static context calculator with the manager.
     *
     * @param calculator the calculator
     */
    void registerStaticCalculator(StaticContextCalculator calculator);

    /**
     * Invalidates the lookup cache for a given subject
     *
     * @param subject the subject
     */
    void invalidateCache(T subject);

    /**
     * Gets the number of calculators registered with the manager.
     *
     * @return the number of calculators registered
     */
    int getCalculatorsSize();

}
