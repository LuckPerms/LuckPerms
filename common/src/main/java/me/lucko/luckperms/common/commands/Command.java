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

package me.lucko.luckperms.common.commands;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.constants.Permission;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public abstract class Command<T, S> {

    @Getter
    private final String name;

    @Getter
    private final String description;

    // Could be null
    private final Permission permission;

    @Getter
    private final Predicate<Integer> argumentCheck;

    // Could be null
    private final List<Arg> args;

    // Could be null
    private final List<Command<S, ?>> children;

    public Command(String name, String description, Permission permission, Predicate<Integer> argumentCheck, List<Arg> args, List<Command<S, ?>> children) {
        this.name = name;
        this.description = description;
        this.permission = permission;
        this.argumentCheck = argumentCheck;
        this.args = args == null ? null : ImmutableList.copyOf(args);
        this.children = children == null ? null : ImmutableList.copyOf(children);
    }

    public Command(String name, String description, Permission permission, Predicate<Integer> argumentCheck, List<Arg> args) {
        this(name, description, permission, argumentCheck, args, null);
    }

    public Command(String name, String description, Predicate<Integer> argumentCheck) {
        this(name, description, null, argumentCheck, null, null);
    }

    public abstract CommandResult execute(LuckPermsPlugin plugin, Sender sender, T t, List<String> args, String label) throws CommandException;

    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return Collections.emptyList();
    }

    public abstract void sendUsage(Sender sender, String label);
    public abstract void sendDetailedUsage(Sender sender, String label);

    public boolean isAuthorized(Sender sender) {
        return permission == null || permission.isAuthorized(sender);
    }

    public Optional<Permission> getPermission() {
        return Optional.ofNullable(permission);
    }

    public Optional<List<Arg>> getArgs() {
        return Optional.ofNullable(args);
    }

    public Optional<List<Command<S, ?>>> getChildren() {
        return Optional.ofNullable(children);
    }

}
