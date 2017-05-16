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

package me.lucko.luckperms.api.caching;

import me.lucko.luckperms.api.metastacking.MetaStackDefinition;

import java.util.Map;
import java.util.SortedMap;

/**
 * Holds cached Meta lookup data for a specific set of contexts
 *
 * @since 2.13
 */
public interface MetaData {

    /**
     * Gets an immutable copy of the meta this user has
     *
     * @return an immutable map of meta
     */
    Map<String, String> getMeta();

    /**
     * Gets an immutable sorted map of all of the prefixes the user has, whereby the first value is the highest priority
     * prefix.
     *
     * @return a sorted map of prefixes
     */
    SortedMap<Integer, String> getPrefixes();

    /**
     * Gets an immutable sorted map of all of the suffixes the user has, whereby the first value is the highest priority
     * suffix.
     *
     * @return a sorted map of suffixes
     */
    SortedMap<Integer, String> getSuffixes();

    /**
     * Gets the user's highest priority prefix, or null if the user has no prefixes
     *
     * @return a prefix string, or null
     */
    String getPrefix();

    /**
     * Gets the user's highest priority suffix, or null if the user has no suffixes
     *
     * @return a suffix string, or null
     */
    String getSuffix();

    /**
     * Gets the definition used for the prefix stack
     *
     * @return the definition used for the prefix stack
     * @since 3.2
     */
    MetaStackDefinition getPrefixStackDefinition();

    /**
     * Gets the definition used for the suffix stack
     *
     * @return the definition used for the suffix stack
     * @since 3.2
     */
    MetaStackDefinition getSuffixStackDefinition();

}
