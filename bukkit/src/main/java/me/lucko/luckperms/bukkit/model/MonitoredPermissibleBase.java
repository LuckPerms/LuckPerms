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

package me.lucko.luckperms.bukkit.model;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.verbose.CheckOrigin;
import me.lucko.luckperms.common.verbose.VerboseHandler;

import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import java.util.Set;

/**
 * A PermissibleBase extension which logs permission checks to the
 * plugin's {@link VerboseHandler} facility.
 *
 * Method calls are forwarded to the delegate permissible.
 */
public class MonitoredPermissibleBase extends PermissibleBase {
    private final VerboseHandler verboseHandler;
    private final PermissibleBase delegate;
    private final String name;

    // remains false until the object has been constructed
    // necessary to catch the superclass call to #recalculatePermissions on init
    @SuppressWarnings("UnusedAssignment")
    private boolean initialised = false;

    public MonitoredPermissibleBase(VerboseHandler verboseHandler, PermissibleBase delegate, String name) {
        super(null);
        DummyPermissibleBase.nullFields(this);

        this.verboseHandler = verboseHandler;
        this.delegate = delegate;
        this.name = name;
        this.initialised = true;

        // since we effectively cancel the execution of this call in the super
        // constructor we need to call it again.
        recalculatePermissions();
    }

    private void logCheck(CheckOrigin origin, String permission, boolean result) {
        this.verboseHandler.offerCheckData(origin, this.name, ContextSet.empty(), permission, Tristate.fromBoolean(result));
    }

    PermissibleBase getDelegate() {
        return this.delegate;
    }

    @Override
    public boolean isPermissionSet(String permission) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }

        final boolean result = this.delegate.isPermissionSet(permission);
        logCheck(CheckOrigin.PLATFORM_LOOKUP_CHECK, permission, result);
        return result;
    }

    @Override
    public boolean isPermissionSet(Permission permission) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }

        final boolean result = this.delegate.isPermissionSet(permission);
        logCheck(CheckOrigin.PLATFORM_LOOKUP_CHECK, permission.getName(), result);
        return result;
    }

    @Override
    public boolean hasPermission(String permission) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }

        final boolean result = this.delegate.hasPermission(permission);
        logCheck(CheckOrigin.PLATFORM_PERMISSION_CHECK, permission, result);
        return result;
    }

    @Override
    public boolean hasPermission(Permission permission) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }

        final boolean result = this.delegate.hasPermission(permission);
        logCheck(CheckOrigin.PLATFORM_PERMISSION_CHECK, permission.getName(), result);
        return result;
    }

    @Override
    public void recalculatePermissions() {
        if (!this.initialised) {
            return;
        }

        this.delegate.recalculatePermissions();
    }

    // just forward calls to the delegate permissible
    @Override public boolean isOp() { return this.delegate.isOp(); }
    @Override public void setOp(boolean value) { this.delegate.setOp(value); }
    @Override public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) { return this.delegate.addAttachment(plugin, name, value); }
    @Override public PermissionAttachment addAttachment(Plugin plugin) { return this.delegate.addAttachment(plugin); }
    @Override public void removeAttachment(PermissionAttachment attachment) { this.delegate.removeAttachment(attachment); }
    @Override public void clearPermissions() { this.delegate.clearPermissions(); }
    @Override public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) { return this.delegate.addAttachment(plugin, name, value, ticks); }
    @Override public PermissionAttachment addAttachment(Plugin plugin, int ticks) { return this.delegate.addAttachment(plugin, ticks); }
    @Override public Set<PermissionAttachmentInfo> getEffectivePermissions() { return this.delegate.getEffectivePermissions(); }

}
