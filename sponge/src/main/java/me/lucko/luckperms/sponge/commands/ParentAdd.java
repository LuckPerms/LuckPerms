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
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;

import java.util.List;

public class ParentAdd extends SubCommand<SubjectData> {
    public ParentAdd() {
        super("add", "Adds a parent to the Subject", Permission.SPONGE_PARENT_ADD, Predicates.inRange(0, 1),
                Arg.list(
                        Arg.create("collection", true, "the subject collection where the parent Subject is"),
                        Arg.create("subject", true, "the name of the parent Subject"),
                        Arg.create("contexts...", false, "the contexts to add the parent in")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, SubjectData subjectData, List<String> args, String label) throws CommandException {
        String collection = args.get(0);
        String name = args.get(1);
        ContextSet contextSet = ArgumentUtils.handleContexts(2, args);

        PermissionService service = Sponge.getServiceManager().provideUnchecked(PermissionService.class);
        if (service.getKnownSubjects().keySet().stream().map(String::toLowerCase).noneMatch(s -> s.equalsIgnoreCase(collection))) {
            Util.sendPluginMessage(sender, "Warning: SubjectCollection '&4" + collection + "&c' doesn't already exist.");
        }

        SubjectCollection c = service.getSubjects(collection);
        if (!c.hasRegistered(name)) {
            Util.sendPluginMessage(sender, "Warning: Subject '&4" + name + "&c' doesn't already exist.");
        }

        Subject subject = c.get(name);

        if (subjectData.addParent(SpongeUtils.convertContexts(contextSet), subject)) {
            Util.sendPluginMessage(sender, "&aAdded parent &b" + subject.getContainingCollection().getIdentifier() +
                    "&a/&b" + subject.getIdentifier() + "&a in context " + SpongeUtils.contextToString(contextSet));
        } else {
            Util.sendPluginMessage(sender, "Unable to add parent. Does the Subject already have it added?");
        }
        return CommandResult.SUCCESS;
    }
}
