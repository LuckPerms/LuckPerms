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

import me.lucko.luckperms.common.actionlog.LogDispatcher;
import me.lucko.luckperms.common.api.ApiRegistrationUtil;
import me.lucko.luckperms.common.api.LuckPermsApiProvider;
import me.lucko.luckperms.common.calculator.CalculatorFactory;
import me.lucko.luckperms.common.config.AbstractConfiguration;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.config.LuckPermsConfiguration;
import me.lucko.luckperms.common.config.adapter.ConfigurationAdapter;
import me.lucko.luckperms.common.context.LPStaticContextsCalculator;
import me.lucko.luckperms.common.dependencies.Dependency;
import me.lucko.luckperms.common.dependencies.DependencyManager;
import me.lucko.luckperms.common.event.AbstractEventBus;
import me.lucko.luckperms.common.event.EventDispatcher;
import me.lucko.luckperms.common.extension.SimpleExtensionManager;
import me.lucko.luckperms.common.inheritance.InheritanceHandler;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.messaging.InternalMessagingService;
import me.lucko.luckperms.common.messaging.MessagingFactory;
import me.lucko.luckperms.common.plugin.logging.PluginLogger;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.storage.Storage;
import me.lucko.luckperms.common.storage.StorageFactory;
import me.lucko.luckperms.common.storage.StorageType;
import me.lucko.luckperms.common.storage.implementation.file.watcher.FileWatcher;
import me.lucko.luckperms.common.tasks.SyncTask;
import me.lucko.luckperms.common.treeview.PermissionRegistry;
import me.lucko.luckperms.common.verbose.VerboseHandler;
import me.lucko.luckperms.common.web.BytebinClient;
import net.luckperms.api.LuckPerms;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Represents an implementation of a LuckPermsPlugin for a modded context where the plugin must be loaded very early on.
 */
public abstract class AbstractLuckPermsMod implements LuckPermsPlugin {

    // init during load
    private DependencyManager dependencyManager;

    // init during enable
    private VerboseHandler verboseHandler;
    private PermissionRegistry permissionRegistry;
    private LogDispatcher logDispatcher;
    private LuckPermsConfiguration configuration;
    private LocaleManager localeManager;
    private BytebinClient bytebin;
    private FileWatcher fileWatcher = null;
    private Storage storage;
    private InternalMessagingService messagingService = null;
    private SyncTask.Buffer syncTaskBuffer;
    private InheritanceHandler inheritanceHandler;
    private CalculatorFactory calculatorFactory;
    private LuckPermsApiProvider apiProvider;
    private EventDispatcher eventDispatcher;
    private SimpleExtensionManager extensionManager;

    /**
     * Performs the initial actions to load the plugin
     */
    public final void load() {
        this.dependencyManager = new DependencyManager(this);
        this.dependencyManager.loadDependencies(getGlobalDependencies());
        initializeMod();
        // load the sender factory instance
        setupSenderFactory();
    }

    public final void enable() {
        // send the startup banner
        displayBanner(getConsoleSender());

        // load some utilities early
        this.verboseHandler = new VerboseHandler(getBootstrap().getScheduler());
        this.permissionRegistry = new PermissionRegistry(getBootstrap().getScheduler());
        this.logDispatcher = new LogDispatcher(this);

        // load configuration
        getLogger().info("Loading configuration...");
        this.configuration = new AbstractConfiguration(this, provideConfigurationAdapter());

        // load locale
        this.localeManager = new LocaleManager();
        this.localeManager.tryLoad(this, getBootstrap().getConfigDirectory().resolve("lang.yml"));

        // setup a bytebin instance
        this.bytebin = new BytebinClient(new OkHttpClient(), getConfiguration().get(ConfigKeys.BYTEBIN_URL), "luckperms");

        // now the configuration is loaded, we can create a storage factory and load initial dependencies
        StorageFactory storageFactory = new StorageFactory(this);
        Set<StorageType> storageTypes = storageFactory.getRequiredTypes();
        this.dependencyManager.loadStorageDependencies(storageTypes);

        // register listeners
        registerPlatformListeners();

        // initialise the storage
        // first, setup the file watcher, if enabled
        if (getConfiguration().get(ConfigKeys.WATCH_FILES)) {
            try {
                this.fileWatcher = new FileWatcher(this, getBootstrap().getDataDirectory());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // initialise storage
        this.storage = storageFactory.getInstance();
        this.messagingService = provideMessagingFactory().getInstance();

        // setup the update task buffer
        this.syncTaskBuffer = new SyncTask.Buffer(this);

        // register commands
        registerCommands();

        // load internal managers
        getLogger().info("Loading internal permission managers...");
        this.inheritanceHandler = new InheritanceHandler(this);

        // setup user/group/track manager
        setupManagers();

        // init calculator factory
        this.calculatorFactory = provideCalculatorFactory();

        // setup contextmanager & register common calculators
        setupContextManager();
        getContextManager().registerCalculator(new LPStaticContextsCalculator(getConfiguration()));

        // setup platform hooks
        setupPlatformHooks();

        // register with the LP API
        this.apiProvider = new LuckPermsApiProvider(this);
        this.eventDispatcher = new EventDispatcher(provideEventBus(this.apiProvider));
        ApiRegistrationUtil.registerProvider(this.apiProvider);
        registerApiOnPlatform(this.apiProvider);

        // setup extension manager
        this.extensionManager = new SimpleExtensionManager(this);
        this.extensionManager.loadExtensions(getBootstrap().getConfigDirectory().resolve("extensions"));

        // schedule update tasks
        int mins = getConfiguration().get(ConfigKeys.SYNC_TIME);
        if (mins > 0) {
            getBootstrap().getScheduler().asyncRepeating(() -> this.syncTaskBuffer.request(), mins, TimeUnit.MINUTES);
        }

        // run an update instantly.
        getLogger().info("Performing initial data load...");
        try {
            new SyncTask(this).run();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // init housekeeping tasks
        registerHousekeepingTasks();

        // perform any platform-specific final setup tasks
        performFinalSetup();

        Duration timeTaken = Duration.between(getBootstrap().getStartupTime(), Instant.now());
        getLogger().info("Successfully enabled. (took " + timeTaken.toMillis() + "ms)");
    }

    public final void disable() {
        getLogger().info("Starting shutdown process...");

        // cancel delayed/repeating tasks
        getBootstrap().getScheduler().shutdownScheduler();

        // shutdown permission vault and verbose handler tasks
        this.permissionRegistry.close();
        this.verboseHandler.close();

        // unload extensions
        this.extensionManager.close();

        // remove any hooks into the platform
        removePlatformHooks();

        // close messaging service
        if (this.messagingService != null) {
            getLogger().info("Closing messaging service...");
            this.messagingService.close();
        }

        // close storage
        getLogger().info("Closing storage...");
        this.storage.shutdown();

        // close file watcher
        if (this.fileWatcher != null) {
            this.fileWatcher.close();
        }

        // unregister api
        ApiRegistrationUtil.unregisterProvider();

        // shutdown async executor pool
        getBootstrap().getScheduler().shutdownExecutor();

        getLogger().info("Goodbye!");
    }

    protected Set<Dependency> getGlobalDependencies() {
        return EnumSet.of(
                Dependency.TEXT,
                Dependency.TEXT_SERIALIZER_GSON,
                Dependency.TEXT_SERIALIZER_LEGACY,
                Dependency.CAFFEINE,
                Dependency.OKIO,
                Dependency.OKHTTP,
                Dependency.EVENT
        );
    }

    protected abstract void setupSenderFactory();
    protected abstract ConfigurationAdapter provideConfigurationAdapter();
    protected abstract void registerPlatformListeners();
    protected abstract MessagingFactory<?> provideMessagingFactory();
    protected abstract void registerCommands();
    protected abstract void setupManagers();
    protected abstract void initializeMod();
    protected abstract CalculatorFactory provideCalculatorFactory();
    protected abstract void setupContextManager();
    protected abstract void setupPlatformHooks();
    protected abstract AbstractEventBus<?> provideEventBus(LuckPermsApiProvider apiProvider);
    protected abstract void registerApiOnPlatform(LuckPerms api);
    protected abstract void registerHousekeepingTasks();
    protected abstract void performFinalSetup();

    protected void removePlatformHooks() {}

    @Override
    public PluginLogger getLogger() {
        return getBootstrap().getPluginLogger();
    }

    @Override
    public void setMessagingService(InternalMessagingService messagingService) {
        if (this.messagingService == null) {
            this.messagingService = messagingService;
        }
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
    public PermissionRegistry getPermissionRegistry() {
        return this.permissionRegistry;
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
    public BytebinClient getBytebin() {
        return this.bytebin;
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
    public SyncTask.Buffer getSyncTaskBuffer() {
        return this.syncTaskBuffer;
    }

    @Override
    public InheritanceHandler getInheritanceHandler() {
        return this.inheritanceHandler;
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
    public SimpleExtensionManager getExtensionManager() {
        return this.extensionManager;
    }

    @Override
    public EventDispatcher getEventDispatcher() {
        return this.eventDispatcher;
    }

    private void displayBanner(Sender sender) {
        sender.sendMessage("        __    ");
        sender.sendMessage("  |    |__)   " + "LuckPerms v" + getBootstrap().getVersion());
        sender.sendMessage("  |___ |      " + "Running on " + getBootstrap().getType().getFriendlyName() + " (" + getBootstrap().getEnvironment().getFriendlyName() + ")" + " - " + getBootstrap().getServerBrand());
        sender.sendMessage("");
    }
}
