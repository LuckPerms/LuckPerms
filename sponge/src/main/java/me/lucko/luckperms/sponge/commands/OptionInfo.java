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

import com.google.common.collect.ImmutableMap;

import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandPermission;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.commands.utils.CommandUtils;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.sponge.service.model.LPSubjectData;

import java.util.List;
import java.util.Map;

public class OptionInfo extends SubCommand<LPSubjectData> {
    public OptionInfo(LocaleManager locale) {
        super(CommandSpec.SPONGE_OPTION_INFO.spec(locale), "info", CommandPermission.SPONGE_OPTION_INFO, Predicates.alwaysFalse());
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, LPSubjectData subjectData, List<String> args, String label) throws CommandException {
        ImmutableContextSet contextSet = ArgumentUtils.handleContextSponge(0, args);
        if (contextSet.isEmpty()) {
            CommandUtils.sendPluginMessage(sender, "&aShowing options matching contexts &bANY&a.");
            Map<ImmutableContextSet, ImmutableMap<String, String>> options = subjectData.getAllOptions();
            if (options.isEmpty()) {
                CommandUtils.sendPluginMessage(sender, "That subject does not have any options defined.");
                return CommandResult.SUCCESS;
            }

            for (Map.Entry<ImmutableContextSet, ImmutableMap<String, String>> e : options.entrySet()) {
                CommandUtils.sendPluginMessage(sender, "&3>> &bContext: " + SpongeCommandUtils.contextToString(e.getKey()) + "\n" + SpongeCommandUtils.optionsToString(e.getValue()));
            }

        } else {
            Map<String, String> options = subjectData.getOptions(contextSet);
            if (options.isEmpty()) {
                CommandUtils.sendPluginMessage(sender, "That subject does not have any options defined in those contexts.");
                return CommandResult.SUCCESS;
            }

            CommandUtils.sendPluginMessage(sender, "&aShowing options matching contexts &b" +
                    SpongeCommandUtils.contextToString(contextSet) + "&a.\n" + SpongeCommandUtils.optionsToString(options));

        }
        return CommandResult.SUCCESS;
    }
}
