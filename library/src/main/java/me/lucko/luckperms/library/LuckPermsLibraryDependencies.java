package me.lucko.luckperms.library;

import java.util.Set;
import java.util.function.Consumer;

import me.lucko.luckperms.common.dependencies.Dependency;

/**
 * loadDefault: Use if the default dependencies are not packaged (they are included in the library module)<br>
 * loadStorage: Use if the storage dependencies are not packaged (they are not included in the library module)<br>
 * modifier: Use to add additional dependencies or remove default dependencies without removing all with loadDefault=false
 */
public class LuckPermsLibraryDependencies {

    public static LuckPermsLibraryDependencies loadNone() {
        return new LuckPermsLibraryDependencies(false, false, null);
    }

    public static LuckPermsLibraryDependencies loadOnlyStorage() {
        return new LuckPermsLibraryDependencies(false, true, null);
    }

    public static LuckPermsLibraryDependencies loadAll() {
        return new LuckPermsLibraryDependencies(true, true, null);
    }

    private boolean loadDefault;
    private boolean loadStorage;
    private Consumer<Set<Dependency>> modifier;

    public LuckPermsLibraryDependencies(boolean loadDefault, boolean loadStorage, Consumer<Set<Dependency>> modifier) {
        this.loadDefault = loadDefault;
        this.loadStorage = loadStorage;
        this.modifier = modifier;
    }

    public LuckPermsLibraryDependencies setLoadDefault(boolean loadDefault) {
        this.loadDefault = loadDefault;
        return this;
    }
    public boolean isLoadDefault() {
        return loadDefault;
    }

    public LuckPermsLibraryDependencies setLoadStorage(boolean loadStorage) {
        this.loadStorage = loadStorage;
        return this;
    }
    public boolean isLoadStorage() {
        return loadStorage;
    }

    public LuckPermsLibraryDependencies setModifier(Consumer<Set<Dependency>> modifier) {
        this.modifier = modifier;
        return this;
    }
    public Consumer<Set<Dependency>> getModifier() {
        return modifier;
    }

}
