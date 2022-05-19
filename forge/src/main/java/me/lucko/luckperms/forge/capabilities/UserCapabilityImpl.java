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

package me.lucko.luckperms.forge.capabilities;

import me.lucko.luckperms.common.cacheddata.type.PermissionCache;
import me.lucko.luckperms.common.context.manager.QueryOptionsCache;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;
import me.lucko.luckperms.forge.context.ForgeContextManager;

import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class UserCapabilityImpl implements UserCapability {

    /**
     * Gets a {@link UserCapability} for a given {@link ServerPlayer}.
     *
     * @param player the player
     * @return the capability
     */
    public static @NotNull UserCapabilityImpl get(@NotNull Player player) {
        return (UserCapabilityImpl) player.getCapability(CAPABILITY)
                .orElseThrow(() -> new IllegalStateException("Capability missing for " + player.getUUID()));
    }

    /**
     * Gets a {@link UserCapability} for a given {@link ServerPlayer}.
     *
     * @param player the player
     * @return the capability, or null
     */
    public static @Nullable UserCapabilityImpl getNullable(@NotNull Player player) {
        return (UserCapabilityImpl) player.getCapability(CAPABILITY).resolve().orElse(null);
    }

    private boolean initialised = false;

    private User user;
    private QueryOptionsCache<ServerPlayer> queryOptionsCache;
    private String language;
    private Locale locale;

    public UserCapabilityImpl() {

    }

    public void initialise(UserCapabilityImpl previous) {
        this.user = previous.user;
        this.queryOptionsCache = previous.queryOptionsCache;
        this.language = previous.language;
        this.locale = previous.locale;
        this.initialised = true;
    }

    public void initialise(User user, ServerPlayer player, ForgeContextManager contextManager) {
        this.user = user;
        this.queryOptionsCache = new QueryOptionsCache<>(player, contextManager);
        this.initialised = true;
    }

    private void assertInitialised() {
        if (!this.initialised) {
            throw new IllegalStateException("Capability has not been initialised");
        }
    }

    @Override
    public Tristate checkPermission(String permission) {
        assertInitialised();

        if (permission == null) {
            throw new NullPointerException("permission");
        }

        return checkPermission(permission, this.queryOptionsCache.getQueryOptions());
    }

    @Override
    public Tristate checkPermission(String permission, QueryOptions queryOptions) {
        assertInitialised();

        if (permission == null) {
            throw new NullPointerException("permission");
        }

        if (queryOptions == null) {
            throw new NullPointerException("queryOptions");
        }

        PermissionCache cache = this.user.getCachedData().getPermissionData(queryOptions);
        return cache.checkPermission(permission, CheckOrigin.PLATFORM_API_HAS_PERMISSION).result();
    }

    public User getUser() {
        assertInitialised();
        return this.user;
    }

    @Override
    public QueryOptions getQueryOptions() {
        return getQueryOptionsCache().getQueryOptions();
    }

    public QueryOptionsCache<ServerPlayer> getQueryOptionsCache() {
        assertInitialised();
        return this.queryOptionsCache;
    }

    public Locale getLocale(ServerPlayer player) {
        if (this.language == null || !this.language.equals(player.getLanguage())) {
            this.language = player.getLanguage();
            this.locale = TranslationManager.parseLocale(this.language);
        }

        return this.locale;
    }
}