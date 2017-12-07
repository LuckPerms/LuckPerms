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

package me.lucko.luckperms.common.utils;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.text.CollationKey;
import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class CollationKeyCache implements Comparator<String> {
    private static final CollationKeyCache INSTANCE = new CollationKeyCache();

    private static final Collator COLLATOR = Collator.getInstance(Locale.ENGLISH);

    static {
        COLLATOR.setStrength(Collator.IDENTICAL);
        COLLATOR.setDecomposition(Collator.FULL_DECOMPOSITION);
    }

    private static final LoadingCache<String, CollationKey> CACHE = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build(COLLATOR::getCollationKey);

    public static Comparator<String> comparator() {
        return INSTANCE;
    }

    private CollationKeyCache() {

    }

    @Override
    public int compare(String o1, String o2) {
        return compareStrings(o1, o2);
    }

    public static int compareStrings(String o1, String o2) {
        //noinspection StringEquality
        if (o1 == o2) {
            return 0;
        }

        try {
            CollationKey o1c = CACHE.get(o1);
            CollationKey o2c = CACHE.get(o2);

            if (o1c != null && o2c != null) {
                int i = o1c.compareTo(o2c);
                if (i != 0) {
                    return i;
                }
            }

            // fallback to standard string comparison
            return o1.compareTo(o2);
        } catch (Exception e) {
            // ignored
        }

        // shrug
        return 0;
    }
}
