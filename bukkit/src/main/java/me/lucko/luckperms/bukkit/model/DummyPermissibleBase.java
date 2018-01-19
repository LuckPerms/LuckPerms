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

import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;

public class DummyPermissibleBase extends PermissibleBase {
    private static final Field ATTACHMENTS_FIELD;
    private static final Field PERMISSIONS_FIELD;

    static {
        Field attachmentsField;
        try {
            attachmentsField = PermissibleBase.class.getDeclaredField("attachments");
            attachmentsField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
        ATTACHMENTS_FIELD = attachmentsField;

        Field permissionsField;
        try {
            permissionsField = PermissibleBase.class.getDeclaredField("permissions");
            permissionsField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
        PERMISSIONS_FIELD = permissionsField;
    }

    public static void nullFields(PermissibleBase permissibleBase) {
        try {
            ATTACHMENTS_FIELD.set(permissibleBase, null);
        } catch (Exception e) {
            // ignore
        }
        try {
            PERMISSIONS_FIELD.set(permissibleBase, null);
        } catch (Exception e) {
            // ignore
        }
    }

    public static final DummyPermissibleBase INSTANCE = new DummyPermissibleBase();

    private DummyPermissibleBase() {
        super(null);
        nullFields(this);
    }

    @Override public boolean isOp() { return false; }
    @Override public void setOp(boolean value) {}
    @Override public boolean isPermissionSet(String name) { return false; }
    @Override public boolean isPermissionSet(Permission perm) { return false; }
    @Override public boolean hasPermission(String inName) { return false; }
    @Override public boolean hasPermission(Permission perm) { return false; }
    @Override public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) { return null; }
    @Override public PermissionAttachment addAttachment(Plugin plugin) { return null; }
    @Override public void removeAttachment(PermissionAttachment attachment) {}
    @Override public void recalculatePermissions() {}
    @Override public void clearPermissions() {}
    @Override public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) { return null; }
    @Override public PermissionAttachment addAttachment(Plugin plugin, int ticks) { return null; }
    @Override public Set<PermissionAttachmentInfo> getEffectivePermissions() { return Collections.emptySet(); }

}
