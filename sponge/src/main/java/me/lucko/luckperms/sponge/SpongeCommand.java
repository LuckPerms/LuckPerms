/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

package me.lucko.luckperms.sponge;

import com.google.common.base.Splitter;
import me.lucko.luckperms.api.data.Callback;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.CommandManager;
import me.lucko.luckperms.common.commands.Util;
import me.lucko.luckperms.common.constants.Patterns;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("NullableProblems")
class SpongeCommand extends CommandManager implements CommandCallable {
    SpongeCommand(LuckPermsPlugin plugin) {
        super(plugin);
    }

    @Override
    public CommandResult process(CommandSource source, String s) throws CommandException {
        onCommand(
                SpongeSenderFactory.get(getPlugin()).wrap(source),
                "perms",
                Util.stripQuotes(Splitter.on(Patterns.COMMAND_SEPARATOR).omitEmptyStrings().splitToList(s)),
                Callback.empty()
        );
        return CommandResult.success();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String s, @Nullable Location<World> location) throws CommandException {
        return onTabComplete(SpongeSenderFactory.get(getPlugin()).wrap(source), Splitter.on(' ').splitToList(s));
    }

    public List<String> getSuggestions(CommandSource source, String s) throws CommandException {
        return onTabComplete(SpongeSenderFactory.get(getPlugin()).wrap(source), Splitter.on(' ').splitToList(s));
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return true;
    }

    @Override
    public Optional<Text> getShortDescription(CommandSource source) {
        return Optional.of(Text.of("LuckPerms main command."));
    }

    @Override
    public Optional<Text> getHelp(CommandSource source) {
        return Optional.of(Text.of("Type /perms for help."));
    }

    @Override
    public Text getUsage(CommandSource source) {
        return Text.of("/perms");
    }
}
