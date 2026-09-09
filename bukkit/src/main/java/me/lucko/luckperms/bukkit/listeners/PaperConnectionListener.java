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

package me.lucko.luckperms.bukkit.listeners;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.connection.PlayerLoginConnection;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.UUID;

/**
 * Tracks players in the configuration phase (Paper 1.20.2+) so the user
 * housekeeper does not unload their data while {@link org.bukkit.Server#getPlayer(UUID)}
 * still returns null for them.
 *
 * <p>Only registered when the Paper connection events exist. Entries expire on
 * their own, join and quit remove them eagerly.</p>
 */
public class PaperConnectionListener implements Listener {
    private final Set<UUID> configuringPlayers;

    public PaperConnectionListener(Set<UUID> configuringPlayers) {
        this.configuringPlayers = configuringPlayers;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onValidateLogin(PlayerConnectionValidateLoginEvent e) {
        if (!(e.getConnection() instanceof PlayerLoginConnection)) {
            return;
        }
        PlayerLoginConnection connection = (PlayerLoginConnection) e.getConnection();

        PlayerProfile profile = connection.getAuthenticatedProfile();
        if (profile == null) {
            profile = connection.getUnsafeProfile();
        }
        if (profile == null || profile.getId() == null) {
            return;
        }
        this.configuringPlayers.add(profile.getId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent e) {
        this.configuringPlayers.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent e) {
        this.configuringPlayers.remove(e.getPlayer().getUniqueId());
    }
}
