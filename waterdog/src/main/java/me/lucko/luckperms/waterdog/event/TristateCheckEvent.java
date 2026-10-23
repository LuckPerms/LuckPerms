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

package me.lucko.luckperms.waterdog.event;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.command.CommandSender;
import dev.waterdog.waterdogpe.event.Event;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.utils.types.Permission;
import net.luckperms.api.util.Tristate;

import java.util.Objects;

/**
 * Copy of the internal WaterdogPE permission check, returning a tristate instead of a boolean.
 */
public class TristateCheckEvent extends Event {

    public static Tristate call(CommandSender sender, String permission) {

        // calculate a default value based on the internal behaviour of
        // ConsoleCommandSender and ProxiedPlayer in order to replicate the
        // behaviour of CommandSender#hasPermission
        Tristate def = Tristate.UNDEFINED;
        if (!(sender instanceof ProxiedPlayer)) {
            // if not a ProxiedPlayer, assume it's console.
            def = Tristate.TRUE;
        } else {
            // check the raw permission map directly to avoid firing another permission check event.
            Permission perm = ((ProxiedPlayer) sender).getPermission(permission);
            if (perm != null && perm.getValue()) {
                def = Tristate.TRUE;
            }
        }

        return call(sender, permission, def);
    }

    public static Tristate call(CommandSender sender, String permission, Tristate def) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(permission, "permission");
        Objects.requireNonNull(def, "def");

        TristateCheckEvent event = new TristateCheckEvent(sender, permission, def);
        // this is not an async or completable event, so it is handled synchronously and the
        // result can be read from the event instance once #callEvent returns.
        ProxyServer.getInstance().getEventManager().callEvent(event);
        return event.getResult();
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
