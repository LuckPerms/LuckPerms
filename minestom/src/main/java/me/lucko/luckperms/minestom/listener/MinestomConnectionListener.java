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

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener;
import me.lucko.luckperms.minestom.LPMinestomPlugin;
import me.lucko.luckperms.minestom.options.PlayerQueryMap;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerLoginEvent;

public class MinestomConnectionListener extends AbstractConnectionListener {
    private final LPMinestomPlugin plugin;

    public MinestomConnectionListener(LPMinestomPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    public void registerListeners() {
        GlobalEventHandler eventManager = MinecraftServer.getGlobalEventHandler();

        eventManager.addListener(AsyncPlayerPreLoginEvent.class, (this::asyncPreLoginHandler));
        eventManager.addListener(PlayerLoginEvent.class, (this::loginEventHandler));
    }

    private void asyncPreLoginHandler(AsyncPlayerPreLoginEvent event) {
        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Preparing login for " + event.getPlayerUuid() + " - " + event.getUsername());
        }

        try {
            User user = loadUser(event.getPlayerUuid(), event.getUsername());
            recordConnection(event.getPlayerUuid());
            this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(event.getPlayerUuid(), event.getUsername(), user);
        } catch (Exception e) {
            this.plugin.getLogger().severe("Exception occurred whilst loading data for " + event.getPlayerUuid() + " - " + event.getUsername(), e);
            Component kickMsg = TranslationManager.render(Message.LOADING_DATABASE_ERROR.build());
            event.getPlayer().kick(kickMsg);
        }
    }

    private void loginEventHandler(PlayerLoginEvent event) {
        final Player player = event.getPlayer();

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing login for " + player.getUuid() + " - " + player.getUsername());
        }

        final User user = this.plugin.getUserManager().getIfLoaded(player.getUuid());

        // If the user is null something went badly wrong, so we need to kick them
        if (user == null) {
            this.plugin.getLogger().warn("User " + player.getUuid() + " - " + player.getUsername() + " doesn't have data preloaded - denying login");
            Component kickMsg = TranslationManager.render(Message.LOADING_STATE_ERROR.build());
            player.kick(kickMsg);
        }

        PlayerQueryMap.initializePermissions(player, user);
        this.plugin.getContextManager().signalContextUpdate(player);
    }

}
