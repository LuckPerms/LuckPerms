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

package me.lucko.luckperms.common.commands.group;

import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.SingleCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.utils.MessageUtils;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.command.CommandSpec;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;

import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;

import java.util.List;
import java.util.stream.Collectors;

public class ListGroups extends SingleCommand {
    public ListGroups(LocaleManager locale) {
        super(CommandSpec.LIST_GROUPS.localize(locale), "ListGroups", CommandPermission.LIST_GROUPS, Predicates.alwaysFalse());
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {

        try {
            plugin.getStorage().loadAllGroups().get();
        } catch (Exception e) {
            e.printStackTrace();
            Message.GROUPS_LOAD_ERROR.send(sender);
            return CommandResult.LOADING_ERROR;
        }

        Message.GROUPS_LIST.send(sender);
        plugin.getGroupManager().getAll().values().stream()
                .sorted((o1, o2) -> {
                    int i = Integer.compare(o2.getWeight().orElse(0), o1.getWeight().orElse(0));
                    return i != 0 ? i : o1.getName().compareToIgnoreCase(o2.getName());
                })
                .forEach(group -> {
                    List<String> tracks = plugin.getTrackManager().getAll().values().stream().filter(t -> t.containsGroup(group)).map(Track::getName).collect(Collectors.toList());
                    TextComponent component;

                    if (tracks.isEmpty()) {
                        component = Message.GROUPS_LIST_ENTRY.asComponent(plugin.getLocaleManager(),
                                group.getFormattedDisplayName(),
                                group.getWeight().orElse(0)
                        );
                    } else {
                        component = Message.GROUPS_LIST_ENTRY_WITH_TRACKS.asComponent(plugin.getLocaleManager(),
                                group.getFormattedDisplayName(),
                                group.getWeight().orElse(0),
                                MessageUtils.toCommaSep(tracks)
                        );
                    }

                    component = component.toBuilder().applyDeep(c -> {
                        c.clickEvent(ClickEvent.runCommand("/" + label + " group " + group.getName() + " info"));
                        c.hoverEvent(HoverEvent.showText(TextComponent.of("Click to view more info about " + group.getName() + ".").color(TextColor.GRAY)));
                    }).build();

                    sender.sendMessage(component);
                });

        return CommandResult.SUCCESS;
    }
}
