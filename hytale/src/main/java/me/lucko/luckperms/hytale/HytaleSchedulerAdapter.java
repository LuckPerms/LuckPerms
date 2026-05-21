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

package me.lucko.luckperms.hytale;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.receiver.IMessageReceiver;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.lucko.luckperms.common.plugin.scheduler.JavaSchedulerAdapter;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;
import me.lucko.luckperms.common.sender.AbstractSender;
import me.lucko.luckperms.common.sender.Sender;

public class HytaleSchedulerAdapter extends JavaSchedulerAdapter implements SchedulerAdapter {

    public HytaleSchedulerAdapter(LPHytaleBootstrap bootstrap) {
        super(bootstrap);
    }

    @Override
    public void executeSync(Sender ctx, Runnable task) {
        executeSync(unwrapSender(ctx), task);
    }

    public void executeSync(IMessageReceiver ctx, Runnable task) {
        if (ctx instanceof PlayerRef playerRef) {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null) {
                World world = ref.getStore().getExternalData().getWorld();
                world.execute(task);
                return;
            }
        } else if (ctx instanceof Player player) {
            World world = player.getWorld();
            if (world != null) {
                world.execute(task);
                return;
            }
        }

        // fallback
        executeAsync(task);
    }

    @SuppressWarnings("unchecked")
    private static IMessageReceiver unwrapSender(Sender sender) {
        if (sender instanceof AbstractSender) {
            return ((AbstractSender<IMessageReceiver>) sender).getSender();
        } else {
            throw new IllegalArgumentException("unknown sender type: " + sender.getClass());
        }
    }
}
