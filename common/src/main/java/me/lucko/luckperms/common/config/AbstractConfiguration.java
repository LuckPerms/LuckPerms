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

package me.lucko.luckperms.common.config;

import me.lucko.luckperms.common.config.adapter.ConfigurationAdapter;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

/**
 * An abstract implementation of {@link LuckPermsConfiguration}.
 *
 * <p>Values are loaded into memory on init.</p>
 */
public class AbstractConfiguration implements LuckPermsConfiguration {

    /**
     * The configurations loaded values.
     *
     * <p>The value corresponding to each key is stored at the index defined
     * by {@link ConfigKey#ordinal()}.</p>
     */
    private Object[] values = null;

    private final LuckPermsPlugin plugin;
    private final ConfigurationAdapter adapter;

    // the contextsfile handler
    private final ContextsFile contextsFile = new ContextsFile(this);

    public AbstractConfiguration(LuckPermsPlugin plugin, ConfigurationAdapter adapter) {
        this.plugin = plugin;
        this.adapter = adapter;

        load();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(ConfigKey<T> key) {
        return (T) this.values[key.ordinal()];
    }

    @Override
    public synchronized void load() {
        // if this is a reload operation
        boolean reload = true;

        // if values are null, must be loading for the first time
        if (this.values == null) {
            this.values = new Object[ConfigKeys.size()];
            reload = false;
        }

        for (ConfigKey<?> key : ConfigKeys.getKeys().values()) {
            // don't reload enduring keys.
            if (reload && key instanceof ConfigKeyTypes.EnduringKey) {
                continue;
            }

            // load the value for the key
            Object value = key.get(this.adapter);
            this.values[key.ordinal()] = value;
        }

        // load the contexts file
        this.contextsFile.load();
    }

    @Override
    public void reload() {
        this.adapter.reload();
        load();

        getPlugin().getEventDispatcher().dispatchConfigReload();
    }

    @Override
    public LuckPermsPlugin getPlugin() {
        return this.plugin;
    }

    @Override
    public ContextsFile getContextsFile() {
        return this.contextsFile;
    }
}
