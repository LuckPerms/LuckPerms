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
import java.util.Map;

public class PermissionInfo extends SubCommand<LPSubjectData> {
    public PermissionInfo() {
        super("info", "Shows info about the subject's permissions", Permission.SPONGE_PERMISSION_INFO, Predicates.alwaysFalse(),
                Arg.list(Arg.create("contexts...", false, "the contexts to filter by"))
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, LPSubjectData subjectData, List<String> args, String label) throws CommandException {
        ImmutableContextSet contextSet = ArgumentUtils.handleContexts(0, args);
        if (contextSet.isEmpty()) {
            Util.sendPluginMessage(sender, "&aShowing permissions matching contexts &bANY&a.");
            Map<ImmutableContextSet, ImmutableMap<String, Boolean>> permissions = subjectData.getAllPermissions();
            if (permissions.isEmpty()) {
                Util.sendPluginMessage(sender, "That subject does not have any permissions defined.");
                return CommandResult.SUCCESS;
            }

            for (Map.Entry<ImmutableContextSet, ImmutableMap<String, Boolean>> e : permissions.entrySet()) {
                Util.sendPluginMessage(sender, "&3>> &bContext: " + SpongeUtils.contextToString(e.getKey()) + "\n" + SpongeUtils.nodesToString(e.getValue()));
            }

        } else {
            Map<String, Boolean> permissions = subjectData.getPermissions(contextSet);
            if (permissions.isEmpty()) {
                Util.sendPluginMessage(sender, "That subject does not have any permissions defined in those contexts.");
                return CommandResult.SUCCESS;
            }

            Util.sendPluginMessage(sender, "&aShowing permissions matching contexts &b" +
                    SpongeUtils.contextToString(contextSet) + "&a.\n" + SpongeUtils.nodesToString(permissions));

        }
        return CommandResult.SUCCESS;
    }
}
