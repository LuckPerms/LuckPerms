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

package me.lucko.luckperms.bukkit.inject.dummy;

import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public class DummyPermissibleBase extends PermissibleBase {
    private static final Field ATTACHMENTS_FIELD;
    private static final Field PERMISSIONS_FIELD;

    static {
        try {
            ATTACHMENTS_FIELD = PermissibleBase.class.getDeclaredField("attachments");
            ATTACHMENTS_FIELD.setAccessible(true);

            PERMISSIONS_FIELD = PermissibleBase.class.getDeclaredField("permissions");
            PERMISSIONS_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static void copyFields(PermissibleBase from, PermissibleBase to) {
        try {
            ATTACHMENTS_FIELD.set(to, ATTACHMENTS_FIELD.get(from));
        } catch (Exception e) {
            // ignore
        }
        try {
            PERMISSIONS_FIELD.set(to, PERMISSIONS_FIELD.get(from));
        } catch (Exception e) {
            // ignore
        }
    }

    public static final DummyPermissibleBase INSTANCE = new DummyPermissibleBase();

    private DummyPermissibleBase() {
        super(null);

        // we want the singleton dummy attachment to be stateless
        // the behaviour of this class is to fail silently, so we can't use Collections.emptyX
        try {
            ATTACHMENTS_FIELD.set(this, new ArrayList<PermissionAttachment>(){
                @Override public boolean add(PermissionAttachment permissionAttachment) { return true; }
                @Override public void add(int index, PermissionAttachment element) { }
                @Override public boolean addAll(Collection<? extends PermissionAttachment> c) { return true; }
                @Override public boolean addAll(int index, Collection<? extends PermissionAttachment> c) { return true; }
            });
        } catch (Exception e) {
            // ignore
        }
        try {
            PERMISSIONS_FIELD.set(this, new HashMap<String, PermissionAttachmentInfo>() {
                @Override public PermissionAttachmentInfo put(String key, PermissionAttachmentInfo value) { return null; }
                @Override public void putAll(Map<? extends String, ? extends PermissionAttachmentInfo> m) { }
                @Override public PermissionAttachmentInfo putIfAbsent(String key, PermissionAttachmentInfo value) { return null; }
                @Override public PermissionAttachmentInfo compute(String key, BiFunction<? super String, ? super PermissionAttachmentInfo, ? extends PermissionAttachmentInfo> remappingFunction) { return null; }
                @Override public PermissionAttachmentInfo computeIfPresent(String key, BiFunction<? super String, ? super PermissionAttachmentInfo, ? extends PermissionAttachmentInfo> remappingFunction) { return null; }
                @Override public PermissionAttachmentInfo computeIfAbsent(String key, Function<? super String, ? extends PermissionAttachmentInfo> mappingFunction) { return null; }
            });
        } catch (Exception e) {
            // ignore
        }
    }

    @Override public boolean isOp() { return false; }
    @Override public void setOp(boolean value) {}
    @Override public boolean isPermissionSet(@NonNull String name) { return false; }
    @Override public boolean isPermissionSet(@NonNull Permission perm) { return false; }
    @Override public boolean hasPermission(@NonNull String inName) { return false; }
    @Override public boolean hasPermission(@NonNull Permission perm) { return false; }
    @Override public @NonNull PermissionAttachment addAttachment(@NonNull Plugin plugin, @NonNull String name, boolean value) { return null; }
    @Override public @NonNull PermissionAttachment addAttachment(@NonNull Plugin plugin) { return null; }
    @Override public void removeAttachment(@NonNull PermissionAttachment attachment) {}
    @Override public void recalculatePermissions() {}
    @Override public void clearPermissions() {}
    @Override public PermissionAttachment addAttachment(@NonNull Plugin plugin, @NonNull String name, boolean value, int ticks) { return null; }
    @Override public PermissionAttachment addAttachment(@NonNull Plugin plugin, int ticks) { return null; }
    @Override public @NonNull Set<PermissionAttachmentInfo> getEffectivePermissions() { return Collections.emptySet(); }

}
