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

import lombok.experimental.UtilityClass;

import me.lucko.luckperms.api.LocalizedNode;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Patterns;
import me.lucko.luckperms.common.utils.DateUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.UUID;

@UtilityClass
public class Util {

    public static final MetaComparator META_COMPARATOR = new MetaComparator();

    public static void sendPluginMessage(Sender sender, String message) {
        String prefix = sender.getPlatform().getLocaleManager().getTranslation(Message.PREFIX);
        if (prefix == null) {
            prefix = Message.PREFIX.getMessage();
        }
        sender.sendMessage(color(prefix + message));
    }

    public static List<String> stripQuotes(List<String> input) {
        input = new ArrayList<>(input);
        ListIterator<String> iterator = input.listIterator();
        while (iterator.hasNext()) {
            String value = iterator.next();
            if (!(value.length() >= 3)) {
                continue;
            }

            if (value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
                iterator.set(value.substring(1, value.length() - 1));
            }
        }
        return input;
    }

    public static String color(String s) {
        return translateAlternateColorCodes('&', s);
    }

    public static String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
        char[] b = textToTranslate.toCharArray();

        for (int i = 0; i < b.length - 1; ++i) {
            if (b[i] == altColorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(b[i + 1]) > -1) {
                b[i] = 167;
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }

        return new String(b);
    }

    public static String stripColor(String s) {
        return s == null ? null : Patterns.STRIP_COLOR_PATTERN.matcher(s).replaceAll("");
    }

    public static String formatBoolean(boolean b) {
        return b ? "&atrue" : "&cfalse";
    }

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

    public static void sendTristate(Sender sender, String node, Tristate t) {
        sender.sendMessage(Util.color("&b" + node + ": " + formatTristate(t)));
    }

    public static String getNodeContextDescription(Node node) {
        StringBuilder sb = new StringBuilder();
        if (node.isServerSpecific()) {
            sb.append(" ").append(contextToString("server", node.getServer().get()));
        }
        if (node.isWorldSpecific()) {
            sb.append(" ").append(contextToString("world", node.getWorld().get()));
        }
        for (Map.Entry<String, String> c : node.getContexts().toSet()) {
            sb.append(" ").append(contextToString(c.getKey(), c.getValue()));
        }

        return sb.toString();
    }

    public static String contextToString(String key, String value) {
        return "&8(&7" + key + "=&f" + value + "&8)";
    }

    public static String permNodesToString(SortedSet<LocalizedNode> nodes) {
        StringBuilder sb = new StringBuilder();
        for (Node node : nodes) {
            if (node.isTemporary()) continue;

            sb.append("&3> ")
                    .append(node.getValue() ? "&a" : "&c")
                    .append(node.getPermission())
                    .append(getNodeContextDescription(node))
                    .append("\n");
        }
        return sb.length() == 0 ? "&3None" : sb.toString();
    }

    public static String tempNodesToString(SortedSet<LocalizedNode> nodes) {
        StringBuilder sb = new StringBuilder();
        for (Node node : nodes) {
            if (!node.isTemporary()) continue;

            sb.append("&3> ")
                    .append(node.getValue() ? "&a" : "&c")
                    .append(node.getPermission())
                    .append(getNodeContextDescription(node))
                    .append("\n&2-    expires in ")
                    .append(DateUtil.formatDateDiff(node.getExpiryUnixTime()))
                    .append("\n");
        }
        return sb.length() == 0 ? "&3None" : sb.toString();
    }

    public static String permGroupsToString(SortedSet<LocalizedNode> nodes) {
        StringBuilder sb = new StringBuilder();
        for (Node node : nodes) {
            if (!node.isGroupNode()) continue;
            if (node.isTemporary()) continue;

            sb.append("&3> &f")
                    .append(node.getGroupName())
                    .append(getNodeContextDescription(node))
                    .append("\n");
        }
        return sb.length() == 0 ? "&3None" : sb.toString();
    }

    public static String tempGroupsToString(SortedSet<LocalizedNode> nodes) {
        StringBuilder sb = new StringBuilder();
        for (Node node : nodes) {
            if (!node.isGroupNode()) continue;
            if (!node.isTemporary()) continue;

            sb.append("&3> &f")
                    .append(node.getGroupName())
                    .append(getNodeContextDescription(node))
                    .append("\n&2-    expires in ")
                    .append(DateUtil.formatDateDiff(node.getExpiryUnixTime()))
                    .append("\n");
        }
        return sb.length() == 0 ? "&3None" : sb.toString();
    }

    public static UUID parseUuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            try {
                return UUID.fromString(s.replaceAll(
                        "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                        "$1-$2-$3-$4-$5"));
            } catch (IllegalArgumentException e1) {
                return null;
            }
        }
    }

    public static String listToCommaSep(List<String> strings) {
        if (strings.isEmpty()) return "&bNone";

        StringBuilder sb = new StringBuilder();
        strings.forEach(s -> sb.append("&3").append(s).append("&7, "));
        return sb.delete(sb.length() - 2, sb.length()).toString();
    }

    public static String listToArrowSep(List<String> strings, String highlight) {
        if (strings.isEmpty()) return "&bNone";

        StringBuilder sb = new StringBuilder();
        for (String s : strings) {
            if (s.equalsIgnoreCase(highlight)) {
                sb.append("&b").append(s).append("&7 ---> ");
            } else {
                sb.append("&3").append(s).append("&7 ---> ");
            }
        }
        return sb.delete(sb.length() - 6, sb.length()).toString();
    }

    public static String listToArrowSep(List<String> strings, String highlightFirst, String highlightSecond, boolean reversed) {
        if (strings.isEmpty()) return "&6None";

        StringBuilder sb = new StringBuilder();
        for (String s : strings) {
            if (s.equalsIgnoreCase(highlightFirst)) {
                sb.append("&b").append(s).append("&4").append(reversed ? " <--- " : " ---> ");
            } else if (s.equalsIgnoreCase(highlightSecond)) {
                sb.append("&b").append(s).append("&7").append(reversed ? " <--- " : " ---> ");
            } else {
                sb.append("&3").append(s).append("&7").append(reversed ? " <--- " : " ---> ");
            }
        }
        return sb.delete(sb.length() - 6, sb.length()).toString();
    }

    public static String listToArrowSep(List<String> strings) {
        if (strings.isEmpty()) return "&6None";

        StringBuilder sb = new StringBuilder();
        strings.forEach(s -> sb.append("&3").append(s).append("&b ---> "));
        return sb.delete(sb.length() - 6, sb.length()).toString();
    }

    public class MetaComparator implements Comparator<Map.Entry<Integer, ? extends Node>> {

        @Override
        public int compare(Map.Entry<Integer, ? extends Node> o1, Map.Entry<Integer, ? extends Node> o2) {
            int result = Integer.compare(o1.getKey(), o2.getKey());
            return result != 0 ? result : 1;
        }
    }
}
