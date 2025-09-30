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

package me.lucko.luckperms.fabric.context;

import me.lucko.luckperms.common.context.manager.DetachedContextManager;
import me.lucko.luckperms.common.context.manager.QueryOptionsSupplier;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.fabric.model.MixinUser;
import net.luckperms.api.query.OptionKey;
import net.luckperms.api.query.QueryOptions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;
import java.util.UUID;

public class FabricContextManager extends DetachedContextManager<ServerPlayerEntity, ServerPlayerEntity> {
    public static final OptionKey<Boolean> INTEGRATED_SERVER_OWNER = OptionKey.of("integrated_server_owner", Boolean.class);

    public FabricContextManager(LuckPermsPlugin plugin) {
        super(plugin, ServerPlayerEntity.class, ServerPlayerEntity.class);
    }

    @Override
    public UUID getUniqueId(ServerPlayerEntity player) {
        return player.getUuid();
    }

    @Override
    public @Nullable QueryOptionsSupplier getQueryOptionsSupplier(ServerPlayerEntity subject) {
        Objects.requireNonNull(subject, "subject");
        return ((MixinUser) subject).luckperms$getQueryOptionsCache(this);
    }

    @Override
    public void customizeQueryOptions(ServerPlayerEntity subject, QueryOptions.Builder builder) {
        if (subject.getEntityWorld().getServer().isHost(subject.getPlayerConfigEntry())) {
            builder.option(INTEGRATED_SERVER_OWNER, true);
        }
    }

}
