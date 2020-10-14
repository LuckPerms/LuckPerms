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

package me.lucko.luckperms.sponge.listeners;

import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.sponge.LPSpongePlugin;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.command.SendCommandEvent;

public class SpongePlatformListener {
    private final LPSpongePlugin plugin;

    public SpongePlatformListener(LPSpongePlugin plugin) {
        this.plugin = plugin;
    }

    @Listener
    public void onSendCommand(SendCommandEvent e) {
        CommandSource source = e.getCause().first(CommandSource.class).orElse(null);
        if (source == null) return;

        final String name = e.getCommand().toLowerCase();
        if (((name.equals("op") || name.equals("minecraft:op")) && source.hasPermission("minecraft.command.op")) || ((name.equals("deop") || name.equals("minecraft:deop")) && source.hasPermission("minecraft.command.deop"))) {
            Message.OP_DISABLED_SPONGE.send(this.plugin.getSenderFactory().wrap(source));
        }
    }
}
