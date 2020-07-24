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

package me.lucko.luckperms.velocity;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.ProxyServer;

import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.command.utils.ArgumentTokenizer;
import me.lucko.luckperms.common.sender.Sender;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Arrays;
import java.util.List;

public class VelocityCommandExecutor extends CommandManager implements Command {
    /** The command aliases */
    private static final String[] ALIASES = {"luckpermsvelocity", "lpv", "vperm", "vperms", "vpermission", "vpermissions"};

    /** The command aliases, prefixed with '/' */
    private static final String[] SLASH_ALIASES = Arrays.stream(ALIASES).map(s -> '/' + s).toArray(String[]::new);

    private final LPVelocityPlugin plugin;

    public VelocityCommandExecutor(LPVelocityPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    public void register() {
        ProxyServer proxy = this.plugin.getBootstrap().getProxy();
        proxy.getCommandManager().register(this, ALIASES);

        // register slash aliases so the console can run '/lpv' in the same way as 'lpv'.
        proxy.getCommandManager().register(new ForwardingCommand(this) {
            @Override
            public boolean hasPermission(CommandSource source, @NonNull String[] args) {
                return source instanceof ConsoleCommandSource;
            }
        }, SLASH_ALIASES);
    }

    @Override
    public void execute(@NonNull CommandSource source, @NonNull String[] args) {
        Sender wrapped = this.plugin.getSenderFactory().wrap(source);
        List<String> arguments = ArgumentTokenizer.EXECUTE.tokenizeInput(args);
        executeCommand(wrapped, "lpv", arguments);
    }

    @Override
    public List<String> suggest(@NonNull CommandSource source, @NonNull String[] args) {
        Sender wrapped = this.plugin.getSenderFactory().wrap(source);
        List<String> arguments = ArgumentTokenizer.TAB_COMPLETE.tokenizeInput(args);
        return tabCompleteCommand(wrapped, arguments);
    }

    private static class ForwardingCommand implements Command {
        private final Command delegate;

        private ForwardingCommand(Command delegate) {
            this.delegate = delegate;
        }

        @Override
        public void execute(CommandSource source, @NonNull String[] args) {
            this.delegate.execute(source, args);
        }

        @Override
        public List<String> suggest(CommandSource source, @NonNull String[] currentArgs) {
            return this.delegate.suggest(source, currentArgs);
        }

        @Override
        public boolean hasPermission(CommandSource source, @NonNull String[] args) {
            return this.delegate.hasPermission(source, args);
        }
    }
}
