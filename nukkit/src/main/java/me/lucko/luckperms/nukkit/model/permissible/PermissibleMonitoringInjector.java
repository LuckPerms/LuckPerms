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

package me.lucko.luckperms.nukkit.model.permissible;

import me.lucko.luckperms.nukkit.LPNukkitPlugin;

import cn.nukkit.command.ConsoleCommandSender;
import cn.nukkit.permission.PermissibleBase;

import java.lang.reflect.Field;
import java.util.Objects;

/**
 * Injects {@link MonitoredPermissibleBase}s into non-player permissibles on
 * the server so their checks can be monitored by the verbose facility.
 */
public class PermissibleMonitoringInjector implements Runnable {
    private final LPNukkitPlugin plugin;

    public PermissibleMonitoringInjector(LPNukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        try {
            injectConsole();
        } catch (Exception e) {
            // ignore
        }
    }

    private MonitoredPermissibleBase wrap(PermissibleBase permBase, String name) {
        Objects.requireNonNull(permBase, "permBase");

        // unwrap any previous injection
        if (permBase instanceof MonitoredPermissibleBase) {
            permBase = ((MonitoredPermissibleBase) permBase).getDelegate();
        }

        // create a monitored instance which delegates to the previous PermissibleBase
        return new MonitoredPermissibleBase(this.plugin, permBase, name);
    }

    private void injectConsole() throws Exception {
        ConsoleCommandSender consoleSender = this.plugin.getServer().getConsoleSender();

        // get the perm field
        Field permField = ConsoleCommandSender.class.getDeclaredField("perm");
        permField.setAccessible(true);

        // get the PermissibleBase instance
        PermissibleBase permBase = (PermissibleBase) permField.get(consoleSender);

        // create a monitored instance which delegates to the previous PermissibleBase
        MonitoredPermissibleBase newPermBase = wrap(permBase, "internal/console");

        // inject the monitored instance
        permField.set(consoleSender, newPermBase);
    }
}
