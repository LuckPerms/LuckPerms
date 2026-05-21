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

package me.lucko.luckperms.common.placeholders;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedDataManager;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * The context passed to a {@link Placeholder} resolve request.
 */
public class PlaceholderContext {

    /** The LuckPerms API instance */
    private final @NonNull LuckPerms api;
    /** The user for the player the placeholder is being resolved for */
    private final @NonNull User user;
    /** The query options for the player the placeholder is being resolved for */
    private final @NonNull QueryOptions queryOptions;

    public PlaceholderContext(@NonNull LuckPerms api, @NonNull User user, @NonNull QueryOptions queryOptions) {
        this.api = api;
        this.user = user;
        this.queryOptions = queryOptions;
    }

    public @NonNull LuckPerms api() {
        return this.api;
    }

    public @NonNull User user() {
        return this.user;
    }

    public @NonNull QueryOptions queryOptions() {
        return this.queryOptions;
    }

    public @NonNull CachedDataManager userData() {
        return this.user.getCachedData();
    }

    public @NonNull CachedPermissionData permissionData() {
        return this.user.getCachedData().getPermissionData(this.queryOptions);
    }

    public @NonNull CachedMetaData metaData() {
        return this.user.getCachedData().getMetaData(this.queryOptions);
    }

    /**
     * Create a copy of this placeholder context, additionally including an argument.
     *
     * @param argument the argument
     * @return the new context
     */
    public WithArgument withArgument(@NonNull String argument) {
        return new WithArgument(this.api, this.user, this.queryOptions, argument);
    }

    /**
     * Extension of {@link PlaceholderContext} with an extra dynamic argument provided by the requester.
     */
    public static class WithArgument extends PlaceholderContext {

        /** An additional argument passed to the placeholder resolve request */
        private final @NonNull String argument;

        public WithArgument(@NonNull LuckPerms api, @NonNull User user, @NonNull QueryOptions queryOptions, @NonNull String argument) {
            super(api, user, queryOptions);
            this.argument = argument;
        }

        /**
         * Gets the argument.
         *
         * @return the argument
         */
        public @NonNull String argument() {
            return this.argument;
        }
    }
}
