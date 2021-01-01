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

import cn.nukkit.Player;
import cn.nukkit.permission.PermissibleBase;
import cn.nukkit.permission.PermissionAttachment;

import java.lang.reflect.Field;
import java.util.Set;

/**
 * Injects a {@link LuckPermsPermissible} into a {@link Player}.
 *
 * This allows LuckPerms to directly intercept permission checks and take over all handling of
 * checks made by plugins.
 */
public final class PermissibleInjector {
    private PermissibleInjector() {}

    /**
     * All permission checks made on standard Nukkit objects are effectively proxied to a
     * {@link PermissibleBase} object, held as a variable on the object.
     *
     * This field is where the permissible is stored on a Player.
     */
    private static final Field PLAYER_PERMISSIBLE_FIELD;

    /**
     * The field where attachments are stored on a permissible base.
     */
    private static final Field PERMISSIBLE_BASE_ATTACHMENTS_FIELD;

    static {
        try {
            // Try to load the permissible field.
            PLAYER_PERMISSIBLE_FIELD = Player.class.getDeclaredField("perm");
            PLAYER_PERMISSIBLE_FIELD.setAccessible(true);

            // Try to load the attachments field.
            PERMISSIBLE_BASE_ATTACHMENTS_FIELD = PermissibleBase.class.getDeclaredField("attachments");
            PERMISSIBLE_BASE_ATTACHMENTS_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Injects a {@link LuckPermsPermissible} into a {@link Player}.
     *
     * @param player the player to inject into
     * @param newPermissible the permissible to inject
     * @throws Exception propagates any exceptions which were thrown during injection
     */
    public static void inject(Player player, LuckPermsPermissible newPermissible) throws Exception {
        // get the existing PermissibleBase held by the player
        PermissibleBase oldPermissible = (PermissibleBase) PLAYER_PERMISSIBLE_FIELD.get(player);

        if (oldPermissible instanceof LuckPermsPermissible) {
            // Nukkit seems to re-use player instances (or perhaps calls the login event twice?)
            // so, just uninject here instead of throwing an exception like we do on Bukkit
            // See: https://github.com/lucko/LuckPerms/issues/2791
            uninject(player, false);
        }

        // Move attachments over from the old permissible

        //noinspection unchecked
        Set<PermissionAttachment> attachments = (Set<PermissionAttachment>) PERMISSIBLE_BASE_ATTACHMENTS_FIELD.get(oldPermissible);

        newPermissible.convertAndAddAttachments(attachments);
        attachments.clear();
        oldPermissible.clearPermissions();

        // Setup the new permissible
        newPermissible.getActive().set(true);
        newPermissible.setOldPermissible(oldPermissible);

        // inject the new instance
        PLAYER_PERMISSIBLE_FIELD.set(player, newPermissible);
    }

    /**
     * Uninjects a {@link LuckPermsPermissible} from a {@link Player}.
     *
     * @param player the player to uninject from
     * @param dummy if the replacement permissible should be a dummy.
     * @throws Exception propagates any exceptions which were thrown during uninjection
     */
    public static void uninject(Player player, boolean dummy) throws Exception {

        // gets the players current permissible.
        PermissibleBase permissible = (PermissibleBase) PLAYER_PERMISSIBLE_FIELD.get(player);

        // only uninject if the permissible was a luckperms one.
        if (permissible instanceof LuckPermsPermissible) {
            LuckPermsPermissible lpPermissible = ((LuckPermsPermissible) permissible);

            // clear all permissions
            lpPermissible.clearPermissions();

            // set to inactive
            lpPermissible.getActive().set(false);

            // handle the replacement permissible.
            if (dummy) {
                // just inject a dummy class. this is used when we know the player is about to quit the server.
                PLAYER_PERMISSIBLE_FIELD.set(player, DummyPermissibleBase.INSTANCE);

            } else {
                PermissibleBase newPb = lpPermissible.getOldPermissible();
                if (newPb == null) {
                    newPb = new PermissibleBase(player);
                }

                PLAYER_PERMISSIBLE_FIELD.set(player, newPb);
            }
        }
    }

}
