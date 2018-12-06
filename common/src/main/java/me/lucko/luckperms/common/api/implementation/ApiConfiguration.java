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

package me.lucko.luckperms.common.api.implementation;

import me.lucko.luckperms.api.LPConfiguration;
import me.lucko.luckperms.api.LookupSetting;
import me.lucko.luckperms.common.config.ConfigKey;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.config.LuckPermsConfiguration;
import me.lucko.luckperms.common.util.ImmutableCollectors;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Map;

public class ApiConfiguration implements LPConfiguration {
    private final LuckPermsConfiguration handle;
    private final Unsafe unsafe;

    public ApiConfiguration(LuckPermsConfiguration handle) {
        this.handle = handle;
        this.unsafe = new UnsafeImpl();
    }

    @Override
    public @NonNull String getServer() {
        return this.handle.get(ConfigKeys.SERVER);
    }

    @Override
    public boolean getIncludeGlobalPerms() {
        return this.handle.get(ConfigKeys.GLOBAL_CONTEXTS).hasSetting(LookupSetting.INCLUDE_NODES_SET_WITHOUT_SERVER);
    }

    @Override
    public boolean getIncludeGlobalWorldPerms() {
        return this.handle.get(ConfigKeys.GLOBAL_CONTEXTS).hasSetting(LookupSetting.INCLUDE_NODES_SET_WITHOUT_WORLD);
    }

    @Override
    public boolean getApplyGlobalGroups() {
        return this.handle.get(ConfigKeys.GLOBAL_CONTEXTS).hasSetting(LookupSetting.APPLY_PARENTS_SET_WITHOUT_SERVER);
    }

    @Override
    public boolean getApplyGlobalWorldGroups() {
        return this.handle.get(ConfigKeys.GLOBAL_CONTEXTS).hasSetting(LookupSetting.APPLY_PARENTS_SET_WITHOUT_WORLD);
    }

    @Override
    public @NonNull String getStorageMethod() {
        return this.handle.get(ConfigKeys.STORAGE_METHOD);
    }

    @Override
    public boolean getSplitStorage() {
        return this.handle.get(ConfigKeys.SPLIT_STORAGE);
    }

    @Override
    public @NonNull Map<String, String> getSplitStorageOptions() {
        return this.handle.get(ConfigKeys.SPLIT_STORAGE_OPTIONS).entrySet().stream()
                .collect(ImmutableCollectors.toMap(e -> e.getKey().name().toLowerCase(), Map.Entry::getValue));
    }

    @Override
    public @NonNull Unsafe unsafe() {
        return this.unsafe;
    }

    private final class UnsafeImpl implements Unsafe {

        @Override
        public @NonNull Object getObject(String key) {
            ConfigKey<?> configKey = ConfigKeys.getKeys().get(key.toUpperCase());
            if (configKey == null) {
                throw new IllegalArgumentException("Unknown key: " + key);
            }
            return ApiConfiguration.this.handle.get(configKey);
        }
    }
}
