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

package me.lucko.luckperms.common.config.generic.key;

import com.google.common.collect.ImmutableMap;
import me.lucko.luckperms.common.config.generic.adapter.ConfigurationAdapter;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public interface ConfigKeyFactory<T> {

    ConfigKeyFactory<Boolean> BOOLEAN = ConfigurationAdapter::getBoolean;
    ConfigKeyFactory<String> STRING = ConfigurationAdapter::getString;
    ConfigKeyFactory<List<String>> STRING_LIST = ConfigurationAdapter::getStringList;
    ConfigKeyFactory<String> LOWERCASE_STRING = (adapter, path, def) -> adapter.getString(path, def).toLowerCase(Locale.ROOT);
    ConfigKeyFactory<Map<String, String>> STRING_MAP = (config, path, def) -> ImmutableMap.copyOf(config.getStringMap(path, ImmutableMap.of()));

    static <T> SimpleConfigKey<T> key(Function<ConfigurationAdapter, T> function) {
        return new SimpleConfigKey<>(function);
    }

    static <T> SimpleConfigKey<T> notReloadable(SimpleConfigKey<T> key) {
        key.setReloadable(false);
        return key;
    }

    static SimpleConfigKey<Boolean> booleanKey(String path, boolean def) {
        return key(new Bound<>(BOOLEAN, path, def));
    }

    static SimpleConfigKey<String> stringKey(String path, String def) {
        return key(new Bound<>(STRING, path, def));
    }

    static SimpleConfigKey<List<String>> stringListKey(String path, List<String> def) {
        return key(new Bound<>(STRING_LIST, path, def));
    }

    static SimpleConfigKey<String> lowercaseStringKey(String path, String def) {
        return key(new Bound<>(LOWERCASE_STRING, path, def));
    }

    static SimpleConfigKey<Map<String, String>> mapKey(String path) {
        return key(new Bound<>(STRING_MAP, path, null));
    }

    /**
     * Extracts the value from the config.
     *
     * @param config the config
     * @param path the path where the value is
     * @param def the default value
     * @return the value
     */
    T getValue(ConfigurationAdapter config, String path, T def);

    /**
     * A {@link ConfigKeyFactory} bound to a given {@code path}.
     *
     * @param <T> the value type
     */
    class Bound<T> implements Function<ConfigurationAdapter, T> {
        private final ConfigKeyFactory<T> factory;
        private final String path;
        private final T def;

        Bound(ConfigKeyFactory<T> factory, String path, T def) {
            this.factory = factory;
            this.path = path;
            this.def = def;
        }

        @Override
        public T apply(ConfigurationAdapter adapter) {
            return this.factory.getValue(adapter, this.path, this.def);
        }
    }

}
