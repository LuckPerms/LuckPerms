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

package me.lucko.luckperms.fabric.listeners;

import me.lucko.luckperms.common.api.implementation.ApiUser;
import me.lucko.luckperms.common.event.LuckPermsEventListener;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.fabric.LPFabricPlugin;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.context.ContextUpdateEvent;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.query.QueryOptions;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;

public class FabricAutoOpListener implements LuckPermsEventListener {
    private static final String NODE = "luckperms.autoop";

    private final LPFabricPlugin plugin;

    public FabricAutoOpListener(LPFabricPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void bind(EventBus bus) {
        bus.subscribe(UserDataRecalculateEvent.class, this::onUserDataRecalculate);
        bus.subscribe(ContextUpdateEvent.class, this::onContextUpdate);
    }

    private void onUserDataRecalculate(UserDataRecalculateEvent e) {
        User user = ApiUser.cast(e.getUser());
        this.plugin.getBootstrap().getPlayer(user.getUniqueId()).ifPresent(p -> refreshAutoOp(p, false));
    }

    private void onContextUpdate(ContextUpdateEvent e) {
        e.getSubject(ServerPlayerEntity.class).ifPresent(p -> refreshAutoOp(p, true));
    }

    private void refreshAutoOp(ServerPlayerEntity player, boolean callerIsSync) {
        if (!callerIsSync && !this.plugin.getBootstrap().getServer().isPresent()) {
            return;
        }

        User user = this.plugin.getUserManager().getIfLoaded(player.getUuid());

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

    private void setOp(ServerPlayerEntity player, boolean value) {
        this.plugin.getBootstrap().getServer().ifPresent(server -> {
            if (value) {
                server.getPlayerManager().addToOperators(player.getGameProfile());
            } else {
                server.getPlayerManager().removeFromOperators(player.getGameProfile());
            }
        });
    }
}
