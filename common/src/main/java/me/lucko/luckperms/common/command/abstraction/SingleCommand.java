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

package me.lucko.luckperms.common.command.abstraction;

import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.Argument;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Represents a single "main" command (one without any children)
 */
public abstract class SingleCommand extends Command<Void> {

    public SingleCommand(CommandSpec spec, String name, CommandPermission permission, Predicate<Integer> argumentCheck) {
        super(spec, name, permission, argumentCheck);
    }

    @Override
    public final void execute(LuckPermsPlugin plugin, Sender sender, Void ignored, ArgumentList args, String label) throws CommandException {
        execute(plugin, sender, args, label);
    }

    public abstract void execute(LuckPermsPlugin plugin, Sender sender, ArgumentList args, String label) throws CommandException;

    @Override
    public void sendUsage(Sender sender, String label) {
        TextComponent.Builder builder = Component.text()
                .append(Component.text('>', NamedTextColor.DARK_AQUA))
                .append(Component.space())
                .append(Component.text(getName().toLowerCase(Locale.ROOT), NamedTextColor.GREEN));

        if (getArgs().isPresent()) {
            List<Component> argUsages = getArgs().get().stream()
                    .map(Argument::asPrettyString)
                    .collect(Collectors.toList());

            builder.append(Component.text(" - ", NamedTextColor.DARK_AQUA))
                    .append(Component.join(JoinConfiguration.separator(Component.space()), argUsages))
                    .build();
        }

        sender.sendMessage(builder.build());
    }

    @Override
    public void sendDetailedUsage(Sender sender, String label) {
        Message.COMMAND_USAGE_DETAILED_HEADER.send(sender, getName(), getDescription());
        if (getArgs().isPresent()) {
            Message.COMMAND_USAGE_DETAILED_ARGS_HEADER.send(sender);
            for (Argument arg : getArgs().get()) {
                Message.COMMAND_USAGE_DETAILED_ARG.send(sender, arg.asPrettyString(), arg.getDescription());
            }
        }
    }
}
