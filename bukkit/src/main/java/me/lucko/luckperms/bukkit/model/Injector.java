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
 * Injects a {@link LPPermissible} into a {@link Player}.
 *
 * This allows LuckPerms to directly intercept permission checks and take over all handling of
 * checks made by plugins.
 */
@UtilityClass
public class Injector {
    private static final Map<UUID, LPPermissible> INJECTED_PERMISSIBLES = new ConcurrentHashMap<>();

    /**
     * All permission checks made on standard Bukkit objects are effectively proxied to a
     * {@link PermissibleBase} object, held as a parameter on the object.
     *
     * This field is where the permissible is stored on a HumanEntity.
     */
    private static Field humanEntityPermissibleField;

    /**
     * The field where attachments are stored on a permissible base.
     */
    private static Field permissibleBaseAttachmentsField;

    private static Throwable cachedThrowable = null;
    static {
        try {
            // Catch all. If this setup doesn't fully complete without
            // exceptions, then the Injector will not work.

            // Try to load the permissible field.
            try {
                // craftbukkit
                humanEntityPermissibleField = ReflectionUtil.obcClass("entity.CraftHumanEntity").getDeclaredField("perm");
                humanEntityPermissibleField.setAccessible(true);

            } catch (Exception e) {
                // glowstone
                humanEntityPermissibleField = Class.forName("net.glowstone.entity.GlowHumanEntity").getDeclaredField("permissions");
                humanEntityPermissibleField.setAccessible(true);
            }

            // Try to load the attachments field.
            permissibleBaseAttachmentsField = PermissibleBase.class.getDeclaredField("attachments");
            permissibleBaseAttachmentsField.setAccessible(true);

        } catch (Throwable t) {
            cachedThrowable = t;
            t.printStackTrace();
        }
    }

    /**
     * Injects a {@link LPPermissible} into a {@link Player}.
     *
     * @param player the player to inject into
     * @param newPermissible the permissible to inject
     * @throws Exception propagates any exceptions which were thrown during injection
     */
    public static void inject(Player player, LPPermissible newPermissible) throws Exception {

        // make sure the class inited without errors, otherwise, print a trace
        if (cachedThrowable != null) {
            throw new RuntimeException("Injector did not init successfully.", cachedThrowable);
        }

        // get the existing PermissibleBase held by the player
        PermissibleBase oldPermissible = (PermissibleBase) humanEntityPermissibleField.get(player);

        // seems we have already injected into this player.
        if (oldPermissible instanceof LPPermissible) {
            throw new IllegalStateException("LPPermissible already injected into player " + player.toString());
        }

        // Move attachments over from the old permissible

        //noinspection unchecked
        List<PermissionAttachment> attachments = (List<PermissionAttachment>) permissibleBaseAttachmentsField.get(oldPermissible);

        newPermissible.addAttachments(attachments);
        attachments.clear();
        oldPermissible.clearPermissions();

        // Setup the new permissible
        newPermissible.getActive().set(true);
        newPermissible.recalculatePermissions(false);
        newPermissible.setOldPermissible(oldPermissible);
        newPermissible.updateSubscriptionsAsync();

        // inject the new instance
        humanEntityPermissibleField.set(player, newPermissible);

        // register the injection with the map
        INJECTED_PERMISSIBLES.put(player.getUniqueId(), newPermissible);
    }

    /**
     * Uninjects a {@link LPPermissible} from a {@link Player}.
     *
     * @param player the player to uninject from
     * @param dummy if the replacement permissible should be a dummy.
     * @param unsubscribe if the extracted permissible should unsubscribe itself. see {@link SubscriptionManager}.
     * @throws Exception propagates any exceptions which were thrown during uninjection
     */
    public static void unInject(Player player, boolean dummy, boolean unsubscribe) throws Exception {
        // make sure the class inited without errors, otherwise, print a trace
        if (cachedThrowable != null) {
            throw new RuntimeException("Injector did not init successfully.", cachedThrowable);
        }

        // gets the players current permissible.
        PermissibleBase permissible = (PermissibleBase) humanEntityPermissibleField.get(player);

        // only uninject if the permissible was a luckperms one.
        if (permissible instanceof LPPermissible) {
            LPPermissible lpPermissible = ((LPPermissible) permissible);

            // clear all permissions
            lpPermissible.clearPermissions();

            // try to unsubscribe
            if (unsubscribe) {
                lpPermissible.unsubscribeFromAllAsync();
            }

            // set to inactive
            lpPermissible.getActive().set(false);

            // handle the replacement permissible.
            if (dummy) {
                // just inject a dummy class. this is used when we know the player is about to quit the server.
                humanEntityPermissibleField.set(player, new DummyPermissibleBase());

            } else {
                // otherwise, inject the permissible they had when we first injected.

                List<PermissionAttachment> lpAttachments = lpPermissible.getAttachments();

                PermissibleBase newPb = lpPermissible.getOldPermissible();
                if (newPb == null) {
                    newPb = new PermissibleBase(player);
                }

                //noinspection unchecked
                List<PermissionAttachment> newPbAttachments = (List<PermissionAttachment>) permissibleBaseAttachmentsField.get(newPb);
                newPbAttachments.addAll(lpAttachments);
                lpAttachments.clear();

                humanEntityPermissibleField.set(player, newPb);
            }
        }

        INJECTED_PERMISSIBLES.remove(player.getUniqueId());
    }

    public static LPPermissible getPermissible(UUID uuid) {
        return INJECTED_PERMISSIBLES.get(uuid);
    }

}
