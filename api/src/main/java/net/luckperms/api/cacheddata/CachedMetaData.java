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

package net.luckperms.api.cacheddata;

import net.luckperms.api.metastacking.MetaStackDefinition;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * Holds cached meta lookup data for a specific set of contexts.
 */
public interface CachedMetaData extends CachedData {

    /**
     * Gets a value for the given meta key.
     *
     * @param key the key
     * @return the value
     */
    @Nullable String getMetaValue(String key);

    /**
     * Gets the user's highest priority prefix, or null if the user has no prefixes
     *
     * @return a prefix string, or null
     */
    @Nullable String getPrefix();

    /**
     * Gets the user's highest priority suffix, or null if the user has no suffixes
     *
     * @return a suffix string, or null
     */
    @Nullable String getSuffix();

    /**
     * Gets an immutable copy of the meta this user has.
     *
     * @return an immutable map of meta
     */
    @NonNull Map<String, List<String>> getMeta();

    /**
     * Gets an immutable sorted map of all of the prefixes the user has, whereby the first value is the highest priority
     * prefix.
     *
     * @return a sorted map of prefixes
     */
    @NonNull SortedMap<Integer, String> getPrefixes();

    /**
     * Gets an immutable sorted map of all of the suffixes the user has, whereby the first value is the highest priority
     * suffix.
     *
     * @return a sorted map of suffixes
     */
    @NonNull SortedMap<Integer, String> getSuffixes();

    /**
     * Gets the definition used for the prefix stack
     *
     * @return the definition used for the prefix stack
     */
    @NonNull MetaStackDefinition getPrefixStackDefinition();

    /**
     * Gets the definition used for the suffix stack
     *
     * @return the definition used for the suffix stack
     */
    @NonNull MetaStackDefinition getSuffixStackDefinition();

}
