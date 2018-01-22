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

import me.lucko.luckperms.api.ActionLogger;
import me.lucko.luckperms.api.LPConfiguration;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.MessagingService;
import me.lucko.luckperms.api.NodeFactory;
import me.lucko.luckperms.api.Storage;
import me.lucko.luckperms.api.UuidCache;
import me.lucko.luckperms.api.context.ContextManager;
import me.lucko.luckperms.api.event.EventBus;
import me.lucko.luckperms.api.manager.GroupManager;
import me.lucko.luckperms.api.manager.TrackManager;
import me.lucko.luckperms.api.manager.UserManager;
import me.lucko.luckperms.api.messenger.MessengerProvider;
import me.lucko.luckperms.api.metastacking.MetaStackFactory;
import me.lucko.luckperms.api.platform.PlatformInfo;
import me.lucko.luckperms.common.api.delegates.manager.ApiContextManager;
import me.lucko.luckperms.common.api.delegates.manager.ApiGroupManager;
import me.lucko.luckperms.common.api.delegates.manager.ApiTrackManager;
import me.lucko.luckperms.common.api.delegates.manager.ApiUserManager;
import me.lucko.luckperms.common.api.delegates.misc.ApiActionLogger;
import me.lucko.luckperms.common.api.delegates.misc.ApiMessagingService;
import me.lucko.luckperms.common.api.delegates.misc.ApiMetaStackFactory;
import me.lucko.luckperms.common.api.delegates.misc.ApiNodeFactory;
import me.lucko.luckperms.common.api.delegates.misc.ApiPlatformInfo;
import me.lucko.luckperms.common.api.delegates.misc.NoopUuidCache;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.messaging.LuckPermsMessagingService;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;

/**
 * Implements the LuckPerms API using the plugin instance
 */
public class LuckPermsApiProvider implements LuckPermsApi {

    private final LuckPermsPlugin plugin;

    private final PlatformInfo platformInfo;
    private final UserManager userManager;
    private final GroupManager groupManager;
    private final TrackManager trackManager;
    private final ActionLogger actionLogger;
    private final ContextManager contextManager;
    private final MetaStackFactory metaStackFactory;

    public LuckPermsApiProvider(LuckPermsPlugin plugin) {
        this.plugin = plugin;

        this.platformInfo = new ApiPlatformInfo(plugin);
        this.userManager = new ApiUserManager(plugin.getUserManager());
        this.groupManager = new ApiGroupManager(plugin.getGroupManager());
        this.trackManager = new ApiTrackManager(plugin.getTrackManager());
        this.actionLogger = new ApiActionLogger(plugin);
        this.contextManager = new ApiContextManager(plugin, plugin.getContextManager());
        this.metaStackFactory = new ApiMetaStackFactory(plugin);
    }

    @Nonnull
    @Override
    public PlatformInfo getPlatformInfo() {
        return this.platformInfo;
    }

    @Nonnull
    @Override
    public UserManager getUserManager() {
        return this.userManager;
    }

    @Nonnull
    @Override
    public GroupManager getGroupManager() {
        return this.groupManager;
    }

    @Nonnull
    @Override
    public TrackManager getTrackManager() {
        return this.trackManager;
    }

    @Nonnull
    @Override
    public CompletableFuture<Void> runUpdateTask() {
        return this.plugin.getUpdateTaskBuffer().request();
    }

    @Nonnull
    @Override
    public EventBus getEventBus() {
        return this.plugin.getEventFactory().getEventBus();
    }

    @Nonnull
    @Override
    public LPConfiguration getConfiguration() {
        return this.plugin.getConfiguration().getDelegate();
    }

    @Nonnull
    @Override
    public Storage getStorage() {
        return this.plugin.getStorage().getDelegate();
    }

    @Nonnull
    @Override
    public Optional<MessagingService> getMessagingService() {
        return this.plugin.getMessagingService().map(ApiMessagingService::new);
    }

    @Override
    public void registerMessengerProvider(@Nonnull MessengerProvider messengerProvider) {
        if (this.plugin.getConfiguration().get(ConfigKeys.MESSAGING_SERVICE).equals("custom")) {
            this.plugin.setMessagingService(new LuckPermsMessagingService(this.plugin, messengerProvider));
        }
    }

    @Override
    public ActionLogger getActionLogger() {
        return this.actionLogger;
    }

    @Nonnull
    @Override
    @Deprecated
    public UuidCache getUuidCache() {
        return NoopUuidCache.INSTANCE;
    }

    @Override
    public ContextManager getContextManager() {
        return this.contextManager;
    }

    @Nonnull
    @Override
    public NodeFactory getNodeFactory() {
        return ApiNodeFactory.INSTANCE;
    }

    @Nonnull
    @Override
    public MetaStackFactory getMetaStackFactory() {
        return this.metaStackFactory;
    }

}
