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

package me.lucko.luckperms.common.config.generic;

import me.lucko.luckperms.common.config.generic.adapter.ConfigurationAdapter;
import me.lucko.luckperms.common.config.generic.key.ConfigKey;
import me.lucko.luckperms.common.config.generic.key.SimpleConfigKey;
import me.lucko.luckperms.common.util.ImmutableCollectors;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

public class KeyedConfiguration {
    private final ConfigurationAdapter adapter;
    private final List<? extends ConfigKey<?>> keys;
    private final ValuesMap values;

    public KeyedConfiguration(ConfigurationAdapter adapter, List<? extends ConfigKey<?>> keys) {
        this.adapter = adapter;
        this.keys = keys;
        this.values = new ValuesMap(keys.size());
    }

    protected void init() {
        load(true);
    }

    /**
     * Gets the value of a given context key.
     *
     * @param key the key
     * @param <T> the key return type
     * @return the value mapped to the given key. May be null.
     */
    public <T> T get(ConfigKey<T> key) {
        return this.values.get(key);
    }

    protected void load(boolean initial) {
        for (ConfigKey<?> key : this.keys) {
            if (initial || key.reloadable()) {
                this.values.put(key, key.get(this.adapter));
            }
        }
    }

    /**
     * Reloads the configuration.
     */
    public void reload() {
        this.adapter.reload();
        load(false);
    }

    /**
     * Initialises the given pseudo-enum keys class.
     *
     * @param keysClass the keys class
     * @return the list of keys defined by the class with their ordinal values set
     */
    public static List<SimpleConfigKey<?>> initialise(Class<?> keysClass) {
        // get a list of all keys
        List<SimpleConfigKey<?>> keys = Arrays.stream(keysClass.getFields())
                .filter(f -> Modifier.isStatic(f.getModifiers()))
                .filter(f -> ConfigKey.class.equals(f.getType()))
                .map(f -> {
                    try {
                        return (SimpleConfigKey<?>) f.get(null);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(ImmutableCollectors.toList());

        // set ordinal values
        for (int i = 0; i < keys.size(); i++) {
            keys.get(i).setOrdinal(i);
        }

        return keys;
    }

    public static class ValuesMap {
        private final Object[] values;

        public ValuesMap(int size) {
            this.values = new Object[size];
        }

        @SuppressWarnings("unchecked")
        public <T> T get(ConfigKey<T> key) {
            return (T) this.values[key.ordinal()];
        }

        public void put(ConfigKey<?> key, Object value) {
            this.values[key.ordinal()] = value;
        }
    }
}
