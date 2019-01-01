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

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;

import me.lucko.luckperms.common.dependencies.relocation.Relocation;
import me.lucko.luckperms.common.dependencies.relocation.RelocationHelper;

import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public enum Dependency {

    ASM(
            "org.ow2.asm",
            "asm",
            "6.2.1",
            "FGDbbDPMmchOXLMORrAX5NHMmn+8F0EB1vhIKbtkwIU="
    ),
    ASM_COMMONS(
            "org.ow2.asm",
            "asm-commons",
            "6.2.1",
            "P1eNMe8w+UttH0SBL0H+T5inzUKvNTNfXUhmqzuQGGU="
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
            "1.11-1.6.4",
            "V821j+n8AWhAvLhHjfXQ+/4284Gn4oXTYYfLkLjvs8o=",
            Relocation.of("text", "net{}kyori{}text")
    ),
    TEXT_ADAPTER_BUKKIT(
            "net{}kyori",
            "text-adapter-bukkit",
            "1.0.1",
            "WZp7wCp2+EJC+Q/hxGLh/FJJCrn5Zdi2A1z2hX67jAM=",
            Relocation.of("text", "net{}kyori{}text")
    ),
    TEXT_ADAPTER_BUNGEECORD(
            "net{}kyori",
            "text-adapter-bungeecord",
            "1.0.2",
            "hj7z8v8AceARfwTPzLzJwZdTqqERN9fPJPhZQAqD1Rc=",
            Relocation.of("text", "net{}kyori{}text")
    ),
    EVENT(
            "net{}kyori",
            "event-api",
            "3.0.0",
            "yjvdTdAyktl3iFEQFLHC3qYwwt7/DbCd7Zc8Q4SlIag=",
            Relocation.of("eventbus", "net{}kyori{}event")
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
            "1.16.0",
            "7ASE/xkDZA44RcKxCruZ7/LTIwj/40WeX5IwmkUbnH4=",
            Relocation.of(RelocationHelper.OKIO_STRING, RelocationHelper.OKIO_STRING)
    ),
    OKHTTP(
            "com{}squareup{}" + RelocationHelper.OKHTTP3_STRING,
            "okhttp",
            "3.12.0",
            "cXh/LFmeBEHHpEE5g7/dk9QLVuG63F4EE9akxIW6PzU=",
            Relocation.allOf(
                    Relocation.of(RelocationHelper.OKHTTP3_STRING, RelocationHelper.OKHTTP3_STRING),
                    Relocation.of(RelocationHelper.OKIO_STRING, RelocationHelper.OKIO_STRING)
            )
    ),
    COMMODORE(
            "me{}lucko",
            "commodore",
            "1.2",
            "KPG1t8vosUNEJy5g7Vq6FGDf8FRslHvhzuZa7A/eIjg=",
            Relocation.of("commodore", "me{}lucko{}commodore")
    ),
    MARIADB_DRIVER(
            "org{}mariadb{}jdbc",
            "mariadb-java-client",
            "2.2.6",
            "TSj72P1Oojmw75SC9Wznfi7xl6YNUjqO48hOuYT8dv4=",
            Relocation.of("mariadb", "org{}mariadb{}jdbc")
    ),
    MYSQL_DRIVER(
            "mysql",
            "mysql-connector-java",
            "5.1.47",
            "5PhASPOSsrN7r0ao1QjkuN2uKG0gnvmVueEYhSAcGSM=",
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
            "3.23.1",
            "1XCvY2or6Z4gvpUQ+2FaqBmyBZhXst1iWuy/93R2YzE="
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
            "3.8.1",
            "gIIEEA9QdRUVcI+CVh+w/py0NlLOD+TBNQ/cjNav1p4=",
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
            "2.6.0",
            "kpPYiPvZCVFrhwCkvZHpglS4sf4Vr7bD1jVsnWckut4=",
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
    SNAKEYAML(
            "org.yaml",
            "snakeyaml",
            "1.22",
            "2rQd+SF15BNbBI4lyB5s//a+4Fv0wo8gAEhYfDKcNJ8=",
            Relocation.of("yaml", "org{}yaml{}snakeyaml")
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
        this(groupId, artifactId, version, checksum, ImmutableList.of());
    }

    Dependency(String groupId, String artifactId, String version, String checksum, Relocation relocation) {
        this(groupId, artifactId, version, checksum, ImmutableList.of(relocation));
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

    Dependency(String url, String version, String checksum, List<Relocation> relocations) {
        this.url = url;
        this.version = version;
        this.checksum = Base64.getDecoder().decode(checksum);
        this.relocations = ImmutableList.copyOf(relocations);
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
