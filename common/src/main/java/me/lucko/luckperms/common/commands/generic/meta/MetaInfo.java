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

import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.SharedSubCommand;
import me.lucko.luckperms.common.command.access.ArgumentPermissions;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.utils.MessageUtils;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.command.CommandSpec;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.node.comparator.NodeWithContextComparator;
import me.lucko.luckperms.common.node.factory.NodeCommandFactory;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.common.util.TextUtils;

import net.kyori.text.ComponentBuilder;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.metadata.types.InheritanceOriginMetadata;
import net.luckperms.api.node.types.ChatMetaNode;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import net.luckperms.api.query.QueryOptions;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;

public class MetaInfo extends SharedSubCommand {
    private static String processLocation(Node node, PermissionHolder holder) {
        String location = node.metadata(InheritanceOriginMetadata.KEY).getOrigin().getName();
        return location.equalsIgnoreCase(holder.getObjectName()) ? "self" : location;
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

        SortedSet<Map.Entry<Integer, PrefixNode>> prefixes = new TreeSet<>(MetaComparator.INSTANCE.reversed());
        SortedSet<Map.Entry<Integer, SuffixNode>> suffixes = new TreeSet<>(MetaComparator.INSTANCE.reversed());
        Set<MetaNode> meta = new LinkedHashSet<>();

        // Collect data
        for (Node node : holder.resolveInheritedNodes(QueryOptions.nonContextual())) {
            if (!NodeType.META_OR_CHAT_META.matches(node)) {
                continue;
            }

            if (node instanceof PrefixNode) {
                PrefixNode pn = (PrefixNode) node;
                prefixes.add(Maps.immutableEntry(pn.getPriority(), pn));
            } else if (node instanceof SuffixNode) {
                SuffixNode sn = (SuffixNode) node;
                suffixes.add(Maps.immutableEntry(sn.getPriority(), sn));
            } else if (node instanceof MetaNode) {
                meta.add(((MetaNode) node));
            }
        }

        if (prefixes.isEmpty()) {
            Message.CHAT_META_PREFIX_NONE.send(sender, holder.getFormattedDisplayName());
        } else {
            Message.CHAT_META_PREFIX_HEADER.send(sender, holder.getFormattedDisplayName());
            sendChatMetaMessage(prefixes, sender, holder, label);
        }

        if (suffixes.isEmpty()) {
            Message.CHAT_META_SUFFIX_NONE.send(sender, holder.getFormattedDisplayName());
        } else {
            Message.CHAT_META_SUFFIX_HEADER.send(sender, holder.getFormattedDisplayName());
            sendChatMetaMessage(suffixes, sender, holder, label);
        }

        if (meta.isEmpty()) {
            Message.META_NONE.send(sender, holder.getFormattedDisplayName());
        } else {
            Message.META_HEADER.send(sender, holder.getFormattedDisplayName());
            sendMetaMessage(meta, sender, holder, label);
        }

        return CommandResult.SUCCESS;
    }

    private static void sendMetaMessage(Set<MetaNode> meta, Sender sender, PermissionHolder holder, String label) {
        for (MetaNode m : meta) {
            String location = processLocation(m, holder);
            if (!m.getContexts().isEmpty()) {
                String context = MessageUtils.getAppendableNodeContextString(sender.getPlugin().getLocaleManager(), m);
                TextComponent.Builder builder = Message.META_ENTRY_WITH_CONTEXT.asComponent(sender.getPlugin().getLocaleManager(), m.getMetaKey(), m.getMetaValue(), location, context).toBuilder();
                builder.applyDeep(makeFancy(holder, label, m));
                sender.sendMessage(builder.build());
            } else {
                TextComponent.Builder builder = Message.META_ENTRY.asComponent(sender.getPlugin().getLocaleManager(), m.getMetaKey(), m.getMetaValue(), location).toBuilder();
                builder.applyDeep(makeFancy(holder, label, m));
                sender.sendMessage(builder.build());
            }
        }
    }

    private static void sendChatMetaMessage(SortedSet<? extends Map.Entry<Integer, ? extends ChatMetaNode<?, ?>>> meta, Sender sender, PermissionHolder holder, String label) {
        for (Map.Entry<Integer, ? extends ChatMetaNode<?, ?>> e : meta) {
            String location = processLocation(e.getValue(), holder);
            if (!e.getValue().getContexts().isEmpty()) {
                String context = MessageUtils.getAppendableNodeContextString(sender.getPlugin().getLocaleManager(), e.getValue());
                TextComponent.Builder builder = Message.CHAT_META_ENTRY_WITH_CONTEXT.asComponent(sender.getPlugin().getLocaleManager(), e.getKey(), e.getValue().getMetaValue(), location, context).toBuilder();
                builder.applyDeep(makeFancy(holder, label, e.getValue()));
                sender.sendMessage(builder.build());
            } else {
                TextComponent.Builder builder = Message.CHAT_META_ENTRY.asComponent(sender.getPlugin().getLocaleManager(), e.getKey(), e.getValue().getMetaValue(), location).toBuilder();
                builder.applyDeep(makeFancy(holder, label, e.getValue()));
                sender.sendMessage(builder.build());
            }
        }
    }

    private static Consumer<ComponentBuilder<?, ?>> makeFancy(PermissionHolder holder, String label, ChatMetaNode<?, ?> node) {
        String location = node.metadata(InheritanceOriginMetadata.KEY).getOrigin().getName();
        if (!location.equals(holder.getObjectName())) {
            // inherited.
            Group group = holder.getPlugin().getGroupManager().getIfLoaded(location);
            if (group != null) {
                holder = group;
            }
        }

        HoverEvent hoverEvent = HoverEvent.showText(TextUtils.fromLegacy(TextUtils.joinNewline(
                "¥3> ¥a" + node.getPriority() + " ¥7- ¥r" + node.getMetaValue(),
                " ",
                "¥7Click to remove this " + node.getMetaType().name().toLowerCase() + " from " + holder.getPlainDisplayName()
        ), '¥'));

        String id = holder.getType() == HolderType.GROUP ? holder.getObjectName() : holder.getFormattedDisplayName();
        boolean explicitGlobalContext = !holder.getPlugin().getConfiguration().getContextsFile().getDefaultContexts().isEmpty();
        String command = "/" + label + " " + NodeCommandFactory.generateCommand(node, id, holder.getType(), false, explicitGlobalContext);
        ClickEvent clickEvent = ClickEvent.suggestCommand(command);

        return component -> {
            component.hoverEvent(hoverEvent);
            component.clickEvent(clickEvent);
        };
    }

    private static Consumer<ComponentBuilder<?, ?>> makeFancy(PermissionHolder holder, String label, MetaNode node) {
        String location = node.metadata(InheritanceOriginMetadata.KEY).getOrigin().getName();
        if (!location.equals(holder.getObjectName())) {
            // inherited.
            Group group = holder.getPlugin().getGroupManager().getIfLoaded(location);
            if (group != null) {
                holder = group;
            }
        }

        HoverEvent hoverEvent = HoverEvent.showText(TextUtils.fromLegacy(TextUtils.joinNewline(
                "¥3> ¥r" + node.getMetaKey() + " ¥7- ¥r" + node.getMetaValue(),
                " ",
                "¥7Click to remove this meta pair from " + holder.getPlainDisplayName()
        ), '¥'));

        String id = holder.getType() == HolderType.GROUP ? holder.getObjectName() : holder.getPlainDisplayName();
        boolean explicitGlobalContext = !holder.getPlugin().getConfiguration().getContextsFile().getDefaultContexts().isEmpty();
        String command = "/" + label + " " + NodeCommandFactory.generateCommand(node, id, holder.getType(), false, explicitGlobalContext);
        ClickEvent clickEvent = ClickEvent.suggestCommand(command);

        return component -> {
            component.hoverEvent(hoverEvent);
            component.clickEvent(clickEvent);
        };
    }

    private static final class MetaComparator implements Comparator<Map.Entry<Integer, ? extends ChatMetaNode<?, ?>>> {
        public static final MetaComparator INSTANCE = new MetaComparator();

        @Override
        public int compare(Map.Entry<Integer, ? extends ChatMetaNode<?, ?>> o1, Map.Entry<Integer, ? extends ChatMetaNode<?, ?>> o2) {
            int result = Integer.compare(o1.getKey(), o2.getKey());
            if (result != 0) {
                return result;
            }
            return NodeWithContextComparator.normal().compare(o1.getValue(), o2.getValue());
        }
    }
}
