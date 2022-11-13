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

package me.lucko.luckperms.minestom.listener;

import me.lucko.luckperms.minestom.LPMinestomExtension;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedDataManager;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.node.NodeClearEvent;
import net.luckperms.api.event.node.NodeRemoveEvent;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PermissionNode;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.permission.Permission;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class PlayerNodeChangeListener {
    private final LPMinestomExtension plugin;
    private final LuckPerms luckPerms;

    public PlayerNodeChangeListener(LPMinestomExtension plugin, LuckPerms luckPerms) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
    }

    public void register() {
        EventBus eventBus = this.luckPerms.getEventBus();
        eventBus.subscribe(this.plugin, NodeAddEvent.class, this::onNodeAdd);
        eventBus.subscribe(this.plugin, NodeRemoveEvent.class, this::onNodeRemove);
        eventBus.subscribe(this.plugin, NodeClearEvent.class, this::onNodeClear);
    }

    private void onNodeAdd(NodeAddEvent e) {
        if (!e.isUser()) {
            return;
        }

        User target = (User) e.getTarget();
        Node node = e.getNode();
        Player player = MinecraftServer.getConnectionManager().getPlayer(target.getUniqueId());
        if(player == null) {
            throw new IllegalArgumentException("Player must be online");
        }
        setPermissionsFromNodes(List.of(node), player, luckPerms.getGroupManager());
    }

    private void onNodeRemove(NodeRemoveEvent e) {
        if (!e.isUser()) {
            return;
        }

        User target = (User) e.getTarget();
        Node node = e.getNode();
        Player player = MinecraftServer.getConnectionManager().getPlayer(target.getUniqueId());
        if(player == null) {
            throw new IllegalArgumentException("Player must be online");
        }
        setPermissionsFromNodes(List.of(node), player, luckPerms.getGroupManager());
    }
    private void onNodeClear(NodeClearEvent e) {
        if (!e.isUser()) {
            return;
        }

        User target = (User) e.getTarget();
        Set<Node> nodes = e.getNodes();
        Player player = MinecraftServer.getConnectionManager().getPlayer(target.getUniqueId());
        if(player == null) {
            throw new IllegalArgumentException("Player must be online");
        }
        setPermissionsFromNodes(nodes, player, luckPerms.getGroupManager());
    }

    public static void setPermissionsFromNodes(Collection<Node> nodes, Player player, GroupManager groupManager) {
        for (Node node : nodes) {
            if (node instanceof PermissionNode) {
                String permission = ((PermissionNode) node).getPermission();
                if(node.getValue()) {
                    player.addPermission(new Permission(permission));
                } else {
                    player.removePermission(new Permission(permission));
                }

            } else if (node instanceof InheritanceNode) {
                InheritanceNode inheritanceNode = ((InheritanceNode) node);
                Group group = groupManager.getGroup(inheritanceNode.getGroupName());
                Collection<PermissionNode> permissionNodes = group.getNodes(NodeType.PERMISSION);
                for (PermissionNode permissionNode : permissionNodes) {
                    if(node.getValue()) {
                        player.addPermission(new Permission(permissionNode.getPermission()));
                    } else {
                        player.removePermission(new Permission(permissionNode.getPermission()));
                    }
                }

            }
        }
    }

}