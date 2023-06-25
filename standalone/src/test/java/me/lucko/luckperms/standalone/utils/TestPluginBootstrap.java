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

import me.lucko.luckperms.common.dependencies.Dependency;
import me.lucko.luckperms.common.dependencies.DependencyManager;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.plugin.classpath.ClassPathAppender;
import me.lucko.luckperms.common.storage.StorageType;
import me.lucko.luckperms.standalone.LPStandaloneBootstrap;
import me.lucko.luckperms.standalone.LPStandalonePlugin;
import me.lucko.luckperms.standalone.StandaloneSenderFactory;
import me.lucko.luckperms.standalone.app.LuckPermsApplication;
import me.lucko.luckperms.standalone.app.integration.SingletonPlayer;
import net.kyori.adventure.text.Component;
import net.luckperms.api.util.Tristate;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/**
 * An extension standalone bootstrap for testing.
 *
 * <p>Key differences:</p>
 * <p>
 * <ul>
 *     <li>Dependency loading system is replaced with a no-op stub that delegates to the test classloader</li>
 *     <li>Sender factory is extended and allows for permission checks to be intercepted</li>
 * </ul>
 * </p>
 */
public final class TestPluginBootstrap extends LPStandaloneBootstrap {
    private static final ClassPathAppender NOOP_APPENDER = file -> {};

    private final Path dataDirectory;
    private TestPlugin plugin;

    TestPluginBootstrap(LuckPermsApplication app, Path dataDirectory) {
        super(app, NOOP_APPENDER);
        this.dataDirectory = dataDirectory;
    }

    public TestPlugin getPlugin() {
        return this.plugin;
    }

    @Override
    public Path getDataDirectory() {
        return this.dataDirectory;
    }

    @Override
    protected LPStandalonePlugin createTestPlugin() {
        this.plugin = new TestPlugin(this);
        return this.plugin;
    }

    public static final class TestPlugin extends LPStandalonePlugin {
        private TestSenderFactory senderFactory;

        TestPlugin(LPStandaloneBootstrap bootstrap) {
            super(bootstrap);
        }

        @Override
        protected DependencyManager createDependencyManager() {
            return new TestDependencyManager();
        }

        @Override
        protected void setupSenderFactory() {
            this.senderFactory = new TestSenderFactory(this);
        }

        @Override
        public TestSenderFactory getSenderFactory() {
            return this.senderFactory;
        }
    }

    static final class TestDependencyManager implements DependencyManager {

        @Override
        public void loadDependencies(Set<Dependency> dependencies) {

        }

        @Override
        public void loadStorageDependencies(Set<StorageType> storageTypes, boolean redis, boolean rabbitmq, boolean nats) {

        }

        @Override
        public ClassLoader obtainClassLoaderWith(Set<Dependency> dependencies) {
            return getClass().getClassLoader();
        }

        @Override
        public void close() {

        }
    }

    public static final class TestSenderFactory extends StandaloneSenderFactory {

        private Function<String, Tristate> permissionChecker;

        public TestSenderFactory(LPStandalonePlugin plugin) {
            super(plugin);
        }

        public void setPermissionChecker(Function<String, Tristate> permissionChecker) {
            this.permissionChecker = permissionChecker;
        }

        public void resetPermissionChecker() {
            this.permissionChecker = null;
        }

        @Override
        protected boolean consoleHasAllPermissions() {
            return false;
        }

        @Override
        protected void sendMessage(SingletonPlayer sender, Component message) {
            Component rendered = TranslationManager.render(message, Locale.ENGLISH);
            sender.sendMessage(rendered);
        }

        @Override
        protected Tristate getPermissionValue(SingletonPlayer sender, String node) {
            return this.permissionChecker == null
                    ? super.getPermissionValue(sender, node)
                    : this.permissionChecker.apply(node);
        }

        @Override
        protected boolean hasPermission(SingletonPlayer sender, String node) {
            return this.permissionChecker == null
                    ? super.hasPermission(sender, node)
                    : this.permissionChecker.apply(node).asBoolean();
        }
    }
}
