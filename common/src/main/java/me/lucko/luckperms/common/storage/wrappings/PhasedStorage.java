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

package me.lucko.luckperms.common.storage.wrappings;

import me.lucko.luckperms.common.storage.Storage;

import java.lang.reflect.Proxy;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A storage wrapping that ensures all tasks are completed before {@link Storage#shutdown()} is called.
 */
public interface PhasedStorage extends Storage {

    /**
     * Creates a new instance of {@link PhasedStorage} which delegates called to the given
     * {@link Storage} instance.
     *
     * @param delegate the delegate storage impl
     * @return the new phased storage instance
     */
    static PhasedStorage wrap(Storage delegate) {
        // create a new phaser to be used by the instance
        Phaser phaser = new Phaser();

        // create and return a proxy instance which directs save calls through the phaser
        return (PhasedStorage) Proxy.newProxyInstance(
                PhasedStorage.class.getClassLoader(),
                new Class[]{PhasedStorage.class},
                (proxy, method, args) -> {

                    // direct delegation
                    switch (method.getName()) {
                        case "getDao":
                        case "getApiDelegate":
                        case "getName":
                        case "init":
                        case "getMeta":
                            return method.invoke(delegate, args);
                    }

                    // await the phaser on shutdown
                    if (method.getName().equals("shutdown")) {
                        try {
                            phaser.awaitAdvanceInterruptibly(phaser.getPhase(), 10, TimeUnit.SECONDS);
                        } catch (InterruptedException | TimeoutException e) {
                            e.printStackTrace();
                        }

                        delegate.shutdown();
                        return null;
                    }

                    // for all other methods, run the call via the phaser
                    phaser.register();
                    try {
                        return method.invoke(delegate, args);
                    } finally {
                        phaser.arriveAndDeregister();
                    }
                }
        );
    }
}
