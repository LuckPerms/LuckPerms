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

package net.luckperms.api.node.metadata;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

import java.util.Objects;

/**
 * Represents a key for a specific type of node metadata.
 *
 * <p>It is intended that {@link NodeMetadataKey}s are obtained and stored as
 * follows.</p>
 * <p><blockquote><pre>
 *     public static final NodeMetadataKey&lt;SpecialMetadata&gt; SPECIAL_METADATA_KEY = NodeMetadataKey.of("specialmetadata", SpecialMetadata.class);
 * </pre></blockquote>
 *
 * @param <T> the metadata type
 */
@NonExtendable
public interface NodeMetadataKey<T> {

    /**
     * Creates a new {@link NodeMetadataKey} for the given name and type.
     *
     * <p>Note that the returned key implements object reference equality.</p>
     *
     * @param name the name
     * @param type the type
     * @param <T> the type parameter
     * @return the key
     */
    static <T> @NonNull NodeMetadataKey<T> of(@NonNull String name, @NonNull Class<T> type) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        return new SimpleNodeMetadataKey<>(name, type);
    }

    /**
     * Gets a name describing the key type.
     *
     * @return the key name
     */
    @NonNull String name();

    /**
     * Gets the type of the key
     *
     * @return the type
     */
    @NonNull Class<T> type();

}
