package me.lucko.luckperms;

import me.lucko.luckperms.commands.CommandManager;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.utils.Patterns;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class SpongeCommand extends CommandManager implements CommandCallable {
    public SpongeCommand(LuckPermsPlugin plugin) {
        super(plugin);
    }

    @Override
    public CommandResult process(CommandSource source, String s) throws CommandException {
        onCommand(makeSender(source), "perms", Arrays.asList(Patterns.SPACE_SPLIT.split(s)));
        return CommandResult.success();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String s) throws CommandException {
        return onTabComplete(makeSender(source), Arrays.asList(Patterns.SPACE_SPLIT.split(s)));
    }

    @Override
    public boolean testPermission(CommandSource commandSource) {
        return true;
    }

    @Override
    public Optional<? extends Text> getShortDescription(CommandSource commandSource) {
        return Optional.of(Text.of("LuckPerms main command."));
    }

    @Override
    public Optional<? extends Text> getHelp(CommandSource commandSource) {
        return Optional.of(Text.of("Type /perms for help."));
    }

    @Override
    public Text getUsage(CommandSource commandSource) {
        return Text.of("/perms");
    }

    private static Sender makeSender(CommandSource source) {
        return new Sender() {
            final WeakReference<CommandSource> cs = new WeakReference<>(source);

            @SuppressWarnings("deprecation")
            @Override
            public void sendMessage(String s) {
                final CommandSource c = cs.get();
                if (c != null) {
                    c.sendMessage(TextSerializers.LEGACY_FORMATTING_CODE.deserialize(s));
                }
            }

            @Override
            public boolean hasPermission(String node) {
                final CommandSource c = cs.get();
                return c != null && c.hasPermission(node);
            }
        };
    }
}
