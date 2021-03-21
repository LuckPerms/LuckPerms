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

import me.lucko.luckperms.common.calculator.result.TristateResult
import me.lucko.luckperms.common.query.QueryOptionsImpl.DEFAULT_CONTEXTUAL
import me.lucko.luckperms.common.verbose.event.PermissionCheckEvent.Origin.PLATFORM_PERMISSION_CHECK
import me.lucko.luckperms.krypton.LPKryptonPlugin
import net.luckperms.api.util.Tristate
import org.kryptonmc.krypton.api.entity.entities.Player
import org.kryptonmc.krypton.api.event.Listener
import org.kryptonmc.krypton.api.event.ListenerPriority
import org.kryptonmc.krypton.api.event.events.play.PermissionCheckEvent
import org.kryptonmc.krypton.api.event.events.play.PermissionCheckResult
import java.lang.Exception

class KryptonPermissionCheckListener(private val plugin: LPKryptonPlugin) {

    @Listener(ListenerPriority.LOW)
    fun onPermissionCheck(event: PermissionCheckEvent) {
        if (event.permission == null) return

        val sender = event.sender
        if (sender !is Player) return

        val user = plugin.userManager.getIfLoaded(sender.uuid)
        if (user == null) {
            plugin.logger.warn(
                "A permission check was made for player ${sender.name} - ${sender.uuid}," +
                        "but LuckPerms does not have any permissions data loaded for them." +
                        "Perhaps their UUID has been altered since last login?", Exception()
            )
            event.result = PermissionCheckResult.FALSE
        }

        val queryOptions = plugin.contextManager.getQueryOptions(sender) ?: return
        val result = user.cachedData.getPermissionData(queryOptions).checkPermission(event.permission, PLATFORM_PERMISSION_CHECK).result()
        if (result == Tristate.UNDEFINED) return // just use the default result provided by Krypton
        event.result = result.toCheckResult()
    }

    @Listener(ListenerPriority.NONE)
    fun onOtherPermissionCheck(event: PermissionCheckEvent) {
        if (event.permission == null) return

        if (event.sender is Player) return

        val permission = event.permission
        val result = event.result.toTristate()
        val name = "internal/${event.sender.name}"

        plugin.verboseHandler.offerPermissionCheckEvent(PLATFORM_PERMISSION_CHECK, name, DEFAULT_CONTEXTUAL, permission, TristateResult.of(result))
        plugin.permissionRegistry.offer(permission)
    }
}

fun Tristate.toCheckResult() = when (this) {
    Tristate.TRUE -> PermissionCheckResult.TRUE
    Tristate.FALSE -> PermissionCheckResult.FALSE
    Tristate.UNDEFINED -> PermissionCheckResult.UNSET
}

fun PermissionCheckResult.toTristate() = when (this) {
    PermissionCheckResult.TRUE -> Tristate.TRUE
    PermissionCheckResult.FALSE -> Tristate.FALSE
    PermissionCheckResult.UNSET -> Tristate.UNDEFINED
}