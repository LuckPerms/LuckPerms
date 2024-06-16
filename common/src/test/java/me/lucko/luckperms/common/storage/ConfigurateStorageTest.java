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

package me.lucko.luckperms.common.storage;

import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.implementation.StorageImplementation;
import me.lucko.luckperms.common.storage.implementation.file.CombinedConfigurateStorage;
import me.lucko.luckperms.common.storage.implementation.file.SeparatedConfigurateStorage;
import me.lucko.luckperms.common.storage.implementation.file.loader.HoconLoader;
import me.lucko.luckperms.common.storage.implementation.file.loader.JsonLoader;
import me.lucko.luckperms.common.storage.implementation.file.loader.TomlLoader;
import me.lucko.luckperms.common.storage.implementation.file.loader.YamlLoader;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.mockito.Mockito.lenient;

public class ConfigurateStorageTest {

    @Nested
    class SeparatedYaml extends AbstractStorageTest {
        @TempDir
        private Path directory;

        @Override
        protected StorageImplementation makeStorage(LuckPermsPlugin plugin) throws Exception {
            lenient().when(this.bootstrap.getDataDirectory()).thenReturn(this.directory);
            return new SeparatedConfigurateStorage(plugin, "YAML", new YamlLoader(), ".yml", "yaml-storage");
        }
    }

    @Nested
    class SeparatedJson extends AbstractStorageTest {
        @TempDir
        private Path directory;

        @Override
        protected StorageImplementation makeStorage(LuckPermsPlugin plugin) throws Exception {
            lenient().when(this.bootstrap.getDataDirectory()).thenReturn(this.directory);
            return new SeparatedConfigurateStorage(plugin, "JSON", new JsonLoader(), ".json", "json-storage");
        }
    }

    @Nested
    class SeparatedHocon extends AbstractStorageTest {
        @TempDir
        private Path directory;

        @Override
        protected StorageImplementation makeStorage(LuckPermsPlugin plugin) throws Exception {
            lenient().when(this.bootstrap.getDataDirectory()).thenReturn(this.directory);
            return new SeparatedConfigurateStorage(plugin, "HOCON", new HoconLoader(), ".conf", "hocon-storage");
        }
    }

    @Nested
    class SeparatedToml extends AbstractStorageTest {
        @TempDir
        private Path directory;

        @Override
        protected StorageImplementation makeStorage(LuckPermsPlugin plugin) throws Exception {
            lenient().when(this.bootstrap.getDataDirectory()).thenReturn(this.directory);
            return new SeparatedConfigurateStorage(plugin, "TOML", new TomlLoader(), ".toml", "toml-storage");
        }
    }

    @Nested
    class CombinedYaml extends AbstractStorageTest {
        @TempDir
        private Path directory;

        @Override
        protected StorageImplementation makeStorage(LuckPermsPlugin plugin) throws Exception {
            lenient().when(this.bootstrap.getDataDirectory()).thenReturn(this.directory);
            return new CombinedConfigurateStorage(plugin, "YAML", new YamlLoader(), ".yml", "yaml-storage");
        }
    }

    @Nested
    class CombinedJson extends AbstractStorageTest {
        @TempDir
        private Path directory;

        @Override
        protected StorageImplementation makeStorage(LuckPermsPlugin plugin) throws Exception {
            lenient().when(this.bootstrap.getDataDirectory()).thenReturn(this.directory);
            return new CombinedConfigurateStorage(plugin, "JSON", new JsonLoader(), ".json", "json-storage");
        }
    }

    @Nested
    class CombinedHocon extends AbstractStorageTest {
        @TempDir
        private Path directory;

        @Override
        protected StorageImplementation makeStorage(LuckPermsPlugin plugin) throws Exception {
            lenient().when(this.bootstrap.getDataDirectory()).thenReturn(this.directory);
            return new CombinedConfigurateStorage(plugin, "HOCON", new HoconLoader(), ".conf", "hocon-storage");
        }
    }

    @Nested
    class CombinedToml extends AbstractStorageTest {
        @TempDir
        private Path directory;

        @Override
        protected StorageImplementation makeStorage(LuckPermsPlugin plugin) throws Exception {
            lenient().when(this.bootstrap.getDataDirectory()).thenReturn(this.directory);
            return new CombinedConfigurateStorage(plugin, "TOML", new TomlLoader(), ".toml", "toml-storage");
        }
    }

}
