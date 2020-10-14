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

package me.lucko.luckperms.sponge;

import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.sender.SenderFactory;
import me.lucko.luckperms.sponge.service.CompatibilityUtil;

import net.kyori.adventure.platform.spongeapi.SpongeAudiences;
import net.kyori.adventure.text.Component;
import net.luckperms.api.util.Tristate;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;

import java.util.UUID;

public class SpongeSenderFactory extends SenderFactory<LPSpongePlugin, CommandSource> {
    private final SpongeAudiences audiences;

    public SpongeSenderFactory(LPSpongePlugin plugin) {
        super(plugin);
        this.audiences = SpongeAudiences.create(plugin.getBootstrap().getPluginContainer(), plugin.getBootstrap().getGame());
    }

    @Override
    protected String getName(CommandSource source) {
        if (source instanceof Player) {
            return source.getName();
        }
        return Sender.CONSOLE_NAME;
    }

    @Override
    protected UUID getUniqueId(CommandSource source) {
        if (source instanceof Player) {
            return ((Player) source).getUniqueId();
        }
        return Sender.CONSOLE_UUID;
    }

    @Override
    protected void sendMessage(CommandSource source, Component message) {
        this.audiences.receiver(source).sendMessage(message);
    }

    @Override
    protected Tristate getPermissionValue(CommandSource source, String node) {
        Tristate result = CompatibilityUtil.convertTristate(source.getPermissionValue(source.getActiveContexts(), node));

        // check the permdefault
        if (result == Tristate.UNDEFINED && source.hasPermission(node)) {
            result = Tristate.TRUE;
        }

        return result;
    }

    @Override
    protected boolean hasPermission(CommandSource source, String node) {
        return source.hasPermission(node);
    }

    @Override
    protected void performCommand(CommandSource source, String command) {
        getPlugin().getBootstrap().getGame().getCommandManager().process(source, command);
    }

    @Override
    public void close() {
        super.close();
        this.audiences.close();
    }
}
