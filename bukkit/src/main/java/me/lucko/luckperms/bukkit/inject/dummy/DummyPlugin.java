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

package me.lucko.luckperms.bukkit.inject.dummy;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

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

    @Override public @NonNull File getDataFolder() { return null; }
    @Override public @NonNull PluginDescriptionFile getDescription() { return null; }
    @Override public @NonNull FileConfiguration getConfig() { return null; }
    @Override public InputStream getResource(@NonNull String s) { return null; }
    @Override public void saveConfig() {}
    @Override public void saveDefaultConfig() {}
    @Override public void saveResource(@NonNull String s, boolean b) {}
    @Override public void reloadConfig() {}
    @Override public @NonNull PluginLoader getPluginLoader() { return null; }
    @Override public @NonNull Server getServer() { return null; }
    @Override public void onDisable() {}
    @Override public void onLoad() {}
    @Override public void onEnable() {}
    @Override public boolean isNaggable() { return false; }
    @Override public void setNaggable(boolean b) {}
    @Override public ChunkGenerator getDefaultWorldGenerator(@NonNull String s, String s1) { return null; }
    @Override public @NonNull Logger getLogger() { return null; }
    @Override public @NonNull String getName() { return null; }
    @Override public boolean onCommand(@NonNull CommandSender commandSender, @NonNull Command command, @NonNull String s, @NonNull String[] strings) { return false; }
    @Override public List<String> onTabComplete(@NonNull CommandSender commandSender, @NonNull Command command, @NonNull String s, @NonNull String[] strings) { return null; }

}
