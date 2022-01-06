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

package me.lucko.luckperms.forge.model;

import me.lucko.luckperms.common.cacheddata.type.MetaCache;
import me.lucko.luckperms.common.cacheddata.type.PermissionCache;
import me.lucko.luckperms.common.context.manager.QueryOptionsCache;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.function.Function;

public class ForgeUser {

    private final User user;
    private final QueryOptionsCache<ServerPlayer> queryOptionsCache;
    private String language;
    private Locale locale;

    public ForgeUser(User user, QueryOptionsCache<ServerPlayer> queryOptionsCache) {
        this.user = user;
        this.queryOptionsCache = queryOptionsCache;
    }

    public Tristate checkPermission(String permission) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }

        return checkPermission(permission, this.queryOptionsCache.getQueryOptions());
    }

    public Tristate checkPermission(String permission, QueryOptions queryOptions) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }

        if (queryOptions == null) {
            throw new NullPointerException("queryOptions");
        }

        PermissionCache cache = this.user.getCachedData().getPermissionData(queryOptions);
        return cache.checkPermission(permission, CheckOrigin.PLATFORM_API_HAS_PERMISSION).result();
    }

    public String getMetaValue(String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        return getMetaValue(key, value -> value);
    }

    public <T> T getMetaValue(String key, Function<String, T> valueTransform) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        if (valueTransform == null) {
            throw new NullPointerException("valueTransform");
        }

        return getMetaValue(key, valueTransform, this.queryOptionsCache.getQueryOptions());
    }

    public <T> T getMetaValue(String key, Function<String, T> valueTransform, QueryOptions queryOptions) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        if (valueTransform == null) {
            throw new NullPointerException("valueTransform");
        }

        if (queryOptions == null) {
            throw new NullPointerException("queryOptions");
        }

        MetaCache cache = this.user.getCachedData().getMetaData(queryOptions);
        return cache.getMetaValue(key, valueTransform).orElse(null);
    }

    public User getUser() {
        return this.user;
    }

    public QueryOptionsCache<ServerPlayer> getQueryOptionsCache() {
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
