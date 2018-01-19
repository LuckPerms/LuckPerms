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

import me.lucko.jarrelocator.Relocation;
import me.lucko.luckperms.common.dependencies.relocation.Relocations;

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
            "5.2",
            "Pl6g19osUVXvT0cNkJLULeNOP1PbZYnHwH1nIa30uj4="
    ),
    ASM_COMMONS(
            "org.ow2.asm",
            "asm-commons",
            "5.2",
            "zBMYiX4sdxy3l6aNX06mQcI6UfBDfKUXq+z5ZN2yZAs="
    ),

    CAFFEINE(
            "com{}github{}ben-manes{}caffeine",
            "caffeine",
            "2.6.0",
            "JmT16VQnCnVBAjRJCQkkkjmSVx2jajpzeBuKwpbzDA8=",
            Relocations.of("caffeine", "com{}github{}benmanes{}caffeine")
    ),
    MARIADB_DRIVER(
            "org{}mariadb{}jdbc",
            "mariadb-java-client",
            "2.2.0",
            "/q0LPGHrp3L9rvKr7TuA6urbtXBqvXis92mP4KhxzUw=",
            Relocations.of("mariadb", "org{}mariadb{}jdbc")
    ),
    MYSQL_DRIVER(
            "mysql",
            "mysql-connector-java",
            "5.1.44",
            "d4RZVzTeWpoHBPB/tQP3mSafNy7L9MDUSOt4Ku9LGCc=",
            Relocations.of("mysql", "com{}mysql")
    ),
    POSTGRESQL_DRIVER(
            "org{}postgresql",
            "postgresql",
            "9.4.1212",
            "DLKhWL4xrPIY4KThjI89usaKO8NIBkaHc/xECUsMNl0=",
            Relocations.of("postgresql", "org{}postgresql")
    ),
    H2_DRIVER(
            "com.h2database",
            "h2",
            "1.4.196",
            "CgX0oNW4WEAUiq3OY6QjtdPDbvRHVjibT6rQjScz+vU=",
            Relocations.of("h2", "org{}h2")
    ),
    SQLITE_DRIVER(
            "org.xerial",
            "sqlite-jdbc",
            "3.21.0",
            "bglRaH4Y+vQFZV7TfOdsVLO3rJpauJ+IwjuRULAb45Y=",
            Relocations.of("sqlite", "org{}sqlite")
    ),
    HIKARI(
            "com{}zaxxer",
            "HikariCP",
            "2.7.4",
            "y9JE6/VmbydCqlV1z468+oqdkBswBk6aw+ECT178AT4=",
            Relocations.of("hikari", "com{}zaxxer{}hikari")
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
            "3.5.0",
            "gxrbKVSI/xM6r+6uL7g7I0DzNV+hlNTtfw4UL13XdK8=",
            ImmutableList.<Relocation>builder()
                    .addAll(Relocations.of("mongodb", "com{}mongodb"))
                    .addAll(Relocations.of("bson", "org{}bson"))
                    .build()
    ),
    CASSANDRA_DRIVER(
            "com.datastax.cassandra",
            "cassandra-driver-core",
            "3.3.2",
            "BauFwmXj/syIoOkmhlyKYVwv8+QiEVHvSoOHhz9OIXs="
    ),
    DROPWIZARD_METRICS(
            "io.dropwizard.metrics",
            "metrics-core",
            "3.2.2",
            "XG9oXkFmTRDHDGWDfLqeWNOf84loEeO1cHqTSxHIWtA="
    ),
    JNR_FFI(
            "com.github.jnr",
            "jnr-ffi",
            "2.0.7",
            "lAbfqtWbw/wN6Xq/ikVecNRgwpTW9EVAfVCA25MjlkM="
    ),
    JNR_POSIX(
            "com.github.jnr",
            "jnr-posix",
            "3.0.27",
            "hNqwMaOGiMsuyAigEboCM8sNEUbTkCDhRHmSrVBfPXg="
    ),
    JEDIS(
            "https://github.com/lucko/jedis/releases/download/jedis-2.9.1-shaded/jedis-2.9.1-shaded.jar",
            "2.9.1-shaded",
            "mM19X6LyD97KP4RSbcCR5BTRAwQ0x9y02voX7ePOSjE=",
            Relocations.of("jedis", "redis{}clients{}jedis{}shaded")
    ),
    NATS(
            "io.nats",
            "jnats",
            "1.0",
            "0VTd86WjPdzwXZxmmWPLwb1Fx+XNrZtgjncP44o0wMo="
    ),
    CONFIGURATE_CORE(
            "ninja{}leaping{}configurate",
            "configurate-core",
            "3.3",
            "4leBJEqj1kVszaifZeKNl4hgHxG5M+Nk5TJKkPW2s4Y=",
            Relocations.of("configurate", "ninja{}leaping{}configurate")
    ),
    CONFIGURATE_GSON(
            "ninja{}leaping{}configurate",
            "configurate-gson",
            "3.3",
            "4HxrW3/ZKdn095x/W4gylQMNskdmteXYVxVv0UKGJA4=",
            Relocations.of("configurate", "ninja{}leaping{}configurate")
    ),
    CONFIGURATE_YAML(
            "ninja{}leaping{}configurate",
            "configurate-yaml",
            "3.3",
            "hgADp3g+xHHPD34bAuxMWtB+OQ718Tlw69jVp2KPJNk=",
            Relocations.of("configurate", "ninja{}leaping{}configurate")
    ),
    CONFIGURATE_HOCON(
            "ninja{}leaping{}configurate",
            "configurate-hocon",
            "3.3",
            "UIy5FVmsBUG6+Z1mpIEE2EXgtOI1ZL0p/eEW+BbtGLU=",
            ImmutableList.<Relocation>builder()
                    .addAll(Relocations.of("configurate", "ninja{}leaping{}configurate"))
                    .addAll(Relocations.of("hocon", "com{}typesafe{}config"))
                    .build()
    ),
    HOCON_CONFIG(
            "com{}typesafe",
            "config",
            "1.3.1",
            "5vrfxhCCINOmuGqn5OFsnnu4V7pYlViGMIuxOXImSvA=",
            Relocations.of("hocon", "com{}typesafe{}config")
    );

    private final String url;
    private final String version;
    private final byte[] checksum;
    private final List<Relocation> relocations;

    private static final String MAVEN_CENTRAL_FORMAT = "https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.jar";

    Dependency(String groupId, String artifactId, String version, String checksum) {
        this(groupId, artifactId, version, checksum, Collections.emptyList());
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
                    System.out.println("MATCH " + dependency.name() + ": " + Base64.getEncoder().encodeToString(hash));
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
