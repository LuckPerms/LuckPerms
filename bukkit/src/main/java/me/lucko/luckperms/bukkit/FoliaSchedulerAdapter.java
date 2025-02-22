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

package me.lucko.luckperms.bukkit;

import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;
import me.lucko.luckperms.common.sender.AbstractSender;
import me.lucko.luckperms.common.sender.Sender;
import org.bukkit.Server;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

public class FoliaSchedulerAdapter extends BukkitSchedulerAdapter implements SchedulerAdapter {
    private final JavaPlugin loader;
    private final Server server;

    public FoliaSchedulerAdapter(LPBukkitBootstrap bootstrap) {
        super(bootstrap);
        this.loader = bootstrap.getLoader();
        this.server = bootstrap.getServer();
    }

    @Override
    public void sync(Runnable task) {
        this.server.getGlobalRegionScheduler().execute(this.loader, task);
    }

    @Override
    public void sync(Sender ctx, Runnable task) {
        sync(unwrapSender(ctx), task);
    }

    @Override
    public void sync(CommandSender ctx, Runnable task) {
        if (ctx instanceof Entity) {
            ((Entity) ctx).getScheduler().execute(this.loader, task, null, 0);
        } else if (ctx instanceof BlockCommandSender) {
            RegionScheduler scheduler = this.server.getRegionScheduler();
            scheduler.execute(this.loader, ((BlockCommandSender) ctx).getBlock().getLocation(), task);
        } else if (ctx instanceof ConsoleCommandSender || ctx instanceof RemoteConsoleCommandSender) {
            this.server.getGlobalRegionScheduler().execute(this.loader, task);
        } else if (ctx instanceof ProxiedCommandSender) {
            sync(((ProxiedCommandSender) ctx).getCallee(), task);
        } else {
            throw new IllegalArgumentException("Unknown command sender type: " + ctx.getClass().getName());
        }
    }

    @SuppressWarnings("unchecked")
    private static CommandSender unwrapSender(Sender sender) {
        if (sender instanceof AbstractSender) {
            return ((AbstractSender<CommandSender>) sender).getSender();
        } else {
            throw new IllegalArgumentException("unknown sender type: " + sender.getClass());
        }
    }

}
