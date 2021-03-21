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

import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap
import me.lucko.luckperms.common.plugin.logging.Log4jPluginLogger
import net.luckperms.api.platform.Platform
import org.kryptonmc.krypton.api.plugin.Plugin
import org.kryptonmc.krypton.api.plugin.PluginContext
import java.nio.file.Path
import java.time.Instant
import java.util.*
import java.util.concurrent.CountDownLatch

/**
 * Bootstrap plugin for LuckPerms running on Krypton.
 */
class LPKryptonBootstrap(context: PluginContext) : Plugin(context), LuckPermsBootstrap {

    val server = context.server

    private val plugin = LPKryptonPlugin(this)

    private val loadLatch = CountDownLatch(1)
    private val enableLatch = CountDownLatch(1)

    private lateinit var startTime: Instant

    private var serverStarting = false
    private var serverStopping = false

    init {
        try {
            plugin.load()
        } finally {
            loadLatch.countDown()
        }
    }

    override suspend fun initialize() {
        serverStarting = true
        serverStopping = false
        startTime = Instant.now()
        try {
            plugin.enable()
            server.scheduler.run(this) { serverStarting = false }
        } finally {
            enableLatch.countDown()
        }
    }

    override fun shutdown() {
        serverStopping = true
        plugin.disable()
    }

    override fun getPluginLogger() = Log4jPluginLogger(context.logger)

    override fun getScheduler() = KryptonSchedulerAdapter(this, server.scheduler)

    override fun getClassPathAppender() = KryptonClassPathAppender(this)

    override fun getLoadLatch() = loadLatch

    override fun getEnableLatch() = enableLatch

    override fun getVersion() = context.description.version

    override fun getStartupTime() = startTime

    override fun getType() = Platform.Type.KRYPTON

    override fun getServerBrand() = server.info.name

    override fun getServerVersion() = "${server.info.minecraftVersion} - ${server.info.version}"

    override fun getDataDirectory(): Path = context.folder.toPath()

    override fun getPlayer(uniqueId: UUID) = Optional.ofNullable(server.player(uniqueId))

    override fun lookupUniqueId(username: String) = Optional.ofNullable(server.player(username)?.uuid)

    override fun lookupUsername(uniqueId: UUID) = Optional.ofNullable(server.player(uniqueId)?.name)

    override fun getPlayerCount() = server.players.size

    override fun getPlayerList() = server.players.map { it.name }

    override fun getOnlinePlayers() = server.players.map { it.uuid }

    override fun isPlayerOnline(uniqueId: UUID) = uniqueId in server.players.map { it.uuid }
}