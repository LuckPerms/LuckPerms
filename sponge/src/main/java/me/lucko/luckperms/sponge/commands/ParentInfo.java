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
import me.lucko.luckperms.common.command.abstraction.ChildCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.sponge.service.model.LPSubjectData;
import me.lucko.luckperms.sponge.service.model.LPSubjectReference;
import net.luckperms.api.context.ImmutableContextSet;

import java.util.List;
import java.util.Map;

public class ParentInfo extends ChildCommand<LPSubjectData> {
    public ParentInfo() {
        super(CommandSpec.SPONGE_PARENT_INFO, "info", CommandPermission.SPONGE_PARENT_INFO, Predicates.alwaysFalse());
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, LPSubjectData subjectData, ArgumentList args, String label) {
        ImmutableContextSet contextSet = args.getContextOrEmpty(0);
        if (contextSet.isEmpty()) {
            SpongeCommandUtils.sendPrefixed(sender, "&aShowing parents matching contexts &bANY&a.");
            Map<ImmutableContextSet, ImmutableList<LPSubjectReference>> parents = subjectData.getAllParents();
            if (parents.isEmpty()) {
                SpongeCommandUtils.sendPrefixed(sender, "That subject does not have any parents defined.");
                return;
            }

            for (Map.Entry<ImmutableContextSet, ImmutableList<LPSubjectReference>> e : parents.entrySet()) {
                SpongeCommandUtils.sendPrefixed(sender, "&3>> &bContext: " + SpongeCommandUtils.contextToString(e.getKey()) + "\n" + SpongeCommandUtils.parentsToString(e.getValue()));
            }

        } else {
            List<LPSubjectReference> parents = subjectData.getParents(contextSet);
            if (parents.isEmpty()) {
                SpongeCommandUtils.sendPrefixed(sender, "That subject does not have any parents defined in those contexts.");
                return;
            }

            SpongeCommandUtils.sendPrefixed(sender, "&aShowing parents matching contexts &b" +
                        SpongeCommandUtils.contextToString(contextSet) + "&a.\n" + SpongeCommandUtils.parentsToString(parents));
        }
    }
}
