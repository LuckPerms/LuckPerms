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
import me.lucko.luckperms.common.dependencies.relocation.Relocation;
import me.lucko.luckperms.common.dependencies.relocation.RelocationHelper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * The dependencies used by LuckPerms.
 */
public enum Dependency {

    ASM(
            "org.ow2.asm",
            "asm",
            "9.8",
            "h26raoPa7K1cpn65/KuwY8l7WuuM8fynqYns3hdSIFE="
    ),
    ASM_COMMONS(
            "org.ow2.asm",
            "asm-commons",
            "9.8",
            "MwGhwctMWfzFKSZI2sHXxa7UwPBn376IhzuM3+d0BPQ="
    ),
    JAR_RELOCATOR(
            "me.lucko",
            "jar-relocator",
            "1.7",
            "b30RhOF6kHiHl+O5suNLh/+eAr1iOFEFLXhwkHHDu4I="
    ),
    ADVENTURE(
            "me{}lucko",
            "adventure-api",
            "4.21.1",
            "kQJlZ0gUxdTRRkskT43qiy2kpt9s654LvB0nqoCP6YE=",
            Relocation.of("adventure", "net{}kyori{}adventure")
    ),
    ADVENTURE_PLATFORM(
            "me{}lucko",
            "adventure-platform-api",
            "4.21.1",
            "Kk8IkEMVa9ITBfC3yocpcXQiZ9CwN9VxeWjKUD8I0n0=",
            Relocation.of("adventure", "net{}kyori{}adventure")
    ),
    ADVENTURE_PLATFORM_BUKKIT(
            "me{}lucko",
            "adventure-platform-bukkit",
            "4.21.1",
            "NffwBnfT/Mc6VpsmTcaXPvckv9T4vPJD83adt0C8vao=",
            Relocation.of("adventure", "net{}kyori{}adventure")
    ),
    ADVENTURE_PLATFORM_BUNGEECORD(
            "me{}lucko",
            "adventure-platform-bungeecord",
            "4.21.1",
            "7hnndD6dO6LZoRbtOBdn6OFK0y/T2PqNLHxCg6zaQlo=",
            Relocation.of("adventure", "net{}kyori{}adventure")
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
            "3.2.0",
            "7EEd/fDAPyUhhkjOiYYWMLcWgOWFippyeOusjlXKs9c=",
            Relocation.of("caffeine", "com{}github{}benmanes{}caffeine")
    ),
    OKIO(
            "com{}squareup{}" + RelocationHelper.OKIO_STRING,
            RelocationHelper.OKIO_STRING,
            "1.17.6",
            "joiwVVI8yAYT37hE1Zh0DhCtpi9L2YMEzdFAxYVMw7Y=",
            Relocation.of(RelocationHelper.OKIO_STRING, RelocationHelper.OKIO_STRING)
    ),
    OKHTTP(
            "com{}squareup{}" + RelocationHelper.OKHTTP3_STRING,
            "okhttp",
            "3.14.9",
            "JXD6tVUVy/iB16TO70n8UVSQvAJwV+Zmd2ooMkZa7KA=",
            Relocation.of(RelocationHelper.OKHTTP3_STRING, RelocationHelper.OKHTTP3_STRING),
            Relocation.of(RelocationHelper.OKIO_STRING, RelocationHelper.OKIO_STRING)
    ),
    BYTEBUDDY(
            "net{}bytebuddy",
            "byte-buddy",
            "1.17.5",
            "cVaMn4OWZ3IZ9lAmj79kk97UhO3NvfLa5hKcpb6B6Ns=",
            Relocation.of("bytebuddy", "net{}bytebuddy")
    ),
    COMMODORE(
            "me{}lucko",
            "commodore",
            "2.2",
            "hmZ3A/Sf8LvrT95buTlFNwdEBZ36X9Ks8SKOS1b7f28=",
            Relocation.of("commodore", "me{}lucko{}commodore")
    ),
    COMMODORE_FILE(
            "me{}lucko",
            "commodore-file",
            "1.0",
            "V9++dyp9RbzD4DLO2R9upF8Z8v5SWasyX8ocqYRAMow=",
            Relocation.of("commodore", "me{}lucko{}commodore")
    ),
    MARIADB_DRIVER(
            "org{}mariadb{}jdbc",
            "mariadb-java-client",
            "3.5.2",
            "8vPDwaO9rKad0dThzYrtB1JC/HKuQUY924LjZ7OI9q0=",
            Relocation.of("mariadb", "org{}mariadb{}jdbc")
    ),
    MYSQL_DRIVER(
            "com{}mysql",
            "mysql-connector-j",
            "9.3.0",
            "bI5mkrUhN22JvFYYwWzer4xhhUMp9PolZ37Qh3bFu3Y=",
            Relocation.of("mysql", "com{}mysql")
    ),
    POSTGRESQL_DRIVER(
            "org{}postgresql",
            "postgresql",
            "42.7.6",
            "8qHMA1LdXlxvZdut/ye+4Awy5DLGrQMNB0R/ilmDxCo=",
            Relocation.of("postgresql", "org{}postgresql")
    ),
    H2_DRIVER_LEGACY(
            "com.h2database",
            "h2",
            // seems to be a compat bug in 1.4.200 with older dbs
            // see: https://github.com/h2database/h2database/issues/2078
            "1.4.199",
            "MSWhZ0O8a0z7thq7p4MgPx+2gjCqD9yXiY95b5ml1C4="
            // we don't apply relocations to h2 - it gets loaded via
            // an isolated classloader
    ),
    H2_DRIVER(
            "com.h2database",
            "h2",
            "2.3.232",
            "ja5i0i24mCw9yzgm7bnHJ8XTAgY6Z+731j2C3kAfB9M="
            // we don't apply relocations to h2 - it gets loaded via
            // an isolated classloader
    ),
    SQLITE_DRIVER(
            "org.xerial",
            "sqlite-jdbc",
            "3.49.1.0",
            "XIYJ0so0HeuMb3F3iXS1ukmVx9MtfHyJ2TkqPnLDkpE="
            // we don't apply relocations to sqlite - it gets loaded via
            // an isolated classloader
    ),
    HIKARI(
            "com{}zaxxer",
            "HikariCP",
            "6.3.0",
            "B8Y0QFmvMKE1FEIJx8i9ZmuIIxJEIuyFmGTSCdSrfKE=",
            Relocation.of("hikari", "com{}zaxxer{}hikari")
    ),
    SLF4J_SIMPLE(
            "org.slf4j",
            "slf4j-simple",
            "1.7.36",
            "Lzm+2UPWJN+o9BAtBXEoOhCHC2qjbxl6ilBvFHAQwQ8="
    ),
    SLF4J_API(
            "org.slf4j",
            "slf4j-api",
            "1.7.36",
            "0+9XXj5JeWeNwBvx3M5RAhSTtNEft/G+itmCh3wWocA="
    ),
    MONGODB_DRIVER_CORE(
            "org.mongodb",
            "mongodb-driver-core",
            "5.5.0",
            "69tQuKep52lbYvX2YM+J6GGlYkNySXkMBDuk6BqtsJE=",
            Relocation.of("mongodb", "com{}mongodb"),
            Relocation.of("bson", "org{}bson")
    ),
    MONGODB_DRIVER_LEGACY(
            "org.mongodb",
            "mongodb-driver-legacy",
            "5.5.0",
            "yo/0wEdLw0/Md1xqgEd/iqiKV+t0AqAcdnS1TNAaygM=",
            Relocation.of("mongodb", "com{}mongodb"),
            Relocation.of("bson", "org{}bson")
    ),
    MONGODB_DRIVER_SYNC(
            "org.mongodb",
            "mongodb-driver-sync",
            "5.5.0",
            "nFECiREXgMc5Ikamvmnzaxumhz75NKG+ajIhAW/ioPI=",
            Relocation.of("mongodb", "com{}mongodb"),
            Relocation.of("bson", "org{}bson")
    ),
    MONGODB_DRIVER_BSON(
            "org.mongodb",
            "bson",
            "5.5.0",
            "hQx5w0v/DuQvASpnGXkLuWxkhXhewDTTAmrifPWbBJQ=",
            Relocation.of("mongodb", "com{}mongodb"),
            Relocation.of("bson", "org{}bson")
    ),
    JEDIS(
            "redis.clients",
            "jedis",
            "6.0.0",
            "ZJq2D7GDoNQa7nAsfhCTsClsedbEp3N2j2V3qXhk8kU=",
            Relocation.of("jedis", "redis{}clients{}jedis"),
            Relocation.of("commonspool2", "org{}apache{}commons{}pool2")
    ),
    NATS(
        "io.nats",
        "jnats",
        "2.21.1",
        "QHUHUCnCCy/oRSwoqhy0245SrvD4lwCfc+ZVmemHXLg=",
        Relocation.of("nats", "io{}nats{}client")
    ),
    RABBITMQ(
            "com{}rabbitmq",
            "amqp-client",
            "5.25.0",
            "WqlvAFCEE56xB32UtV3GQo7KfafizFPqtEp3M5H4qo8=",
            Relocation.of("rabbitmq", "com{}rabbitmq")
    ),
    COMMONS_POOL_2(
            "org.apache.commons",
            "commons-pool2",
            "2.12.1",
            "UnPIvIwNyiIRF1wNJ++9cijvrplomqwAGo4e+Ohy6e8=",
            Relocation.of("commonspool2", "org{}apache{}commons{}pool2")
    ),
    CONFIGURATE_CORE(
            "org{}spongepowered",
            "configurate-core",
            "3.7.3",
            "06R3WDViB84WtSkHTudV8TSPxF1eQyCyfab8L7Pvo2M=",
            Relocation.of("configurate", "ninja{}leaping{}configurate")
    ),
    CONFIGURATE_GSON(
            "org{}spongepowered",
            "configurate-gson",
            "3.7.3",
            "QM+bGrgrzfwT9nvIvTHtR2TUEpun+RwlXIO/a9BU0Mc=",
            Relocation.of("configurate", "ninja{}leaping{}configurate")
    ),
    CONFIGURATE_YAML(
            "org{}spongepowered",
            "configurate-yaml",
            "3.7.3",
            "a04vRkLhigIqiG/gdVvK7c1YiBQJ7k1q/kBNsS9OVDs=",
            Relocation.of("configurate", "ninja{}leaping{}configurate")
    ),
    SNAKEYAML(
            "org.yaml",
            "snakeyaml",
            "1.33",
            "Ef9Fl4jwoteB9WpKhtfmkgLOus0Cc9UmnErp8C8/2PA=",
            Relocation.of("yaml", "org{}yaml{}snakeyaml")
    ),
    CONFIGURATE_HOCON(
            "org{}spongepowered",
            "configurate-hocon",
            "3.7.3",
            "e/UDpbIrWdJNB6yMFXtrOnnNn3nptmSv/J8n46uQPNs=",
            Relocation.of("configurate", "ninja{}leaping{}configurate"),
            Relocation.of("hocon", "com{}typesafe{}config")
    ),
    HOCON_CONFIG(
            "com{}typesafe",
            "config",
            "1.4.1",
            "TAqn4iPHXIhAxB/Bg9TNMRgUCh7lA+PgjOZu0nlMlI8=",
            Relocation.of("hocon", "com{}typesafe{}config")
    ),
    CONFIGURATE_TOML(
            "me{}lucko{}configurate",
            "configurate-toml",
            "3.7",
            "EmyLOfsiR74QGhkktqhexMN8tC3kg1cM1UhM5MCmxuE=",
            Relocation.of("configurate", "ninja{}leaping{}configurate"),
            Relocation.of("toml4j", "com{}moandjiezana{}toml")
    ),
    TOML4J(
            "com{}moandjiezana{}toml",
            "toml4j",
            "0.7.2",
            "9UdeY+fonl22IiNImux6Vr0wNUN3IHehfCy1TBnKOiA=",
            Relocation.of("toml4j", "com{}moandjiezana{}toml")
    );

    private final String mavenRepoPath;
    private final String version;
    private final byte[] checksum;
    private final List<Relocation> relocations;

    private static final String MAVEN_FORMAT = "%s/%s/%s/%s-%s.jar";

    Dependency(String groupId, String artifactId, String version, String checksum) {
        this(groupId, artifactId, version, checksum, new Relocation[0]);
    }

    Dependency(String groupId, String artifactId, String version, String checksum, Relocation... relocations) {
        this.mavenRepoPath = String.format(MAVEN_FORMAT,
                rewriteEscaping(groupId).replace(".", "/"),
                rewriteEscaping(artifactId),
                version,
                rewriteEscaping(artifactId),
                version
        );
        this.version = version;
        this.checksum = Base64.getDecoder().decode(checksum);
        this.relocations = ImmutableList.copyOf(relocations);
    }

    private static String rewriteEscaping(String s) {
        return s.replace("{}", ".");
    }

    public String getFileName(String classifier) {
        String name = name().toLowerCase(Locale.ROOT).replace('_', '-');
        String extra = classifier == null || classifier.isEmpty()
                ? ""
                : "-" + classifier;

        return name + "-" + this.version + extra + ".jar";
    }

    String getMavenRepoPath() {
        return this.mavenRepoPath;
    }

    public byte[] getChecksum() {
        return this.checksum;
    }

    public boolean checksumMatches(byte[] hash) {
        return Arrays.equals(this.checksum, hash);
    }

    public List<Relocation> getRelocations() {
        return this.relocations;
    }

    /**
     * Creates a {@link MessageDigest} suitable for computing the checksums
     * of dependencies.
     *
     * @return the digest
     */
    public static MessageDigest createDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}
