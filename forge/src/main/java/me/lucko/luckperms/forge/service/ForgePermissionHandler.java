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

package me.lucko.luckperms.forge.service;

import me.lucko.luckperms.common.cacheddata.type.MetaCache;
import me.lucko.luckperms.common.cacheddata.type.PermissionCache;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;
import me.lucko.luckperms.forge.LPForgeBootstrap;
import me.lucko.luckperms.forge.LPForgePlugin;
import net.luckperms.api.context.MutableContextSet;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.permission.handler.IPermissionHandler;
import net.minecraftforge.server.permission.nodes.PermissionDynamicContext;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ForgePermissionHandler implements IPermissionHandler {
    public static final ResourceLocation IDENTIFIER = new ResourceLocation(LPForgeBootstrap.ID, "permission_handler");

    private final LPForgePlugin plugin;
    private final Set<PermissionNode<?>> permissionNodes;

    public ForgePermissionHandler(LPForgePlugin plugin, Collection<PermissionNode<?>> permissionNodes) {
        this.plugin = plugin;
        this.permissionNodes = Collections.unmodifiableSet(new HashSet<>(permissionNodes));
    }

    @Override
    public ResourceLocation getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Set<PermissionNode<?>> getRegisteredNodes() {
        return this.permissionNodes;
    }

    @Override
    public <T> T getPermission(ServerPlayer player, PermissionNode<T> node, PermissionDynamicContext<?>... context) {
        User user = this.plugin.getUserManager().getIfLoaded(player.getUUID());
        if (user != null) {
            T value = getPermission(user, node, context);
            if (value != null) {
                return value;
            }
        }

        return node.getDefaultResolver().resolve(player, player.getUUID(), context);
    }

    @Override
    public <T> T getOfflinePermission(UUID player, PermissionNode<T> node, PermissionDynamicContext<?>... context) {
        User user = this.plugin.getUserManager().getIfLoaded(player);
        if (user != null) {
            T value = getPermission(user, node, context);
            if (value != null) {
                return value;
            }
        }

        return node.getDefaultResolver().resolve(null, player, context);
    }

    @SuppressWarnings("unchecked")
    private <T> T getPermission(User user, PermissionNode<T> node, PermissionDynamicContext<?>... context) {
        QueryOptions queryOptions = getQueryOptions(user, context);
        if (node.getType() == PermissionTypes.BOOLEAN) {
            PermissionCache cache = user.getCachedData().getPermissionData(queryOptions);
            Tristate value = cache.checkPermission(node.getNodeName(), CheckOrigin.PLATFORM_API_HAS_PERMISSION).result();
            return (T) (Boolean) value.asBoolean();
        }

        if (node.getType() == PermissionTypes.INTEGER) {
            MetaCache cache = user.getCachedData().getMetaData(queryOptions);
            Integer value = cache.getMetaValue(node.getNodeName(), Integer::parseInt).orElse(null);
            if (value != null) {
                return (T) value;
            }
        }

        if (node.getType() == PermissionTypes.STRING) {
            MetaCache cache = user.getCachedData().getMetaData(queryOptions);
            String value = cache.getMetaValue(node.getNodeName());
            if (value != null) {
                return (T) value;
            }
        }

        return null;
    }

    private QueryOptions getQueryOptions(User user, PermissionDynamicContext<?>... context) {
        QueryOptions queryOptions = user.getQueryOptions();
        QueryOptions.Builder queryOptionsBuilder = queryOptions.toBuilder();
        MutableContextSet contextSet = queryOptions.context().mutableCopy();
        for (PermissionDynamicContext<?> dynamicContext : context) {
            contextSet.add(dynamicContext.getDynamic().name(), dynamicContext.getSerializedValue());
        }

        return queryOptionsBuilder.context(contextSet).build();
    }

}
