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

package me.lucko.luckperms.common.placeholders;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A registry of standard/built-in {@link Placeholder}s.
 */
public class PlaceholderRegistry {

    private static final List<Placeholder> PLACEHOLDERS = Collections.unmodifiableList(Arrays.asList(
            Placeholders.PREFIX,
            Placeholders.SUFFIX,
            Placeholders.ALL_META,
            Placeholders.META,
            Placeholders.PREFIX_ELEMENT,
            Placeholders.SUFFIX_ELEMENT,
            Placeholders.ALL_CONTEXT,
            Placeholders.CONTEXT,
            Placeholders.GROUPS,
            Placeholders.INHERITED_GROUPS,
            Placeholders.PRIMARY_GROUP_NAME,
            Placeholders.HAS_PERMISSION,
            Placeholders.INHERITS_PERMISSION,
            Placeholders.CHECK_PERMISSION,
            Placeholders.IN_GROUP,
            Placeholders.INHERITS_GROUP,
            Placeholders.ON_TRACK,
            Placeholders.HAS_GROUPS_ON_TRACK,
            Placeholders.HIGHEST_GROUP_BY_WEIGHT,
            Placeholders.LOWEST_GROUP_BY_WEIGHT,
            Placeholders.HIGHEST_INHERITED_GROUP_BY_WEIGHT,
            Placeholders.LOWEST_INHERITED_GROUP_BY_WEIGHT,
            Placeholders.HIGHEST_GROUP_WEIGHT,
            Placeholders.CURRENT_GROUP_ON_TRACK,
            Placeholders.NEXT_GROUP_ON_TRACK,
            Placeholders.PREVIOUS_GROUP_ON_TRACK,
            Placeholders.FIRST_GROUP_ON_TRACKS,
            Placeholders.LAST_GROUP_ON_TRACKS,
            Placeholders.EXPIRY_TIME,
            Placeholders.INHERITED_EXPIRY_TIME,
            Placeholders.GROUP_EXPIRY_TIME,
            Placeholders.INHERITED_GROUP_EXPIRY_TIME
    ));

    private static final Map<String, Placeholder> PLACEHOLDER_MAP = PLACEHOLDERS.stream()
            .collect(Collectors.toMap(Placeholder::id, Function.identity()));

    /**
     * Get a list of all placeholders.
     *
     * @return a list of placeholders
     */
    public static @NonNull List<Placeholder> getAll() {
        return PLACEHOLDERS;
    }

    /**
     * Lookup a placeholder by id.
     *
     * @param id the id to lookup
     * @return the placeholder, if found
     */
    public static @Nullable Placeholder lookup(@NonNull String id) {
        return PLACEHOLDER_MAP.get(id);
    }

}
