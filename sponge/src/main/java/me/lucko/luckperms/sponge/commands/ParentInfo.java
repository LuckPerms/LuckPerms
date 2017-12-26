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

import com.google.common.collect.ImmutableList;

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
import me.lucko.luckperms.sponge.service.model.SubjectReference;

import java.util.List;
import java.util.Map;

public class ParentInfo extends SubCommand<LPSubjectData> {
    public ParentInfo(LocaleManager locale) {
        super(CommandSpec.SPONGE_PARENT_INFO.spec(locale), "info", CommandPermission.SPONGE_PARENT_INFO, Predicates.alwaysFalse());
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, LPSubjectData subjectData, List<String> args, String label) throws CommandException {
        ImmutableContextSet contextSet = ArgumentUtils.handleContextSponge(0, args);
        if (contextSet.isEmpty()) {
            CommandUtils.sendPluginMessage(sender, "&aShowing parents matching contexts &bANY&a.");
            Map<ImmutableContextSet, ImmutableList<SubjectReference>> parents = subjectData.getAllParents();
            if (parents.isEmpty()) {
                CommandUtils.sendPluginMessage(sender, "That subject does not have any parents defined.");
                return CommandResult.SUCCESS;
            }

            for (Map.Entry<ImmutableContextSet, ImmutableList<SubjectReference>> e : parents.entrySet()) {
                CommandUtils.sendPluginMessage(sender, "&3>> &bContext: " + SpongeCommandUtils.contextToString(e.getKey()) + "\n" + SpongeCommandUtils.parentsToString(e.getValue()));
            }

        } else {
            List<SubjectReference> parents = subjectData.getParents(contextSet);
            if (parents.isEmpty()) {
                CommandUtils.sendPluginMessage(sender, "That subject does not have any parents defined in those contexts.");
                return CommandResult.SUCCESS;
            }

            CommandUtils.sendPluginMessage(sender, "&aShowing parents matching contexts &b" +
                    SpongeCommandUtils.contextToString(contextSet) + "&a.\n" + SpongeCommandUtils.parentsToString(parents));

        }
        return CommandResult.SUCCESS;
    }
}
