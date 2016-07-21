package me.lucko.luckperms.commands;

/**
 * Wrapper class to represent a CommandSender in Bukkit/Bungee within the luckperms-common command implementations.
 */
public interface Sender {

    void sendMessage(String s);
    boolean hasPermission(String node);

}
