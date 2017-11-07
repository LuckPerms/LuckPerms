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

package me.lucko.luckperms.api.context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Extension of {@link ContextCalculator} which provides the same context
 * regardless of the subject.
 *
 * @since 4.0
 */
@FunctionalInterface
public interface StaticContextCalculator extends ContextCalculator<Object> {

    /**
     * Adds this calculators context to the given accumulator.
     *
     * @param accumulator a map of contexts to add to
     * @return the map
     */
    @Nonnull
    MutableContextSet giveApplicableContext(@Nonnull MutableContextSet accumulator);

    /**
     * Gives the subject all of the applicable contexts they meet
     *
     * @param subject the subject to add contexts to
     * @param accumulator a map of contexts to add to
     * @return the map
     */
    @Nonnull
    @Override
    @Deprecated
    default MutableContextSet giveApplicableContext(@Nullable Object subject, @Nonnull MutableContextSet accumulator) {
        return giveApplicableContext(accumulator);
    }

}
