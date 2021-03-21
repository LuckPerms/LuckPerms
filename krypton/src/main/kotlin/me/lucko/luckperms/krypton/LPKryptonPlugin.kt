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

package me.lucko.luckperms.krypton

import me.lucko.luckperms.common.api.LuckPermsApiProvider
import me.lucko.luckperms.common.command.CommandManager
import me.lucko.luckperms.common.config.ConfigKeys
import me.lucko.luckperms.common.model.User
import me.lucko.luckperms.common.model.manager.group.StandardGroupManager
import me.lucko.luckperms.common.model.manager.track.StandardTrackManager
import me.lucko.luckperms.common.model.manager.user.StandardUserManager
import me.lucko.luckperms.common.plugin.AbstractLuckPermsPlugin
import me.lucko.luckperms.common.sender.Sender
import me.lucko.luckperms.common.tasks.CacheHousekeepingTask
import me.lucko.luckperms.common.tasks.ExpireTemporaryTask
import me.lucko.luckperms.krypton.calculator.KryptonCalculatorFactory
import me.lucko.luckperms.krypton.command.KryptonCommandExecutor
import me.lucko.luckperms.krypton.context.KryptonContextManager
import me.lucko.luckperms.krypton.context.KryptonPlayerCalculator
import me.lucko.luckperms.krypton.event.KryptonEventBus
import me.lucko.luckperms.krypton.listeners.KryptonConnectionListener
import me.lucko.luckperms.krypton.listeners.KryptonPermissionCheckListener
import me.lucko.luckperms.krypton.messaging.KryptonMessagingFactory
import net.luckperms.api.LuckPerms
import net.luckperms.api.query.QueryOptions
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

class LPKryptonPlugin(private val bootstrap: LPKryptonBootstrap) : AbstractLuckPermsPlugin() {

    lateinit var senderFactory: KryptonSenderFactory
    private val connectionListener = KryptonConnectionListener(this)

    private lateinit var commandManager: CommandManager
    private lateinit var userManager: StandardUserManager
    private lateinit var groupManager: StandardGroupManager
    private lateinit var trackManager: StandardTrackManager
    private lateinit var contextManager: KryptonContextManager

    override fun getBootstrap() = bootstrap

    override fun setupSenderFactory() {
        senderFactory = KryptonSenderFactory(this)
    }

    override fun provideConfigurationAdapter() = KryptonConfigAdapter(this, resolveConfig("config.conf").toFile())

    override fun registerPlatformListeners() {
        bootstrap.registerListener(connectionListener)
        bootstrap.registerListener(KryptonPermissionCheckListener(this))
    }

    override fun provideMessagingFactory() = KryptonMessagingFactory(this)

    override fun registerCommands() {
        commandManager = CommandManager(this)
        KryptonCommandExecutor(this, commandManager).register()
    }

    override fun setupManagers() {
        userManager = StandardUserManager(this)
        groupManager = StandardGroupManager(this)
        trackManager = StandardTrackManager(this)
    }

    override fun provideCalculatorFactory() = KryptonCalculatorFactory(this)

    override fun setupContextManager() {
        contextManager = KryptonContextManager(this)
        val playerCalculator = KryptonPlayerCalculator(this, configuration.get(ConfigKeys.DISABLED_CONTEXTS))
        contextManager.registerCalculator(playerCalculator)
    }

    override fun setupPlatformHooks() = Unit

    override fun provideEventBus(apiProvider: LuckPermsApiProvider) = KryptonEventBus(this, apiProvider)

    override fun registerApiOnPlatform(api: LuckPerms) = Unit // no services managers here

    override fun registerHousekeepingTasks() {
        bootstrap.scheduler.asyncRepeating(ExpireTemporaryTask(this), 3, TimeUnit.SECONDS)
        bootstrap.scheduler.asyncRepeating(CacheHousekeepingTask(this), 2, TimeUnit.MINUTES)
    }

    override fun performFinalSetup() = Unit

    override fun getQueryOptionsForUser(user: User): Optional<QueryOptions?> = bootstrap.getPlayer(user.uniqueId)
        .map { contextManager.getQueryOptions(it) }

    override fun getOnlineSenders(): Stream<Sender> = Stream.concat(
        Stream.of(consoleSender),
        bootstrap.server.players.stream().map { senderFactory.wrap(it) }
    )

    override fun getConsoleSender(): Sender = senderFactory.wrap(bootstrap.server.console)

    override fun getConnectionListener() = connectionListener

    override fun getCommandManager() = commandManager

    override fun getUserManager() = userManager

    override fun getGroupManager() = groupManager

    override fun getTrackManager() = trackManager

    override fun getContextManager() = contextManager
}