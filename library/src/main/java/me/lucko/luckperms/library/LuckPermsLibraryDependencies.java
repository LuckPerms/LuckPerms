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

import me.lucko.luckperms.common.dependencies.Dependency;

import java.util.Set;
import java.util.function.Consumer;

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
