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

package me.lucko.luckperms.common.minecraft;

import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public abstract class MinecraftLuckPermsBootstrap implements LuckPermsBootstrap {

    public abstract Optional<MinecraftServer> getServer();

    @Override
    public abstract MinecraftSchedulerAdapter getScheduler();

    @Override
    public final Optional<ServerPlayer> getPlayer(UUID uniqueId) {
        return getServer().map(MinecraftServer::getPlayerList).map(playerList -> playerList.getPlayer(uniqueId));
    }

    @Override
    public final Optional<UUID> lookupUniqueId(String username) {
        return getServer().map(server -> server.services().nameToIdCache())
                .flatMap(resolver -> resolver.get(username)).map(NameAndId::id);
    }

    @Override
    public final Optional<String> lookupUsername(UUID uniqueId) {
        return getServer().map(server -> server.services().nameToIdCache())
                .flatMap(resolver -> resolver.get(uniqueId)).map(NameAndId::name);
    }

    @Override
    public final int getPlayerCount() {
        return getServer().map(MinecraftServer::getPlayerCount).orElse(0);
    }

    @Override
    public final Collection<String> getPlayerList() {
        return getServer().map(MinecraftServer::getPlayerList)
                .map(PlayerList::getPlayers)
                .map(players -> {
                    List<String> list = new ArrayList<>(players.size());
                    for (ServerPlayer player : players) {
                        list.add(player.getGameProfile().name());
                    }
                    return list;
                })
                .orElse(Collections.emptyList());
    }

    @Override
    public final Collection<UUID> getOnlinePlayers() {
        return getServer().map(MinecraftServer::getPlayerList)
                .map(PlayerList::getPlayers)
                .map(players -> {
                    List<UUID> list = new ArrayList<>(players.size());
                    for (ServerPlayer player : players) {
                        list.add(player.getGameProfile().id());
                    }
                    return list;
                })
                .orElse(Collections.emptyList());
    }

    @Override
    public final boolean isPlayerOnline(UUID uniqueId) {
        return getServer().map(MinecraftServer::getPlayerList)
                .map(s -> s.getPlayer(uniqueId) != null)
                .orElse(false);
    }

}
