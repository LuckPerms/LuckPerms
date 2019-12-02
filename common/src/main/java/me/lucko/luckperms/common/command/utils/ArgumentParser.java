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

package me.lucko.luckperms.common.command.utils;

import me.lucko.luckperms.common.command.abstraction.CommandException;
import me.lucko.luckperms.common.commands.user.UserMainCommand;
import me.lucko.luckperms.common.context.contextset.ImmutableContextSetImpl;
import me.lucko.luckperms.common.context.contextset.MutableContextSetImpl;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.storage.misc.DataConstraints;
import me.lucko.luckperms.common.util.DateParser;

import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.context.MutableContextSet;
import net.luckperms.api.model.data.TemporaryNodeMergeStrategy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Utility class to help process arguments, and throw checked exceptions if the arguments are invalid.
 */
public class ArgumentParser {

    public static String parseString(int index, List<String> args) {
        return args.get(index).replace("{SPACE}", " ");
    }

    public static String parseStringOrElse(int index, List<String> args, String other) {
        if (index < 0 || index >= args.size()) {
            return other;
        }

        return args.get(index).replace("{SPACE}", " ");
    }

    public static int parseIntOrElse(int index, List<String> args, int other) {
        if (index < 0 || index >= args.size()) {
            return other;
        }

        try {
            return Integer.parseInt(args.get(index));
        } catch (NumberFormatException e) {
            return other;
        }
    }

    public static String parseName(int index, List<String> args) throws ArgumentException {
        String groupName = args.get(index).toLowerCase();
        if (!DataConstraints.GROUP_NAME_TEST.test(groupName)) {
            throw new DetailedUsageException();
        }
        return groupName;
    }

    public static String parseNameWithSpace(int index, List<String> args) throws ArgumentException {
        String groupName = args.get(index).toLowerCase();
        if (!DataConstraints.GROUP_NAME_TEST_ALLOW_SPACE.test(groupName)) {
            throw new DetailedUsageException();
        }
        return groupName;
    }

    public static boolean parseBoolean(int index, List<String> args) {
        if (index < args.size()) {
            String bool = args.get(index);
            if (bool.equalsIgnoreCase("true") || bool.equalsIgnoreCase("false")) {
                return Boolean.parseBoolean(bool);
            }
        }

        args.add(index, "true");
        return true;
    }

    public static long parseDuration(int index, List<String> args) throws ArgumentException {
        long duration;
        try {
            duration = Long.parseLong(args.get(index));
        } catch (NumberFormatException e) {
            try {
                duration = DateParser.parseDate(args.get(index), true);
            } catch (IllegalArgumentException e1) {
                throw new InvalidDateException(args.get(index));
            }
        }

        if (shouldExpire(duration)) {
            throw new PastDateException();
        }

        return duration;
    }

    private static boolean shouldExpire(long unixTime) {
        return unixTime < (System.currentTimeMillis() / 1000L);
    }

    public static TemporaryNodeMergeStrategy parseTemporaryModifier(String s) {
        switch (s.toLowerCase()) {
            case "accumulate":
                return TemporaryNodeMergeStrategy.ADD_NEW_DURATION_TO_EXISTING;
            case "replace":
                return TemporaryNodeMergeStrategy.REPLACE_EXISTING_IF_DURATION_LONGER;
            case "deny":
            case "none":
                return TemporaryNodeMergeStrategy.NONE;
            default:
                throw new IllegalArgumentException("Unknown value: " + s);
        }
    }

    public static Optional<TemporaryNodeMergeStrategy> parseTemporaryModifier(int index, List<String> args) {
        if (index < 0 || index >= args.size()) {
            return Optional.empty();
        }

        String s = args.get(index);
        try {
            Optional<TemporaryNodeMergeStrategy> ret = Optional.of(parseTemporaryModifier(s));
            args.remove(index);
            return ret;
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static MutableContextSet parseContext(int fromIndex, List<String> args, LuckPermsPlugin plugin) throws CommandException {
        if (args.size() <= fromIndex) {
            return plugin.getConfiguration().getContextsFile().getDefaultContexts().mutableCopy();
        }
        return parseContext(fromIndex, args);
    }

    public static ImmutableContextSet parseContextSponge(int fromIndex, List<String> args) {
        if (args.size() <= fromIndex) {
            return ImmutableContextSetImpl.EMPTY;
        }
        return parseContext(fromIndex, args).immutableCopy();
    }

    private static MutableContextSet parseContext(int fromIndex, List<String> args) {
        MutableContextSet contextSet = new MutableContextSetImpl();
        List<String> pairs = args.subList(fromIndex, args.size());
        for (int i = 0; i < pairs.size(); i++) {
            String pair = pairs.get(i);
            int index = pair.indexOf('=');

            String key;
            String value;

            if (index != -1) {
                key = pair.substring(0, index);
                value = pair.substring(index + 1);
            } else {
                key = i == 1 ? DefaultContextKeys.WORLD_KEY : DefaultContextKeys.SERVER_KEY;
                value = pair;
            }

            if (key.equals("") || key.trim().isEmpty()) {
                continue;
            }
            if (value.equals("") || value.trim().isEmpty()) {
                continue;
            }

            contextSet.add(key, value);
        }

        return contextSet;
    }

    public static int parsePriority(int index, List<String> args) throws ArgumentException {
        try {
            return Integer.parseInt(args.get(index));
        } catch (NumberFormatException e) {
            throw new InvalidPriorityException(args.get(index));
        }
    }

    public static UUID parseUserTarget(int index, List<String> args, LuckPermsPlugin plugin, Sender sender) {
        final String target = args.get(index);
        return UserMainCommand.parseTargetUniqueId(target, plugin, sender);
    }

    public abstract static class ArgumentException extends CommandException {}
    public static class DetailedUsageException extends ArgumentException {}
    public static class InvalidServerWorldException extends ArgumentException {}
    public static class PastDateException extends ArgumentException {}

    public static class InvalidDateException extends ArgumentException {
        private final String invalidDate;

        public InvalidDateException(String invalidDate) {
            this.invalidDate = invalidDate;
        }

        public String getInvalidDate() {
            return this.invalidDate;
        }
    }

    public static class InvalidPriorityException extends ArgumentException {
        private final String invalidPriority;

        public InvalidPriorityException(String invalidPriority) {
            this.invalidPriority = invalidPriority;
        }

        public String getInvalidPriority() {
            return this.invalidPriority;
        }
    }

}
