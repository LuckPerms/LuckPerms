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

package me.lucko.luckperms.sponge.commands;

import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.sponge.service.model.LPSubjectData;

import java.util.List;

public class OptionUnset extends SubCommand<LPSubjectData> {
    public OptionUnset() {
        super("unset", "Unsets an option for the Subject", Permission.SPONGE_OPTION_UNSET, Predicates.is(0),
                Arg.list(
                        Arg.create("key", true, "the key to unset"),
                        Arg.create("contexts...", false, "the contexts to unset the key in")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, LPSubjectData subjectData, List<String> args, String label) throws CommandException {
        String key = args.get(0);
        ImmutableContextSet contextSet = ArgumentUtils.handleContextSponge(1, args);

        if (subjectData.unsetOption(contextSet, key).join()) {
            Util.sendPluginMessage(sender, "&aUnset &f\"" + key + "&f\"&a in context " + SpongeUtils.contextToString(contextSet));
        } else {
            Util.sendPluginMessage(sender, "Unable to unset option. Are you sure the Subject has it set?");
        }
        return CommandResult.SUCCESS;
    }
}
