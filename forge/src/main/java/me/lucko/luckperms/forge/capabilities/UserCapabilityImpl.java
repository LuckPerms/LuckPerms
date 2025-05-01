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
import me.lucko.luckperms.common.context.manager.QueryOptionsSupplier;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;
import me.lucko.luckperms.forge.LPForgePlugin;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class UserCapabilityImpl implements UserCapability {

    private static LazyOptional<UserCapability> getCapability(Player player) {
        LazyOptional<UserCapability> optional = player.getCapability(CAPABILITY);
        if (optional.isPresent()) {
            return optional;
        }

        // if capability is missing, try to restore them before trying again
        player.reviveCaps();
        return player.getCapability(CAPABILITY);
    }

    /**
     * Gets a {@link UserCapability} for a given {@link ServerPlayer}.
     *
     * @param player the player
     * @return the capability
     */
    public static @NotNull UserCapabilityImpl get(@NotNull Player player) {
        return (UserCapabilityImpl) getCapability(player).orElseThrow(() -> new IllegalStateException("Capability missing for " + player.getUUID()));
    }

    /**
     * Gets a {@link UserCapability} for a given {@link ServerPlayer}.
     *
     * @param player the player
     * @return the capability, or null
     */
    public static @Nullable UserCapabilityImpl getNullable(@NotNull Player player) {
        return (UserCapabilityImpl) getCapability(player).resolve().orElse(null);
    }

    private boolean initialised = false;

    private User user;
    private QueryOptionsSupplier queryOptionsSupplier;
    private String language;
    private Locale locale;

    public UserCapabilityImpl() {

    }

    public void initialise(UserCapabilityImpl previous, ServerPlayer player, LPForgePlugin plugin) {
        this.user = previous.user;
        this.queryOptionsSupplier = plugin.getContextManager().createQueryOptionsSupplier(player);
        this.language = previous.language;
        this.locale = previous.locale;
        this.initialised = true;
    }

    public void initialise(User user, ServerPlayer player, LPForgePlugin plugin) {
        this.user = user;
        this.queryOptionsSupplier = plugin.getContextManager().createQueryOptionsSupplier(player);
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

        return checkPermission(permission, this.queryOptionsSupplier.getQueryOptions());
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
        return getQueryOptionsSupplier().getQueryOptions();
    }

    public QueryOptionsSupplier getQueryOptionsSupplier() {
        assertInitialised();
        return this.queryOptionsSupplier;
    }

    public Locale getLocale(ServerPlayer player) {
        if (this.language == null || !this.language.equals(player.getLanguage())) {
            this.language = player.getLanguage();
            this.locale = TranslationManager.parseLocale(this.language);
        }

        return this.locale;
    }
}