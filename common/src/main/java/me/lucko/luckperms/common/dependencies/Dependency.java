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

package me.lucko.luckperms.common.dependencies;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Dependency {

    CAFFEINE("com.github.ben-manes.caffeine", "caffeine", "2.6.0"),
    MARIADB_DRIVER("org.mariadb.jdbc", "mariadb-java-client", "2.2.0"),
    MYSQL_DRIVER("mysql", "mysql-connector-java", "5.1.44"),
    POSTGRESQL_DRIVER("org.postgresql", "postgresql", "9.4.1212"),
    H2_DRIVER("com.h2database", "h2", "1.4.196"),
    SQLITE_DRIVER("org.xerial", "sqlite-jdbc", "3.21.0"),
    HIKARI("com.zaxxer", "HikariCP", "2.7.3"),
    SLF4J_SIMPLE("org.slf4j", "slf4j-simple", "1.7.25"),
    SLF4J_API("org.slf4j", "slf4j-api", "1.7.25"),
    MONGODB_DRIVER("org.mongodb", "mongo-java-driver", "3.5.0"),
    JEDIS("https://github.com/lucko/jedis/releases/download/jedis-2.9.1-shaded/jedis-2.9.1-shaded.jar", "2.9.1-shaded"),
    CONFIGURATE_CORE("ninja.leaping.configurate", "configurate-core", "3.3"),
    CONFIGURATE_GSON("ninja.leaping.configurate", "configurate-gson", "3.3"),
    CONFIGURATE_YAML("ninja.leaping.configurate", "configurate-yaml", "3.3"),
    CONFIGURATE_HOCON("ninja.leaping.configurate", "configurate-hocon", "3.3"),
    HOCON_CONFIG("com.typesafe", "config", "1.3.1");


    private final String url;
    private final String version;

    private static final String MAVEN_CENTRAL_FORMAT = "https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.jar";

    Dependency(String groupId, String artifactId, String version) {
        this(String.format(MAVEN_CENTRAL_FORMAT, groupId.replace(".", "/"), artifactId, version, artifactId, version), version);
    }

}
