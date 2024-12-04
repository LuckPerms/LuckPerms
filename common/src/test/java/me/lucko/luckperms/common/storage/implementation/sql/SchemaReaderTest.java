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

package me.lucko.luckperms.common.storage.implementation.sql;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchemaReaderTest {

    private static List<String> readStatements(String type) throws IOException {
        List<String> statements;
        try (InputStream is = SchemaReaderTest.class.getResourceAsStream("/me/lucko/luckperms/schema/" + type + ".sql")) {
            if (is == null) {
                throw new IOException("Couldn't locate schema file");
            }

            statements = SchemaReader.getStatements(is);
        }
        return statements;
    }

    @Test
    public void testReadH2() throws IOException {
        assertEquals(ImmutableList.of(
                "CREATE TABLE `{prefix}user_permissions` ( `id` INT AUTO_INCREMENT NOT NULL, `uuid` VARCHAR(36) NOT NULL, `permission` VARCHAR(200) NOT NULL, `value` BOOL NOT NULL, `server` VARCHAR(36) NOT NULL, `world` VARCHAR(64) NOT NULL, `expiry` BIGINT NOT NULL, `contexts` VARCHAR(200) NOT NULL, PRIMARY KEY (`id`))",
                "CREATE INDEX ON `{prefix}user_permissions` (`uuid`)",
                "CREATE TABLE `{prefix}group_permissions` ( `id` INT AUTO_INCREMENT NOT NULL, `name` VARCHAR(36) NOT NULL, `permission` VARCHAR(200) NOT NULL, `value` BOOL NOT NULL, `server` VARCHAR(36) NOT NULL, `world` VARCHAR(64) NOT NULL, `expiry` BIGINT NOT NULL, `contexts` VARCHAR(200) NOT NULL, PRIMARY KEY (`id`))",
                "CREATE INDEX ON `{prefix}group_permissions` (`name`)",
                "CREATE TABLE `{prefix}players` ( `uuid` VARCHAR(36) NOT NULL, `username` VARCHAR(16) NOT NULL, `primary_group` VARCHAR(36) NOT NULL, PRIMARY KEY (`uuid`))",
                "CREATE INDEX ON `{prefix}players` (`username`)",
                "CREATE TABLE `{prefix}groups` ( `name` VARCHAR(36) NOT NULL, PRIMARY KEY (`name`))",
                "CREATE TABLE `{prefix}actions` ( `id` INT AUTO_INCREMENT NOT NULL, `time` BIGINT NOT NULL, `actor_uuid` VARCHAR(36) NOT NULL, `actor_name` VARCHAR(100) NOT NULL, `type` CHAR(1) NOT NULL, `acted_uuid` VARCHAR(36) NOT NULL, `acted_name` VARCHAR(36) NOT NULL, `action` VARCHAR(300) NOT NULL, PRIMARY KEY (`id`))",
                "CREATE TABLE `{prefix}tracks` ( `name` VARCHAR(36) NOT NULL, `groups` TEXT NOT NULL, PRIMARY KEY (`name`))"
        ), readStatements("h2"));
    }

    @Test
    public void testReadSqlite() throws IOException {
        assertEquals(ImmutableList.of(
                "CREATE TABLE `{prefix}user_permissions` ( `id` INTEGER PRIMARY KEY NOT NULL, `uuid` VARCHAR(36) NOT NULL, `permission` VARCHAR(200) NOT NULL, `value` BOOL NOT NULL, `server` VARCHAR(36) NOT NULL, `world` VARCHAR(64) NOT NULL, `expiry` BIGINT NOT NULL, `contexts` VARCHAR(200) NOT NULL)",
                "CREATE INDEX `{prefix}user_permissions_uuid` ON `{prefix}user_permissions` (`uuid`)",
                "CREATE TABLE `{prefix}group_permissions` ( `id` INTEGER PRIMARY KEY NOT NULL, `name` VARCHAR(36) NOT NULL, `permission` VARCHAR(200) NOT NULL, `value` BOOL NOT NULL, `server` VARCHAR(36) NOT NULL, `world` VARCHAR(64) NOT NULL, `expiry` BIGINT NOT NULL, `contexts` VARCHAR(200) NOT NULL)",
                "CREATE INDEX `{prefix}group_permissions_name` ON `{prefix}group_permissions` (`name`)",
                "CREATE TABLE `{prefix}players` ( `uuid` VARCHAR(36) NOT NULL, `username` VARCHAR(16) NOT NULL, `primary_group` VARCHAR(36) NOT NULL, PRIMARY KEY (`uuid`))",
                "CREATE INDEX `{prefix}players_username` ON `{prefix}players` (`username`)",
                "CREATE TABLE `{prefix}groups` ( `name` VARCHAR(36) NOT NULL, PRIMARY KEY (`name`))",
                "CREATE TABLE `{prefix}actions` ( `id` INTEGER PRIMARY KEY NOT NULL, `time` BIGINT NOT NULL, `actor_uuid` VARCHAR(36) NOT NULL, `actor_name` VARCHAR(100) NOT NULL, `type` CHAR(1) NOT NULL, `acted_uuid` VARCHAR(36) NOT NULL, `acted_name` VARCHAR(36) NOT NULL, `action` VARCHAR(300) NOT NULL)",
                "CREATE TABLE `{prefix}tracks` ( `name` VARCHAR(36) NOT NULL, `groups` TEXT NOT NULL, PRIMARY KEY (`name`))"
        ), readStatements("sqlite"));
    }

    @Test
    public void testReadMysql() throws IOException {
        ImmutableList<String> expected = ImmutableList.of(
                "CREATE TABLE `{prefix}user_permissions` ( `id` INT AUTO_INCREMENT NOT NULL, `uuid` VARCHAR(36) NOT NULL, `permission` VARCHAR(200) NOT NULL, `value` BOOL NOT NULL, `server` VARCHAR(36) NOT NULL, `world` VARCHAR(64) NOT NULL, `expiry` BIGINT NOT NULL, `contexts` VARCHAR(200) NOT NULL, PRIMARY KEY (`id`)) DEFAULT CHARSET = utf8mb4",
                "CREATE INDEX `{prefix}user_permissions_uuid` ON `{prefix}user_permissions` (`uuid`)",
                "CREATE TABLE `{prefix}group_permissions` ( `id` INT AUTO_INCREMENT NOT NULL, `name` VARCHAR(36) NOT NULL, `permission` VARCHAR(200) NOT NULL, `value` BOOL NOT NULL, `server` VARCHAR(36) NOT NULL, `world` VARCHAR(64) NOT NULL, `expiry` BIGINT NOT NULL, `contexts` VARCHAR(200) NOT NULL, PRIMARY KEY (`id`)) DEFAULT CHARSET = utf8mb4",
                "CREATE INDEX `{prefix}group_permissions_name` ON `{prefix}group_permissions` (`name`)",
                "CREATE TABLE `{prefix}players` ( `uuid` VARCHAR(36) NOT NULL, `username` VARCHAR(16) NOT NULL, `primary_group` VARCHAR(36) NOT NULL, PRIMARY KEY (`uuid`)) DEFAULT CHARSET = utf8mb4",
                "CREATE INDEX `{prefix}players_username` ON `{prefix}players` (`username`)",
                "CREATE TABLE `{prefix}groups` ( `name` VARCHAR(36) NOT NULL, PRIMARY KEY (`name`)) DEFAULT CHARSET = utf8mb4",
                "CREATE TABLE `{prefix}actions` ( `id` INT AUTO_INCREMENT NOT NULL, `time` BIGINT NOT NULL, `actor_uuid` VARCHAR(36) NOT NULL, `actor_name` VARCHAR(100) NOT NULL, `type` CHAR(1) NOT NULL, `acted_uuid` VARCHAR(36) NOT NULL, `acted_name` VARCHAR(36) NOT NULL, `action` VARCHAR(300) NOT NULL, PRIMARY KEY (`id`)) DEFAULT CHARSET = utf8mb4",
                "CREATE TABLE `{prefix}tracks` ( `name` VARCHAR(36) NOT NULL, `groups` TEXT NOT NULL, PRIMARY KEY (`name`)) DEFAULT CHARSET = utf8mb4"
        );
        assertEquals(expected, readStatements("mysql"));
        assertEquals(expected, readStatements("mariadb"));
    }

    @Test
    public void testReadPostgres() throws IOException {
        assertEquals(ImmutableList.of(
                "CREATE TABLE \"{prefix}user_permissions\" ( \"id\" SERIAL PRIMARY KEY NOT NULL, \"uuid\" VARCHAR(36) NOT NULL, \"permission\" VARCHAR(200) NOT NULL, \"value\" BOOL NOT NULL, \"server\" VARCHAR(36) NOT NULL, \"world\" VARCHAR(64) NOT NULL, \"expiry\" BIGINT NOT NULL, \"contexts\" VARCHAR(200) NOT NULL)",
                "CREATE INDEX \"{prefix}user_permissions_uuid\" ON \"{prefix}user_permissions\" (\"uuid\")",
                "CREATE TABLE \"{prefix}group_permissions\" ( \"id\" SERIAL PRIMARY KEY NOT NULL, \"name\" VARCHAR(36) NOT NULL, \"permission\" VARCHAR(200) NOT NULL, \"value\" BOOL NOT NULL, \"server\" VARCHAR(36) NOT NULL, \"world\" VARCHAR(64) NOT NULL, \"expiry\" BIGINT NOT NULL, \"contexts\" VARCHAR(200) NOT NULL)",
                "CREATE INDEX \"{prefix}group_permissions_name\" ON \"{prefix}group_permissions\" (\"name\")",
                "CREATE TABLE \"{prefix}players\" ( \"uuid\" VARCHAR(36) PRIMARY KEY NOT NULL, \"username\" VARCHAR(16) NOT NULL, \"primary_group\" VARCHAR(36) NOT NULL)",
                "CREATE INDEX \"{prefix}players_username\" ON \"{prefix}players\" (\"username\")",
                "CREATE TABLE \"{prefix}groups\" ( \"name\" VARCHAR(36) PRIMARY KEY NOT NULL)",
                "CREATE TABLE \"{prefix}actions\" ( \"id\" SERIAL PRIMARY KEY NOT NULL, \"time\" BIGINT NOT NULL, \"actor_uuid\" VARCHAR(36) NOT NULL, \"actor_name\" VARCHAR(100) NOT NULL, \"type\" CHAR(1) NOT NULL, \"acted_uuid\" VARCHAR(36) NOT NULL, \"acted_name\" VARCHAR(36) NOT NULL, \"action\" VARCHAR(300) NOT NULL)",
                "CREATE TABLE \"{prefix}tracks\" ( \"name\" VARCHAR(36) PRIMARY KEY NOT NULL, \"groups\" TEXT NOT NULL)"
        ), readStatements("postgresql"));
    }

    @Test
    public void testTableFromStatement() throws IOException {
        Set<String> allowedTables = ImmutableSet.of(
                "luckperms_user_permissions",
                "luckperms_group_permissions",
                "luckperms_players",
                "luckperms_groups",
                "luckperms_actions",
                "luckperms_tracks"
        );

        for (String type : new String[]{"h2", "mariadb", "mysql", "postgresql", "sqlite"}) {
            List<String> tables = readStatements(type).stream()
                    .map(s -> s.replace("{prefix}", "luckperms_"))
                    .map(SchemaReader::tableFromStatement)
                    .collect(Collectors.toList());

            assertTrue(allowedTables.containsAll(tables));
        }
    }

    @Test
    public void testFilter() throws IOException {
        StatementProcessor processor = s -> s.replace("{prefix}", "luckperms_");
        List<String> statements = readStatements("mysql").stream().map(processor::process).collect(Collectors.toList());

        // no tables exist, all should be created
        List<String> filtered = SchemaReader.filterStatements(statements, ImmutableList.of());
        assertEquals(statements, filtered);

        // all tables exist, none should be created
        filtered = SchemaReader.filterStatements(statements, ImmutableList.of(
                "luckperms_user_permissions",
                "luckperms_group_permissions",
                "luckperms_players",
                "luckperms_groups",
                "luckperms_actions",
                "luckperms_tracks"
        ));
        assertEquals(ImmutableList.of(), filtered);

        // some tables exist, some should be created
        filtered = SchemaReader.filterStatements(statements, ImmutableList.of(
                "luckperms_user_permissions",
                "luckperms_players",
                "luckperms_groups",
                "luckperms_actions",
                "luckperms_tracks"
        ));
        assertEquals(ImmutableList.of(
                "CREATE TABLE `luckperms_group_permissions` ( `id` INT AUTO_INCREMENT NOT NULL, `name` VARCHAR(36) NOT NULL, `permission` VARCHAR(200) NOT NULL, `value` BOOL NOT NULL, `server` VARCHAR(36) NOT NULL, `world` VARCHAR(64) NOT NULL, `expiry` BIGINT NOT NULL, `contexts` VARCHAR(200) NOT NULL, PRIMARY KEY (`id`)) DEFAULT CHARSET = utf8mb4",
                "CREATE INDEX `luckperms_group_permissions_name` ON `luckperms_group_permissions` (`name`)"
        ), filtered);
    }

}
