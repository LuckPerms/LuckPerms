/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

package me.lucko.luckperms.api.context;

import java.util.HashMap;
import java.util.Map;

/**
 * Calculates whether contexts are applicable to {@link T}
 *
 * <p>Somewhat inspired by the system used on Sponge.
 *
 * @param <T> the subject type. Is ALWAYS the player class of the platform.
 */
public interface IContextCalculator<T> {

    /**
     * Gives the subject all of the applicable contexts they meet
     *
     * @param subject     the subject to add contexts to
     * @param accumulator a map of contexts to add to
     * @return the map
     * @deprecated in favour of {@link #giveApplicableContext(Object, MutableContextSet)}. Older implementations of this
     * interface will still work, as the replacement method is given as a default, and falls back to using this method.
     */
    @Deprecated
    default Map<String, String> giveApplicableContext(T subject, Map<String, String> accumulator) {
        MutableContextSet acc = new MutableContextSet();
        giveApplicableContext(subject, acc);

        accumulator.putAll(acc.toMap());
        return accumulator;
    }

    /**
     * Gives the subject all of the applicable contexts they meet
     *
     * <p><b>You MUST implement this method. The default is only provided for backwards compatibility with
     * {@link #giveApplicableContext(Object, Map)}.</b>
     *
     * @param subject     the subject to add contexts to
     * @param accumulator a map of contexts to add to
     * @return the map
     * @since 2.13
     */
    @SuppressWarnings("deprecation")
    default MutableContextSet giveApplicableContext(T subject, MutableContextSet accumulator) {
        Map<String, String> acc = new HashMap<>();
        giveApplicableContext(subject, acc);

        accumulator.addAll(acc.entrySet());
        return accumulator;
    }

    /**
     * Checks to see if a context is applicable to a subject
     *
     * @param subject the subject to check against
     * @param context the context to check for
     * @return true if met, or false if not. If this calculator does not calculate the given context, return false.
     */
    boolean isContextApplicable(T subject, Map.Entry<String, String> context);

    /**
     * Adds a listener to be called whenever a context handled by this calculator changes
     *
     * @param listener the listener instance
     * @throws NullPointerException if listener is null
     */
    void addListener(ContextListener<T> listener);

}
