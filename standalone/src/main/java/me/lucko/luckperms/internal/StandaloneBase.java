/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

package me.lucko.luckperms.internal;

import lombok.Getter;
import me.lucko.luckperms.LPStandaloneApp;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.Logger;
import me.lucko.luckperms.api.PlatformType;
import me.lucko.luckperms.api.implementation.ApiProvider;
import me.lucko.luckperms.commands.ConsecutiveExecutor;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.config.LPConfiguration;
import me.lucko.luckperms.constants.Constants;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.contexts.ContextManager;
import me.lucko.luckperms.core.UuidCache;
import me.lucko.luckperms.data.Importer;
import me.lucko.luckperms.groups.GroupManager;
import me.lucko.luckperms.storage.Datastore;
import me.lucko.luckperms.tracks.TrackManager;
import me.lucko.luckperms.users.StandaloneUserManager;
import me.lucko.luckperms.users.UserManager;
import me.lucko.luckperms.utils.LocaleManager;
import me.lucko.luckperms.utils.LogFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
public class StandaloneBase implements LuckPermsPlugin {
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    private final java.util.logging.Logger logger;

    private final UserManager userManager;
    private final GroupManager groupManager;
    private final TrackManager trackManager;
    private final LPConfiguration configuration;
    private final Datastore datastore;
    private final Logger log;
    private final UuidCache uuidCache;
    private final ApiProvider apiProvider;
    private final LocaleManager localeManager;

    public StandaloneBase(LPStandaloneApp app) {
        logger = java.util.logging.Logger.getGlobal();
        log = LogFactory.wrap(logger);

        configuration = new StandaloneConfiguration(this);
        localeManager = new LocaleManager();

        // TODO datastore
        datastore = null;
        // Load everything, including all users.

        uuidCache = new UuidCache(true);
        userManager = new StandaloneUserManager(this);
        groupManager = new GroupManager(this);
        trackManager = new TrackManager();

        apiProvider = new ApiProvider(this);

        // TODO callback to the app, start gui stuff?
    }

    public void shutdown() {
        datastore.shutdown();
    }

    @Override
    public String getVersion() {
        return "2.9.0"; // TODO set dynamically
    }

    @Override
    public PlatformType getType() {
        return PlatformType.STANDALONE;
    }

    @Override
    public File getMainDir() {
        return null; // Is this needed? TODO
    }

    @Override
    public File getDataFolder() {
        return null; // Is this needed? TODO
    }

    @Override
    public void runUpdateTask() {
        // Is this needed?? TODO
    }

    @Override
    public void doAsync(Runnable r) {
        threadPool.execute(r);
    }

    @Override
    public void doSync(Runnable r) {
        threadPool.execute(r);
    }



    /*
     * Methods below are only required in plugins.
     * They're just left empty / default.
     */


    @Override
    public Importer getImporter() {
        return null;
    }

    @Override
    public ConsecutiveExecutor getConsecutiveExecutor() {
        return null;
    }

    @Override
    public ContextManager getContextManager() {
        return null;
    }

    @Override
    public Message getPlayerStatus(UUID uuid) {
        return Message.PLAYER_OFFLINE;
    }

    @Override
    public int getPlayerCount() {
        return 0;
    }

    @Override
    public List<String> getPlayerList() {
        return Collections.emptyList();
    }

    @Override
    public List<Sender> getNotifyListeners() {
        return Collections.emptyList();
    }

    @Override
    public Sender getConsoleSender() {
        final LuckPermsPlugin instance = this;
        return new Sender() {
            @Override
            public LuckPermsPlugin getPlatform() {
                return instance;
            }

            @Override
            public String getName() {
                return Constants.getConsoleName();
            }

            @Override
            public UUID getUuid() {
                return Constants.getConsoleUUID();
            }

            @Override
            public void sendMessage(String s) {
                getLogger().info(s);
            }

            @Override
            public boolean hasPermission(Permission permission) {
                return true;
            }
        };
    }

    @Override
    public List<String> getPossiblePermissions() {
        return Collections.emptyList();
    }

    @Override
    public Set<UUID> getIgnoringLogs() {
        return Collections.emptySet();
    }

    @Override
    public Object getPlugin(String name) {
        return null;
    }

    @Override
    public Object getService(Class clazz) {
        return null;
    }

    @Override
    public UUID getUUID(String playerName) {
        return null;
    }

    @Override
    public boolean isPluginLoaded(String name) {
        return false;
    }
}
