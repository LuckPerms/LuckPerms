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

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandPermission;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.locale.LocalizedSpec;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An abstract command class
 *
 * @param <T> the argument type required by the command
 * @param <S> the type of any child commands
 */
public abstract class Command<T, S> {

    /**
     * The commands specification.
     *
     * Contains details about usage, description, etc
     */
    @Nonnull
    private final LocalizedSpec spec;

    /**
     * The name of the command. Should be properly capitalised.
     */
    @Nonnull
    private final String name;

    /**
     * The permission required to use this command. Nullable.
     */
    @Nullable
    private final CommandPermission permission;

    /**
     * A predicate used for testing the size of the arguments list passed to this command
     */
    @Nonnull
    private Predicate<Integer> argumentCheck = Predicates.alwaysFalse();

    /**
     * Child commands. Nullable.
     */
    @Nullable
    private final List<Command<S, ?>> children;

    public Command(@Nonnull LocalizedSpec spec, @Nonnull String name, @Nullable CommandPermission permission, @Nonnull Predicate<Integer> argumentCheck, @Nullable List<Command<S, ?>> children) {
        this.spec = spec;
        this.name = name;
        this.permission = permission;
        this.argumentCheck = argumentCheck;
        this.children = children == null ? null : ImmutableList.copyOf(children);
    }

    public Command(@Nonnull LocalizedSpec spec, @Nonnull String name, @Nullable CommandPermission permission, @Nonnull Predicate<Integer> argumentCheck) {
        this(spec, name, permission, argumentCheck, null);
    }

    public Command(@Nonnull LocalizedSpec spec, @Nonnull String name, @Nonnull Predicate<Integer> argumentCheck) {
        this(spec, name, null, argumentCheck, null);
    }

    /**
     * Gets the commands spec.
     *
     * @return the command spec
     */
    @Nonnull
    public LocalizedSpec getSpec() {
        return this.spec;
    }

    /**
     * Gets the short name of this command
     *
     * <p>The result should be appropriately capitalised.</p>
     *
     * @return the command name
     */
    @Nonnull
    public String getName() {
        return this.name;
    }

    /**
     * Gets the permission required by this command, if present
     *
     * @return the command permission
     */
    @Nonnull
    public Optional<CommandPermission> getPermission() {
        return Optional.ofNullable(this.permission);
    }

    /**
     * Gets the predicate used to validate the number of arguments provided to
     * the command on execution
     *
     * @return the argument checking predicate
     */
    @Nonnull
    public Predicate<Integer> getArgumentCheck() {
        return this.argumentCheck;
    }

    /**
     * Gets the commands children
     *
     * @return any child commands
     */
    @Nonnull
    public Optional<List<Command<S, ?>>> getChildren() {
        return Optional.ofNullable(this.children);
    }

    /**
     * Gets the commands description.
     *
     * @return the description
     */
    public String getDescription() {
        return getSpec().description();
    }

    /**
     * Gets the usage of this command.
     * Will only return a non empty result for main commands.
     *
     * @return the usage of this command.
     */
    public String getUsage() {
        String usage = getSpec().usage();
        return usage == null ? "" : usage;
    }

    /**
     * Gets the arguments required by this command
     *
     * @return the commands arguments
     */
    public Optional<List<Arg>> getArgs() {
        return Optional.ofNullable(getSpec().args());
    }

    // Main execution method for the command.
    public abstract CommandResult execute(LuckPermsPlugin plugin, Sender sender, T t, List<String> args, String label) throws CommandException;

    // Tab completion method - default implementation is provided as some commands do not provide tab completions.
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
        return this.permission == null || this.permission.isAuthorized(sender);
    }

    /**
     * Gets if this command should be displayed in command listings, or "hidden"
     *
     * @return if the command should be displayed
     */
    public boolean shouldDisplay() {
        return true;
    }

}
