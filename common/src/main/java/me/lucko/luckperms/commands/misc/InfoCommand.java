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

package me.lucko.luckperms.commands.misc;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.CommandResult;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SingleMainCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.core.LPConfiguration;

import java.util.List;

import static me.lucko.luckperms.commands.Util.formatBoolean;

public class InfoCommand extends SingleMainCommand {
    public InfoCommand() {
        super("Info", "/%s info", 0, Permission.INFO);
    }

    @Override
    protected CommandResult execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        final LPConfiguration c = plugin.getConfiguration();
        Message.INFO.send(sender,
                plugin.getVersion(),
                plugin.getType().getFriendlyName(),
                plugin.getDatastore().getName(),
                c.getServer(),
                c.getSyncTime(),
                formatBoolean(c.isIncludingGlobalPerms()),
                formatBoolean(c.isIncludingGlobalWorldPerms()),
                formatBoolean(c.isApplyingGlobalGroups()),
                formatBoolean(c.isApplyingGlobalWorldGroups()),
                formatBoolean(c.isOnlineMode()),
                formatBoolean(c.isApplyingWildcards()),
                formatBoolean(c.isApplyingRegex()),
                formatBoolean(c.isApplyingShorthand())
        );

        return CommandResult.SUCCESS;
    }
}
