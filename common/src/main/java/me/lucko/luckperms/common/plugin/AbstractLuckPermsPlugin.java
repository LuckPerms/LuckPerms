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
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.config.LuckPermsConfiguration;
import me.lucko.luckperms.common.config.generic.adapter.ConfigurationAdapter;
import me.lucko.luckperms.common.context.ConfigurationContextCalculator;
import me.lucko.luckperms.common.dependencies.Dependency;
import me.lucko.luckperms.common.dependencies.DependencyManager;
import me.lucko.luckperms.common.event.AbstractEventBus;
import me.lucko.luckperms.common.event.EventDispatcher;
import me.lucko.luckperms.common.event.gen.GeneratedEventClass;
import me.lucko.luckperms.common.extension.SimpleExtensionManager;
import me.lucko.luckperms.common.http.BytebinClient;
import me.lucko.luckperms.common.inheritance.InheritanceGraphFactory;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.messaging.InternalMessagingService;
import me.lucko.luckperms.common.messaging.MessagingFactory;
import me.lucko.luckperms.common.plugin.logging.PluginLogger;
import me.lucko.luckperms.common.storage.Storage;
import me.lucko.luckperms.common.storage.StorageFactory;
import me.lucko.luckperms.common.storage.StorageType;
import me.lucko.luckperms.common.storage.implementation.file.watcher.FileWatcher;
import me.lucko.luckperms.common.tasks.SyncTask;
import me.lucko.luckperms.common.treeview.PermissionRegistry;
import me.lucko.luckperms.common.verbose.VerboseHandler;

import net.luckperms.api.LuckPerms;

import okhttp3.OkHttpClient;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public abstract class AbstractLuckPermsPlugin implements LuckPermsPlugin {

    // init during load
    private DependencyManager dependencyManager;
    private TranslationManager translationManager;

    // init during enable
    private VerboseHandler verboseHandler;
    private PermissionRegistry permissionRegistry;
    private LogDispatcher logDispatcher;
    private LuckPermsConfiguration configuration;
    private BytebinClient bytebin;
    private FileWatcher fileWatcher = null;
    private Storage storage;
    private InternalMessagingService messagingService = null;
    private SyncTask.Buffer syncTaskBuffer;
    private InheritanceGraphFactory inheritanceGraphFactory;
    private CalculatorFactory calculatorFactory;
    private LuckPermsApiProvider apiProvider;
    private EventDispatcher eventDispatcher;
    private SimpleExtensionManager extensionManager;

    /**
     * Performs the initial actions to load the plugin
     */
    public final void load() {
        // load dependencies
        this.dependencyManager = new DependencyManager(this);
        this.dependencyManager.loadDependencies(getGlobalDependencies());

        this.translationManager = new TranslationManager(this);
        this.translationManager.load();
    }

    public final void enable() {
        // load the sender factory instance
        setupSenderFactory();

        // send the startup banner
        Message.STARTUP_BANNER.send(getConsoleSender(), getBootstrap());

        // load some utilities early
        this.verboseHandler = new VerboseHandler(getBootstrap().getScheduler());
        this.permissionRegistry = new PermissionRegistry(getBootstrap().getScheduler());
        this.logDispatcher = new LogDispatcher(this);

        // load configuration
        getLogger().info("Loading configuration...");
        this.configuration = new LuckPermsConfiguration(this, provideConfigurationAdapter());

        // setup a bytebin instance
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .callTimeout(15, TimeUnit.SECONDS)
                .build();

        this.bytebin = new BytebinClient(httpClient, getConfiguration().get(ConfigKeys.BYTEBIN_URL), "luckperms");

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
            } catch (Throwable e) {
                // catch throwable here, seems some JVMs throw UnsatisfiedLinkError when trying
                // to create a watch service. see: https://github.com/lucko/LuckPerms/issues/2066
                getLogger().warn("Error occurred whilst trying to create a file watcher:");
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
        this.inheritanceGraphFactory = new InheritanceGraphFactory(this);

        // setup user/group/track manager
        setupManagers();

        // init calculator factory
        this.calculatorFactory = provideCalculatorFactory();

        // setup contextmanager & register common calculators
        setupContextManager();
        getContextManager().registerCalculator(new ConfigurationContextCalculator(getConfiguration()));

        // setup platform hooks
        setupPlatformHooks();

        // register with the LP API
        this.apiProvider = new LuckPermsApiProvider(this);
        this.eventDispatcher = new EventDispatcher(provideEventBus(this.apiProvider));
        getBootstrap().getScheduler().executeAsync(GeneratedEventClass::preGenerate);
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
                Dependency.ADVENTURE,
                Dependency.CAFFEINE,
                Dependency.OKIO,
                Dependency.OKHTTP,
                Dependency.BYTEBUDDY,
                Dependency.EVENT
        );
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
    public TranslationManager getTranslationManager() {
        return this.translationManager;
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
    public InheritanceGraphFactory getInheritanceGraphFactory() {
        return this.inheritanceGraphFactory;
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

    public static String getPluginName() {
        LocalDate date = LocalDate.now();
        if (date.getMonth() == Month.APRIL && date.getDayOfMonth() == 1) {
            return "LuckyPerms";
        }
        return "LuckPerms";
    }
}
