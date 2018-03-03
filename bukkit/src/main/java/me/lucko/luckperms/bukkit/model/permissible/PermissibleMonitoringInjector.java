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

package me.lucko.luckperms.bukkit.model.permissible;

import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import me.lucko.luckperms.bukkit.compat.ReflectionUtil;

import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.ServerOperator;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Injects {@link MonitoredPermissibleBase}s into non-player permissibles on
 * the server so their checks can be monitored by the verbose facility.
 */
public class PermissibleMonitoringInjector implements Runnable {
    private final LPBukkitPlugin plugin;

    public PermissibleMonitoringInjector(LPBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        try {
            injectConsole();
        } catch (Exception e) {
            // ignore
        }

        try {
            injectCommandBlock();
        } catch (Exception e) {
            // ignore
        }

        try {
            injectEntity();
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
        ConsoleCommandSender consoleSender = this.plugin.getBootstrap().getServer().getConsoleSender();

        // get the ServerCommandSender class
        Class<?> serverCommandSenderClass = ReflectionUtil.obcClass("command.ServerCommandSender");

        // get the perm field
        Field permField = serverCommandSenderClass.getDeclaredField("perm");
        permField.setAccessible(true);

        // get the PermissibleBase instance
        PermissibleBase permBase = (PermissibleBase) permField.get(consoleSender);

        // create a monitored instance which delegates to the previous PermissibleBase
        MonitoredPermissibleBase newPermBase = wrap(permBase, "internal/console");

        // inject the monitored instance
        permField.set(consoleSender, newPermBase);
    }

    private void injectCommandBlock() throws Exception {
        // get the ServerCommandSender class
        Class<?> serverCommandSenderClass = ReflectionUtil.obcClass("command.ServerCommandSender");

        // get the blockPermInst field
        Field permField = serverCommandSenderClass.getDeclaredField("blockPermInst");
        permField.setAccessible(true);

        // get the PermissibleBase instance
        PermissibleBase permBase = (PermissibleBase) permField.get(null);

        // if no commandblock senders have been made yet, this field will be null
        // we can just initialise one anyway
        if (permBase == null) {
            permBase = new PermissibleBase(new CommandBlockServerOperator());
        }

        // create a monitored instance which delegates to the previous PermissibleBase
        MonitoredPermissibleBase newPermBase = wrap(permBase, "internal/commandblock");

        // inject the monitored instance
        permField.set(null, newPermBase);
    }

    private void injectEntity() throws Exception {
        // get the CraftEntity class
        Class<?> entityClass = ReflectionUtil.obcClass("entity.CraftEntity");

        // get the method used to obtain a PermissibleBase
        // this method will initialise a new PB instance if one doesn't yet exist
        Method getPermissibleBaseMethod = entityClass.getDeclaredMethod("getPermissibleBase");
        getPermissibleBaseMethod.setAccessible(true);

        // get the PermissibleBase instance
        PermissibleBase permBase = (PermissibleBase) getPermissibleBaseMethod.invoke(null);

        // get the perm field on CraftEntity
        Field permField = entityClass.getDeclaredField("perm");
        permField.setAccessible(true);

        // create a monitored instance which delegates to the previous PermissibleBase
        MonitoredPermissibleBase newPermBase = wrap(permBase, "internal/entity");

        // inject the monitored instance
        permField.set(null, newPermBase);
    }

    // behaviour copied from the implementation of obc.command.CraftBlockCommandSender
    private static final class CommandBlockServerOperator implements ServerOperator {
        @Override
        public boolean isOp() {
            return true;
        }

        @Override
        public void setOp(boolean value) {
            throw new UnsupportedOperationException("Cannot change operator status of a block");
        }
    }
}
