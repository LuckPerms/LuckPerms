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

package net.luckperms.api.metastacking;

import net.luckperms.api.query.OptionKey;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/**
 * Represents a meta stack model, consisting of a chain of elements, separated by spacers.
 *
 * <p>The resultant string is constructed as:
 * [start spacer] [element] [middle spacer] [element] [middle spacer] [element] [end spacer]</p>
 *
 * <p>Definitions can be passed to a users UserData instance using MetaContexts, and the result of this stack can be
 * retrieved from the returned MetaData instance.</p>
 */
public interface MetaStackDefinition {

    /**
     * The {@link OptionKey} for the prefix {@link MetaStackDefinition}.
     */
    OptionKey<MetaStackDefinition> PREFIX_STACK_KEY = OptionKey.of("prefixstack", MetaStackDefinition.class);

    /**
     * The {@link OptionKey} for the suffix {@link MetaStackDefinition}.
     */
    OptionKey<MetaStackDefinition> SUFFIX_STACK_KEY = OptionKey.of("suffixstack", MetaStackDefinition.class);

    /**
     * Gets an immutable list of the elements in this stack definition
     *
     * @return the elements in this stack
     */
    @NonNull @Unmodifiable List<MetaStackElement> getElements();

    /**
     * Gets the duplicate removal function, applied to the entries before
     * formatting takes place.
     *
     * @return the duplicate removal function
     */
    @NonNull DuplicateRemovalFunction getDuplicateRemovalFunction();

    /**
     * Gets the spacer string added before any stack elements
     *
     * @return the start spacer
     */
    @NonNull String getStartSpacer();

    /**
     * Gets the spacer added between stack elements
     *
     * @return the middle spacer
     */
    @NonNull String getMiddleSpacer();

    /**
     * Gets the spacer added after any stack elements
     *
     * @return the end spacer
     */
    @NonNull String getEndSpacer();

}
