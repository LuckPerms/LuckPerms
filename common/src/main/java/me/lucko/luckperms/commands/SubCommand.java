package me.lucko.luckperms.commands;

import lombok.Getter;

public abstract class SubCommand {

    @Getter
    private final String name;

    @Getter
    private final String description;

    @Getter
    private final String usage;

    @Getter
    private final String permission;

    protected SubCommand(String name, String description, String usage, String permission) {
        this.name = name;
        this.description = description;
        this.usage = usage;
        this.permission = permission;
    }

    public boolean isAuthorized(Sender sender) {
        return sender.hasPermission(permission);
    }

    public void sendUsage(Sender sender) {
        Util.sendPluginMessage(sender, "&e-> &d" + getUsage());
    }

    public abstract boolean isArgLengthInvalid(int argLength);
}
