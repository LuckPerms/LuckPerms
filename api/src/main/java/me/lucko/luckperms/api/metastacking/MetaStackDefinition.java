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

package me.lucko.luckperms.api.metastacking;

import java.util.List;

import javax.annotation.Nonnull;

/**
 * Represents a meta stack model, consisting of a chain of elements, separated by spacers.
 *
 * <p>The resultant string is constructed as:
 * [start spacer] [element] [middle spacer] [element] [middle spacer] [element] [end spacer]</p>
 *
 * <p>Definitions can be passed to a users UserData instance using MetaContexts, and the result of this stack can be
 * retrieved from the returned MetaData instance.</p>
 *
 * @since 2.3
 */
public interface MetaStackDefinition {

    /**
     * Gets an immutable list of the elements in this stack definition
     *
     * @return the elements in this stack
     */
    @Nonnull
    List<MetaStackElement> getElements();

    /**
     * Gets the spacer string added before any stack elements
     *
     * @return the start spacer
     */
    @Nonnull
    String getStartSpacer();

    /**
     * Gets the spacer added between stack elements
     *
     * @return the middle spacer
     */
    @Nonnull
    String getMiddleSpacer();

    /**
     * Gets the spacer added after any stack elements
     *
     * @return the end spacer
     */
    @Nonnull
    String getEndSpacer();

}
