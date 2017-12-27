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

import lombok.AccessLevel;
import lombok.Getter;

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
import me.lucko.luckperms.api.metastacking.MetaStackFactory;
import me.lucko.luckperms.api.platform.PlatformInfo;
import me.lucko.luckperms.common.api.delegates.manager.ApiContextManager;
import me.lucko.luckperms.common.api.delegates.manager.ApiGroupManager;
import me.lucko.luckperms.common.api.delegates.manager.ApiTrackManager;
import me.lucko.luckperms.common.api.delegates.manager.ApiUserManager;
import me.lucko.luckperms.common.api.delegates.misc.ApiActionLogger;
import me.lucko.luckperms.common.api.delegates.misc.ApiMetaStackFactory;
import me.lucko.luckperms.common.api.delegates.misc.ApiNodeFactory;
import me.lucko.luckperms.common.api.delegates.misc.ApiPlatformInfo;
import me.lucko.luckperms.common.event.EventFactory;
import me.lucko.luckperms.common.event.LuckPermsEventBus;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Implements the LuckPerms API using the plugin instance
 */
public class ApiProvider implements LuckPermsApi {

    @Getter(AccessLevel.NONE)
    private final LuckPermsPlugin plugin;

    private final PlatformInfo platformInfo;
    private final UserManager userManager;
    private final GroupManager groupManager;
    private final TrackManager trackManager;
    private final LuckPermsEventBus eventBus;
    private final ActionLogger actionLogger;
    private final ContextManager contextManager;
    private final MetaStackFactory metaStackFactory;
    private final EventFactory eventFactory;

    public ApiProvider(LuckPermsPlugin plugin) {
        this.plugin = plugin;

        this.platformInfo = new ApiPlatformInfo(plugin);
        this.userManager = new ApiUserManager(plugin, plugin.getUserManager());
        this.groupManager = new ApiGroupManager(plugin.getGroupManager());
        this.trackManager = new ApiTrackManager(plugin.getTrackManager());
        this.eventBus = new LuckPermsEventBus(plugin);
        this.actionLogger = new ApiActionLogger(plugin);
        this.contextManager = new ApiContextManager(plugin, plugin.getContextManager());
        this.metaStackFactory = new ApiMetaStackFactory(plugin);
        this.eventFactory = new EventFactory(eventBus);
    }

    public EventFactory getEventFactory() {
        return eventFactory;
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return platformInfo;
    }

    @Override
    public UserManager getUserManager() {
        return userManager;
    }

    @Override
    public GroupManager getGroupManager() {
        return groupManager;
    }

    @Override
    public TrackManager getTrackManager() {
        return trackManager;
    }

    @Override
    public CompletableFuture<Void> runUpdateTask() {
        return plugin.getUpdateTaskBuffer().request();
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public LPConfiguration getConfiguration() {
        return plugin.getConfiguration().getDelegate();
    }

    @Override
    public Storage getStorage() {
        return plugin.getStorage().getDelegate();
    }

    @Override
    public Optional<MessagingService> getMessagingService() {
        return plugin.getMessagingService().map(Function.identity());
    }

    @Override
    public ActionLogger getActionLogger() {
        return actionLogger;
    }

    @Override
    public UuidCache getUuidCache() {
        return plugin.getUuidCache().getDelegate();
    }

    @Override
    public ContextManager getContextManager() {
        return contextManager;
    }

    @Override
    public NodeFactory getNodeFactory() {
        return ApiNodeFactory.INSTANCE;
    }

    @Override
    public MetaStackFactory getMetaStackFactory() {
        return metaStackFactory;
    }

}
