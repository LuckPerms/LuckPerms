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

package me.lucko.luckperms.hytale.listeners;

import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.event.events.BootEvent;
import me.lucko.luckperms.hytale.LPHytalePlugin;

import java.util.Collection;

public class HytalePlatformListener {
    private final LPHytalePlugin plugin;

    public HytalePlatformListener(LPHytalePlugin plugin) {
        this.plugin = plugin;
    }

    public void register(EventRegistry registry) {
        registry.registerGlobal(BootEvent.class, this::onBoot);
    }

    public void onBoot(BootEvent e) {
        insertCommandPermissionsIntoRegistry();
        this.plugin.getVirtualGroupsMap().refresh();
    }

    public void insertCommandPermissionsIntoRegistry() {
        insertCommandPermissionsIntoRegistry(CommandManager.get().getCommandRegistration().values());
    }

    private void insertCommandPermissionsIntoRegistry(Collection<AbstractCommand> commands) {
        for (AbstractCommand command : commands) {
            String permission = command.getPermission();
            if (permission != null) {
                this.plugin.getPermissionRegistry().insert(permission);
            }

            Collection<AbstractCommand> subCommands = command.getSubCommands().values();
            if (!subCommands.isEmpty()) {
                insertCommandPermissionsIntoRegistry(subCommands);
            }
        }
    }

}
