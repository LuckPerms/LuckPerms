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

package me.lucko.luckperms.krypton.listeners

import me.lucko.luckperms.common.config.ConfigKeys
import me.lucko.luckperms.common.locale.Message
import me.lucko.luckperms.common.locale.TranslationManager
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener
import me.lucko.luckperms.krypton.LPKryptonPlugin
import net.kyori.adventure.text.Component
import org.kryptonmc.krypton.api.event.Listener
import org.kryptonmc.krypton.api.event.ListenerPriority
import org.kryptonmc.krypton.api.event.events.login.JoinEvent
import org.kryptonmc.krypton.api.event.events.login.LoginEvent
import org.kryptonmc.krypton.api.event.events.play.QuitEvent
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit

class KryptonConnectionListener(private val plugin: LPKryptonPlugin) : AbstractConnectionListener(plugin) {

    private val deniedAsyncLogin = mutableSetOf<UUID>()
    private val deniedLogin = mutableSetOf<UUID>()

    @Listener(ListenerPriority.HIGH)
    fun onLogin(event: LoginEvent) {
        plugin.bootstrap.enableLatch.await(60, TimeUnit.SECONDS)

        if (plugin.configuration.get(ConfigKeys.DEBUG_LOGINS)) {
            plugin.logger.info("Processing login for ${event.uuid} - ${event.username}")
        }

        if (event.isCancelled) {
            plugin.logger.info("Another plugin has cancelled the connection for ${event.uuid} - ${event.username}. No permissions data will be loaded.")
            deniedAsyncLogin += event.uuid
            return
        }

        try {
            val user = loadUser(event.uuid, event.username)
            recordConnection(event.uuid)
            plugin.eventDispatcher.dispatchPlayerLoginProcess(event.uuid, event.username, user)
        } catch (exception: Exception) {
            plugin.logger.severe("Exception occurred whilst loading data for ${event.uuid} - ${event.username}", exception)
            deniedAsyncLogin += event.uuid

            val reason = TranslationManager.render(Message.LOADING_DATABASE_ERROR.build())
            event.cancel(reason)
            plugin.eventDispatcher.dispatchPlayerLoginProcess(event.uuid, event.username, null)
        }
    }

    @Listener(-128) // lowest possible priority, for monitoring
    fun onLoginMonitor(event: LoginEvent) {
        if (deniedAsyncLogin.remove(event.uuid) && !event.isCancelled) {
            plugin.logger.severe("Player connection was re-allowed for ${event.uuid}")
            event.cancel(Component.empty())
        }
    }

    @Listener(ListenerPriority.MAXIMUM)
    fun onJoin(event: JoinEvent) {
        val player = event.player

        if (plugin.configuration.get(ConfigKeys.DEBUG_LOGINS)) {
            plugin.logger.info("Processing join for ${player.uuid} - ${player.name}")
        }

        val user = plugin.userManager.getIfLoaded(player.uuid)

        if (user == null) {
            deniedLogin += player.uuid

            if (player.uuid !in uniqueConnections) {
                plugin.logger.warn("User ${player.uuid} - ${player.name} doesn't have any data pre-loaded, they have never been processed during login in this session. Denying login.")
            } else {
                plugin.logger.warn("User ${player.uuid} - ${player.name} doesn't currently have any data pre-loaded, but they have been processed before in this session. Denying login.")
            }

            event.cancel(TranslationManager.render(Message.LOADING_STATE_ERROR.build(), player.locale))
            return
        }

        plugin.contextManager.signalContextUpdate(player)
    }

    @Listener(-128) // lowest possible priority, for monitoring
    fun onJoinMonitor(event: JoinEvent) {
        if (deniedLogin.remove(event.player.uuid) && !event.isCancelled) {
            plugin.logger.severe("Player connection was re-allowed for ${event.player.uuid}")
            event.cancel(Component.empty())
        }
    }

    // Sit at the back of the queue so other plugins can still perform permission checks on this event
    @Listener(ListenerPriority.NONE)
    fun onQuit(event: QuitEvent) = handleDisconnect(event.player.uuid)
}