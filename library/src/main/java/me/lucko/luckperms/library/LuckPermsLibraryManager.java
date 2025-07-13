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

package me.lucko.luckperms.library;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import me.lucko.luckperms.common.calculator.processor.PermissionProcessor;
import me.lucko.luckperms.common.plugin.AbstractLuckPermsPlugin;
import me.lucko.luckperms.common.plugin.logging.PluginLogger;
import net.kyori.adventure.text.Component;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;

/**
 * <strong>Warning:</strong> This class CANNOT be loaded before the default dependencies are, so do not instantiate
 * this until then (see {@link LuckPermsLibrary#LuckPermsLibrary(boolean, Consumer, PluginLogger, Supplier)})
 */
public interface LuckPermsLibraryManager {
    public String getServerBrand();

    public String getServerVersion();

    public Path getDataDirectory();

    /**
     * @param resolveConfig Calls {@link AbstractLuckPermsPlugin#resolveConfig}
     * @return
     */
    public default ConfigurationLoader<? extends ConfigurationNode> createConfigLoader(Function<String, Path> resolveConfig) {
        return YAMLConfigurationLoader.builder().setPath(resolveConfig.apply("config.yml")).build();
    }

    public default void modifyPermissionCalculator(List<PermissionProcessor> processors) {
    }

    public default Locale getConsoleLocale() {
        return Locale.getDefault();
    }

    public default boolean shouldConsoleSplitNewlines() {
        return false;
    }

    public void onConsoleMessage(Component message);

    public void performConsoleCommand(String command);

    public default Locale getPlayerLocale(UUID player) {
        return Locale.getDefault();
    }

    public default boolean shouldPlayerSplitNewlines(UUID player) {
        return true;
    }

    public void onPlayerMessage(UUID player, Component message);

    public void performPlayerCommand(UUID player, String command);

    public default Optional<UUID> lookupUniqueId(String username) {
        return Optional.empty();
    }

    public default Optional<String> lookupUsername(UUID uuid) {
        return Optional.empty();
    }
}
