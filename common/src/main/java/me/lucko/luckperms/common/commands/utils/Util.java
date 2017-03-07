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

import com.google.common.collect.Maps;

import me.lucko.luckperms.api.HeldPermission;
import me.lucko.luckperms.api.LocalizedNode;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Patterns;
import me.lucko.luckperms.common.core.NodeFactory;
import me.lucko.luckperms.common.core.model.PermissionHolder;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.utils.DateUtil;

import io.github.mkremins.fanciful.ChatColor;
import io.github.mkremins.fanciful.FancyMessage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    public static FancyMessage appendNodeContextDescription(Node node, FancyMessage message) {
        if (node.isServerSpecific()) {
            message = message.then(" ");
            message = appendContext("server", node.getServer().get(), message);
        }
        if (node.isWorldSpecific()) {
            message = message.then(" ");
            message = appendContext("world", node.getWorld().get(), message);
        }
        for (Map.Entry<String, String> c : node.getContexts().toSet()) {
            message = message.then(" ");
            message = appendContext(c.getKey(), c.getValue(), message);
        }

        return message;
    }

    public static FancyMessage appendNodeExpiry(Node node, FancyMessage message) {
        if (node.isTemporary()) {
            message = message.then(" (").color(ChatColor.getByChar('8'));
            message = message.then("expires in " + DateUtil.formatDateDiff(node.getExpiryUnixTime())).color(ChatColor.getByChar('7'));
            message = message.then(")").color(ChatColor.getByChar('8'));
        }

        return message;
    }

    public static String contextToString(String key, String value) {
        return "&8(&7" + key + "=&f" + value + "&8)";
    }

    public static FancyMessage appendContext(String key, String value, FancyMessage message) {
        return message
                .then("(").color(ChatColor.getByChar('8'))
                .then(key + "=").color(ChatColor.getByChar('7'))
                .then(value).color(ChatColor.getByChar('f'))
                .then(")").color(ChatColor.getByChar('8'));
    }

    public static String permNodesToStringConsole(SortedSet<LocalizedNode> nodes) {
        StringBuilder sb = new StringBuilder();
        for (Node node : nodes) {
            if (node.isTemporary()) continue;

            sb.append("&3> ")
                    .append(node.getValue() ? "&a" : "&c")
                    .append(node.getPermission())
                    .append(" ").append("&7(").append(node.getValue()).append("&7)")
                    .append(getNodeContextDescription(node))
                    .append("\n");
        }
        return sb.length() == 0 ? "&3None" : sb.toString();
    }

    private static FancyMessage makeFancy(PermissionHolder holder, String label, Node node, FancyMessage message) {
        message = message.formattedTooltip(
                new FancyMessage("> ")
                        .color(ChatColor.getByChar('3'))
                        .then(node.getPermission())
                        .color(node.getValue() ? ChatColor.getByChar('a') : ChatColor.getByChar('c')),
                new FancyMessage(" "),
                new FancyMessage("Click to remove this node from " + holder.getFriendlyName()).color(ChatColor.getByChar('7'))
        );

        boolean group = !(holder instanceof User);
        String command = NodeFactory.nodeAsCommand(node, group ? holder.getObjectName() : holder.getFriendlyName(), group)
                .replace("/luckperms", "/" + label)
                .replace("permission set", "permission unset")
                .replace("parent add", "parent remove")
                .replace(" true", "")
                .replace(" false", "");

        message = message.suggest(command);
        return message;
    }

    private static FancyMessage makeFancy(String holderName, boolean group, String label, HeldPermission<?> perm, FancyMessage message) {
        Node node = perm.asNode();

        message = message.formattedTooltip(
                new FancyMessage("> ")
                        .color(ChatColor.getByChar('3'))
                        .then(node.getPermission())
                        .color(node.getValue() ? ChatColor.getByChar('a') : ChatColor.getByChar('c')),
                new FancyMessage(" "),
                new FancyMessage("Click to remove this node from " + holderName).color(ChatColor.getByChar('7'))
        );

        String command = NodeFactory.nodeAsCommand(node, group ? holderName : holderName, group)
                .replace("/luckperms", "/" + label)
                .replace("permission set", "permission unset")
                .replace("parent add", "parent remove")
                .replace(" true", "")
                .replace(" false", "");

        message = message.suggest(command);
        return message;
    }

    public static Map.Entry<FancyMessage, String> permNodesToMessage(SortedSet<LocalizedNode> nodes, PermissionHolder holder, String label, int pageNumber) {
        List<Node> l = new ArrayList<>();
        for (Node node : nodes) {
            if (!node.isTemporary()) {
                l.add(node);
            }
        }

        if (l.isEmpty()) {
            return Maps.immutableEntry(new FancyMessage("None").color(ChatColor.getByChar('3')), null);
        }

        int index = pageNumber - 1;
        List<List<Node>> pages = divideList(l, 15);

        if ((index < 0 || index >= pages.size())) {
            pageNumber = 1;
            index = 0;
        }

        List<Node> page = pages.get(index);

        FancyMessage message = new FancyMessage("");
        String title = "&7(showing page &f" + pageNumber + "&7 of &f" + pages.size() + "&7 - &f" + nodes.size() + "&7 entries)";

        for (Node node : page) {
            message = makeFancy(holder, label, node, message.then("> ").color(ChatColor.getByChar('3')));
            message = makeFancy(holder, label, node, message.then(Util.color(node.getPermission())).color(node.getValue() ? ChatColor.getByChar('a') : ChatColor.getByChar('c')));
            message = appendNodeContextDescription(node, message);
            message = message.then("\n");
        }

        return Maps.immutableEntry(message, title);
    }

    public static Map.Entry<FancyMessage, String> searchUserResultToMessage(List<HeldPermission<UUID>> results, Function<UUID, String> uuidLookup, String label, int pageNumber) {
        if (results.isEmpty()) {
            return Maps.immutableEntry(new FancyMessage("None").color(ChatColor.getByChar('3')), null);
        }

        List<HeldPermission<UUID>> sorted = new ArrayList<>(results);
        sorted.sort(Comparator.comparing(HeldPermission::getHolder));

        int index = pageNumber - 1;
        List<List<HeldPermission<UUID>>> pages = divideList(sorted, 15);

        if (index < 0 || index >= pages.size()) {
            pageNumber = 1;
            index = 0;
        }

        List<HeldPermission<UUID>> page = pages.get(index);
        List<Map.Entry<String, HeldPermission<UUID>>> uuidMappedPage = page.stream()
                .map(hp -> Maps.immutableEntry(uuidLookup.apply(hp.getHolder()), hp))
                .collect(Collectors.toList());

        FancyMessage message = new FancyMessage("");
        String title = "&7(page &f" + pageNumber + "&7 of &f" + pages.size() + "&7 - &f" + sorted.size() + "&7 entries)";

        for (Map.Entry<String, HeldPermission<UUID>> ent : uuidMappedPage) {
            message = makeFancy(ent.getKey(), false, label, ent.getValue(), message.then("> ").color(ChatColor.getByChar('3')));
            message = makeFancy(ent.getKey(), false, label, ent.getValue(), message.then(ent.getKey()).color(ChatColor.getByChar('b')));
            message = makeFancy(ent.getKey(), false, label, ent.getValue(), message.then(" - ").color(ChatColor.getByChar('7')));
            message = makeFancy(ent.getKey(), false, label, ent.getValue(), message.then("" + ent.getValue().getValue()).color(ent.getValue().getValue() ? ChatColor.getByChar('a') : ChatColor.getByChar('c')));
            message = appendNodeExpiry(ent.getValue().asNode(), message);
            message = appendNodeContextDescription(ent.getValue().asNode(), message);
            message = message.then("\n");
        }

        return Maps.immutableEntry(message, title);
    }

    public static Map.Entry<FancyMessage, String> searchGroupResultToMessage(List<HeldPermission<String>> results, String label, int pageNumber) {
        if (results.isEmpty()) {
            return Maps.immutableEntry(new FancyMessage("None").color(ChatColor.getByChar('3')), null);
        }

        List<HeldPermission<String>> sorted = new ArrayList<>(results);
        sorted.sort(Comparator.comparing(HeldPermission::getHolder));

        int index = pageNumber - 1;
        List<List<HeldPermission<String>>> pages = divideList(sorted, 15);

        if (index < 0 || index >= pages.size()) {
            pageNumber = 1;
            index = 0;
        }

        List<HeldPermission<String>> page = pages.get(index);

        FancyMessage message = new FancyMessage("");
        String title = "&7(page &f" + pageNumber + "&7 of &f" + pages.size() + "&7 - &f" + sorted.size() + "&7 entries)";

        for (HeldPermission<String> ent : page) {
            message = makeFancy(ent.getHolder(), true, label, ent, message.then("> ").color(ChatColor.getByChar('3')));
            message = makeFancy(ent.getHolder(), true, label, ent, message.then(ent.getHolder()).color(ChatColor.getByChar('b')));
            message = makeFancy(ent.getHolder(), true, label, ent, message.then(" - ").color(ChatColor.getByChar('7')));
            message = makeFancy(ent.getHolder(), true, label, ent, message.then("" + ent.getValue()).color(ent.getValue() ? ChatColor.getByChar('a') : ChatColor.getByChar('c')));
            message = appendNodeExpiry(ent.asNode(), message);
            message = appendNodeContextDescription(ent.asNode(), message);
            message = message.then("\n");
        }

        return Maps.immutableEntry(message, title);
    }

    public static <T> List<List<T>> divideList(List<T> source, int size) {
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
