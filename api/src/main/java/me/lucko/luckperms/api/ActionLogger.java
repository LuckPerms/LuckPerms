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

package me.lucko.luckperms.api;

import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;

/**
 * Represents the object responsible for handling action logging.
 *
 * @since 4.1
 */
public interface ActionLogger {

    /**
     * Returns a new {@link LogEntry.Builder} instance
     *
     * @return a new builder
     */
    @Nonnull
    LogEntry.Builder newEntryBuilder();

    /**
     * Gets a {@link Log} instance from the plugin storage.
     *
     * <p>Returns the same result as {@link Storage#getLog()}.</p>
     *
     * @return a log instance
     * @see Storage#getLog()
     */
    @Nonnull
    CompletableFuture<Log> getLog();

    /**
     * Submits a log entry to the plugin to be handled.
     *
     * <p>This method submits the log to the storage provider and broadcasts
     * it.</p>
     *
     * <p>It is therefore roughly equivalent to calling
     * {@link #submitToStorage(LogEntry)} and {@link #broadcastAction(LogEntry)},
     * however, using this method is preferred to making the calls individually.</p>
     *
     * <p>If you want to submit a log entry but don't know which method to pick,
     * use this one.</p>
     *
     * @param entry the entry to submit
     * @return a future which will complete when the action is done
     */
    @Nonnull
    CompletableFuture<Void> submit(@Nonnull LogEntry entry);

    /**
     * Submits a log entry to the plugins storage handler.
     *
     * <p>Performs the same action as {@link Storage#logAction(LogEntry)}.</p>
     *
     * @param entry the entry to submit
     * @return a future which will complete when the action is done
     */
    @Nonnull
    CompletableFuture<Void> submitToStorage(@Nonnull LogEntry entry);

    /**
     * Submits a log entry to the plugins log broadcasting handler.
     *
     * <p>If enabled, this method will also dispatch the log entry via the
     * plugins {@link MessagingService}.</p>
     *
     * @param entry the entry to submit
     * @return a future which will complete when the action is done
     */
    @Nonnull
    CompletableFuture<Void> broadcastAction(@Nonnull LogEntry entry);

}
