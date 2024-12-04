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

package me.lucko.luckperms.standalone.utils;

import com.google.common.collect.ImmutableMap;
import me.lucko.luckperms.standalone.app.LuckPermsApplication;
import me.lucko.luckperms.standalone.utils.TestPluginBootstrap.TestPlugin;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class TestPluginProvider {
    private TestPluginProvider() {}

    /**
     * Creates a test LuckPerms plugin instance, loads/enables it, and returns it.
     *
     * @param tempDir the temporary directory to run the plugin in
     * @param config the config to set
     * @return the plugin
     */
    public static Plugin create(Path tempDir, Map<String, String> config) {
        Map<String, String> props = new HashMap<>(config);
        props.putIfAbsent("auto-install-translations", "false");
        props.putIfAbsent("editor-lazily-generate-key", "true");

        props.forEach((k, v) -> System.setProperty("luckperms." + k, v));

        LuckPermsApplication app = new LuckPermsApplication(() -> {});
        TestPluginBootstrap bootstrap = new TestPluginBootstrap(app, tempDir);

        bootstrap.onLoad();
        bootstrap.onEnable();

        props.keySet().forEach((k) -> System.clearProperty("luckperms." + k));

        return new Plugin(app, bootstrap, bootstrap.getPlugin());
    }

    /**
     * Creates a test LuckPerms plugin instance, loads/enables it, and returns it.
     *
     * @param tempDir the temporary directory to run the plugin in
     * @return the plugin
     */
    public static Plugin create(Path tempDir) {
        return create(tempDir, ImmutableMap.of());
    }

    /**
     * Creates a test LuckPerms plugin instance, loads/enables it, runs the consumer, then disables it.
     *
     * @param tempDir the temporary directory to run the plugin in
     * @param config the config to set
     * @param consumer the consumer
     * @param <E> the exception class thrown by the consumer
     * @throws E exception
     */
    public static <E extends Throwable> void use(Path tempDir, Map<String, String> config, Consumer<E> consumer) throws E {
        try (Plugin plugin = create(tempDir, config)) {
            consumer.accept(plugin.app, plugin.bootstrap, plugin.plugin);
        }
    }

    /**
     * Creates a test LuckPerms plugin instance, loads/enables it, runs the consumer, then disables it.
     *
     * @param tempDir the temporary directory to run the plugin in
     * @param consumer the consumer
     * @param <E> the exception class thrown by the consumer
     * @throws E exception
     */
    public static <E extends Throwable> void use(Path tempDir, Consumer<E> consumer) throws E {
        use(tempDir, ImmutableMap.of(), consumer);
    }

    public interface Consumer<E extends Throwable> {
        void accept(LuckPermsApplication app, TestPluginBootstrap bootstrap, TestPlugin plugin) throws E;
    }

    public record Plugin(LuckPermsApplication app, TestPluginBootstrap bootstrap, TestPlugin plugin) implements AutoCloseable {
        @Override
        public void close() {
            this.bootstrap.onDisable();
        }
    }

}
