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

package me.lucko.luckperms.common.plugin;

import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.common.actionlog.LogDispatcher;
import me.lucko.luckperms.common.api.ApiRegistrationUtil;
import me.lucko.luckperms.common.api.LuckPermsApiProvider;
import me.lucko.luckperms.common.buffers.BufferedRequest;
import me.lucko.luckperms.common.buffers.UpdateTaskBuffer;
import me.lucko.luckperms.common.caching.handlers.CachedStateManager;
import me.lucko.luckperms.common.calculators.CalculatorFactory;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.CommandUtils;
import me.lucko.luckperms.common.config.AbstractConfiguration;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.config.LuckPermsConfiguration;
import me.lucko.luckperms.common.config.adapter.ConfigurationAdapter;
import me.lucko.luckperms.common.contexts.LuckPermsCalculator;
import me.lucko.luckperms.common.dependencies.DependencyManager;
import me.lucko.luckperms.common.dependencies.DependencyRegistry;
import me.lucko.luckperms.common.event.EventFactory;
import me.lucko.luckperms.common.inheritance.InheritanceHandler;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.SimpleLocaleManager;
import me.lucko.luckperms.common.logging.Logger;
import me.lucko.luckperms.common.logging.SenderLogger;
import me.lucko.luckperms.common.messaging.InternalMessagingService;
import me.lucko.luckperms.common.messaging.MessagingFactory;
import me.lucko.luckperms.common.storage.Storage;
import me.lucko.luckperms.common.storage.StorageFactory;
import me.lucko.luckperms.common.storage.StorageType;
import me.lucko.luckperms.common.storage.dao.file.FileWatcher;
import me.lucko.luckperms.common.tasks.UpdateTask;
import me.lucko.luckperms.common.treeview.PermissionVault;
import me.lucko.luckperms.common.verbose.VerboseHandler;

import java.io.File;
import java.util.Optional;
import java.util.Set;

public abstract class AbstractLuckPermsPlugin implements LuckPermsPlugin {

    // init during load
    private Logger logger;
    private DependencyManager dependencyManager;

    // init during enable
    private VerboseHandler verboseHandler;
    private PermissionVault permissionVault;
    private LogDispatcher logDispatcher;
    private LuckPermsConfiguration configuration;
    private LocaleManager localeManager;
    private FileWatcher fileWatcher = null;
    private Storage storage;
    private InternalMessagingService messagingService = null;
    private BufferedRequest<Void> updateTaskBuffer;
    private InheritanceHandler inheritanceHandler;
    private CachedStateManager cachedStateManager;
    private CalculatorFactory calculatorFactory;
    private LuckPermsApiProvider apiProvider;
    private EventFactory eventFactory;

    /**
     * Performs the initial actions to load the plugin
     */
    public final void load() {
        // load the sender factory instance and create a new logger for the plugin
        setupSenderFactory();
        this.logger = new SenderLogger(this, getConsoleSender());

        // load dependencies
        this.dependencyManager = new DependencyManager(this);
        this.dependencyManager.loadDependencies(DependencyRegistry.GLOBAL_DEPENDENCIES);
    }

    public final void enable() {
        // send the startup banner
        displayBanner(getConsoleSender());

        // load some utilities early
        this.verboseHandler = new VerboseHandler(getBootstrap().getScheduler().platformAsync());
        this.permissionVault = new PermissionVault(getBootstrap().getScheduler().platformAsync());
        this.logDispatcher = new LogDispatcher(this);

        // load configuration
        getLogger().info("Loading configuration...");
        this.configuration = new AbstractConfiguration(this, provideConfigurationAdapter());
        this.configuration.loadAll();

        // load locale
        this.localeManager = new SimpleLocaleManager();
        this.localeManager.tryLoad(this, new File(getBootstrap().getConfigDirectory(), "lang.yml"));

        // now the configuration is loaded, we can create a storage factory and load initial dependencies
        StorageFactory storageFactory = new StorageFactory(this);
        Set<StorageType> storageTypes = storageFactory.getRequiredTypes(StorageType.H2);
        this.dependencyManager.loadStorageDependencies(storageTypes);

        // register listeners
        registerPlatformListeners();

        // initialise the storage
        // first, setup the file watcher, if enabled
        if (getConfiguration().get(ConfigKeys.WATCH_FILES)) {
            this.fileWatcher = new FileWatcher(this);
            getBootstrap().getScheduler().asyncRepeating(this.fileWatcher, 30L);
        }

        // initialise storage
        this.storage = storageFactory.getInstance(StorageType.H2);
        this.messagingService = provideMessagingFactory().getInstance();

        // setup the update task buffer
        this.updateTaskBuffer = new UpdateTaskBuffer(this);

        // register commands
        registerCommands();

        // load internal managers
        getLogger().info("Loading internal permission managers...");
        this.inheritanceHandler = new InheritanceHandler(this);
        this.cachedStateManager = new CachedStateManager();

        // setup user/group/track manager
        setupManagers();

        // init calculator factory
        this.calculatorFactory = provideCalculatorFactory();

        // setup contextmanager & register common calculators
        setupContextManager();
        getContextManager().registerStaticCalculator(new LuckPermsCalculator(getConfiguration()));

        // setup platform hooks
        setupPlatformHooks();

        // register with the LP API
        this.apiProvider = new LuckPermsApiProvider(this);
        this.eventFactory = new EventFactory(this, this.apiProvider);
        ApiRegistrationUtil.registerProvider(this.apiProvider);
        registerApiOnPlatform(this.apiProvider);

        // schedule update tasks
        int mins = getConfiguration().get(ConfigKeys.SYNC_TIME);
        if (mins > 0) {
            long ticks = mins * 60 * 20;
            getBootstrap().getScheduler().asyncRepeating(() -> this.updateTaskBuffer.request(), ticks);
        }
        getBootstrap().getScheduler().asyncLater(() -> this.updateTaskBuffer.request(), 40L);

        // run an update instantly.
        getLogger().info("Performing initial data load...");
        try {
            new UpdateTask(this, true).run();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // init housekeeping tasks
        registerHousekeepingTasks();

        // perform any platform-specific final setup tasks
        performFinalSetup();

        getLogger().info("Successfully enabled. (took " + (System.currentTimeMillis() - getBootstrap().getStartupTime()) + "ms)");
    }

    public final void disable() {
        // perform initial disable tasks
        performEarlyDisableTasks();

        // shutdown permission vault and verbose handler tasks
        this.permissionVault.shutdown();
        this.verboseHandler.shutdown();

        // remove any hooks into the platform
        removePlatformHooks();

        // close storage
        getLogger().info("Closing storage...");
        this.storage.shutdown();

        // close file watcher
        if (this.fileWatcher != null) {
            this.fileWatcher.close();
        }

        // close messaging service
        if (this.messagingService != null) {
            getLogger().info("Closing messaging service...");
            this.messagingService.close();
        }

        // unregister api
        ApiRegistrationUtil.unregisterProvider();

        // shutdown scheduler
        getLogger().info("Shutting down internal scheduler...");
        getBootstrap().getScheduler().shutdown();

        getLogger().info("Goodbye!");
    }

    protected abstract void setupSenderFactory();
    protected abstract ConfigurationAdapter provideConfigurationAdapter();
    protected abstract void registerPlatformListeners();
    protected abstract MessagingFactory<?> provideMessagingFactory();
    protected abstract void registerCommands();
    protected abstract void setupManagers();
    protected abstract CalculatorFactory provideCalculatorFactory();
    protected abstract void setupContextManager();
    protected abstract void setupPlatformHooks();
    protected abstract void registerApiOnPlatform(LuckPermsApi api);
    protected abstract void registerHousekeepingTasks();
    protected abstract void performFinalSetup();

    protected void performEarlyDisableTasks() {}
    protected void removePlatformHooks() {}

    @Override
    public void setMessagingService(InternalMessagingService messagingService) {
        if (this.messagingService == null) {
            this.messagingService = messagingService;
        }
    }

    @Override
    public Logger getLogger() {
        return this.logger;
    }

    @Override
    public DependencyManager getDependencyManager() {
        return this.dependencyManager;
    }

    @Override
    public VerboseHandler getVerboseHandler() {
        return this.verboseHandler;
    }

    @Override
    public PermissionVault getPermissionVault() {
        return this.permissionVault;
    }

    @Override
    public LogDispatcher getLogDispatcher() {
        return this.logDispatcher;
    }

    @Override
    public LuckPermsConfiguration getConfiguration() {
        return this.configuration;
    }

    @Override
    public LocaleManager getLocaleManager() {
        return this.localeManager;
    }

    @Override
    public Optional<FileWatcher> getFileWatcher() {
        return Optional.ofNullable(this.fileWatcher);
    }

    @Override
    public Storage getStorage() {
        return this.storage;
    }

    @Override
    public Optional<InternalMessagingService> getMessagingService() {
        return Optional.ofNullable(this.messagingService);
    }

    @Override
    public BufferedRequest<Void> getUpdateTaskBuffer() {
        return this.updateTaskBuffer;
    }

    @Override
    public InheritanceHandler getInheritanceHandler() {
        return this.inheritanceHandler;
    }

    @Override
    public CachedStateManager getCachedStateManager() {
        return this.cachedStateManager;
    }

    @Override
    public CalculatorFactory getCalculatorFactory() {
        return this.calculatorFactory;
    }

    @Override
    public LuckPermsApiProvider getApiProvider() {
        return this.apiProvider;
    }

    @Override
    public EventFactory getEventFactory() {
        return this.eventFactory;
    }

    private void displayBanner(Sender sender) {
        sender.sendMessage(CommandUtils.color("&b               __       &3 __   ___  __         __  "));
        sender.sendMessage(CommandUtils.color("&b    |    |  | /  ` |__/ &3|__) |__  |__)  |\\/| /__` "));
        sender.sendMessage(CommandUtils.color("&b    |___ \\__/ \\__, |  \\ &3|    |___ |  \\  |  | .__/ "));
        sender.sendMessage(CommandUtils.color(" "));
        sender.sendMessage(CommandUtils.color("&2  Loading version &bv" + getBootstrap().getVersion() + "&2 on " + getBootstrap().getType().getFriendlyName() + " - " + getBootstrap().getServerBrand()));
        sender.sendMessage(CommandUtils.color("&8  Running on server version " + getBootstrap().getServerVersion()));
        sender.sendMessage(CommandUtils.color(" "));
    }
}
