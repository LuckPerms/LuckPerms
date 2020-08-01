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

/**
 * The dependencies used by LuckPerms.
 */
public enum Dependency {

    ASM(
            "org.ow2.asm",
            "asm",
            "7.1",
            "SrL6K20sycyx6qBeoynEB7R7E+0pFfYvjEuMyWJY1N4="
    ),
    ASM_COMMONS(
            "org.ow2.asm",
            "asm-commons",
            "7.1",
            "5VkEidjxmE2Fv+q9Oxc3TFnCiuCdSOxKDrvQGVns01g="
    ),
    JAR_RELOCATOR(
            "me.lucko",
            "jar-relocator",
            "1.4",
            "1RsiF3BiVztjlfTA+svDCuoDSGFuSpTZYHvUK8yBx8I="
    ),

    TEXT(
            "net{}kyori",
            "text-api",
            "3.0.4",
            "qJCoD0fTnRhI0EpqdiLAT9QH5gIyY8aNw4Exe/gTWm0=",
            Relocation.of("text", "net{}kyori{}text")
    ),
    TEXT_SERIALIZER_GSON(
            "net{}kyori",
            "text-serializer-gson",
            "3.0.4",
            "pes03k1/XKS9OpiK+xqVmk+lXSJIsCEkkg3g36PV65A=",
            Relocation.of("text", "net{}kyori{}text")
    ),
    TEXT_SERIALIZER_LEGACY(
            "net{}kyori",
            "text-serializer-legacy",
            "3.0.4",
            "1ZYqzZ7zhnN2AyU/n/NeRQv0A9R01j/gX1Uq/nE02SI=",
            Relocation.of("text", "net{}kyori{}text")
    ),
    TEXT_ADAPTER_BUKKIT(
            "net{}kyori",
            "text-adapter-bukkit",
            "3.0.5",
            "cXA/7PDtnWpd8l7H4AEhP/3Z/WRNiFhDSqKbqO/1+ig=",
            Relocation.of("text", "net{}kyori{}text")
    ),
    TEXT_ADAPTER_BUNGEECORD(
            "net{}kyori",
            "text-adapter-bungeecord",
            "3.0.5",
            "+yU9AB1mG5wTAFeZc6zArs67loFz00w8VqE34QCjCdw=",
            Relocation.of("text", "net{}kyori{}text")
    ),
    TEXT_ADAPTER_SPONGEAPI(
            "net{}kyori",
            "text-adapter-spongeapi",
            "3.0.5",
            "/plXxpvDwqYECq+0saN13Y/Qf6F7GthJPc/hjR7SL5s=",
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
            "2.8.4",
            "KV9YN5gQj6b507VJApJpPF5PkCon0DZqAi0T7Ln0lag=",
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
            "3.14.7",
            "Yg1PpDxcal72JXYCBKiHmeHkpl4ceh2NoC4GHEy7gAA=",
            Relocation.of(RelocationHelper.OKHTTP3_STRING, RelocationHelper.OKHTTP3_STRING),
            Relocation.of(RelocationHelper.OKIO_STRING, RelocationHelper.OKIO_STRING)
    ),
    BYTEBUDDY(
            "net{}bytebuddy",
            "byte-buddy",
            "1.10.9",
            "B7nKbi+XDLA/SyVlHfHy/OJx1JG0TgQJgniHeG9pLU0=",
            Relocation.of("bytebuddy", "net{}bytebuddy")
    ),
    COMMODORE(
            "me{}lucko",
            "commodore",
            "1.7",
            "ncwmvNFfvyZf1Pa0v4fWyMR0Jxe1v1ZgXOiI255IX5Q=",
            Relocation.of("commodore", "me{}lucko{}commodore")
    ),
    MARIADB_DRIVER(
            "org{}mariadb{}jdbc",
            "mariadb-java-client",
            "2.6.0",
            "fgiCp29Z7X38ULAJNsxZ1wFIVT2u3trSx/VCMxTlA6g=",
            Relocation.of("mariadb", "org{}mariadb{}jdbc")
    ),
    MYSQL_DRIVER(
            "mysql",
            "mysql-connector-java",
            "5.1.48",
            "VuJsqqOCH1rkr0T5x09mz4uE6gFRatOAPLsOkEm27Kg=",
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
            // seems to be a compat bug in 1.4.200 with older dbs
            // see: https://github.com/h2database/h2database/issues/2078
            "1.4.199",
            "MSWhZ0O8a0z7thq7p4MgPx+2gjCqD9yXiY95b5ml1C4="
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
            "3.4.5",
            "i3MvlHBXDUqEHcHvbIJrWGl4sluoMHEv8fpZ3idd+mE=",
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
    MONGODB_DRIVER(
            "org.mongodb",
            "mongo-java-driver",
            "3.12.2",
            "eMxHcEtasb/ubFCv99kE5rVZMPGmBei674ZTdjYe58w=",
            Relocation.of("mongodb", "com{}mongodb"),
            Relocation.of("bson", "org{}bson")
    ),
    JEDIS(
            "redis.clients",
            "jedis",
            "3.3.0",
            "HuTfz9xW/mi1fwVQ3xgPmd6qwTRMF/3fyMzw2LmOgy4=",
            Relocation.of("jedis", "redis{}clients{}jedis"),
            Relocation.of("commonspool2", "org{}apache{}commons{}pool2")
    ),
    COMMONS_POOL_2(
            "org.apache.commons",
            "commons-pool2",
            "2.8.0",
            "Xvqfu1SlixoSIFpfrFZfaYKr/rD/Rb28MYdI71/To/8=",
            Relocation.of("commonspool2", "org{}apache{}commons{}pool2")
    ),
    CONFIGURATE_CORE(
            "org{}spongepowered",
            "configurate-core",
            "3.7",
            "V+M3OFm+O0AHsao557kExxa27lYEX7UYE06G/zC/Kyc=",
            Relocation.of("configurate", "ninja{}leaping{}configurate")
    ),
    CONFIGURATE_GSON(
            "org{}spongepowered",
            "configurate-gson",
            "3.7",
            "0JhMGX6mjY8MDCGGc7lrfoHvWbpGiE5R6N3nqJch+SU=",
            Relocation.of("configurate", "ninja{}leaping{}configurate")
    ),
    CONFIGURATE_YAML(
            "org{}spongepowered",
            "configurate-yaml",
            "3.7",
            "14L0JiDuAfQovxkNySeaf9Kul3Nkl0OaW49Ow4ReV8E=",
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
            "org{}spongepowered",
            "configurate-hocon",
            "3.7",
            "GYdqieCZVgPmoaIFjYN0YHuSVsHO7IsXZrwLAWqCgZM=",
            Relocation.of("configurate", "ninja{}leaping{}configurate"),
            Relocation.of("hocon", "com{}typesafe{}config")
    ),
    HOCON_CONFIG(
            "com{}typesafe",
            "config",
            "1.4.0",
            "qtv9WlJFUb7vENP4kdMFuDuyfVRwPZpN56yioS2YR+I=",
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

    public String getFileName() {
        return name().toLowerCase().replace('_', '-') + "-" + this.version;
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

    /*
    public static void main(String[] args) {
        Dependency[] dependencies = values();
        DependencyRepository[] repos = DependencyRepository.values();

        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newCachedThreadPool();

        for (Dependency dependency : dependencies) {
            for (DependencyRepository repo : repos) {
                pool.submit(() -> {
                    try {
                        byte[] hash = createDigest().digest(repo.downloadRaw(dependency));
                        if (!dependency.checksumMatches(hash)) {
                            System.out.println("NO MATCH - " + repo.name() + " - " + dependency.name() + ": " + Base64.getEncoder().encodeToString(hash));
                        } else {
                            System.out.println("OK - " + repo.name() + " - " + dependency.name());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }

        pool.shutdown();
        try {
            pool.awaitTermination(1, java.util.concurrent.TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    */

}
