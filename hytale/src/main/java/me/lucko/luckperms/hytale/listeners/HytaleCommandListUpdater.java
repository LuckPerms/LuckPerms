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

package me.lucko.luckperms.hytale.listeners;

import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.handlers.game.GamePacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import me.lucko.luckperms.common.event.listeners.AbstractCommandListUpdater;
import me.lucko.luckperms.hytale.LPHytalePlugin;

import java.util.UUID;

public class HytaleCommandListUpdater extends AbstractCommandListUpdater<LPHytalePlugin, PlayerRef> {
    public HytaleCommandListUpdater(LPHytalePlugin plugin) {
        super(plugin, PlayerRef.class);
    }

    @Override
    protected boolean isServerAvailable() {
        return true;
    }

    @Override
    protected UUID getUniqueId(PlayerRef player) {
        return player.getUuid();
    }

    @Override
    protected void sendCommandListUpdate(UUID uniqueId) {
        PlayerRef playerRef = Universe.get().getPlayer(uniqueId);
        if (playerRef != null) {
            PacketHandler handler = playerRef.getPacketHandler();
            if (handler instanceof GamePacketHandler gameHandler) {
                gameHandler.sendCommandTree();
            }
        }
    }

}
