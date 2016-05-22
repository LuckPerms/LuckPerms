package me.lucko.luckperms.commands;

public interface Sender {

    void sendMessage(String s);
    boolean hasPermission(String node);

}
