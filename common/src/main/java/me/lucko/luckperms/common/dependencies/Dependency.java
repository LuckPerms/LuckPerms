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
            "9.1",
            "zaTeRV+rSP8Ly3xItGOUR9TehZp6/DCglKmG8JNr66I="
    ),
    ASM_COMMONS(
            "org.ow2.asm",
            "asm-commons",
            "9.1",
            "r8sm3B/BLAxKma2mcJCN2C4Y38SIyvXuklRplrRwwAw="
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
            "4.13.0",
            "gcmYlY1KDrzbnE9nB8rA44oBZKPSMvv9kNRefoEuud0=",
            Relocation.of("adventure", "net{}kyori{}adventure")
    ),
    ADVENTURE_PLATFORM(
            "me{}lucko",
            "adventure-platform-api",
            "4.13.0",
            "Kk8IkEMVa9ITBfC3yocpcXQiZ9CwN9VxeWjKUD8I0n0=",
            Relocation.of("adventure", "net{}kyori{}adventure")
    ),
    ADVENTURE_PLATFORM_BUKKIT(
            "me{}lucko",
            "adventure-platform-bukkit",
            "4.13.3",
            "ZXw7pf+a6tjGdB2WO1rB/KQ+ETLAH8kQFSu3El7jopY=",
            Relocation.of("adventure", "net{}kyori{}adventure")
    ),
    ADVENTURE_PLATFORM_BUNGEECORD(
            "me{}lucko",
            "adventure-platform-bungeecord",
            "4.13.3",
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
            "2.9.0",
            "VFMotEO3XLbTHfRKfL3m36GlN72E/dzRFH9B5BJiX2o=",
            Relocation.of("caffeine", "com{}github{}benmanes{}caffeine")
    ),
    OKIO(
            "com{}squareup{}" + RelocationHelper.OKIO_STRING,
            RelocationHelper.OKIO_STRING,
            "1.17.5",
            "Gaf/SNhtPPRJf38lD78pX0MME6Uo3Vt7ID+CGAK4hq0=",
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
            "1.10.22",
            "+TGtxDkxd6+lJExHJXqDlV4n/gR8QJN4xu2gkPsHSoQ=",
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
            "3.1.3",
            "ESl+5lYkJsScgTh8hgFTy8ExxMPQQkktT20tl6s6HKU=",
            Relocation.of("mariadb", "org{}mariadb{}jdbc")
    ),
    MYSQL_DRIVER(
            "mysql",
            "mysql-connector-java",
            "8.0.23",
            "/31bQCr9OcEnh0cVBaM6MEEDsjjsG3pE6JNtMynadTU=",
            Relocation.of("mysql", "com{}mysql")
    ),
    POSTGRESQL_DRIVER(
            "org{}postgresql",
            "postgresql",
            "42.6.0",
            "uBfGekDJQkn9WdTmhuMyftDT0/rkJrINoPHnVlLPxGE=",
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
            "2.1.214",
            "1iPNwPYdIYz1SajQnxw5H/kQlhFrIuJHVHX85PvnK9A="
            // we don't apply relocations to h2 - it gets loaded via
            // an isolated classloader
    ),
    SQLITE_DRIVER(
            "org.xerial",
            "sqlite-jdbc",
            "3.28.0",
            "k3hOVtv1RiXgbJks+D9w6cG93Vxq0dPwEwjIex2WG2A="
            // we don't apply relocations to sqlite - it gets loaded via
            // an isolated classloader
    ),
    HIKARI(
            "com{}zaxxer",
            "HikariCP",
            "4.0.3",
            "fAJK7/HBBjV210RTUT+d5kR9jmJNF/jifzCi6XaIxsk=",
            Relocation.of("hikari", "com{}zaxxer{}hikari")
    ),
    SLF4J_SIMPLE(
            "org.slf4j",
            "slf4j-simple",
            "1.7.30",
            "i5J5y/9rn4hZTvrjzwIDm2mVAw7sAj7UOSh0jEFnD+4="
    ),
    SLF4J_API(
            "org.slf4j",
            "slf4j-api",
            "1.7.30",
            "zboHlk0btAoHYUhcax6ML4/Z6x0ZxTkorA1/lRAQXFc="
    ),
    MONGODB_DRIVER_CORE(
            "org.mongodb",
            "mongodb-driver-core",
            "4.5.0",
            "awqoW0ImUcrCTA2d1rDCjDLEjLMCrOjKWIcC7E+zLGA=",
            Relocation.of("mongodb", "com{}mongodb"),
            Relocation.of("bson", "org{}bson")
    ),
    MONGODB_DRIVER_LEGACY(
            "org.mongodb",
            "mongodb-driver-legacy",
            "4.5.0",
            "77KZGIr3KZmzBpN69rGOLXmnlJIBCXRl/U4gEIdlFhY=",
            Relocation.of("mongodb", "com{}mongodb"),
            Relocation.of("bson", "org{}bson")
    ),
    MONGODB_DRIVER_SYNC(
            "org.mongodb",
            "mongodb-driver-sync",
            "4.5.0",
            "q9XDSGJjlo/Ek6jHoCbqWnaK/dghB8y9aDM0hCLiSvk=",
            Relocation.of("mongodb", "com{}mongodb"),
            Relocation.of("bson", "org{}bson")
    ),
    MONGODB_DRIVER_BSON(
            "org.mongodb",
            "bson",
            "4.5.0",
            "6CFyEzxbdeiBEXdDBmcgqWs5dvicgFkBLU3MlQUIqRA=",
            Relocation.of("mongodb", "com{}mongodb"),
            Relocation.of("bson", "org{}bson")
    ),
    JEDIS(
            "redis.clients",
            "jedis",
            "4.4.3",
            "wwwoCDPCywcfoNwpvwP95kXYusXSTtXhuVrB31sxE0k=",
            Relocation.of("jedis", "redis{}clients{}jedis"),
            Relocation.of("commonspool2", "org{}apache{}commons{}pool2")
    ),
    NATS(
        "io.nats",
        "jnats",
        "2.16.4",
        "/WZgFi9iJToRGIiGoShlwE5aHwstOuNGZyr4UaBRilM=",
        Relocation.of("nats", "io{}nats{}client")
    ),
    RABBITMQ(
            "com{}rabbitmq",
            "amqp-client",
            "5.12.0",
            "CxliwVWAnPKi5BwxCu1S1SGzx5fbhTk5JCKdBS27P2c=",
            Relocation.of("rabbitmq", "com{}rabbitmq")
    ),
    COMMONS_POOL_2(
            "org.apache.commons",
            "commons-pool2",
            "2.9.0",
            "vJGbQmv6+zHsxF1mUqnxN0YkZdhJ+zhz142Qw/jTWwE=",
            Relocation.of("commonspool2", "org{}apache{}commons{}pool2")
    ),
    CONFIGURATE_CORE(
            "org{}spongepowered",
            "configurate-core",
            "3.7.2",
            "XF2LzWLkSV0wyQRDt33I+gDlf3t2WzxH1h8JCZZgPp4=",
            Relocation.of("configurate", "ninja{}leaping{}configurate")
    ),
    CONFIGURATE_GSON(
            "org{}spongepowered",
            "configurate-gson",
            "3.7.2",
            "9S/mp3Ig9De7NNd6+2kX+L4R90bHnAosSNVbFjrl7sM=",
            Relocation.of("configurate", "ninja{}leaping{}configurate")
    ),
    CONFIGURATE_YAML(
            "org{}spongepowered",
            "configurate-yaml",
            "3.7.2",
            "OBfYn4nSMGZfVf2DoZhZq+G9TF1mODX/C5OOz/mkPmc=",
            Relocation.of("configurate", "ninja{}leaping{}configurate")
    ),
    SNAKEYAML(
            "org.yaml",
            "snakeyaml",
            "1.28",
            "NURqFCFDXUXkxqwN47U3hSfVzCRGwHGD4kRHcwzh//o=",
            Relocation.of("yaml", "org{}yaml{}snakeyaml")
    ),
    CONFIGURATE_HOCON(
            "org{}spongepowered",
            "configurate-hocon",
            "3.7.2",
            "GOORZlK1FKLzdIm7dKyyXtBdvk7Z89HARAd2H6NiWSY=",
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
