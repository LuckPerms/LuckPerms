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

import lombok.experimental.UtilityClass;

import me.lucko.luckperms.bukkit.compat.ReflectionUtil;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.PermissionAttachment;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Injects a {@link LPPermissible} into a {@link Player}
 */
@SuppressWarnings("unchecked")
@UtilityClass
public class Injector {
    private static final Map<UUID, LPPermissible> INJECTED_PERMISSIBLES = new ConcurrentHashMap<>();

    private static Field humanEntityField;
    private static Field permissibleAttachmentsField;
    private static Throwable cachedThrowable = null;

    static {
        try {
            try {
                // craftbukkit
                humanEntityField = ReflectionUtil.obcClass("entity.CraftHumanEntity").getDeclaredField("perm");
                humanEntityField.setAccessible(true);
            } catch (Exception e) {
                // glowstone
                humanEntityField = Class.forName("net.glowstone.entity.GlowHumanEntity").getDeclaredField("permissions");
                humanEntityField.setAccessible(true);
            }

            permissibleAttachmentsField = PermissibleBase.class.getDeclaredField("attachments");
            permissibleAttachmentsField.setAccessible(true);

        } catch (Throwable t) {
            cachedThrowable = t;
            t.printStackTrace();
        }
    }

    public static boolean inject(Player player, LPPermissible lpPermissible) {
        if (cachedThrowable != null) {
            cachedThrowable.printStackTrace();
            return false;
        }

        try {
            PermissibleBase existing = (PermissibleBase) humanEntityField.get(player);
            if (existing instanceof LPPermissible) {
                // uh oh
                throw new IllegalStateException("LPPermissible already injected into player " + player.toString());
            }

            // Move attachments over from the old permissible.
            List<PermissionAttachment> attachments = (List<PermissionAttachment>) permissibleAttachmentsField.get(existing);
            lpPermissible.addAttachments(attachments);
            attachments.clear();
            existing.clearPermissions();

            lpPermissible.getActive().set(true);
            lpPermissible.recalculatePermissions(false);
            lpPermissible.setOldPermissible(existing);

            lpPermissible.updateSubscriptionsAsync();

            humanEntityField.set(player, lpPermissible);
            INJECTED_PERMISSIBLES.put(player.getUniqueId(), lpPermissible);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean unInject(Player player, boolean dummy, boolean unsubscribe) {
        if (cachedThrowable != null) {
            cachedThrowable.printStackTrace();
            return false;
        }

        try {
            PermissibleBase permissible = (PermissibleBase) humanEntityField.get(player);
            if (permissible instanceof LPPermissible) {

                permissible.clearPermissions();

                if (unsubscribe) {
                    ((LPPermissible) permissible).unsubscribeFromAllAsync();
                }

                ((LPPermissible) permissible).getActive().set(false);

                if (dummy) {
                    humanEntityField.set(player, new DummyPermissibleBase());
                } else {
                    LPPermissible lpp = ((LPPermissible) permissible);
                    List<PermissionAttachment> attachments = lpp.getAttachments();

                    PermissibleBase newPb = lpp.getOldPermissible();
                    if (newPb == null) {
                        newPb = new PermissibleBase(player);
                    }

                    List<PermissionAttachment> newAttachments = (List<PermissionAttachment>) permissibleAttachmentsField.get(newPb);
                    newAttachments.addAll(attachments);
                    attachments.clear();

                    humanEntityField.set(player, newPb);
                }
            }
            INJECTED_PERMISSIBLES.remove(player.getUniqueId());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static LPPermissible getPermissible(UUID uuid) {
        return INJECTED_PERMISSIBLES.get(uuid);
    }

}
