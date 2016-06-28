package me.lucko.luckperms.commands;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Util {

    public static final String PREFIX = "&7&l[&b&lL&a&lP&7&l] &c";

    public static void sendPluginMessage(Sender sender, String message) {
        sender.sendMessage(color(PREFIX + message));
    }

    public static String color(String s) {
        return translateAlternateColorCodes('&', s);
    }

    public static String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
        // Stolen from Bukkit :>
        char[] b = textToTranslate.toCharArray();

        for(int i = 0; i < b.length - 1; ++i) {
            if(b[i] == altColorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(b[i + 1]) > -1) {
                b[i] = 167;
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }

        return new String(b);
    }

    public static void sendBoolean(Sender sender, String node, boolean b) {
        if (b) {
            sender.sendMessage(Util.color("&b" + node + ": &atrue"));
        } else {
            sender.sendMessage(Util.color("&b" + node + ": &cfalse"));
        }
    }

    public static String listToCommaSep(List<String> strings) {
        if (strings.isEmpty()) return "&6None";

        StringBuilder sb = new StringBuilder();

        for (String s : strings) {
            sb.append("&6").append(s).append("&7, ");
        }

        return sb.delete(sb.length() - 2, sb.length()).toString();
    }

    public static String nodesToString(Map<String, Boolean> nodes) {
        if (nodes.isEmpty()) return "&6None";

        StringBuilder sb = new StringBuilder();

        for (String node : nodes.keySet()) {
            if (nodes.get(node)) {
                sb.append("&a").append(node).append("&7, ");
            } else {
                sb.append("&c").append(node).append("&7, ");
            }
        }

        return sb.delete(sb.length() - 2, sb.length()).toString();
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
}
