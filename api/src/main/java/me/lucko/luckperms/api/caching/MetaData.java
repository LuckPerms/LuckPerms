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

package me.lucko.luckperms.api.caching;

import com.google.common.collect.ListMultimap;

import me.lucko.luckperms.api.metastacking.MetaStackDefinition;

import java.util.Map;
import java.util.SortedMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Holds cached meta lookup data for a specific set of contexts.
 *
 * @since 2.13
 */
public interface MetaData extends CachedDataContainer {

    /**
     * Gets the contexts this container is holding data for.
     *
     * @return the contexts this container is caching
     */
    @Nonnull
    MetaContexts getMetaContexts();

    /**
     * Gets an immutable copy of the meta this user has.
     *
     * <p>A list multimap is used because when inherited values are included, each key can be
     * mapped to multiple values.</p>
     *
     * <p>The first value to be accumulated (and used to represent the key in {@link #getMeta()} is at index 0
     * in the list. Any additional values are stored in order of accumulation.</p>
     *
     * @return an immutable multimap of meta
     * @since 3.3
     */
    @Nonnull
    ListMultimap<String, String> getMetaMultimap();

    /**
     * Gets an immutable copy of the meta this user has.
     *
     * <p>This map is formed by taking the entries in {@link #getMetaMultimap()}, and mapping each key
     * to the value at index 0 in the corresponding list.</p>
     *
     * @return an immutable map of meta
     */
    @Nonnull
    Map<String, String> getMeta();

    /**
     * Gets an immutable sorted map of all of the prefixes the user has, whereby the first value is the highest priority
     * prefix.
     *
     * @return a sorted map of prefixes
     */
    @Nonnull
    SortedMap<Integer, String> getPrefixes();

    /**
     * Gets an immutable sorted map of all of the suffixes the user has, whereby the first value is the highest priority
     * suffix.
     *
     * @return a sorted map of suffixes
     */
    @Nonnull
    SortedMap<Integer, String> getSuffixes();

    /**
     * Gets the user's highest priority prefix, or null if the user has no prefixes
     *
     * @return a prefix string, or null
     */
    @Nullable
    String getPrefix();

    /**
     * Gets the user's highest priority suffix, or null if the user has no suffixes
     *
     * @return a suffix string, or null
     */
    @Nullable
    String getSuffix();

    /**
     * Gets the definition used for the prefix stack
     *
     * @return the definition used for the prefix stack
     * @since 3.2
     */
    @Nonnull
    MetaStackDefinition getPrefixStackDefinition();

    /**
     * Gets the definition used for the suffix stack
     *
     * @return the definition used for the suffix stack
     * @since 3.2
     */
    @Nonnull
    MetaStackDefinition getSuffixStackDefinition();

}
