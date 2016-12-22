/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

    MYSQL_DRIVER("https://repo1.maven.org/maven2/mysql/mysql-connector-java/5.1.6/mysql-connector-java-5.1.6.jar", "5.1.6", "com.mysql.jdbc.jdbc2.optional.MysqlDataSource"),
    H2_DRIVER("https://repo1.maven.org/maven2/com/h2database/h2/1.4.193/h2-1.4.193.jar", "1.4.193", "org.h2.Driver"),
    SQLITE_DRIVER("https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.15.1/sqlite-jdbc-3.15.1.jar", "3.15.1", "org.sqlite.JDBC"),
    HIKARI("https://repo1.maven.org/maven2/com/zaxxer/HikariCP/2.5.1/HikariCP-2.5.1.jar", "2.5.1", "com.zaxxer.hikari.HikariDataSource"),
    SLF4J_SIMPLE("https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/1.7.9/slf4j-simple-1.7.9.jar", "1.7.9", "org.slf4j.impl.SimpleLoggerFactory"),
    SLF4J_API("https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.9/slf4j-api-1.7.9.jar", "1.7.9", "org.slf4j.helpers.BasicMarkerFactory"),
    MONGODB_DRIVER("https://repo1.maven.org/maven2/org/mongodb/mongo-java-driver/3.4.1/mongo-java-driver-3.4.1.jar", "3.4.1", "com.mongodb.Mongo"),
    JEDIS("https://repo1.maven.org/maven2/redis/clients/jedis/2.9.0/jedis-2.9.0.jar", "2.9.0", "redis.clients.jedis.Jedis"),
    APACHE_COMMONS_POOL("https://repo1.maven.org/maven2/org/apache/commons/commons-pool2/2.4.2/commons-pool2-2.4.2.jar", "2.4.1", "org.apache.commons.pool2.PoolUtils");

    private final String url;
    private final String version;
    private final String testClass;

}
