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

package me.lucko.luckperms.common.api.implementation;

import me.lucko.luckperms.common.context.ContextManager;
import me.lucko.luckperms.common.model.manager.user.UserManager;

import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.user.User;
import net.luckperms.api.platform.PlayerAdapter;
import net.luckperms.api.query.QueryOptions;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;

public class ApiPlayerAdapter<S, P extends S> implements PlayerAdapter<P> {
    private final UserManager<?> userManager;
    private final ContextManager<S, P> contextManager;

    public ApiPlayerAdapter(UserManager<?> userManager, ContextManager<S, P> contextManager) {
        this.userManager = userManager;
        this.contextManager = contextManager;
    }

    private P checkType(P player) {
        if (!this.contextManager.getPlayerClass().isAssignableFrom(player.getClass())) {
            throw new IllegalStateException("Player class " + player.getClass() + " is not assignable from " + this.contextManager.getPlayerClass());
        }
        return player;
    }

    @Override
    public @NonNull User getUser(@NonNull P player) {
        Objects.requireNonNull(player, "player");
        me.lucko.luckperms.common.model.User user = this.userManager.getIfLoaded(this.contextManager.getUniqueId(checkType(player)));
        Objects.requireNonNull(user, "user");
        return user.getApiProxy();
    }

    @Override
    public @NonNull ImmutableContextSet getContext(@NonNull P player) {
        Objects.requireNonNull(player, "player");
        return this.contextManager.getContext(checkType(player));
    }

    @Override
    public @NonNull QueryOptions getQueryOptions(@NonNull P player) {
        Objects.requireNonNull(player, "player");
        return this.contextManager.getQueryOptions(checkType(player));
    }
}
