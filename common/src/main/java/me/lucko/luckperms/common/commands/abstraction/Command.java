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

package me.lucko.luckperms.common.commands.abstraction;

import lombok.Getter;

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandPermission;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.locale.LocalizedSpec;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * An abstract command class
 *
 * @param <T> the type required by the {@link #execute(LuckPermsPlugin, Sender, Object, List, String)} method of this command
 * @param <S> the type of any child commands
 */
public abstract class Command<T, S> {

    @Getter
    private final LocalizedSpec spec;

    /**
     * The name of the command. Should be properly capitalised.
     */
    @Getter
    private final String name;

    /**
     * The permission required to use this command. Nullable.
     */
    private final CommandPermission permission;

    /**
     * A predicate used for testing the size of the arguments list passed to this command
     */
    @Getter
    private final Predicate<Integer> argumentCheck;

    /**
     * Child commands. Nullable.
     */
    private final List<Command<S, ?>> children;

    public Command(LocalizedSpec spec, String name, CommandPermission permission, Predicate<Integer> argumentCheck, List<Command<S, ?>> children) {
        this.spec = spec;
        this.name = name;
        this.permission = permission;
        this.argumentCheck = argumentCheck;
        this.children = children == null ? null : ImmutableList.copyOf(children);
    }

    public Command(LocalizedSpec spec, String name, CommandPermission permission, Predicate<Integer> argumentCheck) {
        this(spec, name, permission, argumentCheck, null);
    }

    public Command(LocalizedSpec spec, String name, Predicate<Integer> argumentCheck) {
        this(spec, name, null, argumentCheck, null);
    }

    public abstract CommandResult execute(LuckPermsPlugin plugin, Sender sender, T t, List<String> args, String label) throws CommandException;

    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return Collections.emptyList();
    }

    /**
     * Sends a brief command usage message to the Sender.
     * If this command has child commands, the children are listed. Otherwise, a basic usage message is sent.
     *
     * @param sender the sender to send the usage to
     * @param label the label used when executing the command
     */
    public abstract void sendUsage(Sender sender, String label);

    /**
     * Sends a detailed command usage message to the Sender.
     * If this command has child commands, nothing is sent. Otherwise, a detailed messaging containing a description
     * and argument usage is sent.
     *
     * @param sender the sender to send the usage to
     * @param label the label used when executing the command
     */
    public abstract void sendDetailedUsage(Sender sender, String label);

    /**
     * Returns true if the sender is authorised to use this command
     *
     * Commands with children are likely to override this method to check for permissions based upon whether
     * a sender has access to any sub commands.
     *
     * @param sender the sender
     * @return true if the sender has permission to use this command
     */
    public boolean isAuthorized(Sender sender) {
        return permission == null || permission.isAuthorized(sender);
    }

    /**
     * Returns if this command should be displayed in command listings, or "hidden"
     *
     * @return if this command should be displayed in command listings, or "hidden"
     */
    public boolean shouldDisplay() {
        return true;
    }

    public String getDescription() {
        return spec.description();
    }

    /**
     * Returns the usage of this command. Will only return a non empty result for main commands.
     *
     * @return the usage of this command.
     */
    public String getUsage() {
        String usage = spec.usage();
        return usage == null ? "" : usage;
    }

    public Optional<CommandPermission> getPermission() {
        return Optional.ofNullable(permission);
    }

    public Optional<List<Arg>> getArgs() {
        return Optional.ofNullable(spec.args());
    }

    public Optional<List<Command<S, ?>>> getChildren() {
        return Optional.ofNullable(children);
    }

}
