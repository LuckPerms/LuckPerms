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

package me.lucko.luckperms.forge.event;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

public abstract class ConnectionEvent extends Event {

    private final Connection connection;
    private final GameProfile profile;

    public ConnectionEvent(Connection connection, GameProfile profile) {
        this.connection = connection;
        this.profile = profile;
    }

    public Connection getConnection() {
        return connection;
    }

    public GameProfile getProfile() {
        return profile;
    }

    public static class Auth extends ConnectionEvent {

        public Auth(Connection connection, GameProfile profile) {
            super(connection, profile);
        }
    }

    @Cancelable
    public static class Login extends ConnectionEvent {

        private final ServerPlayer player;
        private Component message;

        public Login(Connection connection, ServerPlayer player) {
            super(connection, player.getGameProfile());
            this.player = player;
        }

        public ServerPlayer getPlayer() {
            return player;
        }

        public Component getMessage() {
            return message;
        }

        public void setMessage(Component message) {
            this.message = message;
        }
    }

    public static class Disconnect extends ConnectionEvent {

        public Disconnect(Connection connection, GameProfile profile) {
            super(connection, profile);
        }
    }

}
