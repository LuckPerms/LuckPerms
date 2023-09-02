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

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import me.lucko.luckperms.minestom.LPMinestomPlugin;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
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
    private final LPMinestomPlugin plugin;
    private final LuckPerms luckPerms;

    public PlayerNodeChangeListener(LPMinestomPlugin plugin, LuckPerms luckPerms) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
    }

    public void register() {
        EventBus eventBus = this.luckPerms.getEventBus();
        eventBus.subscribe(this.plugin, UserDataRecalculateEvent.class, this::onUserDataRecalculate);
    }

    private void onUserDataRecalculate(UserDataRecalculateEvent e) {
        User target = (User) e.getUser();
        
        Player player = MinecraftServer.getConnectionManager().getPlayer(target.getUniqueId());
        if(player == null) return;
        
        setPermissionsFromNodes(e.getUser().getNodes(), player,  luckPerms.getGroupManager());
    }

    public static void setPermissionsFromNodes(Collection<Node> nodes, Player player, GroupManager groupManager) {
        UserManager userManager = LuckPermsProvider.get().getUserManager();
        UUID uuid = player.getUuid();
        User user;

        if (userManager.isLoaded(uuid)) {
            user = userManager.getUser(uuid);
        } else {
            try {
                user = userManager.loadUser(uuid).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        Set<Permission> permissions = player.getAllPermissions();
        for (Permission permission : permissions) {
            player.removePermission(permission);
        }

        for (Node node : nodes) {
            if (node instanceof PermissionNode) {
                String permission = ((PermissionNode) node).getPermission();
                boolean hasPermission = user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
                if (hasPermission) {
                    player.addPermission(new Permission(permission));
                }

            } else if (node instanceof InheritanceNode) {
                InheritanceNode inheritanceNode = ((InheritanceNode) node);
                Group group = groupManager.getGroup(inheritanceNode.getGroupName());
                Collection<PermissionNode> permissionNodes = group.getNodes(NodeType.PERMISSION);
                for (PermissionNode permissionNode : permissionNodes) {
                    String permission = permissionNode.getPermission();
                    boolean hasPermission = user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
                    if (hasPermission) {
                        player.addPermission(new Permission(permissionNode.getPermission()));
                    }
                }

            }
        }

        player.refreshCommands();
    }

}
