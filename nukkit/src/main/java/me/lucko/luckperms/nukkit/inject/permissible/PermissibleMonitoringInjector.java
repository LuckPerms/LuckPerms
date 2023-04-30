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

package me.lucko.luckperms.nukkit.inject.permissible;

import cn.nukkit.command.ConsoleCommandSender;
import cn.nukkit.permission.PermissibleBase;
import me.lucko.luckperms.nukkit.LPNukkitPlugin;

import java.lang.reflect.Field;
import java.util.Objects;

/**
 * Injects {@link MonitoredPermissibleBase}s into non-player permissibles on
 * the server so their checks can be monitored by the verbose facility.
 */
public class PermissibleMonitoringInjector implements Runnable {
    private final LPNukkitPlugin plugin;

    public enum Mode {
        INJECT, UNINJECT
    }

    private final Mode mode;

    public PermissibleMonitoringInjector(LPNukkitPlugin plugin, Mode mode) {
        this.plugin = plugin;
        this.mode = mode;
    }

    @Override
    public void run() {
        try {
            injectConsole();
        } catch (Exception e) {
            // ignore
        }
    }

    private PermissibleBase transform(PermissibleBase permBase, String name) {
        Objects.requireNonNull(permBase, "permBase");

        // don't bother injecting if already setup.
        if (this.mode == Mode.INJECT && permBase instanceof MonitoredPermissibleBase && ((MonitoredPermissibleBase) permBase).plugin == this.plugin) {
            return null;
        }

        // unwrap any previous injection
        if (permBase instanceof MonitoredPermissibleBase) {
            permBase = ((MonitoredPermissibleBase) permBase).getDelegate();
        }

        // if the mode is uninject, just return the unwrapped PermissibleBase
        if (this.mode == Mode.UNINJECT) {
            return permBase;
        }

        // create a monitored instance which delegates to the previous PermissibleBase
        return new MonitoredPermissibleBase(this.plugin, permBase, name);
    }

    private void injectConsole() throws Exception {
        ConsoleCommandSender consoleSender = this.plugin.getBootstrap().getServer().getConsoleSender();

        // get the perm field
        Field permField = ConsoleCommandSender.class.getDeclaredField("perm");
        permField.setAccessible(true);

        // get the PermissibleBase instance
        PermissibleBase permBase = (PermissibleBase) permField.get(consoleSender);

        // create a new instance which delegates to the previous PermissibleBase
        PermissibleBase newPermBase = transform(permBase, "internal/console");
        if (newPermBase == null) {
            return;
        }

        // inject the new instance
        permField.set(consoleSender, newPermBase);
    }
}
