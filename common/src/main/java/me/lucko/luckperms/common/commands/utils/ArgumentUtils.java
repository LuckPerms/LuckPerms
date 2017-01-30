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

package me.lucko.luckperms.common.commands.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.utils.ArgumentChecker;
import me.lucko.luckperms.common.utils.DateUtil;

import java.util.List;
import java.util.function.Function;

/**
 * Utility class to help process arguments, and throw checked exceptions if the arguments are invalid.
 */
public class ArgumentUtils {
    public static final Function<String, String> WRAPPER = s -> s.contains(" ") ? "\"" + s + "\"" : s;

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

    public static String handleNode(int index, List<String> args) throws ArgumentException {
        String node = args.get(index).replace("{SPACE}", " ");
        if (ArgumentChecker.checkNode(node)) {
            throw new DetailedUsageException();
        }

        if (node.toLowerCase().startsWith("group.")) {
            throw new UseInheritException();
        }

        return node;
    }

    public static String handleName(int index, List<String> args) throws ArgumentException {
        String groupName = args.get(index).toLowerCase();
        if (ArgumentChecker.checkName(groupName)) {
            throw new DetailedUsageException();
        }
        return groupName;
    }

    public static String handleNameWithSpace(int index, List<String> args) throws ArgumentException {
        String groupName = args.get(index).toLowerCase();
        if (ArgumentChecker.checkNameWithSpace(groupName)) {
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

    public static String handleServer(int index, List<String> args) throws ArgumentException {
        if (args.size() > index) {
            final String server = args.get(index).toLowerCase();
            if (ArgumentChecker.checkServer(server)) {
                throw new InvalidServerException();
            }
            return server;
        }
        return null;
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

    public static String handleWorld(int index, List<String> args) {
        return args.size() > index ? args.get(index).toLowerCase() : null;
    }

    public static int handlePriority(int index, List<String> args) throws ArgumentException {
        try {
            return Integer.parseInt(args.get(index));
        } catch (NumberFormatException e) {
            throw new InvalidPriorityException(args.get(index));
        }
    }

    public static ContextSet handleContexts(int fromIndex, List<String> args) {
        if (args.size() <= fromIndex) {
            return ContextSet.empty();
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

    public static abstract class ArgumentException extends CommandException {
    }

    public static class DetailedUsageException extends ArgumentException {
    }

    public static class UseInheritException extends ArgumentException {
    }

    public static class InvalidServerException extends ArgumentException {
    }

    public static class PastDateException extends ArgumentException {
    }

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
