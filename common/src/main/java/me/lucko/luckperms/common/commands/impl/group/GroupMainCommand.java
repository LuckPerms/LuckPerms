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

package me.lucko.luckperms.common.commands.impl.group;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.common.commands.abstraction.Command;
import me.lucko.luckperms.common.commands.abstraction.MainCommand;
import me.lucko.luckperms.common.commands.impl.generic.meta.CommandMeta;
import me.lucko.luckperms.common.commands.impl.generic.other.HolderClear;
import me.lucko.luckperms.common.commands.impl.generic.other.HolderEditor;
import me.lucko.luckperms.common.commands.impl.generic.other.HolderShowTracks;
import me.lucko.luckperms.common.commands.impl.generic.parent.CommandParent;
import me.lucko.luckperms.common.commands.impl.generic.permission.CommandPermission;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class GroupMainCommand extends MainCommand<Group, String> {

    // we use a lock per unique group
    // this helps prevent race conditions where commands are being executed concurrently
    // and overriding each other.
    // it's not a great solution, but it mostly works.
    private final LoadingCache<String, ReentrantLock> locks = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build(key -> new ReentrantLock());

    public GroupMainCommand(LocaleManager locale) {
        super(CommandSpec.GROUP.spec(locale), "Group", 2, ImmutableList.<Command<Group, ?>>builder()
                .add(new GroupInfo(locale))
                .add(new CommandPermission<>(locale, false))
                .add(new CommandParent<>(locale, false))
                .add(new CommandMeta<>(locale, false))
                .add(new HolderEditor<>(locale, false))
                .add(new GroupListMembers(locale))
                .add(new GroupSetWeight(locale))
                .add(new GroupSetDisplayName(locale))
                .add(new HolderShowTracks<>(locale, false))
                .add(new HolderClear<>(locale, false))
                .add(new GroupRename(locale))
                .add(new GroupClone(locale))
                .build()
        );
    }

    @Override
    protected String parseTarget(String target, LuckPermsPlugin plugin, Sender sender) {
        return target.toLowerCase();
    }

    @Override
    protected Group getTarget(String target, LuckPermsPlugin plugin, Sender sender) {
        if (!plugin.getStorage().loadGroup(target).join().isPresent()) {
            // failed to load, but it might be a display name.

            // nope, not a display name
            if (plugin.getGroupManager().getByDisplayName(target) == null) {
                Message.GROUP_NOT_FOUND.send(sender, target);
                return null;
            }
        }

        Group group = plugin.getGroupManager().getByDisplayName(target);
        if (group == null) {
            Message.GROUP_NOT_FOUND.send(sender, target);
            return null;
        }

        group.auditTemporaryPermissions();
        return group;
    }

    @Override
    protected ReentrantLock getLockForTarget(String target) {
        return locks.get(target);
    }

    @Override
    protected void cleanup(Group group, LuckPermsPlugin plugin) {

    }

    @Override
    protected List<String> getTargets(LuckPermsPlugin plugin) {
        return new ArrayList<>(plugin.getGroupManager().getAll().keySet());
    }
}
