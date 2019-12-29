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

package me.lucko.luckperms.sponge;

import com.google.common.base.Splitter;

import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.sender.Sender;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.selector.Selector;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

public class SpongeCommandExecutor extends CommandManager implements CommandCallable {
    private static final Splitter TAB_COMPLETE_ARGUMENT_SPLITTER = Splitter.on(COMMAND_SEPARATOR_PATTERN);
    private static final Splitter ARGUMENT_SPLITTER = Splitter.on(COMMAND_SEPARATOR_PATTERN).omitEmptyStrings();

    private final LPSpongePlugin plugin;

    public SpongeCommandExecutor(LPSpongePlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    public @NonNull CommandResult process(@NonNull CommandSource source, @NonNull String s) {
        Sender lpSender = this.plugin.getSenderFactory().wrap(source);
        List<String> arguments = processSelectors(source, CommandManager.stripQuotes(ARGUMENT_SPLITTER.splitToList(s)));

        onCommand(lpSender, "lp", arguments);
        return CommandResult.success();
    }

    @Override
    public @NonNull List<String> getSuggestions(@NonNull CommandSource source, @NonNull String s, @Nullable Location<World> location) {
        Sender lpSender = this.plugin.getSenderFactory().wrap(source);
        List<String> arguments = processSelectors(source, CommandManager.stripQuotes(TAB_COMPLETE_ARGUMENT_SPLITTER.splitToList(s)));

        return onTabComplete(lpSender, arguments);
    }

    @Override
    public boolean testPermission(@NonNull CommandSource source) {
        return true; // we run permission checks internally
    }

    @Override
    public @NonNull Optional<Text> getShortDescription(@NonNull CommandSource source) {
        return Optional.of(Text.of("Manage permissions"));
    }

    @Override
    public @NonNull Optional<Text> getHelp(@NonNull CommandSource source) {
        return Optional.of(Text.of("Run /luckperms to view usage."));
    }

    @Override
    public @NonNull Text getUsage(@NonNull CommandSource source) {
        return Text.of("/luckperms");
    }

    private List<String> processSelectors(CommandSource source, List<String> args) {
        ListIterator<String> it = args.listIterator();
        while (it.hasNext()) {
            String element = it.next();
            if (element.startsWith("@")) {
                try {
                    Selector.parse(element).resolve(source).stream()
                            .filter(e -> e instanceof Player)
                            .map(e -> ((Player) e))
                            .findFirst()
                            .ifPresent(player -> it.set(player.getUniqueId().toString()));
                } catch (IllegalArgumentException e) {
                    // ignored
                }
            }
        }
        return args;
    }

}
