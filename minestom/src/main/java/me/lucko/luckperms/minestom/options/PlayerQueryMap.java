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

package me.lucko.luckperms.minestom.options;

import me.lucko.luckperms.common.context.manager.QueryOptionsCache;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.minestom.context.MinestomContextManager;
import me.lucko.luckperms.minestom.listener.PlayerNodeChangeListener;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.Node;
import net.luckperms.api.query.QueryMode;
import net.luckperms.api.query.QueryOptions;
import net.minestom.server.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerQueryMap {

    private static final Map<Player, QueryOptionsCache<Player>> luckpermsQueryMap = new HashMap<>();

    public static QueryOptionsCache<Player> getQueryOptionsCache(Player player, MinestomContextManager contextManager) {
        return luckpermsQueryMap.computeIfAbsent(player, player1 -> new QueryOptionsCache<>(player1, contextManager));
    }

    public static void initializePermissions(Player player, User user) {
        if (luckpermsQueryMap.get(player) == null) {
            getQueryOptionsCache(player, (MinestomContextManager) user.getPlugin().getContextManager());
        }
        List<Node> nodes = user.getOwnNodes(QueryOptions.builder(QueryMode.CONTEXTUAL).build());
        PlayerNodeChangeListener.setPermissionsFromNodes(nodes, player, LuckPermsProvider.get().getGroupManager());
    }
}
