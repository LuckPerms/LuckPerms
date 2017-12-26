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

package me.lucko.luckperms.common.commands.impl.user;

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
import me.lucko.luckperms.common.commands.utils.CommandUtils;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.references.UserIdentifier;
import me.lucko.luckperms.common.storage.DataConstraints;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class UserMainCommand extends MainCommand<User, UserIdentifier> {

    // we use a lock per unique user
    // this helps prevent race conditions where commands are being executed concurrently
    // and overriding each other.
    // it's not a great solution, but it mostly works.
    private final LoadingCache<UUID, ReentrantLock> locks = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build(key -> new ReentrantLock());

    public UserMainCommand(LocaleManager locale) {
        super(CommandSpec.USER.spec(locale), "User", 2, ImmutableList.<Command<User, ?>>builder()
                .add(new UserInfo(locale))
                .add(new CommandPermission<>(locale, true))
                .add(new CommandParent<>(locale, true))
                .add(new CommandMeta<>(locale, true))
                .add(new HolderEditor<>(locale, true))
                .add(new UserSwitchPrimaryGroup(locale))
                .add(new UserPromote(locale))
                .add(new UserDemote(locale))
                .add(new HolderShowTracks<>(locale, true))
                .add(new HolderClear<>(locale, true))
                .add(new UserClone(locale))
                .build()
        );
    }

    @Override
    protected UserIdentifier parseTarget(String target, LuckPermsPlugin plugin, Sender sender) {
        UUID uuid = CommandUtils.parseUuid(target.toLowerCase());
        if (uuid == null) {
            if (!plugin.getConfiguration().get(ConfigKeys.ALLOW_INVALID_USERNAMES)) {
                if (!DataConstraints.PLAYER_USERNAME_TEST.test(target)) {
                    Message.USER_INVALID_ENTRY.send(sender, target);
                    return null;
                }
            } else {
                if (!DataConstraints.PLAYER_USERNAME_TEST_LENIENT.test(target)) {
                    Message.USER_INVALID_ENTRY.send(sender, target);
                    return null;
                }
            }

            uuid = plugin.getStorage().getUUID(target.toLowerCase()).join();
            if (uuid == null) {
                if (!plugin.getConfiguration().get(ConfigKeys.USE_SERVER_UUID_CACHE)) {
                    Message.USER_NOT_FOUND.send(sender, target);
                    return null;
                }

                uuid = plugin.lookupUuid(target).orElse(null);
                if (uuid == null) {
                    Message.USER_NOT_FOUND.send(sender, target);
                    return null;
                }
            }
        }

        String name = plugin.getStorage().getName(uuid).join();
        return UserIdentifier.of(uuid, name);
    }

    @Override
    protected User getTarget(UserIdentifier target, LuckPermsPlugin plugin, Sender sender) {

        try {
            plugin.getStorage().loadUser(target.getUuid(), target.getUsername().orElse(null)).get();
        } catch (Exception e) {
            e.printStackTrace();
            Message.LOADING_ERROR.send(sender);
            return null;
        }

        User user = plugin.getUserManager().getIfLoaded(target.getUuid());
        if (user == null) {
            Message.LOADING_ERROR.send(sender);
            return null;
        }

        user.auditTemporaryPermissions();
        return user;
    }

    @Override
    protected ReentrantLock getLockForTarget(UserIdentifier target) {
        return locks.get(target.getUuid());
    }

    @Override
    protected void cleanup(User user, LuckPermsPlugin plugin) {
        plugin.getUserManager().cleanup(user);
    }

    @Override
    protected List<String> getTargets(LuckPermsPlugin plugin) {
        return plugin.getPlayerList().collect(Collectors.toList());
    }
}