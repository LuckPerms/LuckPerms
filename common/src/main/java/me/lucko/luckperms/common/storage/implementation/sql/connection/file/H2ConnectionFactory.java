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

package me.lucko.luckperms.common.storage.implementation.sql.connection.file;

import me.lucko.luckperms.common.dependencies.Dependency;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.implementation.sql.StatementProcessor;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Properties;

public class H2ConnectionFactory extends FlatfileConnectionFactory {
    public static final StatementProcessor STATEMENT_PROCESSOR = s -> s
            .replace('\'', '`')
            .replace("LIKE", "ILIKE")
            .replace("value", "`value`")
            .replace("``value``", "`value`");

    private Constructor<?> connectionConstructor;

    public H2ConnectionFactory(Path file) {
        super(file);
    }

    @Override
    public String getImplementationName() {
        return "H2";
    }

    @Override
    public void init(LuckPermsPlugin plugin) {
        migrateOldDatabaseFile("luckperms.db.mv.db");

        ClassLoader classLoader = plugin.getDependencyManager().obtainClassLoaderWith(EnumSet.of(Dependency.H2_DRIVER));
        try {
            Class<?> connectionClass = classLoader.loadClass("org.h2.jdbc.JdbcConnection");
            this.connectionConstructor = connectionClass.getConstructor(String.class, Properties.class, String.class, Object.class, boolean.class);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        try {
            new MigrateH2ToVersion2(plugin, super.getWriteFile().getParent()).run(this);
        } catch (Exception e) {
            plugin.getLogger().warn("Something went wrong whilst upgrading the LuckPerms database. Please report this on GitHub.", e);
        }
    }

    @Override
    protected Connection createConnection(Path file) throws SQLException {
        try {
            return (Connection) this.connectionConstructor.newInstance("jdbc:h2:" + file.toString(), new Properties(), null, null, false);
        } catch (ReflectiveOperationException e) {
            if (e.getCause() instanceof SQLException) {
                throw (SQLException) e.getCause();
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Path getWriteFile() {
        // h2 appends '.mv.db' to the end of the database name
        Path writeFile = super.getWriteFile();
        return writeFile.getParent().resolve(writeFile.getFileName().toString() + ".mv.db");
    }

    @Override
    public StatementProcessor getStatementProcessor() {
        return STATEMENT_PROCESSOR;
    }

    /**
     * Migrates the old (version 1) H2 database to version 2.
     *
     * See <a href="http://www.h2database.com/html/migration-to-v2.html">here</a> for more info.
     */
    private static final class MigrateH2ToVersion2 {
        private final LuckPermsPlugin plugin;
        private final Path directory;

        MigrateH2ToVersion2(LuckPermsPlugin plugin, Path directory) {
            this.plugin = plugin;
            this.directory = directory;
        }

        public void run(H2ConnectionFactory newFactory) throws Exception {
            Path oldDatabase = this.directory.resolve("luckperms-h2");
            Path oldDatabaseWriteFile = this.directory.resolve("luckperms-h2.mv.db");

            if (!Files.exists(oldDatabaseWriteFile)) {
                return;
            }

            Path tempMigrationFile = this.directory.resolve("luckperms-h2-migration.sql");

            this.plugin.getLogger().warn("[DB Upgrade] Found an old (v1) H2 database file. LuckPerms will now attempt to upgrade it to v2 (this is a one time operation).");

            try {
                Files.deleteIfExists(tempMigrationFile);
            } catch (IOException e) {
                this.plugin.getLogger().warn("[DB Upgrade] Unable to delete temporary data from a previous migration attempt", e);
            }

            this.plugin.getLogger().info("[DB Upgrade] Stage 1: Exporting the old database to an intermediary file...");
            Constructor<?> constructor = getConnectionConstructor();
            try (Connection c = getConnection(constructor, oldDatabase)) {
                try (Statement stmt = c.createStatement()) {
                    stmt.execute(String.format("SCRIPT TO '%s'", tempMigrationFile));
                }
            }

            this.plugin.getLogger().info("[DB Upgrade] Stage 2: Importing the intermediary file into the new database...");
            try (Connection c = newFactory.getConnection()) {
                try (Statement stmt = c.createStatement()) {
                    stmt.execute(String.format("RUNSCRIPT FROM '%s'", tempMigrationFile));
                }
            }

            this.plugin.getLogger().info("[DB Upgrade] Stage 3: Tidying up...");
            Files.deleteIfExists(tempMigrationFile);
            Files.move(oldDatabaseWriteFile, this.directory.resolve("luckperms-h2-v1-backup.mv.db"));

            this.plugin.getLogger().info("[DB Upgrade] All done!");
        }

        private Constructor<?> getConnectionConstructor() {
            this.plugin.getDependencyManager().loadDependencies(Collections.singleton(Dependency.H2_DRIVER_LEGACY));
            ClassLoader classLoader = this.plugin.getDependencyManager().obtainClassLoaderWith(EnumSet.of(Dependency.H2_DRIVER_LEGACY));
            try {
                Class<?> connectionClass = classLoader.loadClass("org.h2.jdbc.JdbcConnection");
                return connectionClass.getConstructor(String.class, Properties.class);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        private Connection getConnection(Constructor<?> constructor, Path file) {
            try {
                return (Connection) constructor.newInstance("jdbc:h2:" + file.toString(), new Properties());
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }


}
