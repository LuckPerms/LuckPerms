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

package me.lucko.luckperms.common.commands.generic.meta;

import com.google.common.collect.Maps;

import me.lucko.luckperms.api.ChatMetaType;
import me.lucko.luckperms.api.LocalizedNode;
import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.SharedSubCommand;
import me.lucko.luckperms.common.command.access.ArgumentPermissions;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.utils.MessageUtils;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.command.CommandSpec;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.node.comparator.NodeWithContextComparator;
import me.lucko.luckperms.common.node.factory.NodeFactory;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.common.utils.TextUtils;

import net.kyori.text.BuildableComponent;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;

public class MetaInfo extends SharedSubCommand {
    private static String processLocation(LocalizedNode node, PermissionHolder holder) {
        return node.getLocation().equalsIgnoreCase(holder.getObjectName()) ? "self" : node.getLocation();
    }

    public MetaInfo(LocaleManager locale) {
        super(CommandSpec.META_INFO.localize(locale), "info", CommandPermission.USER_META_INFO, CommandPermission.GROUP_META_INFO, Predicates.alwaysFalse());
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args, String label, CommandPermission permission) {
        if (ArgumentPermissions.checkViewPerms(plugin, sender, permission, holder)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        SortedSet<Map.Entry<Integer, LocalizedNode>> prefixes = new TreeSet<>(MetaComparator.INSTANCE.reversed());
        SortedSet<Map.Entry<Integer, LocalizedNode>> suffixes = new TreeSet<>(MetaComparator.INSTANCE.reversed());
        Set<LocalizedNode> meta = new LinkedHashSet<>();

        // Collect data
        for (LocalizedNode node : holder.resolveInheritances()) {
            if (!node.isSuffix() && !node.isPrefix() && !node.isMeta()) {
                continue;
            }

            if (node.isPrefix()) {
                prefixes.add(Maps.immutableEntry(node.getPrefix().getKey(), node));
            } else if (node.isSuffix()) {
                suffixes.add(Maps.immutableEntry(node.getSuffix().getKey(), node));
            } else if (node.isMeta()) {
                meta.add(node);
            }
        }

        if (prefixes.isEmpty()) {
            Message.CHAT_META_PREFIX_NONE.send(sender, holder.getFriendlyName());
        } else {
            Message.CHAT_META_PREFIX_HEADER.send(sender, holder.getFriendlyName());
            sendChatMetaMessage(ChatMetaType.PREFIX, prefixes, sender, holder, label);
        }

        if (suffixes.isEmpty()) {
            Message.CHAT_META_SUFFIX_NONE.send(sender, holder.getFriendlyName());
        } else {
            Message.CHAT_META_SUFFIX_HEADER.send(sender, holder.getFriendlyName());
            sendChatMetaMessage(ChatMetaType.SUFFIX, suffixes, sender, holder, label);
        }

        if (meta.isEmpty()) {
            Message.META_NONE.send(sender, holder.getFriendlyName());
        } else {
            Message.META_HEADER.send(sender, holder.getFriendlyName());
            sendMetaMessage(meta, sender, holder, label);
        }

        return CommandResult.SUCCESS;
    }

    private static void sendMetaMessage(Set<LocalizedNode> meta, Sender sender, PermissionHolder holder, String label) {
        for (LocalizedNode m : meta) {
            String location = processLocation(m, holder);
            if (m.hasSpecificContext()) {
                String context = MessageUtils.getAppendableNodeContextString(sender.getPlugin().getLocaleManager(), m);
                TextComponent.Builder builder = Message.META_ENTRY_WITH_CONTEXT.asComponent(sender.getPlugin().getLocaleManager(), m.getMeta().getKey(), m.getMeta().getValue(), location, context).toBuilder();
                builder.applyDeep(makeFancy(holder, label, m));
                sender.sendMessage(builder.build());
            } else {
                TextComponent.Builder builder = Message.META_ENTRY.asComponent(sender.getPlugin().getLocaleManager(), m.getMeta().getKey(), m.getMeta().getValue(), location).toBuilder();
                builder.applyDeep(makeFancy(holder, label, m));
                sender.sendMessage(builder.build());
            }
        }
    }

    private static void sendChatMetaMessage(ChatMetaType type, SortedSet<Map.Entry<Integer, LocalizedNode>> meta, Sender sender, PermissionHolder holder, String label) {
        for (Map.Entry<Integer, LocalizedNode> e : meta) {
            String location = processLocation(e.getValue(), holder);
            if (e.getValue().hasSpecificContext()) {
                String context = MessageUtils.getAppendableNodeContextString(sender.getPlugin().getLocaleManager(), e.getValue());
                TextComponent.Builder builder = Message.CHAT_META_ENTRY_WITH_CONTEXT.asComponent(sender.getPlugin().getLocaleManager(), e.getKey(), type.getEntry(e.getValue()).getValue(), location, context).toBuilder();
                builder.applyDeep(makeFancy(type, holder, label, e.getValue()));
                sender.sendMessage(builder.build());
            } else {
                TextComponent.Builder builder = Message.CHAT_META_ENTRY.asComponent(sender.getPlugin().getLocaleManager(), e.getKey(), type.getEntry(e.getValue()).getValue(), location).toBuilder();
                builder.applyDeep(makeFancy(type, holder, label, e.getValue()));
                sender.sendMessage(builder.build());
            }
        }
    }

    private static Consumer<BuildableComponent.Builder<?, ?>> makeFancy(ChatMetaType type, PermissionHolder holder, String label, LocalizedNode node) {
        if (!node.getLocation().equals(holder.getObjectName())) {
            // inherited.
            Group group = holder.getPlugin().getGroupManager().getIfLoaded(node.getLocation());
            if (group != null) {
                holder = group;
            }
        }

        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextUtils.fromLegacy(TextUtils.joinNewline(
                "¥3> ¥a" + type.getEntry(node).getKey() + " ¥7- ¥r" + type.getEntry(node).getValue(),
                " ",
                "¥7Click to remove this " + type.name().toLowerCase() + " from " + holder.getFriendlyName()
        ), '¥'));

        String command = "/" + label + " " + NodeFactory.nodeAsCommand(node, holder.getType().isGroup() ? holder.getObjectName() : holder.getFriendlyName(), holder.getType(), false, !holder.getPlugin().getConfiguration().getContextsFile().getDefaultContexts().isEmpty());
        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command);

        return component -> {
            component.hoverEvent(hoverEvent);
            component.clickEvent(clickEvent);
        };
    }

    private static Consumer<BuildableComponent.Builder<?, ?>> makeFancy(PermissionHolder holder, String label, LocalizedNode node) {
        if (!node.getLocation().equals(holder.getObjectName())) {
            // inherited.
            Group group = holder.getPlugin().getGroupManager().getIfLoaded(node.getLocation());
            if (group != null) {
                holder = group;
            }
        }

        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextUtils.fromLegacy(TextUtils.joinNewline(
                "¥3> ¥r" + node.getMeta().getKey() + " ¥7- ¥r" + node.getMeta().getValue(),
                " ",
                "¥7Click to remove this meta pair from " + holder.getFriendlyName()
        ), '¥'));

        String command = "/" + label + " " + NodeFactory.nodeAsCommand(node, holder.getType().isGroup() ? holder.getObjectName() : holder.getFriendlyName(), holder.getType(), false, !holder.getPlugin().getConfiguration().getContextsFile().getDefaultContexts().isEmpty());
        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command);

        return component -> {
            component.hoverEvent(hoverEvent);
            component.clickEvent(clickEvent);
        };
    }

    private static final class MetaComparator implements Comparator<Map.Entry<Integer, LocalizedNode>> {
        public static final MetaComparator INSTANCE = new MetaComparator();

        @Override
        public int compare(Map.Entry<Integer, LocalizedNode> o1, Map.Entry<Integer, LocalizedNode> o2) {
            int result = Integer.compare(o1.getKey(), o2.getKey());
            if (result != 0) {
                return result;
            }
            return NodeWithContextComparator.normal().compare(o1.getValue(), o2.getValue());
        }
    }
}
