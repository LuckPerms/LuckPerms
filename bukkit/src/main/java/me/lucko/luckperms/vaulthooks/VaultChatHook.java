package me.lucko.luckperms.vaulthooks;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;

/**
 * Dummy class for hooking with Vault plugins that need both Chat + Perms
 * This doesn't return anything useful, or change anything internally
 *
 * Registered on the lowest priority so other plugins can override
 */
class VaultChatHook extends Chat {

    VaultChatHook(Permission perms) {
        super(perms);
    }

    private void throwNotSupported() {
        // throw new UnsupportedOperationException("LuckPerms cannot perform this operation.");
    }

    public String getName() {
        return "LuckPerms";
    }


    public boolean isEnabled() {
        return true;
    }

    public String getPlayerPrefix(String world, String player) {
        return "";
    }

    public void setPlayerPrefix(String world, String player, String prefix) {
        throwNotSupported();
    }

    public String getPlayerSuffix(String world, String player) {
        return "";
    }

    public void setPlayerSuffix(String world, String player, String suffix) {
        throwNotSupported();
    }

    public String getGroupPrefix(String world, String group) {
        return "";
    }

    public void setGroupPrefix(String world, String group, String prefix) {
        throwNotSupported();
    }

    public String getGroupSuffix(String world, String group) {
        return "";
    }

    public void setGroupSuffix(String world, String group, String suffix) {
        throwNotSupported();
    }

    public int getPlayerInfoInteger(String world, String player, String node, int defaultValue) {
        return defaultValue;
    }

    public void setPlayerInfoInteger(String world, String player, String node, int value) {
        throwNotSupported();
    }

    public int getGroupInfoInteger(String world, String group, String node, int defaultValue) {
        return defaultValue;
    }

    public void setGroupInfoInteger(String world, String group, String node, int value) {
        throwNotSupported();
    }

    public double getPlayerInfoDouble(String world, String player, String node, double defaultValue) {
        return defaultValue;
    }

    public void setPlayerInfoDouble(String world, String player, String node, double value) {
        throwNotSupported();
    }

    public double getGroupInfoDouble(String world, String group, String node, double defaultValue) {
        return defaultValue;
    }

    public void setGroupInfoDouble(String world, String group, String node, double value) {
        throwNotSupported();
    }

    public boolean getPlayerInfoBoolean(String world, String player, String node, boolean defaultValue) {
        return defaultValue;
    }

    public void setPlayerInfoBoolean(String world, String player, String node, boolean value) {
        throwNotSupported();
    }

    public boolean getGroupInfoBoolean(String world, String group, String node, boolean defaultValue) {
        return defaultValue;
    }

    public void setGroupInfoBoolean(String world, String group, String node, boolean value) {
        throwNotSupported();
    }

    public String getPlayerInfoString(String world, String player, String node, String defaultValue) {
        return defaultValue;
    }

    public void setPlayerInfoString(String world, String player, String node, String value) {
        throwNotSupported();
    }

    public String getGroupInfoString(String world, String group, String node, String defaultValue) {
        return defaultValue;
    }

    public void setGroupInfoString(String world, String group, String node, String value) {
        throwNotSupported();
    }

}
