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

package net.luckperms.api.actionlog;

import net.luckperms.api.actionlog.filter.ActionFilter;
import net.luckperms.api.util.Page;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the object responsible for handling action logging.
 */
public interface ActionLogger {

    /**
     * Returns a new {@link Action.Builder} instance
     *
     * @return a new builder
     */
    Action.@NonNull Builder actionBuilder();

    /**
     * Gets a {@link ActionLog} instance from the plugin storage.
     *
     * @return a log instance
     * @deprecated Use {@link #queryActions(ActionFilter)} or {@link #queryActions(ActionFilter, int, int)} instead. These methods
     * are more efficient (they don't load the full action log into memory) and allow for pagination.
     */
    @Deprecated
    @NonNull CompletableFuture<ActionLog> getLog();

    /**
     * Gets all actions from the action log matching the given {@code filter}.
     *
     * <p>If the filter is {@code null}, all actions will be returned.</p>
     *
     * <p>Unlike {@link #queryActions(ActionFilter, int, int)}, this method does not implement any pagination and will return
     * all entries at once.</p>
     *
     * @param filter the filter, optional
     * @return the actions
     * @since 5.5
     */
    @NonNull CompletableFuture<List<Action>> queryActions(@NonNull ActionFilter filter);

    /**
     * Gets a page of actions from the action log matching the given {@code filter}.
     *
     * <p>If the filter is {@code null}, all actions will be returned.</p>
     *
     * @param filter the filter, optional
     * @param pageSize the size of the page
     * @param pageNumber the page number
     * @return the page of actions
     * @since 5.5
     */
    @NonNull CompletableFuture<Page<Action>> queryActions(@NonNull ActionFilter filter, int pageSize, int pageNumber);

    /**
     * Submits a logged action to LuckPerms.
     *
     * <p>This method submits the action to the storage provider to be persisted in the action log.
     * It also broadcasts it to administrator players on the current instance and to admins on other
     * connected servers if a messaging service is configured.</p>
     *
     * <p>It is roughly equivalent to calling
     * {@link #submitToStorage(Action)} followed by {@link #broadcastAction(Action)},
     * however using this method is preferred to making the calls individually.</p>
     *
     * <p>If you want to submit an action log entry but don't know which method to pick,
     * use this one.</p>
     *
     * @param entry the entry to submit
     * @return a future which will complete when the action is submitted
     */
    @NonNull CompletableFuture<Void> submit(@NonNull Action entry);

    /**
     * Submits a logged action to LuckPerms and persists it in the storage backend.
     *
     * <p>This method does not broadcast the action or send it through the messaging service.</p>
     *
     * @param entry the entry to submit
     * @return a future which will complete when the action is submitted
     */
    @NonNull CompletableFuture<Void> submitToStorage(@NonNull Action entry);

    /**
     * Submits a logged action to LuckPerms and broadcasts it to administrators.
     *
     * <p>The broadcast is made to administrator players on the current instance
     * and to admins on other connected servers if a messaging service is configured.</p>
     *
     * <p>This method does not save the action to the plugin storage backend.</p>
     *
     * @param entry the entry to submit
     * @return a future which will complete when the action is broadcasted
     */
    @NonNull CompletableFuture<Void> broadcastAction(@NonNull Action entry);

}
