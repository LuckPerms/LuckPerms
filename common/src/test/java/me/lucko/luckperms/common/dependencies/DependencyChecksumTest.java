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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DependencyChecksumTest {

    @Test
    @Tag("dependency_checksum")
    public void check() {
        Dependency[] dependencies = Dependency.values();
        DependencyRepository[] repos = DependencyRepository.values();
        ExecutorService pool = Executors.newCachedThreadPool();

        AtomicBoolean failed = new AtomicBoolean(false);

        for (Dependency dependency : dependencies) {
            for (DependencyRepository repo : repos) {
                pool.submit(() -> {
                    try {
                        byte[] hash = Dependency.createDigest().digest(repo.downloadRaw(dependency));
                        if (!dependency.checksumMatches(hash)) {
                            System.out.println("NO MATCH - " + repo.name() + " - " + dependency.name() + ": " + Base64.getEncoder().encodeToString(hash));
                            failed.set(true);
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
            pool.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (failed.get()) {
            Assertions.fail("Some dependency checksums did not match");
        }
    }

}
