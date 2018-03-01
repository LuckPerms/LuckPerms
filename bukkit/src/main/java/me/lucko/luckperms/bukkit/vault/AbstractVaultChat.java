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

import java.util.Objects;
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
        return player(Bukkit.getPlayerExact(player));
    }

    private static UUID player(OfflinePlayer player) {
        if (player == null) {
            return null;
        }
        return player.getUniqueId();
    }

    private String world(String world) {
        return this.worldMappingFunction.apply(world);
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
        Objects.requireNonNull(player, "player");
        return getUserChatPrefix(world(world), player(player));
    }

    @Override
    public String getPlayerPrefix(String world, OfflinePlayer player) {
        Objects.requireNonNull(player, "player");
        return getUserChatPrefix(world(world), player(player));
    }

    @Override
    public String getPlayerPrefix(World world, String player) {
        Objects.requireNonNull(player, "player");
        return getUserChatPrefix(world(world), player(player));
    }

    @Override
    public String getPlayerPrefix(Player player) {
        Objects.requireNonNull(player, "player");
        return getUserChatPrefix(world(player), player(player));
    }

    @Override
    public void setPlayerPrefix(String world, String player, String prefix) {
        Objects.requireNonNull(player, "player");
        setUserChatPrefix(world(world), player(player), prefix);
    }

    @Override
    public void setPlayerPrefix(String world, OfflinePlayer player, String prefix) {
        Objects.requireNonNull(player, "player");
        setUserChatPrefix(world(world), player(player), prefix);
    }

    @Override
    public void setPlayerPrefix(World world, String player, String prefix) {
        Objects.requireNonNull(player, "player");
        setUserChatPrefix(world(world), player(player), prefix);
    }

    @Override
    public void setPlayerPrefix(Player player, String prefix) {
        Objects.requireNonNull(player, "player");
        setUserChatPrefix(world(player), player(player), prefix);
    }

    @Override
    public String getPlayerSuffix(String world, String player) {
        Objects.requireNonNull(player, "player");
        return getUserChatSuffix(world(world), player(player));
    }

    @Override
    public String getPlayerSuffix(String world, OfflinePlayer player) {
        Objects.requireNonNull(player, "player");
        return getUserChatSuffix(world(world), player(player));
    }

    @Override
    public String getPlayerSuffix(World world, String player) {
        Objects.requireNonNull(player, "player");
        return getUserChatSuffix(world(world), player(player));
    }

    @Override
    public String getPlayerSuffix(Player player) {
        Objects.requireNonNull(player, "player");
        return getUserChatSuffix(world(player), player(player));
    }

    @Override
    public void setPlayerSuffix(String world, String player, String suffix) {
        Objects.requireNonNull(player, "player");
        setUserChatSuffix(world(world), player(player), suffix);
    }

    @Override
    public void setPlayerSuffix(String world, OfflinePlayer player, String suffix) {
        Objects.requireNonNull(player, "player");
        setUserChatSuffix(world(world), player(player), suffix);
    }

    @Override
    public void setPlayerSuffix(World world, String player, String suffix) {
        Objects.requireNonNull(player, "player");
        setUserChatSuffix(world(world), player(player), suffix);
    }

    @Override
    public void setPlayerSuffix(Player player, String suffix) {
        Objects.requireNonNull(player, "player");
        setUserChatSuffix(world(player), player(player), suffix);
    }

    @Override
    public String getGroupPrefix(String world, String group) {
        Objects.requireNonNull(group, "group");
        return getGroupChatPrefix(world(world), group);
    }

    @Override
    public String getGroupPrefix(World world, String group) {
        Objects.requireNonNull(group, "group");
        return getGroupChatPrefix(world(world), group);
    }

    @Override
    public void setGroupPrefix(String world, String group, String prefix) {
        Objects.requireNonNull(group, "group");
        setGroupChatPrefix(world(world), group, prefix);
    }

    @Override
    public void setGroupPrefix(World world, String group, String prefix) {
        Objects.requireNonNull(group, "group");
        setGroupChatPrefix(world(world), group, prefix);
    }

    @Override
    public String getGroupSuffix(String world, String group) {
        Objects.requireNonNull(group, "group");
        return getGroupChatSuffix(world(world), group);
    }

    @Override
    public String getGroupSuffix(World world, String group) {
        Objects.requireNonNull(group, "group");
        return getGroupChatSuffix(world(world), group);
    }

    @Override
    public void setGroupSuffix(String world, String group, String suffix) {
        Objects.requireNonNull(group, "group");
        setGroupChatSuffix(world(world), group, suffix);
    }

    @Override
    public void setGroupSuffix(World world, String group, String suffix) {
        Objects.requireNonNull(group, "group");
        setGroupChatSuffix(world(world), group, suffix);
    }

    @Override
    public int getPlayerInfoInteger(String world, OfflinePlayer player, String node, int defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return intConvert(getUserMeta(world(world), player(player), node), defaultValue);
    }

    @Override
    public int getPlayerInfoInteger(String world, String player, String node, int defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return intConvert(getUserMeta(world(world), player(player), node), defaultValue);
    }

    @Override
    public int getPlayerInfoInteger(World world, String player, String node, int defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return intConvert(getUserMeta(world(world), player(player), node), defaultValue);
    }

    @Override
    public int getPlayerInfoInteger(Player player, String node, int defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return intConvert(getUserMeta(world(player), player(player), node), defaultValue);
    }

    @Override
    public void setPlayerInfoInteger(String world, OfflinePlayer player, String node, int value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(world(world), player(player), node, value);
    }

    @Override
    public void setPlayerInfoInteger(String world, String player, String node, int value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(world(world), player(player), node, value);
    }

    @Override
    public void setPlayerInfoInteger(World world, String player, String node, int value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(world(world), player(player), node, value);
    }

    @Override
    public void setPlayerInfoInteger(Player player, String node, int value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(world(player), player(player), node, value);
    }

    @Override
    public int getGroupInfoInteger(String world, String group, String node, int defaultValue) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        return intConvert(getGroupMeta(world(world), group, node), defaultValue);
    }

    @Override
    public int getGroupInfoInteger(World world, String group, String node, int defaultValue) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        return intConvert(getGroupMeta(world(world), group, node), defaultValue);
    }

    @Override
    public void setGroupInfoInteger(String world, String group, String node, int value) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        setGroupMeta(world(world), group, node, value);
    }

    @Override
    public void setGroupInfoInteger(World world, String group, String node, int value) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        setGroupMeta(world(world), group, node, value);
    }

    @Override
    public double getPlayerInfoDouble(String world, OfflinePlayer player, String node, double defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return doubleConvert(getUserMeta(world(world), player(player), node), defaultValue);
    }

    @Override
    public double getPlayerInfoDouble(String world, String player, String node, double defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return doubleConvert(getUserMeta(world(world), player(player), node), defaultValue);
    }

    @Override
    public double getPlayerInfoDouble(World world, String player, String node, double defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return doubleConvert(getUserMeta(world(world), player(player), node), defaultValue);
    }

    @Override
    public double getPlayerInfoDouble(Player player, String node, double defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return doubleConvert(getUserMeta(world(player), player(player), node), defaultValue);
    }

    @Override
    public void setPlayerInfoDouble(String world, OfflinePlayer player, String node, double value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(world(world), player(player), node, value);
    }

    @Override
    public void setPlayerInfoDouble(String world, String player, String node, double value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(world(world), player(player), node, value);
    }

    @Override
    public void setPlayerInfoDouble(World world, String player, String node, double value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(world(world), player(player), node, value);
    }

    @Override
    public void setPlayerInfoDouble(Player player, String node, double value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(world(player), player(player), node, value);
    }

    @Override
    public double getGroupInfoDouble(String world, String group, String node, double defaultValue) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        return doubleConvert(getGroupMeta(world(world), group, node), defaultValue);
    }

    @Override
    public double getGroupInfoDouble(World world, String group, String node, double defaultValue) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        return doubleConvert(getGroupMeta(world(world), group, node), defaultValue);
    }

    @Override
    public void setGroupInfoDouble(String world, String group, String node, double value) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        setGroupMeta(world(world), group, node, value);
    }

    @Override
    public void setGroupInfoDouble(World world, String group, String node, double value) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        setGroupMeta(world(world), group, node, value);
    }

    @Override
    public boolean getPlayerInfoBoolean(String world, OfflinePlayer player, String node, boolean defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return booleanConvert(getUserMeta(world(world), player(player), node), defaultValue);
    }

    @Override
    public boolean getPlayerInfoBoolean(String world, String player, String node, boolean defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return booleanConvert(getUserMeta(world(world), player(player), node), defaultValue);
    }

    @Override
    public boolean getPlayerInfoBoolean(World world, String player, String node, boolean defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return booleanConvert(getUserMeta(world(world), player(player), node), defaultValue);
    }

    @Override
    public boolean getPlayerInfoBoolean(Player player, String node, boolean defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return booleanConvert(getUserMeta(world(player), player(player), node), defaultValue);
    }

    @Override
    public void setPlayerInfoBoolean(String world, OfflinePlayer player, String node, boolean value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(world(world), player(player), node, value);
    }

    @Override
    public void setPlayerInfoBoolean(String world, String player, String node, boolean value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(world(world), player(player), node, value);
    }

    @Override
    public void setPlayerInfoBoolean(World world, String player, String node, boolean value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(world(world), player(player), node, value);
    }

    @Override
    public void setPlayerInfoBoolean(Player player, String node, boolean value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(world(player), player(player), node, value);
    }

    @Override
    public boolean getGroupInfoBoolean(String world, String group, String node, boolean defaultValue) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        return booleanConvert(getGroupMeta(world(world), group, node), defaultValue);
    }

    @Override
    public boolean getGroupInfoBoolean(World world, String group, String node, boolean defaultValue) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        return booleanConvert(getGroupMeta(world(world), group, node), defaultValue);
    }

    @Override
    public void setGroupInfoBoolean(String world, String group, String node, boolean value) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        setGroupMeta(world(world), group, node, value);
    }

    @Override
    public void setGroupInfoBoolean(World world, String group, String node, boolean value) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        setGroupMeta(world(world), group, node, value);
    }

    @Override
    public String getPlayerInfoString(String world, OfflinePlayer player, String node, String defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return strConvert(getUserMeta(world(world), player(player), node), defaultValue);
    }

    @Override
    public String getPlayerInfoString(String world, String player, String node, String defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return strConvert(getUserMeta(world(world), player(player), node), defaultValue);
    }

    @Override
    public String getPlayerInfoString(World world, String player, String node, String defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return strConvert(getUserMeta(world(world), player(player), node), defaultValue);
    }

    @Override
    public String getPlayerInfoString(Player player, String node, String defaultValue) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        return strConvert(getUserMeta(world(player), player(player), node), defaultValue);
    }

    @Override
    public void setPlayerInfoString(String world, OfflinePlayer player, String node, String value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(world(world), player(player), node, value);
    }

    @Override
    public void setPlayerInfoString(String world, String player, String node, String value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(world(world), player(player), node, value);
    }

    @Override
    public void setPlayerInfoString(World world, String player, String node, String value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(world(world), player(player), node, value);
    }

    @Override
    public void setPlayerInfoString(Player player, String node, String value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(node, "node");
        setUserMeta(world(player), player(player), node, value);
    }

    @Override
    public String getGroupInfoString(String world, String group, String node, String defaultValue) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        return strConvert(getGroupMeta(world(world), group, node), defaultValue);
    }

    @Override
    public String getGroupInfoString(World world, String group, String node, String defaultValue) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        return strConvert(getGroupMeta(world(world), group, node), defaultValue);
    }

    @Override
    public void setGroupInfoString(String world, String group, String node, String value) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        setGroupMeta(world(world), group, node, value);
    }

    @Override
    public void setGroupInfoString(World world, String group, String node, String value) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(node, "node");
        setGroupMeta(world(world), group, node, value);
    }

    @Override
    public boolean playerInGroup(String world, OfflinePlayer player, String group) {
        return this.permissionApi.playerInGroup(world, player, group);
    }

    @Override
    public boolean playerInGroup(String world, String player, String group) {
        return this.permissionApi.playerInGroup(world, player, group);
    }

    @Override
    public boolean playerInGroup(World world, String player, String group) {
        return this.permissionApi.playerInGroup(world, player, group);
    }

    @Override
    public boolean playerInGroup(Player player, String group) {
        return this.permissionApi.playerInGroup(player, group);
    }

    @Override
    public String[] getPlayerGroups(String world, OfflinePlayer player) {
        return this.permissionApi.getPlayerGroups(world, player);
    }

    @Override
    public String[] getPlayerGroups(String world, String player) {
        return this.permissionApi.getPlayerGroups(world, player);
    }

    @Override
    public String[] getPlayerGroups(World world, String player) {
        return this.permissionApi.getPlayerGroups(world, player);
    }

    @Override
    public String[] getPlayerGroups(Player player) {
        return this.permissionApi.getPlayerGroups(player);
    }

    @Override
    public String getPrimaryGroup(String world, OfflinePlayer player) {
        return this.permissionApi.getPrimaryGroup(world, player);
    }

    @Override
    public String getPrimaryGroup(String world, String player) {
        return this.permissionApi.getPrimaryGroup(world, player);
    }

    @Override
    public String getPrimaryGroup(World world, String player) {
        return this.permissionApi.getPrimaryGroup(world, player);
    }

    @Override
    public String getPrimaryGroup(Player player) {
        return this.permissionApi.getPrimaryGroup(player);
    }

    @Override
    public String[] getGroups() {
        return this.permissionApi.getGroups();
    }

}
