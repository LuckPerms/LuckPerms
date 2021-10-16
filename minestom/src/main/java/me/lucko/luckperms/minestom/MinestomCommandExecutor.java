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

package me.lucko.luckperms.minestom;

import java.util.Arrays;
import me.lucko.luckperms.common.command.CommandManager;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.SimpleCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// todo tab completion support
public class MinestomCommandExecutor extends CommandManager {
    private final LuckPermsCommand command;
    private final LPMinestomPlugin plugin;

    public MinestomCommandExecutor(LPMinestomPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
        this.command = new LuckPermsCommand(this);
    }

    public void register() {
        MinecraftServer.getCommandManager().register(this.command);
    }

    public void unregister() {
        MinecraftServer.getCommandManager().unregister(this.command);
    }

    private static class LuckPermsCommand extends SimpleCommand {
        private final MinestomCommandExecutor commandExecutor;

        public LuckPermsCommand(@NotNull MinestomCommandExecutor commandExecutor) {
            super("luckperms", "lp", "perm", "perms", "permission", "permissions");
            this.commandExecutor = commandExecutor;
        }

        @Override
        public boolean process(@NotNull CommandSender sender, @NotNull String command, @NotNull String[] args) {
            this.commandExecutor.executeCommand(this.commandExecutor.plugin.getSenderFactory().wrap(sender), command, Arrays.asList(args));
            return true;
        }

        @Override
        public boolean hasAccess(@NotNull CommandSender sender, @Nullable String commandString) {
            return true;
        }
    }
}
