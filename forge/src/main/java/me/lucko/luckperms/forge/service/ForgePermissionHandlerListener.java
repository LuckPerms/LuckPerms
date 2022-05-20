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

import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.forge.LPForgePlugin;

import net.minecraftforge.common.ForgeConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.handler.DefaultPermissionHandler;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;

public class ForgePermissionHandlerListener {
    private final LPForgePlugin plugin;

    public ForgePermissionHandlerListener(LPForgePlugin plugin) {
        this.plugin = plugin;
    }

    @SubscribeEvent
    public void onPermissionGatherHandler(PermissionGatherEvent.Handler event) {
        // Override the default permission handler with LuckPerms
        ForgeConfigSpec.ConfigValue<String> permissionHandler = ForgeConfig.SERVER.permissionHandler;
        if (permissionHandler.get().equals(DefaultPermissionHandler.IDENTIFIER.toString())) {
            permissionHandler.set(ForgePermissionHandler.IDENTIFIER.toString());
        }

        event.addPermissionHandler(ForgePermissionHandler.IDENTIFIER, permissions -> new ForgePermissionHandler(this.plugin, permissions));
    }

    @SubscribeEvent
    public void onPermissionGatherNodes(PermissionGatherEvent.Nodes event) {
        // register luckperms nodes
        for (CommandPermission permission : CommandPermission.values()) {
            event.addNodes(new PermissionNode<>("luckperms", permission.getNode(), PermissionTypes.BOOLEAN, (player, uuid, context) -> false));
        }
    }

}
