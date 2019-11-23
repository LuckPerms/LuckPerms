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

import net.luckperms.api.util.Tristate;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Event;

import java.util.Objects;

/**
 * Copy of the internal BungeeCord PermissionCheckEvent, returning a tristate instead of a boolean.
 */
public class TristateCheckEvent extends Event {

    public static Tristate call(CommandSender sender, String permission) {

        // calculate a default value based on the internal behaviour of
        // ConsoleCommandSender and UserConnection in order to replicate the
        // behaviour of CommandSender#hasPermission
        Tristate def = Tristate.UNDEFINED;
        if (!(sender instanceof ProxiedPlayer)) {
            // if not a ProxiedPlayer, assume it's console.
            def = Tristate.TRUE;
        } else if (sender.getPermissions().contains(permission)) {
            def = Tristate.TRUE;
        }

        return call(sender, permission, def);
    }

    public static Tristate call(CommandSender sender, String permission, Tristate def) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(permission, "permission");
        Objects.requireNonNull(def, "def");

        TristateCheckEvent event = new TristateCheckEvent(sender, permission, def);
        return ProxyServer.getInstance().getPluginManager().callEvent(event).getResult();
    }

    private final CommandSender sender;
    private final String permission;
    private Tristate result;

    private TristateCheckEvent(CommandSender sender, String permission, Tristate result) {
        this.sender = sender;
        this.permission = permission;
        this.result = result;
    }

    public CommandSender getSender() {
        return this.sender;
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
                "sender=" + this.sender + ", " +
                "permission=" + this.permission + ", " +
                "result=" + this.result + ")";
    }

}
