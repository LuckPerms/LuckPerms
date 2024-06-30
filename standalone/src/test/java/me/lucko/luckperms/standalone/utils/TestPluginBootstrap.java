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
import me.lucko.luckperms.common.plugin.classpath.ClassPathAppender;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.storage.StorageType;
import me.lucko.luckperms.standalone.LPStandaloneBootstrap;
import me.lucko.luckperms.standalone.LPStandalonePlugin;
import me.lucko.luckperms.standalone.app.LuckPermsApplication;
import me.lucko.luckperms.standalone.app.integration.StandaloneSender;
import me.lucko.luckperms.standalone.app.integration.StandaloneUser;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;

/**
 * An extension standalone bootstrap for testing.
 *
 * <p>Key differences:</p>
 * <p>
 * <ul>
 *     <li>Dependency loading system is replaced with a no-op stub that delegates to the test classloader</li>
 *     <li>Ability to register additional sender instances as being online</li>
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
        private final Set<StandaloneSender> onlineSenders = new CopyOnWriteArraySet<>();

        TestPlugin(LPStandaloneBootstrap bootstrap) {
            super(bootstrap);
        }

        @Override
        protected DependencyManager createDependencyManager() {
            return new TestDependencyManager();
        }

        @Override
        public Stream<Sender> getOnlineSenders() {
            return Stream.concat(
                    Stream.of(StandaloneUser.INSTANCE),
                    this.onlineSenders.stream()
            ).map(player -> getSenderFactory().wrap(player));
        }

        public void addOnlineSender(StandaloneSender player) {
            this.onlineSenders.add(player);
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
}
