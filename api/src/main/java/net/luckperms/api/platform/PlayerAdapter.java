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

package net.luckperms.api.platform;

import net.luckperms.api.cacheddata.CachedDataManager;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.query.QueryOptions;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.UUID;

/**
 * A utility class for adapting platform Player instances to LuckPerms {@link User}s.
 *
 * <p>Note: this class will only work for online players.</p>
 *
 * <p>The "player type" parameter must be equal to the class or interface used by the
 * server platform to represent players.</p>
 *
 * <p>Specifically:</p>
 *
 * <p></p>
 * <ul>
 * <li>{@code org.bukkit.entity.Player}</li>
 * <li>{@code net.md_5.bungee.api.connection.ProxiedPlayer}</li>
 * <li>{@code org.spongepowered.api/entity.living.player.Player}</li>
 * <li>{@code cn.nukkit.Player}</li>
 * <li>{@code com.velocitypowered.api.proxy.Player}</li>
 * </ul>
 *
 * @param <T> the player type
 * @since 5.1
 */
public interface PlayerAdapter<T> {

    /**
     * Gets the {@link User} instance for the given {@code player}.
     *
     * @param player the player
     * @return the user
     * @see UserManager#getUser(UUID)
     */
    @NonNull User getUser(@NonNull T player);

    /**
     * Gets current {@link ImmutableContextSet active context} for the {@code player}.
     *
     * @param player the player
     * @return the active context for the player
     * @see ContextManager#getContext(Object)
     */
    @NonNull ImmutableContextSet getContext(@NonNull T player);

    /**
     * Gets current {@link QueryOptions active query options} for the {@code player}.
     *
     * @param player the player
     * @return the active query options for the player
     * @see ContextManager#getQueryOptions(Object)
     */
    @NonNull QueryOptions getQueryOptions(@NonNull T player);

    /**
     * Gets the current {@link CachedPermissionData} for the {@code player},
     * using their {@link #getQueryOptions(Object) active query options}.
     *
     * @param player the player
     * @return the cached permission data for the player
     * @see CachedDataManager#getPermissionData()
     */
    default @NonNull CachedPermissionData getPermissionData(@NonNull T player) {
        return getUser(player).getCachedData().getPermissionData(getQueryOptions(player));
    }

    /**
     * Gets the current {@link CachedMetaData} for the {@code player},
     * using their {@link #getQueryOptions(Object) active query options}.
     *
     * @param player the player
     * @return the cached meta data for the player
     * @see CachedDataManager#getMetaData()
     */
    default @NonNull CachedMetaData getMetaData(@NonNull T player) {
        return getUser(player).getCachedData().getMetaData(getQueryOptions(player));
    }

}
