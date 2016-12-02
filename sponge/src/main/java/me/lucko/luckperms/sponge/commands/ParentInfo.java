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

package me.lucko.luckperms.sponge.commands;

import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.utils.Predicates;

import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ParentInfo extends SubCommand<SubjectData> {
    public ParentInfo() {
        super("info", "Shows info about the subject's parents", Permission.SPONGE_PARENT_INFO, Predicates.alwaysFalse(),
                Arg.list(Arg.create("contexts...", false, "the contexts to filter by"))
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, SubjectData subjectData, List<String> args, String label) throws CommandException {
        ContextSet contextSet = ArgumentUtils.handleContexts(0, args);
        if (contextSet.isEmpty()) {
            Util.sendPluginMessage(sender, "&aShowing parents matching contexts &bANY&a.");
            Map<Set<Context>, List<Subject>> parents = subjectData.getAllParents();
            if (parents.isEmpty()) {
                Util.sendPluginMessage(sender, "That subject does not have any parents defined.");
                return CommandResult.SUCCESS;
            }

            for (Map.Entry<Set<Context>, List<Subject>> e : parents.entrySet()) {
                ContextSet set = SpongeUtils.convertContexts(e.getKey());
                Util.sendPluginMessage(sender, "&3>> &bContext: " + SpongeUtils.contextToString(set) + "\n" + SpongeUtils.parentsToString(e.getValue()));
            }

        } else {
            List<Subject> parents = subjectData.getParents(SpongeUtils.convertContexts(contextSet));
            if (parents.isEmpty()) {
                Util.sendPluginMessage(sender, "That subject does not have any parents defined in those contexts.");
                return CommandResult.SUCCESS;
            }

            Util.sendPluginMessage(sender, "&aShowing parents matching contexts &b" +
                    SpongeUtils.contextToString(contextSet) + "&a.\n" + SpongeUtils.parentsToString(parents));

        }
        return CommandResult.SUCCESS;
    }
}
