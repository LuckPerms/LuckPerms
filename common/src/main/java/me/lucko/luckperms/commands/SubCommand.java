package me.lucko.luckperms.commands;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public abstract class SubCommand {
    private final String name;
    private final String description;
    private final String usage;
    private final Permission permission;

    public boolean isAuthorized(Sender sender) {
        return permission.isAuthorized(sender);
    }

    public void sendUsage(Sender sender) {
        Util.sendPluginMessage(sender, "&e-> &d" + getUsage());
    }

    public abstract boolean isArgLengthInvalid(int argLength);
}
