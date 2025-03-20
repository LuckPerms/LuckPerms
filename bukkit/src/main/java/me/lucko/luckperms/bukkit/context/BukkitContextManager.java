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

package me.lucko.luckperms.bukkit.context;

import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import me.lucko.luckperms.bukkit.inject.permissible.LuckPermsPermissible;
import me.lucko.luckperms.bukkit.inject.permissible.PermissibleInjector;
import me.lucko.luckperms.common.context.manager.DetachedContextManager;
import me.lucko.luckperms.common.context.manager.QueryOptionsSupplier;
import net.luckperms.api.query.OptionKey;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;
import java.util.UUID;

public class BukkitContextManager extends DetachedContextManager<Player, Player> {

    public static final OptionKey<Boolean> OP_OPTION = OptionKey.of("op", Boolean.class);

    public BukkitContextManager(LPBukkitPlugin plugin) {
        super(plugin, Player.class, Player.class);
    }

    @Override
    public UUID getUniqueId(Player player) {
        return player.getUniqueId();
    }

    @Override
    public @Nullable QueryOptionsSupplier getQueryOptionsSupplier(Player subject) {
        Objects.requireNonNull(subject, "subject");
        LuckPermsPermissible permissible = PermissibleInjector.get(subject);
        if (permissible != null) {
            return permissible.getQueryOptionsSupplier();
        }
        return null;
    }

    @Override
    public void customizeQueryOptions(Player subject, QueryOptions.Builder builder) {
        if (subject.isOp()) {
            builder.option(OP_OPTION, true);
        }
    }
}
