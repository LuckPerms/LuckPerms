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

package me.lucko.luckperms.neoforge.listeners;

import me.lucko.luckperms.common.api.implementation.ApiUser;
import me.lucko.luckperms.common.event.LuckPermsEventListener;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.neoforge.LPNeoForgePlugin;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.context.ContextUpdateEvent;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.query.QueryOptions;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public class NeoForgeAutoOpListener implements LuckPermsEventListener {
    private static final String NODE = "luckperms.autoop";

    private final LPNeoForgePlugin plugin;

    public NeoForgeAutoOpListener(LPNeoForgePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void bind(EventBus bus) {
        bus.subscribe(ContextUpdateEvent.class, this::onContextUpdate);
        bus.subscribe(UserDataRecalculateEvent.class, this::onUserDataRecalculate);
    }

    private void onContextUpdate(ContextUpdateEvent event) {
        event.getSubject(ServerPlayer.class).ifPresent(player -> refreshAutoOp(player, true));
    }

    private void onUserDataRecalculate(UserDataRecalculateEvent event) {
        User user = ApiUser.cast(event.getUser());
        this.plugin.getBootstrap().getPlayer(user.getUniqueId()).ifPresent(player -> refreshAutoOp(player, false));
    }

    private void refreshAutoOp(ServerPlayer player, boolean callerIsSync) {
        if (!callerIsSync && !this.plugin.getBootstrap().getServer().isPresent()) {
            return;
        }

        User user = this.plugin.getUserManager().getIfLoaded(player.getUUID());

        boolean value;
        if (user != null) {
            QueryOptions queryOptions = this.plugin.getContextManager().getQueryOptions(player);
            Map<String, Boolean> permData = user.getCachedData().getPermissionData(queryOptions).getPermissionMap();
            value = permData.getOrDefault(NODE, false);
        } else {
            value = false;
        }

        if (callerIsSync) {
            setOp(player, value);
        } else {
            this.plugin.getBootstrap().getScheduler().sync(() -> setOp(player, value));
        }
    }

    private void setOp(ServerPlayer player, boolean value) {
        this.plugin.getBootstrap().getServer().ifPresent(server -> {
            if (value) {
                server.getPlayerList().op(player.getGameProfile());
            } else {
                server.getPlayerList().deop(player.getGameProfile());
            }
        });
    }

}
