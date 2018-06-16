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

import com.google.common.io.ByteStreams;

import me.lucko.luckperms.common.dependencies.relocation.Relocation;
import me.lucko.luckperms.common.dependencies.relocation.RelocationHelper;

import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public enum Dependency {

    ASM(
            "org.ow2.asm",
            "asm",
            "6.2",
            "kXvaiIvFQxhzJdX7wQNCB+7RUldO943xc0ygruQLf8g="
    ),
    ASM_COMMONS(
            "org.ow2.asm",
            "asm-commons",
            "6.2",
            "FVRZE9sGyYeqQE8CjjNQHZ8n+M7WEvc3J+NUesTeh4w="
    ),
    JAR_RELOCATOR(
            "me.lucko",
            "jar-relocator",
            "1.3",
            "mmz3ltQbS8xXGA2scM0ZH6raISlt4nukjCiU2l9Jxfs="
    ),

    TEXT(
            "net{}kyori",
            "text",
            "1.11-1.4.0",
            "drQpwf+oI1+DPrn0iCvEtoID+xXR3dpZK5ySaBrUiok=",
            Relocation.of("text", "net{}kyori{}text")
    ),
    CAFFEINE(
            "com{}github{}ben-manes{}caffeine",
            "caffeine",
            "2.6.2",
            "53pEV3NfB1FY29Ahx2YXl91IVpX8Ttkt/d401HFNl1A=",
            Relocation.of("caffeine", "com{}github{}benmanes{}caffeine")
    ),
    OKIO(
            "com{}squareup{}" + RelocationHelper.OKIO_STRING,
            RelocationHelper.OKIO_STRING,
            "1.14.1",
            "InCF6E8zEsc1QxiVJF3nwKe29qUK30KayCVqFQoR7ck=",
            Relocation.of(RelocationHelper.OKIO_STRING, RelocationHelper.OKIO_STRING)
    ),
    OKHTTP(
            "com{}squareup{}" + RelocationHelper.OKHTTP3_STRING,
            "okhttp",
            "3.10.0",
            "Sso+VSr7HOtH+JVmhfYpWiduSfoD+QZvi2voO+xW+2Y=",
            Relocation.allOf(
                    Relocation.of(RelocationHelper.OKHTTP3_STRING, RelocationHelper.OKHTTP3_STRING),
                    Relocation.of(RelocationHelper.OKIO_STRING, RelocationHelper.OKIO_STRING)
            )
    ),
    MARIADB_DRIVER(
            "org{}mariadb{}jdbc",
            "mariadb-java-client",
            "2.2.5",
            "kFfgzoMFrFKirAFh/DgobV7vAu9NhdnhZLHD4/PCddI=",
            Relocation.of("mariadb", "org{}mariadb{}jdbc")
    ),
    MYSQL_DRIVER(
            "mysql",
            "mysql-connector-java",
            "5.1.46",
            "MSIIl2HmQD8C6Kge1KLWWi4QKXNGUboA8uqS2SD/ex4=",
            Relocation.of("mysql", "com{}mysql")
    ),
    POSTGRESQL_DRIVER(
            "org{}postgresql",
            "postgresql",
            "9.4.1212",
            "DLKhWL4xrPIY4KThjI89usaKO8NIBkaHc/xECUsMNl0=",
            Relocation.of("postgresql", "org{}postgresql")
    ),
    H2_DRIVER(
            "com.h2database",
            "h2",
            "1.4.197",
            "N/UhbhSvJ3KTDf+bhzQ1PwqA6Juj8z4GVEHeZTfF6EI="
            // we don't apply relocations to h2 - it gets loaded via
            // an isolated classloader
    ),
    SQLITE_DRIVER(
            "org.xerial",
            "sqlite-jdbc",
            "3.21.0",
            "bglRaH4Y+vQFZV7TfOdsVLO3rJpauJ+IwjuRULAb45Y="
            // we don't apply relocations to sqlite - it gets loaded via
            // an isolated classloader
    ),
    HIKARI(
            "com{}zaxxer",
            "HikariCP",
            "3.2.0",
            "sAjeaLvYWBH0tujwhg0JZsastPLnX6vUbsIJRWnL7+s=",
            Relocation.of("hikari", "com{}zaxxer{}hikari")
    ),
    SLF4J_SIMPLE(
            "org.slf4j",
            "slf4j-simple",
            "1.7.25",
            "CWbob/+lvlLT2ee4ndZ02YoD7tCkVPuvfBvZSTvZ2HQ="
    ),
    SLF4J_API(
            "org.slf4j",
            "slf4j-api",
            "1.7.25",
            "GMSgCV1cHaa4F1kudnuyPSndL1YK1033X/OWHb3iW3k="
    ),
    MONGODB_DRIVER(
            "org.mongodb",
            "mongo-java-driver",
            "3.7.1",
            "yllBCqAZwWCNUoMPR0JWilqhVA46+9F47wIcnYOcoy4=",
            Relocation.allOf(
                    Relocation.of("mongodb", "com{}mongodb"),
                    Relocation.of("bson", "org{}bson")
            )
    ),
    JEDIS(
            "redis.clients",
            "jedis",
            "2.9.0",
            "HqqWy45QVeTVF0Z/DzsrPLvGKn2dHotqI8YX7GDThvo=",
            Relocation.allOf(
                    Relocation.of("jedis", "redis{}clients{}jedis"),
                    Relocation.of("jedisutil", "redis{}clients{}util"),
                    Relocation.of("commonspool2", "org{}apache{}commons{}pool2")
            )
    ),
    COMMONS_POOL_2(
            "org.apache.commons",
            "commons-pool2",
            "2.5.0",
            "IRhwQ8eZcdnISLlxhA3oQdoOXE3zDJI6lpFBMfue+Wk=",
            Relocation.of("commonspool2", "org{}apache{}commons{}pool2")
    ),
    CONFIGURATE_CORE(
            "me{}lucko{}configurate",
            "configurate-core",
            "3.5",
            "J+1WnX1g5gr4ne8qA7DuBadLDOsZnOZjwHbdRmVgF6c=",
            Relocation.of("configurate", "ninja{}leaping{}configurate")
    ),
    CONFIGURATE_GSON(
            "me{}lucko{}configurate",
            "configurate-gson",
            "3.5",
            "Q3wp3xpqy41bJW3yUhbHOzm+NUkT4bUUBI2/AQLaa3c=",
            Relocation.of("configurate", "ninja{}leaping{}configurate")
    ),
    CONFIGURATE_YAML(
            "me{}lucko{}configurate",
            "configurate-yaml",
            "3.5",
            "Dxr1o3EPbpOOmwraqu+cors8O/nKwJnhS5EiPkTb3fc=",
            Relocation.of("configurate", "ninja{}leaping{}configurate")
    ),
    CONFIGURATE_HOCON(
            "me{}lucko{}configurate",
            "configurate-hocon",
            "3.5",
            "sOym1KPmQylGSfk90ZFqobuvoZfEWb7XMmMBwbHuxFw=",
            Relocation.allOf(
                    Relocation.of("configurate", "ninja{}leaping{}configurate"),
                    Relocation.of("hocon", "com{}typesafe{}config")
            )
    ),
    HOCON_CONFIG(
            "com{}typesafe",
            "config",
            "1.3.3",
            "tfHWBx8VSNBb6C9Z+QOcfTeheHvY48Z34x7ida9KRiE=",
            Relocation.of("hocon", "com{}typesafe{}config")
    ),
    CONFIGURATE_TOML(
            "me{}lucko{}configurate",
            "configurate-toml",
            "3.5",
            "U8p0XSTaNT/uebvLpO/vb6AhVGQDYiZsauSGB9zolPU=",
            Relocation.allOf(
                    Relocation.of("configurate", "ninja{}leaping{}configurate"),
                    Relocation.of("toml4j", "com{}moandjiezana{}toml")
            )
    ),
    TOML4J(
            "com{}moandjiezana{}toml",
            "toml4j",
            "0.7.2",
            "9UdeY+fonl22IiNImux6Vr0wNUN3IHehfCy1TBnKOiA=",
            Relocation.of("toml4j", "com{}moandjiezana{}toml")
    );

    private final String url;
    private final String version;
    private final byte[] checksum;
    private final List<Relocation> relocations;

    private static final String MAVEN_CENTRAL_FORMAT = "https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.jar";

    Dependency(String groupId, String artifactId, String version, String checksum) {
        this(groupId, artifactId, version, checksum, Collections.emptyList());
    }

    Dependency(String groupId, String artifactId, String version, String checksum, Relocation relocation) {
        this(groupId, artifactId, version, checksum, Collections.singletonList(relocation));
    }

    Dependency(String groupId, String artifactId, String version, String checksum, List<Relocation> relocations) {
        this(
                String.format(MAVEN_CENTRAL_FORMAT,
                        rewriteEscaping(groupId).replace(".", "/"),
                        rewriteEscaping(artifactId),
                        version,
                        rewriteEscaping(artifactId),
                        version
                ),
                version, checksum, relocations
        );
    }

    Dependency(String url, String version, String checksum) {
        this(url, version, checksum, Collections.emptyList());
    }

    Dependency(String url, String version, String checksum, Relocation relocation) {
        this(url, version, checksum, Collections.singletonList(relocation));
    }

    Dependency(String url, String version, String checksum, List<Relocation> relocations) {
        this.url = url;
        this.version = version;
        this.checksum = Base64.getDecoder().decode(checksum);
        this.relocations = relocations;
    }

    private static String rewriteEscaping(String s) {
        return s.replace("{}", ".");
    }

    public static void main(String[] args) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        for (Dependency dependency : values()) {
            URL url = new URL(dependency.getUrl());
            try (InputStream in = url.openStream()) {
                byte[] bytes = ByteStreams.toByteArray(in);
                if (bytes.length == 0) {
                    throw new RuntimeException("Empty stream");
                }

                byte[] hash = digest.digest(bytes);

                if (Arrays.equals(hash, dependency.getChecksum())) {
                    System.out.println("MATCH    " + dependency.name() + ": " + Base64.getEncoder().encodeToString(hash));
                } else {
                    System.out.println("NO MATCH " + dependency.name() + ": " + Base64.getEncoder().encodeToString(hash));
                }
            }
        }
    }

    public String getUrl() {
        return this.url;
    }

    public String getVersion() {
        return this.version;
    }

    public byte[] getChecksum() {
        return this.checksum;
    }

    public List<Relocation> getRelocations() {
        return this.relocations;
    }
}
