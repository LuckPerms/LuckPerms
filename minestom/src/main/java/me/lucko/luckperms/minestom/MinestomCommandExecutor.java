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

import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.command.utils.ArgumentTokenizer;
import me.lucko.luckperms.common.sender.Sender;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandProcessor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("deprecation") // todo check that we can't make it something else
public class MinestomCommandExecutor extends CommandManager implements CommandProcessor {
    private static final String[] COMMAND_ALIASES = new String[] {"lp", "perm", "perms", "permission", "permissions"};

    private final LPMinestomPlugin plugin;

    public MinestomCommandExecutor(LPMinestomPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    public void register() {
        MinecraftServer.getCommandManager().register(this);
    }

    @Override
    public @NotNull String getCommandName() {
        return "luckperms";
    }

    @Override
    public @Nullable String[] getAliases() {
        return COMMAND_ALIASES;
    }

    @Override
    public boolean process(@NotNull CommandSender sender, @NotNull String command, @NotNull String[] args) {
        Sender wrapped = this.plugin.getSenderFactory().wrap(sender);
        List<String> arguments = ArgumentTokenizer.EXECUTE.tokenizeInput(args);
        executeCommand(wrapped, command, arguments);
        return true;
    }

    @Override
    public String[] onWrite(@NotNull CommandSender sender, String text) {
        Sender wrapped = this.plugin.getSenderFactory().wrap(sender);
        List<String> arguments = ArgumentTokenizer.TAB_COMPLETE.tokenizeInput(text);
        return tabCompleteCommand(wrapped, arguments).toArray(new String[0]);
    }

    @Override
    public boolean enableWritingTracking() {
        return true;
    }

    @Override
    public boolean hasAccess(@NotNull Player player) {
        return false;
    }
}
