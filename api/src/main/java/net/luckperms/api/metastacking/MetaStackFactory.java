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

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.Optional;

/**
 * Factory to create meta stack elements and definitions.
 */
public interface MetaStackFactory {

    /**
     * Parses a standard {@link MetaStackElement} from string, using the pre-defined elements in the plugin.
     *
     * @param definition the definition
     * @return the parsed element, if present
     */
    @NonNull Optional<MetaStackElement> fromString(@NonNull String definition);

    /**
     * Parses a list of {@link MetaStackElement}s from string, using the pre-defined elements in the plugin.
     *
     * <p>If an element cannot be parsed, it will not be included in the resultant list.</p>
     *
     * @param definitions the definition strings
     * @return a list of parsed elements
     */
    @NonNull List<MetaStackElement> fromStrings(@NonNull List<String> definitions);

    /**
     * Creates a new {@link MetaStackDefinition} with the given properties.
     *
     * @param elements                 the elements to be included in the stack.
     * @param duplicateRemovalFunction the duplicate removal function
     * @param startSpacer              the spacer to be included at the start of the stacks output
     * @param middleSpacer             the spacer to be included between stack elements
     * @param endSpacer                the spacer to be included at the end of the stacks output
     * @return the new stack definition instance
     */
    @NonNull MetaStackDefinition createDefinition(@NonNull List<MetaStackElement> elements, @NonNull DuplicateRemovalFunction duplicateRemovalFunction, @NonNull String startSpacer, @NonNull String middleSpacer, @NonNull String endSpacer);


}
