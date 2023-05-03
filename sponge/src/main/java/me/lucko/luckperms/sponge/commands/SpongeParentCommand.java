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
import com.google.common.collect.ImmutableMap;
import me.lucko.luckperms.common.command.abstraction.ChildCommand;
import me.lucko.luckperms.common.command.abstraction.Command;
import me.lucko.luckperms.common.command.abstraction.CommandException;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.sponge.LPSpongePlugin;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectCollection;
import me.lucko.luckperms.sponge.service.model.LPSubjectData;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class SpongeParentCommand extends Command<Void> {
    private final LPSpongePlugin plugin;

    private final Map<String, List<ChildCommand<LPSubjectData>>> children;

    public SpongeParentCommand(LPSpongePlugin plugin) {
        super(CommandSpec.SPONGE, "Sponge", null, Predicates.alwaysFalse());
        this.children = ImmutableMap.<String, List<ChildCommand<LPSubjectData>>>builder()
                .put("permission", ImmutableList.<ChildCommand<LPSubjectData>>builder()
                        .add(new PermissionInfo())
                        .add(new PermissionSet())
                        .add(new PermissionClear())
                        .build()
                )
                .put("parent", ImmutableList.<ChildCommand<LPSubjectData>>builder()
                        .add(new ParentInfo())
                        .add(new ParentAdd())
                        .add(new ParentRemove())
                        .add(new ParentClear())
                        .build()
                )
                .put("option", ImmutableList.<ChildCommand<LPSubjectData>>builder()
                        .add(new OptionInfo())
                        .add(new OptionSet())
                        .add(new OptionUnset())
                        .add(new OptionClear())
                        .build()
                )
                .build();

        this.plugin = plugin;
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, Void v, ArgumentList args, String label) {
        LuckPermsService service = this.plugin.getService();

        if (args.size() < 1) {
            SpongeCommandUtils.sendPrefixed(sender, "&aCurrent Subject Collections:\n" +
                    SpongeCommandUtils.toCommaSep(service.getLoadedCollections().keySet().stream()
                                .filter(s -> !s.equalsIgnoreCase("user") && !s.equalsIgnoreCase("group"))
                                .sorted()
                                .collect(Collectors.toList())
                        ));
            return;
        }

        String subjectCollection = args.get(0);

        if (subjectCollection.equalsIgnoreCase("user") || subjectCollection.equalsIgnoreCase("group")) {
            SpongeCommandUtils.sendPrefixed(sender, "Please use the main LuckPerms commands to edit users and groups.");
            return;
        }

        if (service.getLoadedCollections().keySet().stream().map(String::toLowerCase).noneMatch(s -> s.equalsIgnoreCase(subjectCollection))) {
            SpongeCommandUtils.sendPrefixed(sender, "Warning: SubjectCollection '&4" + subjectCollection + "&c' doesn't already exist. Creating it now.");
        }

        LPSubjectCollection collection = service.getCollection(subjectCollection);

        if (args.size() < 2) {
            List<String> subjects = collection.getLoadedSubjects().stream()
                    .map(lpSubject -> lpSubject.getIdentifier().getName())
                    .collect(Collectors.toList());

            if (subjects.size() > 50) {
                List<String> extra = subjects.subList(50, subjects.size());
                int overflow = extra.size();
                extra.clear();
                SpongeCommandUtils.sendPrefixed(sender, "&aCurrent Subjects:\n" + SpongeCommandUtils.toCommaSep(subjects) + "&b ... and &a" + overflow + " &bmore.");
            } else {
                SpongeCommandUtils.sendPrefixed(sender, "&aCurrent Subjects:\n" + SpongeCommandUtils.toCommaSep(subjects));
            }

            return;
        }

        if (args.size() < 4) {
            sendDetailedUsage(sender, label);
            return;
        }

        boolean persistent = true;
        if (args.get(2).toLowerCase(Locale.ROOT).startsWith("-t")) {
            persistent = false;
            args.remove(2);
        }

        String type = args.get(2).toLowerCase(Locale.ROOT);
        if (!type.equals("permission") && !type.equals("parent") && !type.equals("option")) {
            sendDetailedUsage(sender, label);
            return;
        }

        String cmd = args.get(3);
        ChildCommand<LPSubjectData> sub = this.children.get(type).stream()
                .filter(s -> s.getName().equalsIgnoreCase(cmd))
                .findAny()
                .orElse(null);

        if (sub == null) {
            sendDetailedUsage(sender, label);
            return;
        }

        if (!sub.isAuthorized(sender)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        if (sub.getArgumentCheck().test(args.size() - 4)) {
            sub.sendDetailedUsage(sender, label);
            return;
        }

        String subjectId = args.get(1);
        if (!collection.hasRegistered(subjectId).join()) {
            SpongeCommandUtils.sendPrefixed(sender, "Warning: Subject '&4" + subjectId + "&c' doesn't already exist. Creating it now.");
        }

        LPSubject subject = collection.loadSubject(subjectId).join();
        LPSubjectData subjectData = persistent ? subject.getSubjectData() : subject.getTransientSubjectData();

        try {
            sub.execute(plugin, sender, subjectData, args.subList(4, args.size()), label);
        } catch (CommandException e) {
            e.handle(sender, label, sub);
        }
    }

    @Override
    public void sendUsage(Sender sender, String label) {
        SpongeCommandUtils.sendPrefixed(sender, "&3> &a" + String.format(getUsage(), label));
    }

    @Override
    public void sendDetailedUsage(Sender sender, String label) {
        SpongeCommandUtils.sendPrefixed(sender, "&b" + getName() + " Sub Commands: &7(" + String.format("/%s sponge <collection> <subject> [-transient]", label) + " ...)");
        for (String s : Arrays.asList("Permission", "Parent", "Option")) {
            List<Command<?>> subs = this.children.get(s.toLowerCase(Locale.ROOT)).stream()
                    .filter(sub -> sub.isAuthorized(sender))
                    .collect(Collectors.toList());

            if (!subs.isEmpty()) {
                SpongeCommandUtils.sendPrefixed(sender, "&3>>  &b" + s);
                for (Command<?> sub : subs) {
                    sub.sendUsage(sender, label);
                }
            }
        }
    }

    @Override
    public boolean isAuthorized(Sender sender) {
        return this.children.values().stream().flatMap(List::stream).anyMatch(sc -> sc.isAuthorized(sender));
    }

}
