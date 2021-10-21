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

package me.lucko.luckperms.waterdog;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.command.Command;
import dev.waterdog.waterdogpe.command.CommandSender;
import dev.waterdog.waterdogpe.command.CommandSettings;
import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.command.utils.ArgumentTokenizer;
import me.lucko.luckperms.common.sender.Sender;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class WaterdogCommandExecutor extends Command {
    /** The main command name */
    private static final String NAME = "luckpermswaterdog";

    /** The command aliases */
    private static final String[] ALIASES = {"lpw"};

    /** The main command name + aliases, prefixed with '/' */
    private static final String[] SLASH_ALIASES = Stream.concat(
            Stream.of(NAME),
            Arrays.stream(ALIASES)
    ).map(s -> '/' + s).toArray(String[]::new);

    /**
     * The aliases to register, {@link #ALIASES} + {@link #SLASH_ALIASES}.
     *
     * <p>SLASH_ALIASES are registered too so the console can run '/lpw'
     * in the same way as 'lpw'.</p>
     */
    private static final String[] ALIASES_TO_REGISTER = Stream.concat(
            Arrays.stream(ALIASES),
            Arrays.stream(SLASH_ALIASES)
    ).toArray(String[]::new);

    private final LPWaterdogPlugin plugin;
    private final CommandManager manager;

    public WaterdogCommandExecutor(LPWaterdogPlugin plugin, CommandManager manager) {
        super(NAME, CommandSettings.builder().setAliases(ALIASES_TO_REGISTER).build());
        this.plugin = plugin;
        this.manager = manager;
    }

    public void register() {
        ProxyServer proxy = this.plugin.getBootstrap().getProxy();
        proxy.getCommandMap().registerCommand(this);
    }

    @Override
    public boolean onExecute(CommandSender sender, String alias, String[] args) {
        Sender wrapped = this.plugin.getSenderFactory().wrap(sender);
        List<String> arguments = ArgumentTokenizer.EXECUTE.tokenizeInput(args);
        this.manager.executeCommand(wrapped, "lpw", arguments);

        return true;
    }
}
