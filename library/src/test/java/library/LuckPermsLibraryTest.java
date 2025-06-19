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

package library;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.UUID;

import me.lucko.luckperms.common.plugin.logging.PluginLogger;
import me.lucko.luckperms.library.LuckPermsLibrary;
import me.lucko.luckperms.library.LuckPermsLibraryManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import net.kyori.ansi.ColorLevel;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;

public class LuckPermsLibraryTest implements LuckPermsLibraryManager, PluginLogger {

    public static void main(String[] args) {
        new LuckPermsLibraryTest();
    }

    private final LuckPermsLibrary luckPerms;

    public LuckPermsLibraryTest() {
        luckPerms = new LuckPermsLibrary(this);
        luckPerms.start();

        luckPerms.execFromConsole("user 140e3c62-f31a-4aea-8e69-3ad34c464e64 permission set luckperms.* true").join();
        luckPerms.playerJoined(UUID.fromString("140e3c62-f31a-4aea-8e69-3ad34c464e64"), "mega12345mega");
        luckPerms.execFromPlayer(UUID.fromString("140e3c62-f31a-4aea-8e69-3ad34c464e64"), "group default permission info").join();
        luckPerms.playerDisconnected(UUID.fromString("140e3c62-f31a-4aea-8e69-3ad34c464e64"));

        luckPerms.close();
    }

    @Override
    public PluginLogger getLogger() {
        return this;
    }

    @Override
    public String getServerBrand() {
        return "Library Test";
    }

    @Override
    public String getServerVersion() {
        return "n/a";
    }

    @Override
    public Path getDataDirectory() {
        return Path.of("data");
    }

    @Override
    public ConfigurationLoader<? extends ConfigurationNode> createConfigLoader() {
        return YAMLConfigurationLoader.builder().setSource(() -> new BufferedReader(new InputStreamReader(InputStream.nullInputStream()))).build();
    }

    @Override
    public void onConsoleMessage(Component message) {
        System.out.println(ANSIComponentSerializer.builder().colorLevel(ColorLevel.INDEXED_16).build().serialize(message));
    }

    @Override
    public void performConsoleCommand(String command) {
        System.err.println("Console exec: " + command);
        luckPerms.execFromConsole(command);
    }

    @Override
    public void onPlayerMessage(UUID player, Component message) {
        System.out.println("To " + player + ": " + ANSIComponentSerializer.builder().colorLevel(ColorLevel.INDEXED_16).build().serialize(message));
    }

    @Override
    public void performPlayerCommand(UUID player, String command) {
        System.err.println(player + " exec: " + command);
        luckPerms.execFromPlayer(player, command);
    }

    @Override
    public void info(String s) {
        System.out.println(s);
    }

    @Override
    public void warn(String s) {
        System.out.println("Warning: " + s);
    }

    @Override
    public void warn(String s, Throwable t) {
        System.out.println("Warning: " + s);
        t.printStackTrace(System.out);
    }

    @Override
    public void severe(String s) {
        System.err.println(s);
    }

    @Override
    public void severe(String s, Throwable t) {
        System.err.println(s);
        t.printStackTrace();
    }

}
