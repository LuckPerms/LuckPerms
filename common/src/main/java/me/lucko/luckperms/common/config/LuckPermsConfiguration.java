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

import me.lucko.luckperms.common.config.generic.KeyedConfiguration;
import me.lucko.luckperms.common.config.generic.adapter.ConfigurationAdapter;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

public class LuckPermsConfiguration extends KeyedConfiguration {
    private final LuckPermsPlugin plugin;
    private final ContextsFile contextsFile;

    public LuckPermsConfiguration(LuckPermsPlugin plugin, ConfigurationAdapter adapter) {
        super(adapter, ConfigKeys.getKeys());
        this.plugin = plugin;
        this.contextsFile = new ContextsFile(this);

        init();
    }

    @Override
    protected void load(boolean initial) {
        super.load(initial);
        this.contextsFile.load();
    }

    @Override
    public void reload() {
        super.reload();
        getPlugin().getEventDispatcher().dispatchConfigReload();
    }

    public ContextsFile getContextsFile() {
        return this.contextsFile;
    }

    public LuckPermsPlugin getPlugin() {
        return this.plugin;
    }
}
