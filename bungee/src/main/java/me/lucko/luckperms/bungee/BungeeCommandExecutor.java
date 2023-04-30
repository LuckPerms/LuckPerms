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

package me.lucko.luckperms.bungee;

import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.command.utils.ArgumentTokenizer;
import me.lucko.luckperms.common.sender.Sender;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class BungeeCommandExecutor extends Command implements TabExecutor {
    /** The main command name */
    private static final String NAME = "luckpermsbungee";

    /** The command aliases */
    private static final String[] ALIASES = {"lpb"};

    /** The main command name + aliases, prefixed with '/' */
    private static final String[] SLASH_ALIASES = Stream.concat(
            Stream.of(NAME),
            Arrays.stream(ALIASES)
    ).map(s -> '/' + s).toArray(String[]::new);

    /**
     * The aliases to register, {@link #ALIASES} + {@link #SLASH_ALIASES}.
     *
     * <p>SLASH_ALIASES are registered too so the console can run '/lpb'
     * in the same way as 'lpb'.</p>
     */
    private static final String[] ALIASES_TO_REGISTER = Stream.concat(
            Arrays.stream(ALIASES),
            Arrays.stream(SLASH_ALIASES)
    ).toArray(String[]::new);

    private final LPBungeePlugin plugin;
    private final CommandManager manager;

    public BungeeCommandExecutor(LPBungeePlugin plugin, CommandManager manager) {
        super(NAME, null, ALIASES_TO_REGISTER);
        this.plugin = plugin;
        this.manager = manager;
    }

    public void register() {
        ProxyServer proxy = this.plugin.getBootstrap().getProxy();
        proxy.getPluginManager().registerCommand(this.plugin.getLoader(), this);

        // don't allow players to execute the slash aliases - these are just for the console.
        proxy.getDisabledCommands().addAll(Arrays.asList(SLASH_ALIASES));
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Sender wrapped = this.plugin.getSenderFactory().wrap(sender);
        List<String> arguments = ArgumentTokenizer.EXECUTE.tokenizeInput(args);
        this.manager.executeCommand(wrapped, "lpb", arguments);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        Sender wrapped = this.plugin.getSenderFactory().wrap(sender);
        List<String> arguments = ArgumentTokenizer.TAB_COMPLETE.tokenizeInput(args);
        return this.manager.tabCompleteCommand(wrapped, arguments);
    }
}
