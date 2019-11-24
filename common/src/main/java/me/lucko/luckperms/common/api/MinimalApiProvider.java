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

package me.lucko.luckperms.common.api;

import me.lucko.luckperms.common.actionlog.LoggedAction;
import me.lucko.luckperms.common.api.implementation.ApiContextSetFactory;
import me.lucko.luckperms.common.api.implementation.ApiNodeBuilderRegistry;
import me.lucko.luckperms.common.query.QueryOptionsBuilderImpl;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.actionlog.Action;
import net.luckperms.api.actionlog.ActionLog;
import net.luckperms.api.actionlog.ActionLogger;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.context.ContextSetFactory;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.messaging.MessagingService;
import net.luckperms.api.messenger.MessengerProvider;
import net.luckperms.api.metastacking.MetaStackFactory;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.NodeBuilderRegistry;
import net.luckperms.api.platform.Platform;
import net.luckperms.api.platform.PluginMetadata;
import net.luckperms.api.query.QueryMode;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.track.TrackManager;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Implements the LuckPerms API using the plugin instance
 */
public class MinimalApiProvider implements LuckPerms {
    public static final MinimalApiProvider INSTANCE = new MinimalApiProvider();
    
    private MinimalApiProvider() {
        
    }
    
    private static UnsupportedOperationException exception() {
        return new UnsupportedOperationException("API is not fully loaded yet - current implementation is minimal.");
    }

    @Override
    public @NonNull ActionLogger getActionLogger() {
        return MinimalActionLogger.INSTANCE;
    }

    @Override
    public @NonNull ContextManager getContextManager() {
        return MinimalContextManager.INSTANCE;
    }

    @Override
    public @NonNull NodeBuilderRegistry getNodeBuilderRegistry() {
        return ApiNodeBuilderRegistry.INSTANCE;
    }

    @Override public @NonNull String getServerName() { throw exception(); }
    @Override public @NonNull UserManager getUserManager() { throw exception(); }
    @Override public @NonNull GroupManager getGroupManager() { throw exception(); }
    @Override public @NonNull TrackManager getTrackManager() { throw exception(); }
    @Override public @NonNull Platform getPlatform() { throw exception(); }
    @Override public @NonNull PluginMetadata getPluginMetadata() { throw exception(); }
    @Override public @NonNull EventBus getEventBus() { throw exception(); }
    @Override public @NonNull Optional<MessagingService> getMessagingService() { throw exception(); }
    @Override public @NonNull MetaStackFactory getMetaStackFactory() { throw exception(); }
    @Override public @NonNull CompletableFuture<Void> runUpdateTask() { throw exception(); }
    @Override public void registerMessengerProvider(@NonNull MessengerProvider messengerProvider) { throw exception(); }

    private static final class MinimalActionLogger implements ActionLogger {
        private static final MinimalActionLogger INSTANCE = new MinimalActionLogger();

        private MinimalActionLogger() {

        }

        @Override
        public Action.@NonNull Builder actionBuilder() {
            return LoggedAction.build();
        }

        @Override public @NonNull CompletableFuture<ActionLog> getLog() { throw exception(); }
        @Override public @NonNull CompletableFuture<Void> submit(@NonNull Action entry) { throw exception(); }
        @Override public @NonNull CompletableFuture<Void> submitToStorage(@NonNull Action entry) { throw exception(); }
        @Override public @NonNull CompletableFuture<Void> broadcastAction(@NonNull Action entry) { throw exception(); }
    }
    
    private static final class MinimalContextManager implements ContextManager {
        private static final MinimalContextManager INSTANCE = new MinimalContextManager();
        
        private MinimalContextManager() {
            
        }
        
        @Override
        public QueryOptions.@NonNull Builder queryOptionsBuilder(@NonNull QueryMode mode) {
            Objects.requireNonNull(mode, "mode");
            return new QueryOptionsBuilderImpl(mode);
        }

        @Override
        public @NonNull ContextSetFactory getContextSetFactory() {
            return ApiContextSetFactory.INSTANCE;
        }

        @Override public @NonNull ImmutableContextSet getContext(@NonNull Object subject) { throw exception(); }
        @Override public @NonNull Optional<ImmutableContextSet> getContext(@NonNull User user) { throw exception(); }
        @Override public @NonNull ImmutableContextSet getStaticContext() { throw exception(); }
        @Override public @NonNull QueryOptions getQueryOptions(@NonNull Object subject) { throw exception(); }
        @Override public @NonNull Optional<QueryOptions> getQueryOptions(@NonNull User user) { throw exception(); }
        @Override public @NonNull QueryOptions getStaticQueryOptions() { throw exception(); }
        @Override public void registerCalculator(@NonNull ContextCalculator<?> calculator) { throw exception(); }
        @Override public void unregisterCalculator(@NonNull ContextCalculator<?> calculator) { throw exception(); }
        @Override public void invalidateCache(@NonNull Object subject) { throw exception(); }
    }

}
