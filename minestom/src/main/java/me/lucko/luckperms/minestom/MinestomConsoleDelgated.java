package me.lucko.luckperms.minestom;

import me.lucko.luckperms.minestom.app.integration.MinestomPermissible;
import net.kyori.adventure.identity.Identity;
import net.minestom.server.command.CommandSender;
import net.minestom.server.tag.TagHandler;
import org.jetbrains.annotations.NotNull;

public record MinestomConsoleDelgated(CommandSender console) implements MinestomPermissible, CommandSender {
    @Override
    public boolean hasPermission(String node) {
        return true;
    }

    @Override
    public @NotNull Identity identity() {
        return this.console.identity();
    }

    @Override
    public TagHandler tagHandler() {
        return this.console.tagHandler();
    }

    public static MinestomConsoleDelgated of(CommandSender console) {
        return new MinestomConsoleDelgated(console);
    }
}
