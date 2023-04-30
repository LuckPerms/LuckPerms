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

package me.lucko.luckperms.common.config.generic.adapter;

import com.google.common.base.Splitter;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Map;

public abstract class StringBasedConfigurationAdapter implements ConfigurationAdapter {

    private static final Splitter LIST_SPLITTER = Splitter.on(',');
    private static final Splitter.MapSplitter MAP_SPLITTER = Splitter.on(',').withKeyValueSeparator('=');

    protected abstract @Nullable String resolveValue(String path);

    @Override
    public String getString(String path, String def) {
        String value = resolveValue(path);
        if (value == null) {
            return def;
        }

        return value;
    }

    @Override
    public int getInteger(String path, int def) {
        String value = resolveValue(path);
        if (value == null) {
            return def;
        }

        try {
            return Integer.parseInt(value);
        } catch (IllegalArgumentException e) {
            return def;
        }
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        String value = resolveValue(path);
        if (value == null) {
            return def;
        }

        try {
            return Boolean.parseBoolean(value);
        } catch (IllegalArgumentException e) {
            return def;
        }
    }

    @Override
    public List<String> getStringList(String path, List<String> def) {
        String value = resolveValue(path);
        if (value == null) {
            return def;
        }

        return LIST_SPLITTER.splitToList(value);
    }

    @Override
    public Map<String, String> getStringMap(String path, Map<String, String> def) {
        String value = resolveValue(path);
        if (value == null) {
            return def;
        }

        return MAP_SPLITTER.split(value);
    }
}
