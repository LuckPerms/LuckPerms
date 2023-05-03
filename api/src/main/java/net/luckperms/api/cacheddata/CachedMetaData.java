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
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import net.luckperms.api.node.types.WeightNode;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.function.Function;

/**
 * Holds cached meta lookup data for a specific set of contexts.
 * 
 * <p>Meta data refers to {@link PrefixNode prefixes}, {@link SuffixNode suffixes} and
 * {@link MetaNode meta (options)} held by a permission holder.</p>
 *
 * <p>All calls will account for inheritance, as well as any default data
 * provided by the platform. These calls are heavily cached and are therefore
 * fast.</p>
 */
public interface CachedMetaData extends CachedData {

    /**
     * Query a meta value for the given {@code key}.
     * 
     * <p>This method will always return a {@link Result}, but the
     * {@link Result#result() inner result} {@link String} will be null if a value
     * for the given key was not found.</p>
     *
     * @param key the key
     * @return a result containing the value
     * @since 5.4
     */
    @NonNull Result<String, MetaNode> queryMetaValue(@NonNull String key);

    /**
     * Gets a value for the given meta key.
     * 
     * <p>If no such meta value exists for the given key, {@code null} is returned.</p>
     *
     * @param key the key
     * @return the value
     */
    default @Nullable String getMetaValue(@NonNull String key) {
        return queryMetaValue(key).result();
    }

    /**
     * Gets a value for the given meta key, and runs it through the given {@code transformer}.
     *
     * <p>If no such meta value exists, an {@link Optional#empty() empty optional} is returned.
     * (the transformer will never be passed a null argument)</p>
     *
     * <p>The transformer is allowed to throw {@link IllegalArgumentException} or return null. This
     * will also result in an {@link Optional#empty() empty optional} being returned.</p>
     *
     * <p>For example, to parse and return an integer meta value, use:</p>
     * <p><blockquote><pre>
     *     getMetaValue("my-int-val", Integer::parseInt).orElse(0);
     * </pre></blockquote>
     *
     * @param key the key
     * @param valueTransformer the transformer used to transform the value
     * @param <T> the type of the transformed result
     * @return the meta value
     * @since 5.3
     */
    default <T> @NonNull Optional<T> getMetaValue(@NonNull String key, @NonNull Function<String, ? extends T> valueTransformer) {
        return Optional.ofNullable(getMetaValue(key)).map(value -> {
            try {
                return valueTransformer.apply(value);
            } catch (IllegalArgumentException e) {
                return null;
            }
        });
    }

    /**
     * Query for a prefix.
     * 
     * <p>This method uses the rules defined by the {@link #getPrefixStackDefinition() prefix stack}
     * to produce a {@link String} output.</p>
     * 
     * <p>Assuming the default configuration is used, this will usually be the value of the
     * holder's highest priority prefix node.</p>
     * 
     * <p>This method will always return a {@link Result}, but the
     * {@link Result#result() inner result} {@link String} will be null if
     * a the resultant prefix stack contained no elements.</p>
     *
     * @return a result containing the prefix
     * @since 5.4
     */
    @NonNull Result<String, PrefixNode> queryPrefix();

    /**
     * Gets the prefix.
     * 
     * <p>This method uses the rules defined by the {@link #getPrefixStackDefinition() prefix stack}
     * to produce a {@link String} output.</p>
     * 
     * <p>Assuming the default configuration is used, this will usually be the value of the
     * holder's highest priority prefix node.</p>
     * 
     * <p>If the resultant prefix stack contained no elements, {@code null} is returned.</p>
     *
     * @return a prefix string, or null
     */
    default @Nullable String getPrefix() {
        return queryPrefix().result();
    }

    /**
     * Query for a suffix.
     *
     * <p>This method uses the rules defined by the {@link #getSuffixStackDefinition() suffix stack}
     * to produce a {@link String} output.</p>
     *
     * <p>Assuming the default configuration is used, this will usually be the value of the
     * holder's highest priority suffix node.</p>
     *
     * <p>This method will always return a {@link Result}, but the
     * {@link Result#result() inner result} {@link String} will be null if
     * a the resultant suffix stack contained no elements.</p>
     *
     * @return a result containing the suffix
     * @since 5.4
     */
    @NonNull Result<String, SuffixNode> querySuffix();

    /**
     * Gets the suffix.
     *
     * <p>This method uses the rules defined by the {@link #getSuffixStackDefinition() suffix stack}
     * to produce a {@link String} output.</p>
     *
     * <p>Assuming the default configuration is used, this will usually be the value of the
     * holder's highest priority suffix node.</p>
     *
     * <p>If the resultant suffix stack contained no elements, {@code null} is returned.</p>
     *
     * @return a suffix string, or null
     */
    default @Nullable String getSuffix() {
        return querySuffix().result();
    }

    /**
     * Query for a weight.
     *
     * <p>This method will always return a {@link Result}, and the
     * {@link Result#result() inner result} {@link Integer} will never be null.
     * A value of {@code 0} is equivalent to null.</p>
     *
     * @return a result containing the weight
     * @since 5.5
     */
    @NonNull Result<Integer, WeightNode> queryWeight();

    /**
     * Gets the weight.
     *
     * <p>If the there is no defined weight, {@code 0} is returned.</p>
     *
     * @return the weight
     * @since 5.5
     */
    default int getWeight() {
        return queryWeight().result();
    }

    /**
     * Gets a map of all accumulated {@link MetaNode meta}.
     *
     * <p>Prefer using the {@link #getMetaValue(String)} method for querying values.</p>
     *
     * @return a map of meta
     */
    @NonNull @Unmodifiable Map<String, List<String>> getMeta();

    /**
     * Gets a sorted map of all accumulated {@link PrefixNode prefixes}.
     *
     * <p>Prefer using the {@link #getPrefix()} method for querying.</p>
     *
     * @return a sorted map of prefixes
     */
    @NonNull @Unmodifiable SortedMap<Integer, String> getPrefixes();

    /**
     * Gets a sorted map of all accumulated {@link SuffixNode suffixes}.
     *
     * <p>Prefer using the {@link #getSuffix()} method for querying.</p>
     *
     * @return a sorted map of suffixes
     */
    @NonNull @Unmodifiable SortedMap<Integer, String> getSuffixes();

    /**
     * Gets the name of the holders primary group.
     *
     * <p>Will return {@code null} for Group holder types.</p>
     *
     * @return the name of the primary group
     * @since 5.1
     */
    @Nullable String getPrimaryGroup();

    /**
     * Gets the definition used for the prefix stack.
     *
     * @return the definition used for the prefix stack
     */
    @NonNull MetaStackDefinition getPrefixStackDefinition();

    /**
     * Gets the definition used for the suffix stack.
     *
     * @return the definition used for the suffix stack
     */
    @NonNull MetaStackDefinition getSuffixStackDefinition();

}
