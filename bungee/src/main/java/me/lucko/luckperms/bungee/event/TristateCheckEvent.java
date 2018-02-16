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

package me.lucko.luckperms.bungee.event;

import me.lucko.luckperms.api.Tristate;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Event;

/**
 * Copy of the internal BungeeCord PermissionCheckEvent, returning a tristate instead of a boolean.
 */
public class TristateCheckEvent extends Event {
    public static Tristate call(ProxiedPlayer player, String permission) {
        return ProxyServer.getInstance().getPluginManager().callEvent(new TristateCheckEvent(player, permission)).getResult();
    }

    private final ProxiedPlayer player;
    private final String permission;

    private Tristate result;

    public TristateCheckEvent(ProxiedPlayer player, String permission) {
        this(player, permission, player.getPermissions().contains(permission) ? Tristate.TRUE : Tristate.UNDEFINED);
    }

    public TristateCheckEvent(ProxiedPlayer player, String permission, Tristate result) {
        this.player = player;
        this.permission = permission;
        this.result = result;
    }

    public ProxiedPlayer getPlayer() {
        return this.player;
    }

    public String getPermission() {
        return this.permission;
    }

    public Tristate getResult() {
        return this.result;
    }

    public void setResult(Tristate result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "TristateCheckEvent(" +
                "player=" + this.player + ", " +
                "permission=" + this.permission + ", " +
                "result=" + this.result + ")";
    }

}
