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

import com.github.benmanes.caffeine.cache.LoadingCache
import me.lucko.luckperms.common.context.ContextManager
import me.lucko.luckperms.common.context.QueryOptionsSupplier
import me.lucko.luckperms.common.util.CaffeineFactory
import me.lucko.luckperms.krypton.LPKryptonPlugin
import net.luckperms.api.context.ImmutableContextSet
import net.luckperms.api.query.QueryOptions
import org.kryptonmc.krypton.api.entity.entities.Player
import java.util.*
import java.util.concurrent.TimeUnit

class KryptonContextManager(plugin: LPKryptonPlugin) : ContextManager<Player, Player>(plugin, Player::class.java, Player::class.java) {

    private val contextsCache = CaffeineFactory.newBuilder()
        .expireAfterWrite(50, TimeUnit.MILLISECONDS)
        .build(this::calculate)

    override fun getUniqueId(player: Player) = player.uuid

    override fun getCacheFor(subject: Player) = InlineQueryOptionsSupplier(subject, contextsCache)

    override fun getContext(subject: Player) = getQueryOptions(subject)?.context()

    override fun getQueryOptions(subject: Player) = contextsCache[subject]

    override fun invalidateCache(subject: Player) = contextsCache.invalidate(subject)

    override fun formQueryOptions(subject: Player, contextSet: ImmutableContextSet): QueryOptions = formQueryOptions(contextSet)

    class InlineQueryOptionsSupplier(
        private val key: Player,
        private val cache: LoadingCache<Player, QueryOptions>
    ) : QueryOptionsSupplier {

        override fun getQueryOptions() = cache[key]
    }
}