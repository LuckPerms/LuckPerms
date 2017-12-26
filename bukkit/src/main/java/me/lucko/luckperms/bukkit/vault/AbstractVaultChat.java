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
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Function;

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
 * API a total mess. This class reverses this decision, and instead upgrades
 * requests to use UUIDs. This makes implementing Vault significantly easier for modern
 * plugins, and because requests are upgraded instead of being downgraded then upgraded,
 * much faster for plugins querying data.
 */
@SuppressWarnings("deprecation")
public abstract class AbstractVaultChat extends Chat {

    // when upgrading and forwarding requests, all world strings are passed through this function.
    // it lets the overriding class define some custom behaviour for world handling.
    protected Function<String, String> worldMappingFunction = Function.identity();

    // the permission api instance
    private final Permission permissionApi;

    public AbstractVaultChat(Permission permissionApi) {
        super(permissionApi);
        this.permissionApi = permissionApi;
    }

    @Override
    public boolean isEnabled() {
        // always return true
        return true;
    }

    // methods subclasses are expected to implement
    public abstract String getPlayerPrefix(String world, UUID uuid);
    public abstract String getPlayerSuffix(String world, UUID uuid);
    public abstract void setPlayerPrefix(String world, UUID uuid, String prefix);
    public abstract void setPlayerSuffix(String world, UUID uuid, String suffix);
    public abstract String getPlayerInfo(String world, UUID uuid, String key);
    public abstract void setPlayerInfo(String world, UUID uuid, String key, Object value);
    public abstract String getGroupsPrefix(String world, String name); // note "groups" not "group"
    public abstract String getGroupsSuffix(String world, String name); // note "groups" not "group"
    public abstract void setGroupsPrefix(String world, String name, String prefix); // note "groups" not "group"
    public abstract void setGroupsSuffix(String world, String name, String suffix); // note "groups" not "group"
    public abstract String getGroupInfo(String world, String name, String key);
    public abstract void setGroupInfo(String world, String name, String key, Object value);

    // utility methods for parsing metadata values from strings

    private static String strConvert(String s, String def) {
        if (s != null) {
            return s;
        }
        return def;
    }

    private static int intConvert(String s, int def) {
        if (s == null) {
            return def;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static double doubleConvert(String s, double def) {
        if (s == null) {
            return def;
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static boolean booleanConvert(String s, boolean def) {
        if (s == null) {
            return def;
        }
        if (s.equalsIgnoreCase("true")) {
            return true;
        } else if (s.equalsIgnoreCase("false")) {
            return false;
        }
        return def;
    }

    // utility methods for upgrading legacy requests

    private static UUID player(String player) {
        if (player == null) {
            return null;
        }
        return player(Bukkit.getOfflinePlayer(player));
    }

    private static UUID player(OfflinePlayer player) {
        if (player == null) {
            return null;
        }
        return player.getUniqueId();
    }

    private String world(String world) {
        return worldMappingFunction.apply(world);
    }

    private String world(Player player) {
        if (player == null) {
            return null;
        }
        return world(player.getWorld());
    }

    private String world(World world) {
        if (world == null) {
            return null;
        }
        return world(world.getName());
    }

    @Override
    public String getPlayerPrefix(String world, String player) {
        return getPlayerPrefix(world(world), player(player));
    }

    @Override
    public String getPlayerPrefix(String world, OfflinePlayer player) {
        return getPlayerPrefix(world(world), player(player));
    }

    @Override
    public String getPlayerPrefix(World world, String player) {
        return getPlayerPrefix(world(world), player(player));
    }

    @Override
    public String getPlayerPrefix(Player player) {
        return getPlayerPrefix(world(player), player(player));
    }

    @Override
    public void setPlayerPrefix(String world, String player, String prefix) {
        setPlayerPrefix(world(world), player(player), prefix);
    }

    @Override
    public void setPlayerPrefix(String world, OfflinePlayer player, String prefix) {
        setPlayerPrefix(world(world), player(player), prefix);
    }

    @Override
    public void setPlayerPrefix(World world, String player, String prefix) {
        setPlayerPrefix(world(world), player(player), prefix);
    }

    @Override
    public void setPlayerPrefix(Player player, String prefix) {
        setPlayerPrefix(world(player), player(player), prefix);
    }

    @Override
    public String getPlayerSuffix(String world, String player) {
        return getPlayerSuffix(world(world), player(player));
    }

    @Override
    public String getPlayerSuffix(String world, OfflinePlayer player) {
        return getPlayerSuffix(world(world), player(player));
    }

    @Override
    public String getPlayerSuffix(World world, String player) {
        return getPlayerSuffix(world(world), player(player));
    }

    @Override
    public String getPlayerSuffix(Player player) {
        return getPlayerSuffix(world(player), player(player));
    }

    @Override
    public void setPlayerSuffix(String world, String player, String suffix) {
        setPlayerSuffix(world(world), player(player), suffix);
    }

    @Override
    public void setPlayerSuffix(String world, OfflinePlayer player, String suffix) {
        setPlayerSuffix(world(world), player(player), suffix);
    }

    @Override
    public void setPlayerSuffix(World world, String player, String suffix) {
        setPlayerSuffix(world(world), player(player), suffix);
    }

    @Override
    public void setPlayerSuffix(Player player, String suffix) {
        setPlayerSuffix(world(player), player(player), suffix);
    }

    @Override
    public String getGroupPrefix(String world, String group) {
        return getGroupsPrefix(world(world), group);
    }

    @Override
    public String getGroupPrefix(World world, String group) {
        return getGroupsPrefix(world(world), group);
    }

    @Override
    public void setGroupPrefix(String world, String group, String prefix) {
        setGroupsPrefix(world(world), group, prefix);
    }

    @Override
    public void setGroupPrefix(World world, String group, String prefix) {
        setGroupsPrefix(world(world), group, prefix);
    }

    @Override
    public String getGroupSuffix(String world, String group) {
        return getGroupsSuffix(world(world), group);
    }

    @Override
    public String getGroupSuffix(World world, String group) {
        return getGroupsSuffix(world(world), group);
    }

    @Override
    public void setGroupSuffix(String world, String group, String suffix) {
        setGroupsSuffix(world(world), group, suffix);
    }

    @Override
    public void setGroupSuffix(World world, String group, String suffix) {
        setGroupsSuffix(world(world), group, suffix);
    }

    @Override
    public int getPlayerInfoInteger(String world, OfflinePlayer player, String node, int defaultValue) {
        return intConvert(getPlayerInfo(world(world), player(player), node), defaultValue);
    }

    @Override
    public int getPlayerInfoInteger(String world, String player, String node, int defaultValue) {
        return intConvert(getPlayerInfo(world(world), player(player), node), defaultValue);
    }

    @Override
    public int getPlayerInfoInteger(World world, String player, String node, int defaultValue) {
        return intConvert(getPlayerInfo(world(world), player(player), node), defaultValue);
    }

    @Override
    public int getPlayerInfoInteger(Player player, String node, int defaultValue) {
        return intConvert(getPlayerInfo(world(player), player(player), node), defaultValue);
    }

    @Override
    public void setPlayerInfoInteger(String world, OfflinePlayer player, String node, int value) {
        setPlayerInfo(world(world), player(player), node, value);
    }

    @Override
    public void setPlayerInfoInteger(String world, String player, String node, int value) {
        setPlayerInfo(world(world), player(player), node, value);
    }

    @Override
    public void setPlayerInfoInteger(World world, String player, String node, int value) {
        setPlayerInfo(world(world), player(player), node, value);
    }

    @Override
    public void setPlayerInfoInteger(Player player, String node, int value) {
        setPlayerInfo(world(player), player(player), node, value);
    }

    @Override
    public int getGroupInfoInteger(String world, String group, String node, int defaultValue) {
        return intConvert(getGroupInfo(world(world), group, node), defaultValue);
    }

    @Override
    public int getGroupInfoInteger(World world, String group, String node, int defaultValue) {
        return intConvert(getGroupInfo(world(world), group, node), defaultValue);
    }

    @Override
    public void setGroupInfoInteger(String world, String group, String node, int value) {
        setGroupInfo(world(world), group, node, value);
    }

    @Override
    public void setGroupInfoInteger(World world, String group, String node, int value) {
        setGroupInfo(world(world), group, node, value);
    }

    @Override
    public double getPlayerInfoDouble(String world, OfflinePlayer player, String node, double defaultValue) {
        return doubleConvert(getPlayerInfo(world(world), player(player), node), defaultValue);
    }

    @Override
    public double getPlayerInfoDouble(String world, String player, String node, double defaultValue) {
        return doubleConvert(getPlayerInfo(world(world), player(player), node), defaultValue);
    }

    @Override
    public double getPlayerInfoDouble(World world, String player, String node, double defaultValue) {
        return doubleConvert(getPlayerInfo(world(world), player(player), node), defaultValue);
    }

    @Override
    public double getPlayerInfoDouble(Player player, String node, double defaultValue) {
        return doubleConvert(getPlayerInfo(world(player), player(player), node), defaultValue);
    }

    @Override
    public void setPlayerInfoDouble(String world, OfflinePlayer player, String node, double value) {
        setPlayerInfo(world(world), player(player), node, value);
    }

    @Override
    public void setPlayerInfoDouble(String world, String player, String node, double value) {
        setPlayerInfo(world(world), player(player), node, value);
    }

    @Override
    public void setPlayerInfoDouble(World world, String player, String node, double value) {
        setPlayerInfo(world(world), player(player), node, value);
    }

    @Override
    public void setPlayerInfoDouble(Player player, String node, double value) {
        setPlayerInfo(world(player), player(player), node, value);
    }

    @Override
    public double getGroupInfoDouble(String world, String group, String node, double defaultValue) {
        return doubleConvert(getGroupInfo(world(world), group, node), defaultValue);
    }

    @Override
    public double getGroupInfoDouble(World world, String group, String node, double defaultValue) {
        return doubleConvert(getGroupInfo(world(world), group, node), defaultValue);
    }

    @Override
    public void setGroupInfoDouble(String world, String group, String node, double value) {
        setGroupInfo(world(world), group, node, value);
    }

    @Override
    public void setGroupInfoDouble(World world, String group, String node, double value) {
        setGroupInfo(world(world), group, node, value);
    }

    @Override
    public boolean getPlayerInfoBoolean(String world, OfflinePlayer player, String node, boolean defaultValue) {
        return booleanConvert(getPlayerInfo(world(world), player(player), node), defaultValue);
    }

    @Override
    public boolean getPlayerInfoBoolean(String world, String player, String node, boolean defaultValue) {
        return booleanConvert(getPlayerInfo(world(world), player(player), node), defaultValue);
    }

    @Override
    public boolean getPlayerInfoBoolean(World world, String player, String node, boolean defaultValue) {
        return booleanConvert(getPlayerInfo(world(world), player(player), node), defaultValue);
    }

    @Override
    public boolean getPlayerInfoBoolean(Player player, String node, boolean defaultValue) {
        return booleanConvert(getPlayerInfo(world(player), player(player), node), defaultValue);
    }

    @Override
    public void setPlayerInfoBoolean(String world, OfflinePlayer player, String node, boolean value) {
        setPlayerInfo(world(world), player(player), node, value);
    }

    @Override
    public void setPlayerInfoBoolean(String world, String player, String node, boolean value) {
        setPlayerInfo(world(world), player(player), node, value);
    }

    @Override
    public void setPlayerInfoBoolean(World world, String player, String node, boolean value) {
        setPlayerInfo(world(world), player(player), node, value);
    }

    @Override
    public void setPlayerInfoBoolean(Player player, String node, boolean value) {
        setPlayerInfo(world(player), player(player), node, value);
    }

    @Override
    public boolean getGroupInfoBoolean(String world, String group, String node, boolean defaultValue) {
        return booleanConvert(getGroupInfo(world(world), group, node), defaultValue);
    }

    @Override
    public boolean getGroupInfoBoolean(World world, String group, String node, boolean defaultValue) {
        return booleanConvert(getGroupInfo(world(world), group, node), defaultValue);
    }

    @Override
    public void setGroupInfoBoolean(String world, String group, String node, boolean value) {
        setGroupInfo(world(world), group, node, value);
    }

    @Override
    public void setGroupInfoBoolean(World world, String group, String node, boolean value) {
        setGroupInfo(world(world), group, node, value);
    }

    @Override
    public String getPlayerInfoString(String world, OfflinePlayer player, String node, String defaultValue) {
        return strConvert(getPlayerInfo(world(world), player(player), node), defaultValue);
    }

    @Override
    public String getPlayerInfoString(String world, String player, String node, String defaultValue) {
        return strConvert(getPlayerInfo(world(world), player(player), node), defaultValue);
    }

    @Override
    public String getPlayerInfoString(World world, String player, String node, String defaultValue) {
        return strConvert(getPlayerInfo(world(world), player(player), node), defaultValue);
    }

    @Override
    public String getPlayerInfoString(Player player, String node, String defaultValue) {
        return strConvert(getPlayerInfo(world(player), player(player), node), defaultValue);
    }

    @Override
    public void setPlayerInfoString(String world, OfflinePlayer player, String node, String value) {
        setPlayerInfo(world(world), player(player), node, value);
    }

    @Override
    public void setPlayerInfoString(String world, String player, String node, String value) {
        setPlayerInfo(world(world), player(player), node, value);
    }

    @Override
    public void setPlayerInfoString(World world, String player, String node, String value) {
        setPlayerInfo(world(world), player(player), node, value);
    }

    @Override
    public void setPlayerInfoString(Player player, String node, String value) {
        setPlayerInfo(world(player), player(player), node, value);
    }

    @Override
    public String getGroupInfoString(String world, String group, String node, String defaultValue) {
        return strConvert(getGroupInfo(world(world), group, node), defaultValue);
    }

    @Override
    public String getGroupInfoString(World world, String group, String node, String defaultValue) {
        return strConvert(getGroupInfo(world(world), group, node), defaultValue);
    }

    @Override
    public void setGroupInfoString(String world, String group, String node, String value) {
        setGroupInfo(world(world), group, node, value);
    }

    @Override
    public void setGroupInfoString(World world, String group, String node, String value) {
        setGroupInfo(world(world), group, node, value);
    }

    @Override
    public boolean playerInGroup(String world, OfflinePlayer player, String group) {
        return permissionApi.playerInGroup(world, player, group);
    }

    @Override
    public boolean playerInGroup(String world, String player, String group) {
        return permissionApi.playerInGroup(world, player, group);
    }

    @Override
    public boolean playerInGroup(World world, String player, String group) {
        return permissionApi.playerInGroup(world, player, group);
    }

    @Override
    public boolean playerInGroup(Player player, String group) {
        return permissionApi.playerInGroup(player, group);
    }

    @Override
    public String[] getPlayerGroups(String world, OfflinePlayer player) {
        return permissionApi.getPlayerGroups(world, player);
    }

    @Override
    public String[] getPlayerGroups(String world, String player) {
        return permissionApi.getPlayerGroups(world, player);
    }

    @Override
    public String[] getPlayerGroups(World world, String player) {
        return permissionApi.getPlayerGroups(world, player);
    }

    @Override
    public String[] getPlayerGroups(Player player) {
        return permissionApi.getPlayerGroups(player);
    }

    @Override
    public String getPrimaryGroup(String world, OfflinePlayer player) {
        return permissionApi.getPrimaryGroup(world, player);
    }

    @Override
    public String getPrimaryGroup(String world, String player) {
        return permissionApi.getPrimaryGroup(world, player);
    }

    @Override
    public String getPrimaryGroup(World world, String player) {
        return permissionApi.getPrimaryGroup(world, player);
    }

    @Override
    public String getPrimaryGroup(Player player) {
        return permissionApi.getPrimaryGroup(player);
    }

    @Override
    public String[] getGroups() {
        return permissionApi.getGroups();
    }

}
