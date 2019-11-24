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
            "3.0.2",
            "seZnPElZhfba3XO8/LLUWQHE35kqVM02HpswEVcJqz0=",
            Relocation.of("text", "net{}kyori{}text")
    ),
    TEXT_SERIALIZER_GSON(
            "net{}kyori",
            "text-serializer-gson",
            "3.0.2",
            "8gJFpnqSHp762TWXn3x5ICLiKg2KgEwEA1FFyOtJ92Y=",
            Relocation.of("text", "net{}kyori{}text")
    ),
    TEXT_SERIALIZER_LEGACY(
            "net{}kyori",
            "text-serializer-legacy",
            "3.0.2",
            "ks8pZV6ZUHWtwD93Xynyg0KaAMs8CLa61Zb8CaLPsdM=",
            Relocation.of("text", "net{}kyori{}text")
    ),
    TEXT_ADAPTER_BUKKIT(
            "net{}kyori",
            "text-adapter-bukkit",
            "3.0.2",
            "VnbfELmOHV6DVidHJql6gmsCjfhABEnF6q9LTezUrQg=",
            Relocation.of("text", "net{}kyori{}text")
    ),
    TEXT_ADAPTER_BUNGEECORD(
            "net{}kyori",
            "text-adapter-bungeecord",
            "3.0.2",
            "ZmCmc67danKpZru8rJsDppJZ+f8joquULZI8y/YX4ks=",
            Relocation.of("text", "net{}kyori{}text")
    ),
    TEXT_ADAPTER_SPONGEAPI(
            "net{}kyori",
            "text-adapter-spongeapi",
            "3.0.2",
            "WPWFw1g8niEGgymV9PdyLADMSL6+UQ7FKdtxnJb79JE=",
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
            "2.8.0",
            "sRB6QJe+RRWpI6Vbxj2gTkEeaWSqBFvs4bx6y4SHLtc=",
            Relocation.of("caffeine", "com{}github{}benmanes{}caffeine")
    ),
    OKIO(
            "com{}squareup{}" + RelocationHelper.OKIO_STRING,
            RelocationHelper.OKIO_STRING,
            "1.17.4",
            "14+sWIRY/AmebILpH+XwN1xnQ0YmRRo6d3csZdnu6Fs=",
            Relocation.of(RelocationHelper.OKIO_STRING, RelocationHelper.OKIO_STRING)
    ),
    OKHTTP(
            "com{}squareup{}" + RelocationHelper.OKHTTP3_STRING,
            "okhttp",
            "3.14.4",
            "WMyzdU8VzELfjxaHs7qGMw7ZfH5S5cPoP+ETG4DOPg8=",
            Relocation.of(RelocationHelper.OKHTTP3_STRING, RelocationHelper.OKHTTP3_STRING),
            Relocation.of(RelocationHelper.OKIO_STRING, RelocationHelper.OKIO_STRING)
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
            "2.5.1",
            "/AxG0o0JnIme7hnDTO2WEUxgF1yXPiWPhMKermXAzZE=",
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
            "1.4.200",
            "OtmsS2qunNnTrBxEdGXh7QYBm4UbiT3WqNdt222FvKY="
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
            "3.4.1",
            "uCbLTp8iz699ZJS3TxSAf4j9UfrikmgxvTHT0+N/Bck=",
            Relocation.of("hikari", "com{}zaxxer{}hikari")
    ),
    SLF4J_SIMPLE(
            "org.slf4j",
            "slf4j-simple",
            "1.7.28",
            "YO863GwYR8RuGr16gGIlWqPizh2ywI37H9Q/GkYgdzY="
    ),
    SLF4J_API(
            "org.slf4j",
            "slf4j-api",
            "1.7.28",
            "+25PZ6KkaJ4+cTWE2xel0QkMHr5u7DDp4DSabuEYFB4="
    ),
    MONGODB_DRIVER(
            "org.mongodb",
            "mongo-java-driver",
            "3.11.1",
            "tIWEOrQegZK3TT7mNV4ZnjkpW4ViwPiFEpD6yYPyFmE=",
            Relocation.of("mongodb", "com{}mongodb"),
            Relocation.of("bson", "org{}bson")
    ),
    JEDIS(
            "redis.clients",
            "jedis",
            "2.10.2",
            "06PKnEnk08yYpdI2IUAZYxJjp0d6lDp0nGQkWw3CWsU=",
            Relocation.of("jedis", "redis{}clients{}jedis"),
            Relocation.of("jedisutil", "redis{}clients{}util"),
            Relocation.of("commonspool2", "org{}apache{}commons{}pool2")
    ),
    COMMONS_POOL_2(
            "org.apache.commons",
            "commons-pool2",
            "2.7.0",
            "a1TGdcc4fhV9KMcJiHPy53LCI8ejW8mxNxc2fJdToeQ=",
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
            Relocation.of("configurate", "ninja{}leaping{}configurate"),
            Relocation.of("hocon", "com{}typesafe{}config")
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

    private final List<URL> urls;
    private final String version;
    private final byte[] checksum;
    private final List<Relocation> relocations;

    private static final String MAVEN_CENTRAL_REPO = "https://repo1.maven.org/maven2/";
    private static final String LUCK_MIRROR_REPO = "https://nexus.lucko.me/repository/maven-central/";
    private static final String MAVEN_FORMAT = "%s/%s/%s/%s-%s.jar";

    Dependency(String groupId, String artifactId, String version, String checksum) {
        this(groupId, artifactId, version, checksum, new Relocation[0]);
    }

    Dependency(String groupId, String artifactId, String version, String checksum, Relocation... relocations) {
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
