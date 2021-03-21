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

package me.lucko.luckperms.krypton.context

import me.lucko.luckperms.common.config.ConfigKeys
import me.lucko.luckperms.common.context.contextset.ImmutableContextSetImpl
import me.lucko.luckperms.common.util.EnumNamer
import me.lucko.luckperms.krypton.LPKryptonPlugin
import net.luckperms.api.context.Context
import net.luckperms.api.context.ContextCalculator
import net.luckperms.api.context.ContextConsumer
import net.luckperms.api.context.DefaultContextKeys
import org.kryptonmc.krypton.api.entity.entities.Player
import org.kryptonmc.krypton.api.world.Gamemode

// TODO: Add event listening for gamemode and world changes and joining worlds to signal context updates
// when Krypton supports this
class KryptonPlayerCalculator(
    private val plugin: LPKryptonPlugin,
    disabled: Set<String>
) : ContextCalculator<Player> {

    private val gamemode = DefaultContextKeys.GAMEMODE_KEY !in disabled
    private val world = DefaultContextKeys.WORLD_KEY !in disabled

    // TODO: Add more checking here when Krypton supports per player worlds and gamemodes
    override fun calculate(target: Player, consumer: ContextConsumer) {
        if (gamemode) consumer.accept(
            DefaultContextKeys.GAMEMODE_KEY,
            GAMEMODE_NAMER.name(plugin.bootstrap.server.gamemode)
        )

        val world = plugin.bootstrap.server.worldManager.worlds.values.firstOrNull() ?: return
        if (this.world) plugin.configuration.get(ConfigKeys.WORLD_REWRITES).rewriteAndSubmit(world.name, consumer)
    }

    override fun estimatePotentialContexts() = ImmutableContextSetImpl.BuilderImpl().apply {
        if (gamemode) Gamemode.values().forEach { add(DefaultContextKeys.GAMEMODE_KEY, GAMEMODE_NAMER.name(it)) }
        if (world) plugin.bootstrap.server.worldManager.worlds.forEach { (key, _) ->
            if (Context.isValidValue(key)) add(DefaultContextKeys.WORLD_KEY, key)
        }
    }.build()

    companion object {

        private val GAMEMODE_NAMER = EnumNamer(Gamemode::class.java, EnumNamer.LOWER_CASE_NAME)
    }
}