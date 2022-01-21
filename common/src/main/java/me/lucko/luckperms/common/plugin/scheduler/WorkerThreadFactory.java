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

package me.lucko.luckperms.common.plugin.scheduler;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

public final class WorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {

    @Override
    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        return new WorkerThread(pool);
    }

    private static final class WorkerThread extends ForkJoinWorkerThread {

        private WorkerThread(ForkJoinPool pool) {
            super(pool);
            // By default, the ContextClassLoader is set to the SystemClassLoader, This causes ClassNotFoundExceptions
            // due to modlauncher loading LuckPerms on its TransformingClassLoader,
            // we must replace the ContextClassLoader with the current one to ensure the correct ClassLoader is used.
            setContextClassLoader(Thread.currentThread().getContextClassLoader());
        }
    }

}
