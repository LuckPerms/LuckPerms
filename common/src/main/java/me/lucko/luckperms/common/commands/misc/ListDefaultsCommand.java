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

package me.lucko.luckperms.common.commands.misc;

import me.lucko.luckperms.common.command.abstraction.CommandException;
import me.lucko.luckperms.common.command.abstraction.SingleCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.tabcomplete.CompletionSupplier;
import me.lucko.luckperms.common.command.tabcomplete.TabCompleter;
import me.lucko.luckperms.common.command.utils.ArgumentException;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.plugin.util.PlatformPermissionDefault;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Iterators;
import me.lucko.luckperms.common.util.Predicates;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ListDefaultsCommand extends SingleCommand {
    public ListDefaultsCommand() {
        super(CommandSpec.LIST_DEFAULTS, "ListDefaults", CommandPermission.LIST_DEFAULTS, Predicates.notInRange(0, 2));
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, ArgumentList args, String label) throws CommandException {
        Optional<Map<String, PlatformPermissionDefault>> defaults = plugin.getPermissionDefaults();
        if (!defaults.isPresent()) {
            Message.LIST_DEFAULTS_NOT_SUPPORTED.send(sender);
            return;
        }

        PlatformPermissionDefault filter = null;
        int page = 1;

        if (!args.isEmpty()) {
            filter = PlatformPermissionDefault.parse(args.get(0));
            if (filter != null) {
                page = args.getIntOrDefault(1, 1);
            } else if (args.size() == 1 && isInteger(args.get(0))) {
                // only a page number was given
                page = Integer.parseInt(args.get(0));
            } else {
                throw new ArgumentException.DetailedUsage();
            }
        }

        PlatformPermissionDefault value = filter;
        List<Map.Entry<String, PlatformPermissionDefault>> entries = defaults.get().entrySet().stream()
                .filter(e -> value == null || e.getValue() == value)
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        if (entries.isEmpty()) {
            Message.LIST_DEFAULTS_EMPTY.send(sender);
            return;
        }

        int pageIndex = page - 1;
        List<List<Map.Entry<String, PlatformPermissionDefault>>> pages = Iterators.divideIterable(entries, 15);

        if (pageIndex < 0 || pageIndex >= pages.size()) {
            page = 1;
            pageIndex = 0;
        }

        Message.LIST_DEFAULTS_HEADER.send(sender, filter, page, pages.size(), entries.size());
        for (Map.Entry<String, PlatformPermissionDefault> entry : pages.get(pageIndex)) {
            Message.LIST_DEFAULTS_ENTRY.send(sender, entry.getKey(), entry.getValue());
        }
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(0, CompletionSupplier.startsWith(Arrays.stream(PlatformPermissionDefault.values())
                        .map(PlatformPermissionDefault::getName)
                        .collect(Collectors.toList())))
                .complete(args);
    }

    private static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
