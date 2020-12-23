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

import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.ProxyServer;

import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.command.utils.ArgumentTokenizer;
import me.lucko.luckperms.common.sender.Sender;

import java.util.Arrays;
import java.util.List;

public class VelocityCommandExecutor extends CommandManager implements RawCommand {
    /* The command aliases */
    private static final String PRIMARY_ALIAS = "luckpermsvelocity";
    private static final String[] ALIASES = {"lpv", "vperm", "vperms", "vpermission", "vpermissions"};

    /* The command aliases, prefixed with '/' */
    private static final String SLASH_PRIMARY_ALIAS = "/luckpermsvelocity";
    private static final String[] SLASH_ALIASES = Arrays.stream(ALIASES).map(s -> '/' + s).toArray(String[]::new);

    private final LPVelocityPlugin plugin;

    public VelocityCommandExecutor(LPVelocityPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    public void register() {
        ProxyServer proxy = this.plugin.getBootstrap().getProxy();
        proxy.getCommandManager().register(PRIMARY_ALIAS, this, ALIASES);

        // register slash aliases so the console can run '/lpv' in the same way as 'lpv'.
        proxy.getCommandManager().register(SLASH_PRIMARY_ALIAS, new ForwardingCommand(this) {
            @Override
            public boolean hasPermission(Invocation invocation) {
                return invocation.source() instanceof ConsoleCommandSource;
            }
        }, SLASH_ALIASES);
    }

    @Override
    public void execute(Invocation invocation) {
        Sender wrapped = this.plugin.getSenderFactory().wrap(invocation.source());
        List<String> arguments = ArgumentTokenizer.EXECUTE.tokenizeInput(invocation.arguments());
        executeCommand(wrapped, "lpv", arguments);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        Sender wrapped = this.plugin.getSenderFactory().wrap(invocation.source());
        List<String> arguments = ArgumentTokenizer.TAB_COMPLETE.tokenizeInput(invocation.arguments());
        return tabCompleteCommand(wrapped, arguments);
    }

    private static class ForwardingCommand implements RawCommand {
        private final RawCommand delegate;

        private ForwardingCommand(RawCommand delegate) {
            this.delegate = delegate;
        }

        @Override
        public void execute(Invocation invocation) {
            this.delegate.execute(invocation);
        }

        @Override
        public List<String> suggest(Invocation invocation) {
            return this.delegate.suggest(invocation);
        }
    }
}
