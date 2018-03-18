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

import me.lucko.luckperms.common.buffers.Buffer;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.storage.Storage;

import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;

/**
 * A storage wrapping that passes save tasks through a buffer
 */
public interface BufferedOutputStorage extends Storage {

    /**
     * Creates a new instance of {@link BufferedOutputStorage} which delegates called to the given
     * {@link Storage} instance.
     *
     * @param delegate the delegate storage impl
     * @param flushTime default flush time for the buffer. See {@link Buffer#flush(long)}
     * @return the new buffered storage instance
     */
    static BufferedOutputStorage wrap(Storage delegate, long flushTime) {
        // create a buffer handler - we pass the unwrapped delegate here.
        StorageBuffer buffer = new StorageBuffer(delegate, flushTime);

        // create and return a proxy instance which directs save calls through the buffer
        return (BufferedOutputStorage) Proxy.newProxyInstance(
                BufferedOutputStorage.class.getClassLoader(),
                new Class[]{BufferedOutputStorage.class},
                (proxy, method, args) -> {
                    // run save methods through the buffer instance
                    switch (method.getName()) {
                        case "saveUser":
                            return buffer.saveUser((User) args[0]);
                        case "saveGroup":
                            return buffer.saveGroup((Group) args[0]);
                        case "saveTrack":
                            return buffer.saveTrack((Track) args[0]);
                    }

                    // provide implementation of #noBuffer
                    if (method.getName().equals("noBuffer")) {
                        return delegate;
                    }

                    // provide implementation of #buffer
                    if (method.getName().equals("buffer")) {
                        return buffer;
                    }

                    // flush the buffer on shutdown
                    if (method.getName().equals("shutdown")) {
                        buffer.forceFlush();
                        // ...and then delegate
                    }

                    // delegate the call
                    return method.invoke(delegate, args);
                }
        );
    }

    /**
     * Gets the buffer behind this instance
     *
     * @return the buffer
     */
    StorageBuffer buffer();

    final class StorageBuffer implements Runnable {
        private final long flushTime;

        private final Buffer<User, Void> userOutputBuffer;
        private final Buffer<Group, Void> groupOutputBuffer;
        private final Buffer<Track, Void> trackOutputBuffer;

        private StorageBuffer(Storage delegate, long flushTime) {
            this.flushTime = flushTime;
            this.userOutputBuffer = Buffer.of(user -> delegate.saveUser(user).join());
            this.groupOutputBuffer = Buffer.of(group -> delegate.saveGroup(group).join());
            this.trackOutputBuffer = Buffer.of(track -> delegate.saveTrack(track).join());
        }

        public void run() {
            flush(this.flushTime);
        }

        public void forceFlush() {
            flush(-1);
        }

        public void flush(long flushTime) {
            this.userOutputBuffer.flush(flushTime);
            this.groupOutputBuffer.flush(flushTime);
            this.trackOutputBuffer.flush(flushTime);
        }

        // copy the required implementation methods from the Storage interface

        private CompletableFuture<Void> saveUser(User user) {
            return this.userOutputBuffer.enqueue(user);
        }

        private CompletableFuture<Void> saveGroup(Group group) {
            return this.groupOutputBuffer.enqueue(group);
        }

        private CompletableFuture<Void> saveTrack(Track track) {
            return this.trackOutputBuffer.enqueue(track);
        }
    }
}
