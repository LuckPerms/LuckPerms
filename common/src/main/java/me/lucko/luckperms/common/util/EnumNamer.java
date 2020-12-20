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

package me.lucko.luckperms.common.util;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

/**
 * Small utility to cache custom name lookups for enum values.
 *
 * @param <E> the enum type
 */
public class EnumNamer<E extends Enum<E>> {
    public static final Function<Enum<?>, String> LOWER_CASE_NAME = value -> value.name().toLowerCase();

    private final String[] names;
    private final Function<? super E, String> namingFunction;

    public EnumNamer(Class<E> enumClass, Map<? super E, String> definedNames, Function<? super E, String> namingFunction) {
        E[] values = enumClass.getEnumConstants();
        this.names = new String[values.length];
        for (E value : values) {
            String name = definedNames.get(value);
            if (name == null) {
                name = namingFunction.apply(value);
            }
            this.names[value.ordinal()] = name;
        }
        this.namingFunction = namingFunction;
    }

    public EnumNamer(Class<E> enumClass, Function<? super E, String> namingFunction) {
        this(enumClass, Collections.emptyMap(), namingFunction);
    }

    public String name(E value) {
        int ordinal = value.ordinal();
        // support the Bukkit-Forge hack where enum constants are added at runtime...
        if (ordinal >= this.names.length) {
            return this.namingFunction.apply(value);
        }
        return this.names[ordinal];
    }

}
