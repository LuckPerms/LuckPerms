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

package me.lucko.luckperms.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.SenderFactory;
import me.lucko.luckperms.common.utils.TextUtils;

import net.kyori.text.Component;

import java.util.UUID;

public class VelocitySenderFactory extends SenderFactory<CommandSource> {
    public VelocitySenderFactory(LuckPermsPlugin plugin) {
        super(plugin);
    }

    @Override
    protected String getName(CommandSource source) {
        if (source instanceof Player) {
            return ((Player) source).getUsername();
        }
        return CommandManager.CONSOLE_NAME;
    }

    @Override
    protected UUID getUuid(CommandSource source) {
        if (source instanceof Player) {
            return ((Player) source).getUniqueId();
        }
        return CommandManager.CONSOLE_UUID;
    }

    @Override
    protected void sendMessage(CommandSource source, String s) {
        sendMessage(source, TextUtils.fromLegacy(s));
    }

    @Override
    protected void sendMessage(CommandSource source, Component message) {
        source.sendMessage(message);
    }

    @Override
    protected Tristate getPermissionValue(CommandSource source, String node) {
        return Tristate.fromBoolean(hasPermission(source, node));
    }

    @Override
    protected boolean hasPermission(CommandSource source, String node) {
        return source.hasPermission(node);
    }
}
