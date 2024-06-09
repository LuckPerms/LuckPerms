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
