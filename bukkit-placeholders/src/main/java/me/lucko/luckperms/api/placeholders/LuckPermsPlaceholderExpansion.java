/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

package me.lucko.luckperms.api.placeholders;

import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.util.TimeUtil;
import me.lucko.luckperms.api.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;

/*
 * PlaceholderAPI Expansion for LuckPerms, implemented using the LuckPerms API.
 *
 * Placeholders:
 * - group_name
 * - has_permission_<node>
 * - inherits_permission_<node>
 * - in_group_<name>
 * - inherits_group_<name>
 * - on_track_<track>
 * - expiry_time_<node>
 * - group_expiry_time_<group>
 * - prefix
 * - suffix
 * - meta_<node>
 */
public class LuckPermsPlaceholderExpansion extends PlaceholderExpansion {
    private static final String IDENTIFIER = "luckperms";
    private static final String PLUGIN_NAME = "LuckPerms";
    private static final String AUTHOR = "Luck";

    private LuckPermsApi api = null;

    @Override
    public boolean canRegister() {
        return Bukkit.getServicesManager().isProvidedFor(LuckPermsApi.class);
    }

    @Override
    public boolean register() {
        if (!canRegister()) {
            return false;
        }

        api = Bukkit.getServicesManager().getRegistration(LuckPermsApi.class).getProvider();
        return super.register();
    }

    @Override
    public String getVersion() {
        return "2.8";
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null || api == null) {
            return "";
        }

        Optional<User> u = api.getUserSafe(api.getUuidCache().getUUID(player.getUniqueId()));
        if (!u.isPresent()) {
            return "";
        }

        identifier = identifier.toLowerCase();
        final User user = u.get();

        if (identifier.equalsIgnoreCase("group_name")) {
            return user.getPrimaryGroup();
        }

        if (identifier.startsWith("has_permission_") && identifier.length() > "has_permission_".length()) {
            String node = identifier.substring("has_permission_".length());
            return formatBoolean(user.hasPermission(node, true));
        }

        if (identifier.startsWith("inherits_permission_") && identifier.length() > "inherits_permission_".length()) {
            String node = identifier.substring("inherits_permission_".length());
            return formatBoolean(user.inheritsPermission(node, true));
        }

        if (identifier.startsWith("in_group_") && identifier.length() > "in_group_".length()) {
            String groupName = identifier.substring("in_group_".length());
            return formatBoolean(user.getGroupNames().contains(groupName));
        }

        if (identifier.startsWith("inherits_group_") && identifier.length() > "inherits_group_".length()) {
            String groupName = identifier.substring("inherits_group_".length());
            return formatBoolean(user.getLocalGroups("global").contains(groupName));
        }

        if (identifier.startsWith("on_track_") && identifier.length() > "on_track_".length()) {
            String trackName = identifier.substring("on_track_".length());

            Optional<Track> track = api.getTrackSafe(trackName);
            if (!track.isPresent()) return "";

            return formatBoolean(track.get().containsGroup(user.getPrimaryGroup()));
        }

        if (identifier.startsWith("expiry_time_") && identifier.length() > "expiry_time_".length()) {
            String node = identifier.substring("expiry_time_".length());
            long currentTime = System.currentTimeMillis() / 1000L;
            for (Node n : user.getTemporaryPermissionNodes()) {
                if (n.getPermission().equalsIgnoreCase(node)) {
                    return TimeUtil.getTime((int) (n.getExpiryUnixTime() - currentTime));
                }
            }

            return "";
        }

        if (identifier.startsWith("group_expiry_time_") && identifier.length() > "group_expiry_time_".length()) {
            String node = "group." + identifier.substring("group_expiry_time_".length());
            long currentTime = System.currentTimeMillis() / 1000L;
            for (Node n : user.getTemporaryPermissionNodes()) {
                if (n.getPermission().equalsIgnoreCase(node)) {
                    return TimeUtil.getTime((int) (n.getExpiryUnixTime() - currentTime));
                }
            }

            return "";
        }

        if (identifier.equalsIgnoreCase("prefix")) {
            return MetaUtils.getPrefix(user, null, null, true);
        }

        if (identifier.equalsIgnoreCase("suffix")) {
            return MetaUtils.getSuffix(user, null, null, true);
        }

        if (identifier.startsWith("meta_") && identifier.length() > "meta_".length()) {
            String node = identifier.substring("meta_".length());
            return MetaUtils.getMeta(user, null, null, node, "", true);
        }

        return null;
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public String getPlugin() {
        return PLUGIN_NAME;
    }

    @Override
    public String getAuthor() {
        return AUTHOR;
    }

    private static String formatBoolean(boolean b) {
        return b ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
    }
}
