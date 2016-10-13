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

import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.constants.Permission;

import java.util.Collections;
import java.util.List;

/**
 * An extension of {@link MainCommand} for implementations without any subcommands
 */
public class SingleMainCommand extends MainCommand<Object> {
    private final Permission permission;

    public SingleMainCommand(String name, String usage, int requiredArgsLength, Permission permission) {
        super(name, usage, requiredArgsLength);
        this.permission = permission;
    }

    @Override
    protected CommandResult execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        // Do nothing, allow the implementation to override this
        return null;
    }

    @Override
    protected Object getTarget(String target, LuckPermsPlugin plugin, Sender sender) {
        return null;
    }

    @Override
    protected void cleanup(Object o, LuckPermsPlugin plugin) {
        // Do nothing
    }

    @Override
    protected List<String> getObjects(LuckPermsPlugin plugin) {
        return Collections.emptyList();
    }

    @Override
    protected List<String> onTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin) {
        return Collections.emptyList();
    }

    @Override
    public List<SubCommand<Object>> getSubCommands() {
        return Collections.emptyList();
    }

    @Override
    protected boolean isAuthorized(Sender sender) {
        return permission.isAuthorized(sender);
    }
}
