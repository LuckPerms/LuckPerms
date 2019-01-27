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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.ArrayList;
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
            "1.12-1.6.5",
            "gcY9BvK2YbqrLR9EZE81UCyRT+c+ZWT5xDqBo6bhVXU=",
            Relocation.of("text", "net{}kyori{}text")
    ),
    TEXT_ADAPTER_BUKKIT(
            "net{}kyori",
            "text-adapter-bukkit",
            "1.0.2",
            "jZx0BgSlfyeamadYiE52wlJbQOwSyB4fffPoE526hOk=",
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
            "1.17.2",
            "+AzkLS/6xHrUxH4db5gNYE0kfOsaiGcFz0WBqwyf4rg=",
            Relocation.of(RelocationHelper.OKIO_STRING, RelocationHelper.OKIO_STRING)
    ),
    OKHTTP(
            "com{}squareup{}" + RelocationHelper.OKHTTP3_STRING,
            "okhttp",
            "3.12.1",
            "B8PYLKfq9HIvALLagH3Hhg9haa5gz+3PXUAhj5CICkY=",
            Relocation.allOf(
                    Relocation.of(RelocationHelper.OKHTTP3_STRING, RelocationHelper.OKHTTP3_STRING),
                    Relocation.of(RelocationHelper.OKIO_STRING, RelocationHelper.OKIO_STRING)
            )
    ),
    COMMODORE(
            "me{}lucko",
            "commodore",
            "1.3",
            "3+fpQ9eQpO73jm4z7G+6x+q87b8NwjY1KN+nmNGjplk=",
            Relocation.of("commodore", "me{}lucko{}commodore")
    ),
    MARIADB_DRIVER(
            "org{}mariadb{}jdbc",
            "mariadb-java-client",
            "2.3.0",
            "JvxnswIqboUyO3brtMJplOwaaxHsc0RIMkTE5Q/3nOA=",
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
            "3.25.2",
            "pF2mGr7WFWilM/3s4SUJMYCCjt6w1Lb21XLgz0V0ZfY="
            // we don't apply relocations to sqlite - it gets loaded via
            // an isolated classloader
    ),
    HIKARI(
            "com{}zaxxer",
            "HikariCP",
            "3.3.0",
            "hpK1CDgTul4n8ICT0y12qDIFwT6uzzu9xr8+vrbGrco=",
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
            "3.9.1",
            "mxKxkvmYluxV+Hdn57uyt+MjjSQUsFjxFw9tjhx0bm4=",
            Relocation.allOf(
                    Relocation.of("mongodb", "com{}mongodb"),
                    Relocation.of("bson", "org{}bson")
            )
    ),
    JEDIS(
            "redis.clients",
            "jedis",
            "2.10.1",
            "Pp7msE2yJk5QRF9ASqM/6glX95xsnrwkm9BfN37vLI4=",
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
            "1.23",
            "EwCfte3jzyvlqNDxYCFVrqoM5e9fk2aJK9JY2NPU0rE=",
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

    private final List<URL> urls;
    private final String version;
    private final byte[] checksum;
    private final List<Relocation> relocations;

    private static final String MAVEN_CENTRAL_REPO = "https://repo1.maven.org/maven2/";
    private static final String LUCK_MIRROR_REPO = "https://nexus.lucko.me/repository/maven-central/";
    private static final String MAVEN_FORMAT = "%s/%s/%s/%s-%s.jar";

    Dependency(String groupId, String artifactId, String version, String checksum) {
        this(groupId, artifactId, version, checksum, ImmutableList.of());
    }

    Dependency(String groupId, String artifactId, String version, String checksum, Relocation relocation) {
        this(groupId, artifactId, version, checksum, ImmutableList.of(relocation));
    }

    Dependency(String groupId, String artifactId, String version, String checksum, List<Relocation> relocations) {
        String path = String.format(MAVEN_FORMAT,
                rewriteEscaping(groupId).replace(".", "/"),
                rewriteEscaping(artifactId),
                version,
                rewriteEscaping(artifactId),
                version
        );
        try {
            this.urls = ImmutableList.of(
                    new URL(LUCK_MIRROR_REPO + path),
                    new URL(MAVEN_CENTRAL_REPO + path)
            );
        } catch (MalformedURLException e) {
            throw new RuntimeException(e); // propagate
        }
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
            List<byte[]> hashes = new ArrayList<>();
            for (URL url : dependency.getUrls()) {
                URLConnection connection = url.openConnection();
                connection.setRequestProperty("User-Agent", "luckperms");

                try (InputStream in = connection.getInputStream()) {
                    byte[] bytes = ByteStreams.toByteArray(in);
                    if (bytes.length == 0) {
                        throw new RuntimeException("Empty stream");
                    }

                    hashes.add(digest.digest(bytes));
                }
            }

            for (int i = 0; i < hashes.size(); i++) {
                byte[] hash = hashes.get(i);
                if (!Arrays.equals(hash, dependency.getChecksum())) {
                    System.out.println("NO MATCH - REPO " + i + " - " + dependency.name() + ": " + Base64.getEncoder().encodeToString(hash));
                }
            }
        }
    }

    public List<URL> getUrls() {
        return this.urls;
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
