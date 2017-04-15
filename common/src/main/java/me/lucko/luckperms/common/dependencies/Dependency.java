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

    CAFFEINE("https://repo1.maven.org/maven2/com/github/ben-manes/caffeine/caffeine/2.4.0/caffeine-2.4.0.jar", "2.4.0"),
    MARIADB_DRIVER("https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/1.5.9/mariadb-java-client-1.5.9.jar", "1.5.9"),
    MYSQL_DRIVER("https://repo1.maven.org/maven2/mysql/mysql-connector-java/5.1.6/mysql-connector-java-5.1.6.jar", "5.1.6"),
    POSTGRESQL_DRIVER("https://repo1.maven.org/maven2/org/postgresql/postgresql/9.4.1212/postgresql-9.4.1212.jar", "9.4.1212"),
    H2_DRIVER("https://repo1.maven.org/maven2/com/h2database/h2/1.4.193/h2-1.4.193.jar", "1.4.193"),
    SQLITE_DRIVER("https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.15.1/sqlite-jdbc-3.15.1.jar", "3.15.1"),
    HIKARI("https://repo1.maven.org/maven2/com/zaxxer/HikariCP/2.6.1/HikariCP-2.6.1.jar", "2.6.1"),
    SLF4J_SIMPLE("https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/1.7.9/slf4j-simple-1.7.9.jar", "1.7.9"),
    SLF4J_API("https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.9/slf4j-api-1.7.9.jar", "1.7.9"),
    MONGODB_DRIVER("https://repo1.maven.org/maven2/org/mongodb/mongo-java-driver/3.4.1/mongo-java-driver-3.4.1.jar", "3.4.1"),
    JEDIS("https://github.com/lucko/jedis/releases/download/jedis-2.9.1-shaded/jedis-2.9.1-shaded.jar", "2.9.1-shaded");

    private final String url;
    private final String version;

}
