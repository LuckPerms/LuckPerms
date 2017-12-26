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

import lombok.experimental.UtilityClass;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.locale.Message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@UtilityClass
public class CommandUtils {
    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)" + String.valueOf('§') + "[0-9A-FK-OR]");

    /**
     * Sends a message to the sender, formatted with the plugin prefix and color scheme
     *
     * @param sender the sender to send the message to
     * @param message the message content
     */
    public static void sendPluginMessage(Sender sender, String message) {
        String prefix = sender.getPlatform().getLocaleManager().getTranslation(Message.PREFIX);
        if (prefix == null) {
            prefix = Message.PREFIX.getMessage();
        }
        sender.sendMessage(color(prefix + message));
    }

    /**
     * Colorizes a message.
     *
     * @param s the message to colorize
     * @return a colored message
     */
    public static String color(String s) {
        char[] b = s.toCharArray();

        for (int i = 0; i < b.length - 1; ++i) {
            if (b[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(b[i + 1]) > -1) {
                b[i] = 167;
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }

        return new String(b);
    }

    /**
     * Strips all color from a message
     *
     * @param s the message to strip color from
     * @return the message without color
     */
    public static String stripColor(String s) {
        return s == null ? null : STRIP_COLOR_PATTERN.matcher(s).replaceAll("");
    }

    public static <T> List<T> nInstances(int count, Supplier<T> supplier) {
        List<T> ret = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ret.add(supplier.get());
        }
        return ret;
    }

    public static <T> List<List<T>> divideList(Iterable<T> source, int size) {
        List<List<T>> lists = new ArrayList<>();
        Iterator<T> it = source.iterator();
        while (it.hasNext()) {
            List<T> subList = new ArrayList<>();
            for (int i = 0; it.hasNext() && i < size; i++) {
                subList.add(it.next());
            }
            lists.add(subList);
        }
        return lists;
    }

    public static UUID parseUuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            try {
                return UUID.fromString(s.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
            } catch (IllegalArgumentException e1) {
                return null;
            }
        }
    }

    public static String toCommaSep(Collection<String> strings) {
        if (strings.isEmpty()) {
            return "&bNone";
        }

        StringBuilder sb = new StringBuilder();
        strings.forEach(s -> sb.append("&3").append(s).append("&7, "));
        return sb.delete(sb.length() - 2, sb.length()).toString();
    }

    public static String listToArrowSep(Collection<String> strings, String highlight) {
        if (strings.isEmpty()) {
            return "&bNone";
        }

        StringBuilder sb = new StringBuilder();
        strings.forEach(s -> sb.append(s.equalsIgnoreCase(highlight) ? "&b" : "&3").append(s).append("&7 ---> "));
        return sb.delete(sb.length() - 6, sb.length()).toString();
    }

    public static String listToArrowSep(Collection<String> strings, String highlightFirst, String highlightSecond, boolean reversed) {
        if (strings.isEmpty()) {
            return "&6None";
        }

        StringBuilder sb = new StringBuilder();
        for (String s : strings) {
            if (s.equalsIgnoreCase(highlightFirst)) {
                sb.append("&b").append(s).append("&4");
            } else if (s.equalsIgnoreCase(highlightSecond)) {
                sb.append("&b").append(s).append("&7");
            } else {
                sb.append("&3").append(s).append("&7");
            }

            sb.append(reversed ? " <--- " : " ---> ");
        }
        return sb.delete(sb.length() - 6, sb.length()).toString();
    }

    public static String listToArrowSep(List<String> strings) {
        if (strings.isEmpty()) {
            return "&6None";
        }

        StringBuilder sb = new StringBuilder();
        strings.forEach(s -> sb.append("&3").append(s).append("&b ---> "));
        return sb.delete(sb.length() - 6, sb.length()).toString();
    }

    /**
     * Formats a boolean to a colored string
     *
     * @param b the boolean value
     * @return a formatted boolean string
     */
    public static String formatBoolean(boolean b) {
        return b ? "&atrue" : "&cfalse";
    }

    /**
     * Formats a tristate to a colored string
     *
     * @param t the tristate value
     * @return a formatted tristate string
     */
    public static String formatTristate(Tristate t) {
        switch (t) {
            case TRUE:
                return "&atrue";
            case FALSE:
                return "&cfalse";
            default:
                return "&cundefined";
        }
    }

    /**
     * Produces a string representing a Nodes context, suitable for appending onto another message line.
     *
     * @param node the node to query context from
     * @return a string representing the nodes context, or an empty string if the node applies globally.
     */
    public static String getAppendableNodeContextString(Node node) {
        StringBuilder sb = new StringBuilder();
        if (node.isServerSpecific()) {
            sb.append(" ").append(contextToString(Contexts.SERVER_KEY, node.getServer().get()));
        }
        if (node.isWorldSpecific()) {
            sb.append(" ").append(contextToString(Contexts.WORLD_KEY, node.getWorld().get()));
        }
        for (Map.Entry<String, String> c : node.getContexts().toSet()) {
            sb.append(" ").append(contextToString(c.getKey(), c.getValue()));
        }

        return sb.toString();
    }

    /**
     * Converts a context pair to a formatted string, surrounded by (  ) brackets.
     *
     * @param key the context key
     * @param value the context value
     * @return a formatted string
     */
    public static String contextToString(String key, String value) {
        return Message.CONTEXT_PAIR.asString(null, key, value);
    }

    public static String contextSetToString(ContextSet set) {
        if (set.isEmpty()) {
            return Message.CONTEXT_PAIR__GLOBAL_INLINE.asString(null);
        }

        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, String> e : set.toSet()) {
            sb.append(Message.CONTEXT_PAIR_INLINE.asString(null, e.getKey(), e.getValue()));
            sb.append(Message.CONTEXT_PAIR_SEP.asString(null));
        }

        return sb.delete(sb.length() - Message.CONTEXT_PAIR_SEP.asString(null).length(), sb.length()).toString();
    }
}
