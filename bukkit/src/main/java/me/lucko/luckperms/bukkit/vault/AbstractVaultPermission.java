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

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * An extended abstraction of the Vault {@link Permission} API.
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
public abstract class AbstractVaultPermission extends Permission {

    // when upgrading and forwarding requests, all world strings are passed through this function.
    // it lets the overriding class define some custom behaviour for world handling.
    protected Function<String, String> worldMappingFunction = Function.identity();

    public AbstractVaultPermission() {
        super.plugin = JavaPlugin.getProvidingPlugin(Permission.class);
    }

    @Override
    public boolean isEnabled() {
        // always return true
        return true;
    }

    @Override
    public boolean hasSuperPermsCompat() {
        // always return true
        return true;
    }

    @Override
    public boolean hasGroupSupport() {
        // always return true
        return true;
    }

    // methods subclasses are expected to implement
    public abstract boolean userHasPermission(String world, UUID uuid, String permission);
    public abstract boolean userAddPermission(String world, UUID uuid, String permission);
    public abstract boolean userRemovePermission(String world, UUID uuid, String permission);
    public abstract boolean userInGroup(String world, UUID uuid, String group);
    public abstract boolean userAddGroup(String world, UUID uuid, String group);
    public abstract boolean userRemoveGroup(String world, UUID uuid, String group);
    public abstract String[] userGetGroups(String world, UUID uuid);
    public abstract String userGetPrimaryGroup(String world, UUID uuid);
    public abstract boolean groupHasPermission(String world, String name, String permission);
    public abstract boolean groupAddPermission(String world, String name, String permission);
    public abstract boolean groupRemovePermission(String world, String name, String permission);

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
    public boolean has(String world, String player, String permission) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(permission, "permission");
        return userHasPermission(world(world), player(player), permission);
    }

    @Override
    public boolean has(World world, String player, String permission) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(permission, "permission");
        return userHasPermission(world(world), player(player), permission);
    }

    @Override
    public boolean has(Player player, String permission) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(permission, "permission");
        return userHasPermission(world(player), player(player), permission);
    }

    @Override
    public boolean playerHas(String world, String player, String permission) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(permission, "permission");
        return userHasPermission(world(world), player(player), permission);
    }

    @Override
    public boolean playerHas(World world, String player, String permission) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(permission, "permission");
        return userHasPermission(world(world), player(player), permission);
    }

    @Override
    public boolean playerHas(String world, OfflinePlayer player, String permission) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(permission, "permission");
        return userHasPermission(world(world), player(player), permission);
    }

    @Override
    public boolean playerHas(Player player, String permission) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(permission, "permission");
        return userHasPermission(world(player), player(player), permission);
    }

    @Override
    public boolean playerAdd(String world, String player, String permission) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(permission, "permission");
        return userAddPermission(world(world), player(player), permission);
    }

    @Override
    public boolean playerAdd(World world, String player, String permission) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(permission, "permission");
        return userAddPermission(world(world), player(player), permission);
    }

    @Override
    public boolean playerAdd(String world, OfflinePlayer player, String permission) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(permission, "permission");
        return userAddPermission(world(world), player(player), permission);
    }

    @Override
    public boolean playerAdd(Player player, String permission) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(permission, "permission");
        return userAddPermission(world(player), player(player), permission);
    }

    @Override
    public boolean playerRemove(String world, String player, String permission) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(permission, "permission");
        return userRemovePermission(world(world), player(player), permission);
    }

    @Override
    public boolean playerRemove(String world, OfflinePlayer player, String permission) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(permission, "permission");
        return userRemovePermission(world(world), player(player), permission);
    }

    @Override
    public boolean playerRemove(World world, String player, String permission) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(permission, "permission");
        return userRemovePermission(world(world), player(player), permission);
    }

    @Override
    public boolean playerRemove(Player player, String permission) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(permission, "permission");
        return userRemovePermission(world(player), player(player), permission);
    }

    @Override
    public boolean groupHas(String world, String group, String permission) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(permission, "permission");
        return groupHasPermission(world(world), group, permission);
    }

    @Override
    public boolean groupHas(World world, String group, String permission) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(permission, "permission");
        return groupHasPermission(world(world), group, permission);
    }

    @Override
    public boolean groupAdd(String world, String group, String permission) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(permission, "permission");
        return groupAddPermission(world(world), group, permission);
    }

    @Override
    public boolean groupAdd(World world, String group, String permission) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(permission, "permission");
        return groupAddPermission(world(world), group, permission);
    }

    @Override
    public boolean groupRemove(String world, String group, String permission) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(permission, "permission");
        return groupRemovePermission(world(world), group, permission);
    }

    @Override
    public boolean groupRemove(World world, String group, String permission) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(permission, "permission");
        return groupRemovePermission(world(world), group, permission);
    }

    @Override
    public boolean playerInGroup(String world, String player, String group) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(group, "group");
        return userInGroup(world(world), player(player), group);
    }

    @Override
    public boolean playerInGroup(World world, String player, String group) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(group, "group");
        return userInGroup(world(world), player(player), group);
    }

    @Override
    public boolean playerInGroup(String world, OfflinePlayer player, String group) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(group, "group");
        return userInGroup(world(world), player(player), group);
    }

    @Override
    public boolean playerInGroup(Player player, String group) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(group, "group");
        return userInGroup(world(player), player(player), group);
    }

    @Override
    public boolean playerAddGroup(String world, String player, String group) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(group, "group");
        return userAddGroup(world(world), player(player), group);
    }

    @Override
    public boolean playerAddGroup(World world, String player, String group) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(group, "group");
        return userAddGroup(world(world), player(player), group);
    }

    @Override
    public boolean playerAddGroup(String world, OfflinePlayer player, String group) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(group, "group");
        return userAddGroup(world(world), player(player), group);
    }

    @Override
    public boolean playerAddGroup(Player player, String group) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(group, "group");
        return userAddGroup(world(player), player(player), group);
    }

    @Override
    public boolean playerRemoveGroup(String world, String player, String group) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(group, "group");
        return userRemoveGroup(world(world), player(player), group);
    }

    @Override
    public boolean playerRemoveGroup(World world, String player, String group) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(group, "group");
        return userRemoveGroup(world(world), player(player), group);
    }

    @Override
    public boolean playerRemoveGroup(String world, OfflinePlayer player, String group) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(group, "group");
        return userRemoveGroup(world(world), player(player), group);
    }

    @Override
    public boolean playerRemoveGroup(Player player, String group) {
        Objects.requireNonNull(player, "player");
        return userRemoveGroup(world(player), player(player), group);
    }

    @Override
    public String[] getPlayerGroups(String world, String player) {
        Objects.requireNonNull(player, "player");
        return userGetGroups(world(world), player(player));
    }

    @Override
    public String[] getPlayerGroups(World world, String player) {
        Objects.requireNonNull(player, "player");
        return userGetGroups(world(world), player(player));
    }

    @Override
    public String[] getPlayerGroups(String world, OfflinePlayer player) {
        Objects.requireNonNull(player, "player");
        return userGetGroups(world(world), player(player));
    }

    @Override
    public String[] getPlayerGroups(Player player) {
        Objects.requireNonNull(player, "player");
        return userGetGroups(world(player), player(player));
    }

    @Override
    public String getPrimaryGroup(String world, String player) {
        Objects.requireNonNull(player, "player");
        return userGetPrimaryGroup(world(world), player(player));
    }

    @Override
    public String getPrimaryGroup(World world, String player) {
        Objects.requireNonNull(player, "player");
        return userGetPrimaryGroup(world(world), player(player));
    }

    @Override
    public String getPrimaryGroup(String world, OfflinePlayer player) {
        Objects.requireNonNull(player, "player");
        return userGetPrimaryGroup(world(world), player(player));
    }

    @Override
    public String getPrimaryGroup(Player player) {
        Objects.requireNonNull(player, "player");
        return userGetPrimaryGroup(world(player), player(player));
    }

}
