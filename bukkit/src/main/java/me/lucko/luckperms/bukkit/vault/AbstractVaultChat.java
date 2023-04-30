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

package me.lucko.luckperms.bukkit.vault;

import net.milkbowl.vault.chat.Chat;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;

/**
 * An extended abstraction of the Vault {@link Chat} API.
 *
 * The original Vault API only contained methods to query data by username. Over
 * time, the maintainers added additional methods to query by (Offline)Player, but
 * in order to keep backwards compatibility with implementations which only supported
 * usernames, they implemented the Player query methods and downgraded the requests
 * to get a result using the players username.
 *
 * Whilst this meant the old plugins would still be supported, it made the whole
 * API a total mess. This class reverses this action, and instead upgrades
 * requests to use UUIDs. This makes implementing Vault significantly easier for modern
 * plugins, and because requests are upgraded instead of being downgraded then upgraded,
 * much faster for plugins querying data.
 */
@SuppressWarnings("deprecation")
public abstract class AbstractVaultChat extends Chat {

    // the permission api instance
    private final AbstractVaultPermission permissionApi;

    public AbstractVaultChat(AbstractVaultPermission permissionApi) {
        super(permissionApi);
        this.permissionApi = permissionApi;
    }

    @Override
    public final boolean isEnabled() {
        // always return true
        return true;
    }

    // methods subclasses are expected to implement
    public abstract String getUserChatPrefix(String world, UUID uuid);
    public abstract String getUserChatSuffix(String world, UUID uuid);
    public abstract void setUserChatPrefix(String world, UUID uuid, String prefix);
    public abstract void setUserChatSuffix(String world, UUID uuid, String suffix);
    public abstract String getUserMeta(String world, UUID uuid, String key);
    public abstract void setUserMeta(String world, UUID uuid, String key, Object value);
    public abstract String getGroupChatPrefix(String world, String name);
    public abstract String getGroupChatSuffix(String world, String name);
    public abstract void setGroupChatPrefix(String world, String name, String prefix);
    public abstract void setGroupChatSuffix(String world, String name, String suffix);
    public abstract String getGroupMeta(String world, String name, String key);
    public abstract void setGroupMeta(String world, String name, String key, Object value);

    // utility methods for parsing metadata values from strings

    private static String parseString(String s, String def) {
        if (s == null) {
            return def;
        }
        return s;
    }

    private static int parseInt(String s, int def) {
        if (s == null) {
            return def;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static double parseDouble(String s, double def) {
        if (s == null) {
            return def;
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static boolean parseBoolean(String s, boolean def) {
        if (s == null) {
            return def;
        }
        if (s.equalsIgnoreCase("true")) {
            return true;
        } else if (s.equalsIgnoreCase("false")) {
            return false;
        } else {
            return def;
        }
    }

    /* allow implementations to override and implement custom world remapping / handling rules */
    protected String convertWorld(String world) {
        return world;
    }

    private String convertWorld(World world) {
        if (world == null) {
            return null;
        }
        return convertWorld(world.getName());
    }

    private String convertWorld(Player player) {
        if (player == null) {
            return null;
        }
        return convertWorld(player.getWorld());
    }

    @Override
    public final String getPlayerPrefix(String world, String player) {
        Objects.requireNonNull(player, "player");
        return getUserChatPrefix(convertWorld(world), this.permissionApi.lookupUuid(player));
    }

    @Override
    public final String getPlayerPrefix(String world, OfflinePlayer player) {
        Objects.requireNonNull(player, "player");
        return getUserChatPrefix(convertWorld(world), player.getUniqueId());
    }

    @Override
    public final String getPlayerPrefix(World world, String player) {
        Objects.requireNonNull(player, "player");
        return getUserChatPrefix(convertWorld(world), this.permissionApi.lookupUuid(player));
    }

    @Override
    public final String getPlayerPrefix(Player player) {
        Objects.requireNonNull(player, "player");
        return getUserChatPrefix(convertWorld(player), ((OfflinePlayer) player).getUniqueId());
    }

    @Override
    public final void setPlayerPrefix(String world, String player, String prefix) {
        Objects.requireNonNull(player, "player");
        setUserChatPrefix(convertWorld(world), this.permissionApi.lookupUuid(player), prefix);
    }

    @Override
    public final void setPlayerPrefix(String world, OfflinePlayer player, String prefix) {
        Objects.requireNonNull(player, "player");
        setUserChatPrefix(convertWorld(world), player.getUniqueId(), prefix);
    }

    @Override
    public final void setPlayerPrefix(World world, String player, String prefix) {
        Objects.requireNonNull(player, "player");
        setUserChatPrefix(convertWorld(world), this.permissionApi.lookupUuid(player), prefix);
    }

    @Override
    public final void setPlayerPrefix(Player player, String prefix) {
        Objects.requireNonNull(player, "player");
        setUserChatPrefix(convertWorld(player), ((OfflinePlayer) player).getUniqueId(), prefix);
    }

    @Override
    public final String getPlayerSuffix(String world, String player) {
        Objects.requireNonNull(player, "player");
        return getUserChatSuffix(convertWorld(world), this.permissionApi.lookupUuid(player));
    }

    @Override
    public final String getPlayerSuffix(String world, OfflinePlayer player) {
        Objects.requireNonNull(player, "player");
        return getUserChatSuffix(convertWorld(world), player.getUniqueId());
    }

    @Override
    public final String getPlayerSuffix(World world, String player) {
        Objects.requireNonNull(player, "player");
        return getUserChatSuffix(convertWorld(world), this.permissionApi.lookupUuid(player));
    }

    @Override
    public final String getPlayerSuffix(Player player) {
        Objects.requireNonNull(player, "player");
        return getUserChatSuffix(convertWorld(player), ((OfflinePlayer) player).getUniqueId());
    }

    @Override
    public final void setPlayerSuffix(String world, String player, String suffix) {
        Objects.requireNonNull(player, "player");
        setUserChatSuffix(convertWorld(world), this.permissionApi.lookupUuid(player), suffix);
    }

    @Override
    public final void setPlayerSuffix(String world, OfflinePlayer player, String suffix) {
        Objects.requireNonNull(player, "player");
        setUserChatSuffix(convertWorld(world), player.getUniqueId(), suffix);
    }

    @Override
    public final void setPlayerSuffix(World world, String player, String suffix) {
        Objects.requireNonNull(player, "player");
        setUserChatSuffix(convertWorld(world), this.permissionApi.lookupUuid(player), suffix);
    }

    @Override
    public final void setPlayerSuffix(Player player, String suffix) {
        Objects.requireNonNull(player, "player");
        setUserChatSuffix(convertWorld(player), ((OfflinePlayer) player).getUniqueId(), suffix);
    }

    @Override
    public final String getGroupPrefix(String world, String group) {
        Objects.requireNonNull(group, "group");
        return getGroupChatPrefix(convertWorld(world), group);
    }

    @Override
    public final String getGroupPrefix(World world, String group) {
        Objects.requireNonNull(group, "group");
        return getGroupChatPrefix(convertWorld(world), group);
    }

    @Override
    public final void setGroupPrefix(String world, String group, String prefix) {
        Objects.requireNonNull(group, "group");
        setGroupChatPrefix(convertWorld(world), group, prefix);
    }

    @Override
    public final void setGroupPrefix(World world, String group, String prefix) {
        Objects.requireNonNull(group, "group");
        setGroupChatPrefix(convertWorld(world), group, prefix);
    }

    @Override
    public final String getGroupSuffix(String world, String group) {
        Objects.requireNonNull(group, "group");
        return getGroupChatSuffix(convertWorld(world), group);
    }

    @Override
    public final String getGroupSuffix(World world, String group) {
        Objects.requireNonNull(group, "group");
        return getGroupChatSuffix(convertWorld(world), group);
    }

    @Override
    public final void setGroupSuffix(String world, String group, String suffix) {
        Objects.requireNonNull(group, "group");
        setGroupChatSuffix(convertWorld(world), group, suffix);
    }

    @Override
    public final void setGroupSuffix(World world, String group, String suffix) {
        Objects.requireNonNull(group, "group");
        setGroupChatSuffix(convertWorld(world), group, suffix);
    }

    @Override
    public final int getPlayerInfoInteger(String world, OfflinePlayer player, String node, int defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return parseInt(getUserMeta(convertWorld(world), player.getUniqueId(), node), defaultValue);
    }

    @Override
    public final int getPlayerInfoInteger(String world, String player, String node, int defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return parseInt(getUserMeta(convertWorld(world), this.permissionApi.lookupUuid(player), node), defaultValue);
    }

    @Override
    public final int getPlayerInfoInteger(World world, String player, String node, int defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return parseInt(getUserMeta(convertWorld(world), this.permissionApi.lookupUuid(player), node), defaultValue);
    }

    @Override
    public final int getPlayerInfoInteger(Player player, String node, int defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return parseInt(getUserMeta(convertWorld(player), ((OfflinePlayer) player).getUniqueId(), node), defaultValue);
    }

    @Override
    public final void setPlayerInfoInteger(String world, OfflinePlayer player, String node, int value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(convertWorld(world), player.getUniqueId(), node, value);
    }

    @Override
    public final void setPlayerInfoInteger(String world, String player, String node, int value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(convertWorld(world), this.permissionApi.lookupUuid(player), node, value);
    }

    @Override
    public final void setPlayerInfoInteger(World world, String player, String node, int value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(convertWorld(world), this.permissionApi.lookupUuid(player), node, value);
    }

    @Override
    public final void setPlayerInfoInteger(Player player, String node, int value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(convertWorld(player), ((OfflinePlayer) player).getUniqueId(), node, value);
    }

    @Override
    public final int getGroupInfoInteger(String world, String group, String node, int defaultValue) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        return parseInt(getGroupMeta(convertWorld(world), group, node), defaultValue);
    }

    @Override
    public final int getGroupInfoInteger(World world, String group, String node, int defaultValue) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        return parseInt(getGroupMeta(convertWorld(world), group, node), defaultValue);
    }

    @Override
    public final void setGroupInfoInteger(String world, String group, String node, int value) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        setGroupMeta(convertWorld(world), group, node, value);
    }

    @Override
    public final void setGroupInfoInteger(World world, String group, String node, int value) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        setGroupMeta(convertWorld(world), group, node, value);
    }

    @Override
    public final double getPlayerInfoDouble(String world, OfflinePlayer player, String node, double defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return parseDouble(getUserMeta(convertWorld(world), player.getUniqueId(), node), defaultValue);
    }

    @Override
    public final double getPlayerInfoDouble(String world, String player, String node, double defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return parseDouble(getUserMeta(convertWorld(world), this.permissionApi.lookupUuid(player), node), defaultValue);
    }

    @Override
    public final double getPlayerInfoDouble(World world, String player, String node, double defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return parseDouble(getUserMeta(convertWorld(world), this.permissionApi.lookupUuid(player), node), defaultValue);
    }

    @Override
    public final double getPlayerInfoDouble(Player player, String node, double defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return parseDouble(getUserMeta(convertWorld(player), ((OfflinePlayer) player).getUniqueId(), node), defaultValue);
    }

    @Override
    public final void setPlayerInfoDouble(String world, OfflinePlayer player, String node, double value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(convertWorld(world), player.getUniqueId(), node, value);
    }

    @Override
    public final void setPlayerInfoDouble(String world, String player, String node, double value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(convertWorld(world), this.permissionApi.lookupUuid(player), node, value);
    }

    @Override
    public final void setPlayerInfoDouble(World world, String player, String node, double value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(convertWorld(world), this.permissionApi.lookupUuid(player), node, value);
    }

    @Override
    public final void setPlayerInfoDouble(Player player, String node, double value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(convertWorld(player), ((OfflinePlayer) player).getUniqueId(), node, value);
    }

    @Override
    public final double getGroupInfoDouble(String world, String group, String node, double defaultValue) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        return parseDouble(getGroupMeta(convertWorld(world), group, node), defaultValue);
    }

    @Override
    public final double getGroupInfoDouble(World world, String group, String node, double defaultValue) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        return parseDouble(getGroupMeta(convertWorld(world), group, node), defaultValue);
    }

    @Override
    public final void setGroupInfoDouble(String world, String group, String node, double value) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        setGroupMeta(convertWorld(world), group, node, value);
    }

    @Override
    public final void setGroupInfoDouble(World world, String group, String node, double value) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        setGroupMeta(convertWorld(world), group, node, value);
    }

    @Override
    public final boolean getPlayerInfoBoolean(String world, OfflinePlayer player, String node, boolean defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return parseBoolean(getUserMeta(convertWorld(world), player.getUniqueId(), node), defaultValue);
    }

    @Override
    public final boolean getPlayerInfoBoolean(String world, String player, String node, boolean defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return parseBoolean(getUserMeta(convertWorld(world), this.permissionApi.lookupUuid(player), node), defaultValue);
    }

    @Override
    public final boolean getPlayerInfoBoolean(World world, String player, String node, boolean defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return parseBoolean(getUserMeta(convertWorld(world), this.permissionApi.lookupUuid(player), node), defaultValue);
    }

    @Override
    public final boolean getPlayerInfoBoolean(Player player, String node, boolean defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return parseBoolean(getUserMeta(convertWorld(player), ((OfflinePlayer) player).getUniqueId(), node), defaultValue);
    }

    @Override
    public final void setPlayerInfoBoolean(String world, OfflinePlayer player, String node, boolean value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(convertWorld(world), player.getUniqueId(), node, value);
    }

    @Override
    public final void setPlayerInfoBoolean(String world, String player, String node, boolean value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(convertWorld(world), this.permissionApi.lookupUuid(player), node, value);
    }

    @Override
    public final void setPlayerInfoBoolean(World world, String player, String node, boolean value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(convertWorld(world), this.permissionApi.lookupUuid(player), node, value);
    }

    @Override
    public final void setPlayerInfoBoolean(Player player, String node, boolean value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(convertWorld(player), ((OfflinePlayer) player).getUniqueId(), node, value);
    }

    @Override
    public final boolean getGroupInfoBoolean(String world, String group, String node, boolean defaultValue) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        return parseBoolean(getGroupMeta(convertWorld(world), group, node), defaultValue);
    }

    @Override
    public final boolean getGroupInfoBoolean(World world, String group, String node, boolean defaultValue) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        return parseBoolean(getGroupMeta(convertWorld(world), group, node), defaultValue);
    }

    @Override
    public final void setGroupInfoBoolean(String world, String group, String node, boolean value) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        setGroupMeta(convertWorld(world), group, node, value);
    }

    @Override
    public final void setGroupInfoBoolean(World world, String group, String node, boolean value) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        setGroupMeta(convertWorld(world), group, node, value);
    }

    @Override
    public final String getPlayerInfoString(String world, OfflinePlayer player, String node, String defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return parseString(getUserMeta(convertWorld(world), player.getUniqueId(), node), defaultValue);
    }

    @Override
    public final String getPlayerInfoString(String world, String player, String node, String defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return parseString(getUserMeta(convertWorld(world), this.permissionApi.lookupUuid(player), node), defaultValue);
    }

    @Override
    public final String getPlayerInfoString(World world, String player, String node, String defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return parseString(getUserMeta(convertWorld(world), this.permissionApi.lookupUuid(player), node), defaultValue);
    }

    @Override
    public final String getPlayerInfoString(Player player, String node, String defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return parseString(getUserMeta(convertWorld(player), ((OfflinePlayer) player).getUniqueId(), node), defaultValue);
    }

    @Override
    public final void setPlayerInfoString(String world, OfflinePlayer player, String node, String value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(convertWorld(world), player.getUniqueId(), node, value);
    }

    @Override
    public final void setPlayerInfoString(String world, String player, String node, String value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(convertWorld(world), this.permissionApi.lookupUuid(player), node, value);
    }

    @Override
    public final void setPlayerInfoString(World world, String player, String node, String value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(convertWorld(world), this.permissionApi.lookupUuid(player), node, value);
    }

    @Override
    public final void setPlayerInfoString(Player player, String node, String value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(convertWorld(player), ((OfflinePlayer) player).getUniqueId(), node, value);
    }

    @Override
    public final String getGroupInfoString(String world, String group, String node, String defaultValue) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        return parseString(getGroupMeta(convertWorld(world), group, node), defaultValue);
    }

    @Override
    public final String getGroupInfoString(World world, String group, String node, String defaultValue) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        return parseString(getGroupMeta(convertWorld(world), group, node), defaultValue);
    }

    @Override
    public final void setGroupInfoString(String world, String group, String node, String value) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        setGroupMeta(convertWorld(world), group, node, value);
    }

    @Override
    public final void setGroupInfoString(World world, String group, String node, String value) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        setGroupMeta(convertWorld(world), group, node, value);
    }

    @Override
    public final boolean playerInGroup(String world, OfflinePlayer player, String group) {
        return this.permissionApi.playerInGroup(world, player, group);
    }

    @Override
    public final boolean playerInGroup(String world, String player, String group) {
        return this.permissionApi.playerInGroup(world, player, group);
    }

    @Override
    public final boolean playerInGroup(World world, String player, String group) {
        return this.permissionApi.playerInGroup(world, player, group);
    }

    @Override
    public final boolean playerInGroup(Player player, String group) {
        return this.permissionApi.playerInGroup(player, group);
    }

    @Override
    public final String[] getPlayerGroups(String world, OfflinePlayer player) {
        return this.permissionApi.getPlayerGroups(world, player);
    }

    @Override
    public final String[] getPlayerGroups(String world, String player) {
        return this.permissionApi.getPlayerGroups(world, player);
    }

    @Override
    public final String[] getPlayerGroups(World world, String player) {
        return this.permissionApi.getPlayerGroups(world, player);
    }

    @Override
    public final String[] getPlayerGroups(Player player) {
        return this.permissionApi.getPlayerGroups(player);
    }

    @Override
    public final String getPrimaryGroup(String world, OfflinePlayer player) {
        return this.permissionApi.getPrimaryGroup(world, player);
    }

    @Override
    public final String getPrimaryGroup(String world, String player) {
        return this.permissionApi.getPrimaryGroup(world, player);
    }

    @Override
    public final String getPrimaryGroup(World world, String player) {
        return this.permissionApi.getPrimaryGroup(world, player);
    }

    @Override
    public final String getPrimaryGroup(Player player) {
        return this.permissionApi.getPrimaryGroup(player);
    }

    @Override
    public final String[] getGroups() {
        return this.permissionApi.getGroups();
    }

}
