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

import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.Command;
import me.lucko.luckperms.common.command.abstraction.CommandException;
import me.lucko.luckperms.common.command.utils.MessageUtils;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.command.CommandSpec;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.ImmutableCollectors;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.sponge.LPSpongePlugin;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectCollection;
import me.lucko.luckperms.sponge.service.model.LPSubjectData;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SpongeMainCommand extends Command<Void, LPSubjectData> {
    private final LPSpongePlugin plugin;

    private final Map<String, List<Command<LPSubjectData, ?>>> subCommands;

    public SpongeMainCommand(LPSpongePlugin plugin) {
        super(CommandSpec.SPONGE.localize(plugin.getLocaleManager()), "Sponge", null, Predicates.alwaysFalse());

        LocaleManager locale = plugin.getLocaleManager();

        this.subCommands = ImmutableMap.<String, List<Command<LPSubjectData, ?>>>builder()
                .put("permission", ImmutableList.<Command<LPSubjectData, ?>>builder()
                        .add(new PermissionInfo(locale))
                        .add(new PermissionSet(locale))
                        .add(new PermissionClear(locale))
                        .build()
                )
                .put("parent", ImmutableList.<Command<LPSubjectData, ?>>builder()
                        .add(new ParentInfo(locale))
                        .add(new ParentAdd(locale))
                        .add(new ParentRemove(locale))
                        .add(new ParentClear(locale))
                        .build()
                )
                .put("option", ImmutableList.<Command<LPSubjectData, ?>>builder()
                        .add(new OptionInfo(locale))
                        .add(new OptionSet(locale))
                        .add(new OptionUnset(locale))
                        .add(new OptionClear(locale))
                        .build()
                )
                .build();

        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Void v, List<String> args, String label) {
        LuckPermsService service = this.plugin.getService();

        if (args.size() < 1) {
            Message.BLANK.send(sender, "&aCurrent Subject Collections:\n" +
                        MessageUtils.toCommaSep(service.getLoadedCollections().keySet().stream()
                                .filter(s -> !s.equalsIgnoreCase("user") && !s.equalsIgnoreCase("group"))
                                .sorted()
                                .collect(Collectors.toList())
                        ));
            return CommandResult.SUCCESS;
        }

        String subjectCollection = args.get(0);

        if (subjectCollection.equalsIgnoreCase("user") || subjectCollection.equalsIgnoreCase("group")) {
            Message.BLANK.send(sender, "Please use the main LuckPerms commands to edit users and groups.");
            return CommandResult.STATE_ERROR;
        }

        if (service.getLoadedCollections().keySet().stream().map(String::toLowerCase).noneMatch(s -> s.equalsIgnoreCase(subjectCollection))) {
            Message.BLANK.send(sender, "Warning: SubjectCollection '&4" + subjectCollection + "&c' doesn't already exist. Creating it now.");
        }

        LPSubjectCollection collection = service.getCollection(subjectCollection);

        if (args.size() < 2) {
            List<String> subjects = collection.getLoadedSubjects().stream()
                    .map(LPSubject::getIdentifier)
                    .collect(Collectors.toList());

            if (subjects.size() > 50) {
                List<String> extra = subjects.subList(50, subjects.size());
                int overflow = extra.size();
                extra.clear();
                Message.BLANK.send(sender, "&aCurrent Subjects:\n" + MessageUtils.toCommaSep(subjects) + "&b ... and &a" + overflow + " &bmore.");
            } else {
                Message.BLANK.send(sender, "&aCurrent Subjects:\n" + MessageUtils.toCommaSep(subjects));
            }

            return CommandResult.SUCCESS;
        }

        if (args.size() < 4) {
            sendDetailedUsage(sender, label);
            return CommandResult.SUCCESS;
        }

        boolean persistent = true;
        if (args.get(2).toLowerCase().startsWith("-t")) {
            persistent = false;
            args.remove(2);
        }

        String type = args.get(2).toLowerCase();
        if (!type.equals("permission") && !type.equals("parent") && !type.equals("option")) {
            sendDetailedUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        String cmd = args.get(3);
        Optional<Command<LPSubjectData, ?>> o = this.subCommands.get(type).stream()
                .filter(s -> s.getName().equalsIgnoreCase(cmd))
                .findAny();

        if (!o.isPresent()) {
            sendDetailedUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        final Command<LPSubjectData, ?> sub = o.get();
        if (!sub.isAuthorized(sender)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        List<String> strippedArgs = new ArrayList<>();
        if (args.size() > 4) {
            strippedArgs.addAll(args.subList(4, args.size()));
        }

        if (sub.getArgumentCheck().test(strippedArgs.size())) {
            sub.sendDetailedUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        String subjectId = args.get(1);
        if (!collection.hasRegistered(subjectId).join()) {
            Message.BLANK.send(sender, "Warning: Subject '&4" + subjectId + "&c' doesn't already exist. Creating it now.");
        }

        LPSubject subject = collection.loadSubject(subjectId).join();
        LPSubjectData subjectData = persistent ? subject.getSubjectData() : subject.getTransientSubjectData();

        CommandResult result;
        try {
            result = sub.execute(plugin, sender, subjectData, strippedArgs, label);
        } catch (CommandException e) {
            result = CommandManager.handleException(e, sender, label, sub);
        }
        return result;
    }

    @Override
    public void sendUsage(Sender sender, String label) {
        Message.BLANK.send(sender, "&3> &a" + String.format(getUsage(), label));
    }

    @Override
    public void sendDetailedUsage(Sender sender, String label) {
        Message.BLANK.send(sender, "&b" + getName() + " Sub Commands: &7(" + String.format("/%s sponge <collection> <subject> [-transient]", label) + " ...)");
        for (String s : Arrays.asList("Permission", "Parent", "Option")) {
            List<Command> subs = this.subCommands.get(s.toLowerCase()).stream()
                    .filter(sub -> sub.isAuthorized(sender))
                    .collect(Collectors.toList());

            if (!subs.isEmpty()) {
                Message.BLANK.send(sender, "&3>>  &b" + s);
                for (Command sub : subs) {
                    sub.sendUsage(sender, label);
                }
            }
        }
    }

    @Override
    public boolean isAuthorized(Sender sender) {
        return getSubCommands().stream().anyMatch(sc -> sc.isAuthorized(sender));
    }

    public List<Command<LPSubjectData, ?>> getSubCommands() {
        return this.subCommands.values().stream().flatMap(List::stream).collect(ImmutableCollectors.toList());
    }

    @Override
    public @NonNull Optional<List<Command<LPSubjectData, ?>>> getChildren() {
        return Optional.of(getSubCommands());
    }
}
