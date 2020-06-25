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

package me.lucko.luckperms.fabric.listeners;

import com.mojang.authlib.GameProfile;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener;
import me.lucko.luckperms.fabric.LPFabricPlugin;
import net.minecraft.server.network.ServerPlayerEntity;

public class FabricConnectionListener extends AbstractConnectionListener {

    private final LPFabricPlugin plugin;

    public FabricConnectionListener(LPFabricPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    public void onDisconnect(ServerPlayerEntity playerEntity) {
        this.handleDisconnect(playerEntity.getUuid());
    }

    public void onLogin(ServerPlayerEntity playerEntity) {
        GameProfile gameProfile = playerEntity.getGameProfile();
        this.loadUser(gameProfile.getId(), gameProfile.getName());
    }

    public void onEarlyLogin(GameProfile gameProfile) {
        // TODO: Use Networking v1 when merged to delay login till database has loaded the user
        User user = this.loadUser(gameProfile.getId(), gameProfile.getName());
        this.recordConnection(gameProfile.getId());
        this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(gameProfile.getId(), gameProfile.getName(), user);
    }
}
