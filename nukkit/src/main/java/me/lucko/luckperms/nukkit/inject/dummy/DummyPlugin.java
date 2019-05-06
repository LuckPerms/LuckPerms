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

package me.lucko.luckperms.nukkit.inject.dummy;

import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.plugin.PluginDescription;
import cn.nukkit.plugin.PluginLoader;
import cn.nukkit.plugin.PluginLogger;
import cn.nukkit.utils.Config;

import java.io.File;
import java.io.InputStream;

/**
 * Dummy plugin instance
 */
public class DummyPlugin implements Plugin {
    public static final DummyPlugin INSTANCE = new DummyPlugin();

    private DummyPlugin() {

    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean isDisabled() {
        return false;
    }

    @Override public File getDataFolder() { return null; }
    @Override public PluginDescription getDescription() { return null; }
    @Override public Config getConfig() { return null; }
    @Override public InputStream getResource(String s) { return null; }
    @Override public boolean saveResource(String s) { return false; }
    @Override public void saveConfig() {}
    @Override public void saveDefaultConfig() {}
    @Override public boolean saveResource(String s, boolean b) { return false; }
    @Override public boolean saveResource(String s, String s1, boolean b) { return false; }
    @Override public void reloadConfig() {}
    @Override public PluginLoader getPluginLoader() { return null; }
    @Override public Server getServer() { return null; }
    @Override public void onDisable() {}
    @Override public void onLoad() {}
    @Override public void onEnable() {}
    @Override public PluginLogger getLogger() { return null; }
    @Override public String getName() { return null; }
    @Override public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) { return false; }

}
