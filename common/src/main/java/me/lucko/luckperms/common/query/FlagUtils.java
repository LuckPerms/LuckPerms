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

package me.lucko.luckperms.common.query;

import net.luckperms.api.query.Flag;

import java.util.EnumSet;
import java.util.Set;

final class FlagUtils {
    private FlagUtils() {}

    private static final EnumSet<Flag> DEFAULT_FLAGS_SET = EnumSet.allOf(Flag.class);
    private static final int DEFAULT_FLAGS_SIZE = DEFAULT_FLAGS_SET.size();
    static final byte DEFAULT_FLAGS = toByte0(DEFAULT_FLAGS_SET);

    /* bitwise utility methods */

    static boolean read(byte b, Flag setting) {
        return ((b >> setting.ordinal()) & 1) == 1;
    }

    static byte toByte(Set<Flag> settings) {
        // fast path for the default set of flags.
        if (settings.size() == DEFAULT_FLAGS_SIZE) {
            return DEFAULT_FLAGS;
        }
        return toByte0(settings);
    }

    private static byte toByte0(Set<Flag> settings) {
        byte b = 0;
        for (Flag setting : settings) {
            b |= (1 << setting.ordinal());
        }
        return b;
    }

    static Set<Flag> toSet(byte b) {
        EnumSet<Flag> settings = EnumSet.noneOf(Flag.class);
        for (Flag setting : Flag.values()) {
            if (read(b, setting)) {
                settings.add(setting);
            }
        }
        return settings;
    }
    
}
