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

package me.lucko.luckperms.library;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import me.lucko.luckperms.common.command.utils.ArgumentTokenizer;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;
import me.lucko.luckperms.library.sender.ConsoleLibrarySender;
import me.lucko.luckperms.library.sender.PlayerLibrarySender;
import me.lucko.luckperms.library.stub.LibraryContextManager;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;

public class LuckPermsLibrary implements AutoCloseable {

    private final LuckPermsLibraryManager manager;

    private final LPLibraryBootstrap bootstrap;
    private final LPLibraryPlugin plugin;
    private LuckPerms luckPerms;

    private final ConsoleLibrarySender consoleSender;
    private final Map<UUID, PlayerLibrarySender> playerSenders;

    public LuckPermsLibrary(LuckPermsLibraryManager manager) {
        this.manager = manager;
        this.bootstrap = new LPLibraryBootstrap(manager, this);
        this.plugin = bootstrap.getPlugin();

        consoleSender = new ConsoleLibrarySender(manager);
        playerSenders = new HashMap<>();
    }

    public void start() {
        bootstrap.onLoad();
        bootstrap.onEnable();

        if (luckPerms == null)
            throw new NullPointerException("Failed to set luckPerms!");
    }

    public void close() {
        bootstrap.onDisable();
    }

    public Optional<Tristate> getLoadedPermissionValue(UUID player, String permission) {
        return Optional.ofNullable(luckPerms.getUserManager().getUser(player))
                .map(user -> user.getCachedData().getPermissionData().checkPermission(permission));
    }

    public Optional<Boolean> hasLoadedPermission(UUID player, String permission) {
        return getLoadedPermissionValue(player, permission).map(Tristate::asBoolean);
    }

    public CompletableFuture<Tristate> getPermissionValue(UUID player, String permission) {
        return luckPerms.getUserManager().loadUser(player)
                .thenApply(user -> user.getCachedData().getPermissionData().checkPermission(permission));
    }

    public CompletableFuture<Boolean> hasPermission(UUID player, String permission) {
        return getPermissionValue(player, permission).thenApply(Tristate::asBoolean);
    }

    public void playerJoined(UUID uuid, String username) {
        User user = plugin.getConnectionListener().loadUser(uuid, username);
        ((LibraryConnectionListener) plugin.getConnectionListener()).recordConnection(uuid);
        playerSenders.put(uuid, new PlayerLibrarySender(manager, uuid, username, permission -> {
            QueryOptions queryOptions = ((LibraryContextManager) plugin.getContextManager())
                    .getQueryOptions(playerSenders.get(uuid));
            return user.getCachedData().getPermissionData(queryOptions)
                    .checkPermission(permission, CheckOrigin.PLATFORM_API_HAS_PERMISSION).result();
        }));
    }

    public void playerDisconnected(UUID uuid) {
        plugin.getConnectionListener().handleDisconnect(uuid);
        playerSenders.remove(uuid);
    }

    /**
     * @param command Does not include lp or the slash
     */
    public CompletableFuture<Void> execFromConsole(String command) {
        return execFromConsole(ArgumentTokenizer.EXECUTE.tokenizeInput(command));
    }

    /**
     * @param command Does not include lp or the slash
     */
    public CompletableFuture<Void> execFromConsole(List<String> command) {
        return plugin.getCommandManager().executeCommand(plugin.getSenderFactory().wrap(consoleSender), "lp", command);
    }

    /**
     * @param player The player that is executing the command - must be joined with {@link #playerJoined(UUID, String)}
     * @param command Does not include lp or the slash
     */
    public CompletableFuture<Void> execFromPlayer(UUID player, String command) {
        return execFromPlayer(player, ArgumentTokenizer.EXECUTE.tokenizeInput(command));
    }

    /**
     * @param player The player that is executing the command - must be joined with {@link #playerJoined(UUID, String)}
     * @param command Does not include lp or the slash
     */
    public CompletableFuture<Void> execFromPlayer(UUID player, List<String> command) {
        return plugin.getCommandManager().executeCommand(plugin.getSenderFactory().wrap(playerSenders.get(player)), "lp", command);
    }

    public LuckPermsBootstrap getBootstrap() {
        return bootstrap;
    }

    public LuckPermsPlugin getPlugin() {
        return plugin;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    Optional<PlayerLibrarySender> getPlayer(UUID uuid) {
        return Optional.ofNullable(playerSenders.get(uuid));
    }

    Optional<UUID> lookupUniqueId(String username) {
        return playerSenders.values().stream().filter(player -> player.getName().equals(username))
                .findFirst().map(PlayerLibrarySender::getUniqueId);
    }

    Optional<String> lookupUsername(UUID uuid) {
        return Optional.ofNullable(playerSenders.get(uuid)).map(PlayerLibrarySender::getName);
    }

    int getPlayerCount() {
        return playerSenders.size();
    }

    Collection<String> getPlayerList() {
        return playerSenders.values().stream().map(PlayerLibrarySender::getName).toList();
    }

    Collection<UUID> getOnlinePlayers() {
        return playerSenders.values().stream().map(PlayerLibrarySender::getUniqueId).toList();
    }

    boolean isPlayerOnline(UUID uuid) {
        return playerSenders.containsKey(uuid);
    }

    void setLuckPerms(LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
    }

    ConsoleLibrarySender getConsoleSender() {
        return consoleSender;
    }

    Collection<PlayerLibrarySender> getOnlineSenders() {
        return playerSenders.values();
    }

}
