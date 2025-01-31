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

import com.google.common.collect.ImmutableList;

import java.util.List;

public enum StorageType {

    // Config file based
    YAML("YAML", "yaml", "yml"),
    JSON("JSON", "json", "flatfile"),
    HOCON("HOCON", "hocon"),
    TOML("TOML", "toml"),
    YAML_COMBINED("YAML Combined", "yaml-combined"),
    JSON_COMBINED("JSON Combined", "json-combined"),
    HOCON_COMBINED("HOCON Combined", "hocon-combined"),
    TOML_COMBINED("TOML Combined", "toml-combined"),

    // Remote databases
    MONGODB("MongoDB", "mongodb"),
    MARIADB("MariaDB", "mariadb"),
    MYSQL("MySQL", "mysql"),
    POSTGRESQL("PostgreSQL", "postgresql"),

    // Local databases
    SQLITE("SQLite", "sqlite"),
    H2("H2", "h2"),

    // REST
    REST("REST", "rest"),

    // Custom
    CUSTOM("Custom", "custom");

    private final String name;

    private final List<String> identifiers;

    StorageType(String name, String... identifiers) {
        this.name = name;
        this.identifiers = ImmutableList.copyOf(identifiers);
    }

    public static StorageType parse(String name, StorageType def) {
        for (StorageType t : values()) {
            for (String id : t.getIdentifiers()) {
                if (id.equalsIgnoreCase(name)) {
                    return t;
                }
            }
        }
        return def;
    }

    public String getName() {
        return this.name;
    }

    public List<String> getIdentifiers() {
        return this.identifiers;
    }
}
