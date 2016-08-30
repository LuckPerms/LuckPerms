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

package me.lucko.luckperms.commands;

import lombok.experimental.UtilityClass;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Patterns;
import me.lucko.luckperms.utils.DateUtil;

import java.util.*;

@UtilityClass
public class Util {

    public static void sendPluginMessage(Sender sender, String message) {
        sender.sendMessage(color(Message.PREFIX + message));
    }

    public static String color(String s) {
        return translateAlternateColorCodes('&', s);
    }

    public static String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
        // Stolen from Bukkit :>
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

    public static void sendBoolean(Sender sender, String node, boolean b) {
        sender.sendMessage(Util.color("&b" + node + ": " + formatBoolean(b)));
    }

    public static String listToCommaSep(List<String> strings) {
        if (strings.isEmpty()) return "&6None";

        StringBuilder sb = new StringBuilder();
        strings.forEach(s -> sb.append("&6").append(s).append("&7, "));
        return sb.delete(sb.length() - 2, sb.length()).toString();
    }

    public static String listToArrowSep(List<String> strings, String highlight) {
        if (strings.isEmpty()) return "&6None";

        StringBuilder sb = new StringBuilder();
        for (String s : strings) {
            if (s.equalsIgnoreCase(highlight)) {
                sb.append("&e").append(s).append("&7 ---> ");
            } else {
                sb.append("&6").append(s).append("&7 ---> ");
            }
        }
        return sb.delete(sb.length() - 6, sb.length()).toString();
    }

    public static String listToArrowSep(List<String> strings, String highlightFirst, String highlightSecond, boolean reversed) {
        if (strings.isEmpty()) return "&6None";

        StringBuilder sb = new StringBuilder();
        for (String s : strings) {
            if (s.equalsIgnoreCase(highlightFirst)) {
                sb.append("&e").append(s).append("&4").append(reversed ? " <--- " : " ---> ");
            } else if (s.equalsIgnoreCase(highlightSecond)) {
                sb.append("&e").append(s).append("&7").append(reversed ? " <--- " : " ---> ");
            } else {
                sb.append("&6").append(s).append("&7").append(reversed ? " <--- " : " ---> ");
            }
        }
        return sb.delete(sb.length() - 6, sb.length()).toString();
    }

    public static String listToArrowSep(List<String> strings) {
        if (strings.isEmpty()) return "&6None";

        StringBuilder sb = new StringBuilder();
        strings.forEach(s -> sb.append("&6").append(s).append("&e ---> "));
        return sb.delete(sb.length() - 6, sb.length()).toString();
    }

    public static String permNodesToString(SortedSet<Node> nodes) {
        StringBuilder sb = new StringBuilder();
        for (Node node : nodes) {
            if (node.isTemporary()) {
                continue;
            }

            sb.append("&6-> ").append(node.getValue() ? "&a" : "&c");
            sb.append(node.getPermission());
            if (node.isServerSpecific()) {
                sb.append(" &7(&f").append(node.getServer().get()).append("&7)");
            }
            if (node.isWorldSpecific()) {
                sb.append(" &7(&f").append(node.getWorld().get()).append("&7)");
            }
            sb.append("\n");
        }

        if (sb.length() == 0) {
            return "&6None";
        }

        return sb.toString();
    }

    public static String tempNodesToString(SortedSet<Node> nodes) {
        StringBuilder sb = new StringBuilder();

        for (Node node : nodes) {
            if (!node.isTemporary()) {
                continue;
            }

            sb.append("&6-> ").append(node.getValue() ? "&a" : "&c");
            sb.append(node.getPermission());
            if (node.isServerSpecific()) {
                sb.append(" &7(&f").append(node.getServer().get()).append("&7)");
            }
            if (node.isWorldSpecific()) {
                sb.append(" &7(&f").append(node.getWorld().get()).append("&7)");
            }

            sb.append("&6 - expires in ").append(DateUtil.formatDateDiff(node.getExpiryUnixTime())).append("\n");
        }

        if (sb.length() == 0) {
            return "&6None";
        }

        return sb.toString();
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

    public static synchronized MetaComparator getMetaComparator() {
        if (metaComparator == null) {
            metaComparator = new MetaComparator();
        }
        return metaComparator;
    }


    private static MetaComparator metaComparator = null;
    public class MetaComparator implements Comparator<Map.Entry<Integer, String>> {

        @Override
        public int compare(Map.Entry<Integer, String> o1, Map.Entry<Integer, String> o2) {
            int result = Integer.compare(o1.getKey(), o2.getKey());
            if (result == 0) {
                result = o1.getValue().compareTo(o2.getValue());
            }
            return result;
        }
    }
}
