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

package me.lucko.luckperms.common.commands.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.DataConstraints;
import me.lucko.luckperms.common.utils.DateUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to help process arguments, and throw checked exceptions if the arguments are invalid.
 */
public class ArgumentUtils {

    public static String handleString(int index, List<String> args) {
        return args.get(index).replace("{SPACE}", " ");
    }

    public static String handleStringOrElse(int index, List<String> args, String other) {
        if (index < 0 || index >= args.size()) {
            return other;
        }

        return args.get(index).replace("{SPACE}", " ");
    }

    public static int handleIntOrElse(int index, List<String> args, int other) {
        if (index < 0 || index >= args.size()) {
            return other;
        }

        try {
            return Integer.parseInt(args.get(index));
        } catch (NumberFormatException e) {
            return other;
        }
    }

    public static String handleName(int index, List<String> args) throws ArgumentException {
        String groupName = args.get(index).toLowerCase();
        if (!DataConstraints.GROUP_NAME_TEST.test(groupName)) {
            throw new DetailedUsageException();
        }
        return groupName;
    }

    public static String handleNameWithSpace(int index, List<String> args) throws ArgumentException {
        String groupName = args.get(index).toLowerCase();
        if (!DataConstraints.GROUP_NAME_TEST_ALLOW_SPACE.test(groupName)) {
            throw new DetailedUsageException();
        }
        return groupName;
    }

    public static boolean handleBoolean(int index, List<String> args) throws ArgumentException {
        if (index < args.size()) {
            String bool = args.get(index);
            if (bool.equalsIgnoreCase("true") || bool.equalsIgnoreCase("false")) {
                return Boolean.parseBoolean(bool);
            }
        }

        args.add(index, "true");
        return true;
    }

    public static long handleDuration(int index, List<String> args) throws ArgumentException {
        long duration;
        try {
            duration = Long.parseLong(args.get(index));
        } catch (NumberFormatException e) {
            try {
                duration = DateUtil.parseDateDiff(args.get(index), true);
            } catch (DateUtil.IllegalDateException e1) {
                throw new InvalidDateException(args.get(index));
            }
        }

        if (DateUtil.shouldExpire(duration)) {
            throw new PastDateException();
        }

        return duration;
    }

    public static MutableContextSet handleContext(int fromIndex, List<String> args, LuckPermsPlugin plugin) throws CommandException {
        if (args.size() > fromIndex) {
            MutableContextSet set = MutableContextSet.create();

            List<String> contexts = args.subList(fromIndex, args.size());

            for (int i = 0; i < contexts.size(); i++) {
                String pair = contexts.get(i);

                // one of the first two values, and doesn't have a key
                if (i <= 1 && !pair.contains("=")) {
                    String key = i == 0 ? Contexts.SERVER_KEY : Contexts.WORLD_KEY;
                    set.add(key, pair);
                    continue;
                }

                int index = pair.indexOf('=');
                if (index == -1) {
                    continue;
                }

                String key = pair.substring(0, index);
                if (key.equals("")) {
                    continue;
                }

                String value = pair.substring(index + 1);
                if (value.equals("")) {
                    continue;
                }

                set.add(key, value);
            }

            return sanitizeContexts(set);
        } else {
            return sanitizeContexts(plugin.getConfiguration().getContextsFile().getDefaultContexts().mutableCopy());
        }
    }

    public static MutableContextSet sanitizeContexts(MutableContextSet set) throws ArgumentException {
        // remove any potential "global" context mappings
        set.remove(Contexts.SERVER_KEY, "global");
        set.remove(Contexts.WORLD_KEY, "global");
        set.remove(Contexts.SERVER_KEY, "null");
        set.remove(Contexts.WORLD_KEY, "null");
        set.remove(Contexts.SERVER_KEY, "*");
        set.remove(Contexts.WORLD_KEY, "*");

        // remove excess entries from the set.
        // (it can only have one server and one world.)
        List<String> servers = new ArrayList<>(set.getValues(Contexts.SERVER_KEY));
        if (servers.size() > 1) {
            // start iterating at index 1
            for (int i = 1; i < servers.size(); i++) {
                set.remove(Contexts.SERVER_KEY, servers.get(i));
            }
        }

        List<String> worlds = new ArrayList<>(set.getValues(Contexts.WORLD_KEY));
        if (worlds.size() > 1) {
            // start iterating at index 1
            for (int i = 1; i < worlds.size(); i++) {
                set.remove(Contexts.WORLD_KEY, worlds.get(i));
            }
        }

        // there's either none or 1
        for (String server : servers) {
            if (!DataConstraints.SERVER_NAME_TEST.test(server)) {
                throw new InvalidServerWorldException();
            }
        }

        // there's either none or 1
        for (String world : worlds) {
            if (!DataConstraints.WORLD_NAME_TEST.test(world)) {
                throw new InvalidServerWorldException();
            }
        }

        return set;
    }

    public static int handlePriority(int index, List<String> args) throws ArgumentException {
        try {
            return Integer.parseInt(args.get(index));
        } catch (NumberFormatException e) {
            throw new InvalidPriorityException(args.get(index));
        }
    }

    public static ImmutableContextSet handleContextSponge(int fromIndex, List<String> args) {
        if (args.size() <= fromIndex) {
            return ImmutableContextSet.empty();
        }

        MutableContextSet contextSet = MutableContextSet.create();
        List<String> toQuery = args.subList(fromIndex, args.size());
        for (String s : toQuery) {
            int index = s.indexOf('=');
            if (index != -1) {
                String key = s.substring(0, index);
                if (key.equals("")) {
                    continue;
                }

                String value = s.substring(index + 1);
                if (value.equals("")) {
                    continue;
                }

                contextSet.add(key, value);
            }
        }

        return contextSet.makeImmutable();
    }

    public static abstract class ArgumentException extends CommandException {}
    public static class DetailedUsageException extends ArgumentException {}
    public static class InvalidServerWorldException extends ArgumentException {}
    public static class PastDateException extends ArgumentException {}

    @Getter
    @AllArgsConstructor
    public static class InvalidDateException extends ArgumentException {
        private final String invalidDate;
    }

    @Getter
    @AllArgsConstructor
    public static class InvalidPriorityException extends ArgumentException {
        private final String invalidPriority;
    }

}
