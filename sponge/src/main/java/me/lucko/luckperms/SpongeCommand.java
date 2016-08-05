package me.lucko.luckperms;

import me.lucko.luckperms.commands.CommandManager;
import me.lucko.luckperms.commands.SenderFactory;
import me.lucko.luckperms.utils.Patterns;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

class SpongeCommand extends CommandManager implements CommandCallable {
    private static final Factory FACTORY = new Factory();

    SpongeCommand(LuckPermsPlugin plugin) {
        super(plugin);
    }

    @Override
    public CommandResult process(CommandSource source, String s) throws CommandException {
        onCommand(FACTORY.wrap(source), "perms", Arrays.asList(Patterns.SPACE.split(s)));
        return CommandResult.success();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String s) throws CommandException {
        return onTabComplete(FACTORY.wrap(source), Arrays.asList(Patterns.SPACE.split(s)));
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return true;
    }

    @Override
    public Optional<? extends Text> getShortDescription(CommandSource source) {
        return Optional.of(Text.of("LuckPerms main command."));
    }

    @Override
    public Optional<? extends Text> getHelp(CommandSource source) {
        return Optional.of(Text.of("Type /perms for help."));
    }

    @Override
    public Text getUsage(CommandSource source) {
        return Text.of("/perms");
    }

    private static class Factory extends SenderFactory<CommandSource> {

        @SuppressWarnings("deprecation")
        @Override
        protected void sendMessage(CommandSource source, String s) {
            source.sendMessage(TextSerializers.LEGACY_FORMATTING_CODE.deserialize(s));
        }

        @Override
        protected boolean hasPermission(CommandSource source, String node) {
            return source.hasPermission(node);
        }
    }
}
