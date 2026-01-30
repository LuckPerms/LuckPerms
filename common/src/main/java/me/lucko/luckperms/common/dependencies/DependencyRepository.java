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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Represents a repository which contains {@link Dependency}s.
 */
public enum DependencyRepository {

    /**
     * A {@link DependencyRepository} that downloads from the
     * LuckPerms Maven Central mirror repository.
     *
     * <p>This is used to reduce the load on repo.maven.org.</p>
     *
     * <p>Although Maven Central is technically a CDN, it is meant for developer use,
     * not end-user products. It is trivial and not very expensive for us to provide a
     * mirror, which will absorb any traffic caused by LP.</p>
     *
     * <p>LuckPerms will fallback to the real-thing if the mirror ever goes offline.
     * Retrieved content is validated with a checksum, so there is no risk to integrity.</p>
     */
    MAVEN_CENTRAL_MIRROR {
        private static final String URL = "https://libraries.luckperms.net/";

        @Override
        protected InputStream openStream(Dependency dependency) throws IOException {
            URL dependencyUrl = new URL(URL + dependency.getMavenRepoPath());

            URLConnection connection = dependencyUrl.openConnection();
            connection.setRequestProperty("User-Agent", "luckperms");

            // Set a connect/read timeout, so if the mirror goes offline we can fallback
            // to Maven Central within a reasonable time.
            connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(5));
            connection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(10));

            return connection.getInputStream();
        }
    },

    /**
     * A {@link DependencyRepository} that downloads from the official Maven Central mirror repository.
     */
    MAVEN_CENTRAL {
        private static final String URL = "https://repo1.maven.org/maven2/";

        @Override
        protected InputStream openStream(Dependency dependency) throws IOException {
            URL dependencyUrl = new URL(URL + dependency.getMavenRepoPath());
            return dependencyUrl.openStream();
        }
    },

    /**
     * A {@link DependencyRepository} that searches for dependencies bundled inside the jar (jar-in-jar).
     */
    JAR_IN_JAR {
        private static final String PATH = "luckperms/deps/";

        @Override
        protected synchronized InputStream openStream(Dependency dependency) throws IOException {
            String path = PATH + dependency.getFileName(null) + "injar"; // extension becomes .jarinjar
            return getClass().getClassLoader().getResourceAsStream(path);
        }
    };

    public static final Collection<DependencyRepository> REMOTE_MAVEN_REPOSITORIES = List.of(MAVEN_CENTRAL_MIRROR, MAVEN_CENTRAL);

    /**
     * Opens an InputStream to the given {@code dependency}.
     *
     * @param dependency the dependency to download
     * @return the input stream
     * @throws IOException if unable to open an input stream
     */
    protected abstract InputStream openStream(Dependency dependency) throws IOException;

    /**
     * Downloads the raw bytes of the {@code dependency}.
     *
     * @param dependency the dependency to download
     * @return the downloaded bytes
     * @throws DependencyDownloadException if unable to download
     */
    public byte[] downloadRaw(Dependency dependency) throws DependencyDownloadException {
        try {
            try (InputStream in = openStream(dependency)) {
                if (in == null) {
                    throw new DependencyDownloadException("Repository " + this.name() + " returned null stream for dependency " + dependency);
                }

                byte[] bytes = ByteStreams.toByteArray(in);
                if (bytes.length == 0) {
                    throw new DependencyDownloadException("Empty stream");
                }
                return bytes;
            }
        } catch (Exception e) {
            throw new DependencyDownloadException(e);
        }
    }

    /**
     * Downloads the raw bytes of the {@code dependency}, and ensures the downloaded
     * bytes match the checksum.
     *
     * @param dependency the dependency to download
     * @return the downloaded bytes
     * @throws DependencyDownloadException if unable to download
     */
    public byte[] download(Dependency dependency) throws DependencyDownloadException {
        byte[] bytes = downloadRaw(dependency);

        // compute a hash for the downloaded file
        byte[] hash = Dependency.createDigest().digest(bytes);

        // ensure the hash matches the expected checksum
        if (!dependency.checksumMatches(hash)) {
            throw new DependencyDownloadException("Downloaded file had an invalid hash. " +
                    "Expected: " + Base64.getEncoder().encodeToString(dependency.getChecksum()) + " " +
                    "Actual: " + Base64.getEncoder().encodeToString(hash));
        }

        return bytes;
    }

    /**
     * Downloads the {@code dependency} to the {@code file}, ensuring the
     * downloaded bytes match the checksum.
     *
     * @param dependency the dependency to download
     * @param file the file to write to
     * @throws DependencyDownloadException if unable to download
     */
    public void download(Dependency dependency, Path file) throws DependencyDownloadException {
        try {
            Files.write(file, download(dependency));
        } catch (IOException e) {
            throw new DependencyDownloadException(e);
        }
    }

}
