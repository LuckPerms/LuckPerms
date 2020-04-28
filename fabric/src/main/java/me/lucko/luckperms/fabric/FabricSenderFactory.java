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

package me.lucko.luckperms.fabric;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.luckperms.common.cacheddata.type.PermissionCache;
import me.lucko.luckperms.common.context.QueryOptionsCache;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.sender.SenderFactory;
import me.lucko.luckperms.common.verbose.event.PermissionCheckEvent;
import me.lucko.luckperms.fabric.adapter.FabricTextAdapter;
import net.kyori.text.Component;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;

import java.util.UUID;

public class FabricSenderFactory extends SenderFactory<ServerCommandSource> {
    private LPFabricPlugin plugin;

    public FabricSenderFactory(LPFabricPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    protected LPFabricPlugin getPlugin() {
        return this.plugin;
    }

    @Override
    protected UUID getUniqueId(ServerCommandSource commandSource) {
        if (commandSource.getEntity() instanceof ServerPlayerEntity) {
            return commandSource.getEntity().getUuid();
        }

        return Sender.CONSOLE_UUID;
    }

    @Override
    protected String getName(ServerCommandSource commandSource) {
        return commandSource.getName();
    }

    @Override
    protected void sendMessage(ServerCommandSource commandSource, String s) {
        // Sending to a ServerCommandSource async is always since it will refer to the CommandSource's CommandOutput
        commandSource.sendFeedback(new LiteralText(s), false);
    }

    @Override
    protected void sendMessage(ServerCommandSource commandSource, Component message) {
        commandSource.sendFeedback(FabricTextAdapter.convert(message), false);
    }

    @Override
    protected boolean hasPermission(ServerCommandSource commandSource, String node) {
        Tristate value = this.getPermissionValue(commandSource, node);
        return value.asBoolean();
    }

    @Override
    protected Tristate getPermissionValue(ServerCommandSource commandSource, String node) {
        // TODO: Route through Fabric API's PermissionProviders
        Entity entity = commandSource.getEntity();

        if (entity instanceof ServerPlayerEntity) {
            final ServerPlayerEntity player = (ServerPlayerEntity) entity;
            User user = this.getPlugin().getUserManager().getIfLoaded(player.getGameProfile().getId());

            if (user == null) {
                return Tristate.UNDEFINED;
            }

            QueryOptions queryOptions = this.plugin.getContextManager().getQueryOptions(player);

            PermissionCache permissionData = user.getCachedData().getPermissionData(queryOptions);

            return permissionData.checkPermission(node, PermissionCheckEvent.Origin.INTERNAL).result();
        }

        return Tristate.UNDEFINED;
    }
}
