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

package me.lucko.luckperms.common.commands;

import lombok.experimental.UtilityClass;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

@UtilityClass
public class ArgumentPermissions {
    private static final String USER_MODIFY_SELF = CommandPermission.ROOT + "modify.user.self";
    private static final String USER_MODIFY_OTHERS = CommandPermission.ROOT + "modify.user.others";
    private static final Function<String, String> GROUP_MODIFY = s -> CommandPermission.ROOT + "modify.group." + s;
    private static final Function<String, String> TRACK_MODIFY = s -> CommandPermission.ROOT + "modify.track." + s;

    private static final String USER_VIEW_SELF = CommandPermission.ROOT + "view.user.self";
    private static final String USER_VIEW_OTHERS = CommandPermission.ROOT + "view.user.others";
    private static final Function<String, String> GROUP_VIEW = s -> CommandPermission.ROOT + "view.group." + s;
    private static final Function<String, String> TRACK_VIEW = s -> CommandPermission.ROOT + "view.track." + s;

    private static final String CONTEXT_USE_GLOBAL = CommandPermission.ROOT + "usecontext.global";
    private static final BiFunction<String, String, String> CONTEXT_USE = (k, v) -> CommandPermission.ROOT + "usecontext." + k + "." + v;

    public static boolean checkArguments(LuckPermsPlugin plugin, Sender sender, CommandPermission base, String... args) {
        if (!plugin.getConfiguration().get(ConfigKeys.USE_ARGUMENT_BASED_COMMAND_PERMISSIONS)) {
            return false;
        }

        if (args.length == 0) {
            throw new IllegalStateException();
        }

        StringBuilder permission = new StringBuilder(base.getPermission());
        for (String arg : args) {
            permission.append(".").append(arg);
        }

        return !sender.hasPermission(permission.toString());
    }

    public static boolean checkModifyPerms(LuckPermsPlugin plugin, Sender sender, CommandPermission base, Object target) {
        if (!plugin.getConfiguration().get(ConfigKeys.USE_ARGUMENT_BASED_COMMAND_PERMISSIONS)) {
            return false;
        }
        
        if (target instanceof User) {
            User targetUser = ((User) target);
            
            if (plugin.getUuidCache().getExternalUUID(targetUser.getUuid()).equals(sender.getUuid())) {
                // the sender is trying to edit themselves
                Tristate ret = sender.getPermissionValue(base.getPermission() + ".modify.self");
                if (ret != Tristate.UNDEFINED) {
                    return !ret.asBoolean();
                } else {
                    // fallback to the global perm if the one for the specific command is undefined
                    Tristate globalRet = sender.getPermissionValue(USER_MODIFY_SELF);
                    return !globalRet.asBoolean();
                }
            } else {
                // they're trying to edit another user
                Tristate ret = sender.getPermissionValue(base.getPermission() + ".modify.others");
                if (ret != Tristate.UNDEFINED) {
                    return !ret.asBoolean();
                } else {
                    // fallback to the global perm if the one for the specific command is undefined
                    Tristate globalRet = sender.getPermissionValue(USER_MODIFY_OTHERS);
                    return !globalRet.asBoolean();
                }
            }
        } else if (target instanceof Group) {
            Group targetGroup = ((Group) target);

            Tristate ret = sender.getPermissionValue(base.getPermission() + ".modify." + targetGroup.getName());
            if (ret != Tristate.UNDEFINED) {
                return !ret.asBoolean();
            } else {
                // fallback to the global perm if the one for the specific command is undefined
                Tristate globalRet = sender.getPermissionValue(GROUP_MODIFY.apply(targetGroup.getName()));
                return !globalRet.asBoolean();
            }
        } else if (target instanceof Track) {
            Track targetTrack = ((Track) target);

            Tristate ret = sender.getPermissionValue(base.getPermission() + ".modify." + targetTrack.getName());
            if (ret != Tristate.UNDEFINED) {
                return !ret.asBoolean();
            } else {
                // fallback to the global perm if the one for the specific command is undefined
                Tristate globalRet = sender.getPermissionValue(TRACK_MODIFY.apply(targetTrack.getName()));
                return !globalRet.asBoolean();
            }
        } else {
            throw new IllegalStateException();
        }
    }

    public static boolean checkViewPerms(LuckPermsPlugin plugin, Sender sender, CommandPermission base, Object target) {
        if (!plugin.getConfiguration().get(ConfigKeys.USE_ARGUMENT_BASED_COMMAND_PERMISSIONS)) {
            return false;
        }

        if (target instanceof User) {
            User targetUser = ((User) target);

            if (plugin.getUuidCache().getExternalUUID(targetUser.getUuid()).equals(sender.getUuid())) {
                // the sender is trying to view themselves
                Tristate ret = sender.getPermissionValue(base.getPermission() + ".view.self");
                if (ret != Tristate.UNDEFINED) {
                    return !ret.asBoolean();
                } else {
                    // fallback to the global perm if the one for the specific command is undefined
                    Tristate globalRet = sender.getPermissionValue(USER_VIEW_SELF);
                    return !globalRet.asBoolean();
                }
            } else {
                // they're trying to view another user
                Tristate ret = sender.getPermissionValue(base.getPermission() + ".view.others");
                if (ret != Tristate.UNDEFINED) {
                    return !ret.asBoolean();
                } else {
                    // fallback to the global perm if the one for the specific command is undefined
                    Tristate globalRet = sender.getPermissionValue(USER_VIEW_OTHERS);
                    return !globalRet.asBoolean();
                }
            }
        } else if (target instanceof Group) {
            Group targetGroup = ((Group) target);

            Tristate ret = sender.getPermissionValue(base.getPermission() + ".view." + targetGroup.getName());
            if (ret != Tristate.UNDEFINED) {
                return !ret.asBoolean();
            } else {
                // fallback to the global perm if the one for the specific command is undefined
                Tristate globalRet = sender.getPermissionValue(GROUP_VIEW.apply(targetGroup.getName()));
                return !globalRet.asBoolean();
            }
        } else if (target instanceof Track) {
            Track targetTrack = ((Track) target);

            Tristate ret = sender.getPermissionValue(base.getPermission() + ".view." + targetTrack.getName());
            if (ret != Tristate.UNDEFINED) {
                return !ret.asBoolean();
            } else {
                // fallback to the global perm if the one for the specific command is undefined
                Tristate globalRet = sender.getPermissionValue(TRACK_VIEW.apply(targetTrack.getName()));
                return !globalRet.asBoolean();
            }
        }

        return false;
    }

    public static boolean checkContext(LuckPermsPlugin plugin, Sender sender, CommandPermission base, ContextSet contextSet) {
        if (!plugin.getConfiguration().get(ConfigKeys.USE_ARGUMENT_BASED_COMMAND_PERMISSIONS)) {
            return false;
        }

        if (contextSet.isEmpty()) {
            Tristate ret = sender.getPermissionValue(base.getPermission() + ".usecontext.global");
            if (ret != Tristate.UNDEFINED) {
                return !ret.asBoolean();
            } else {
                // fallback to the global perm if the one for the specific command is undefined
                Tristate globalRet = sender.getPermissionValue(CONTEXT_USE_GLOBAL);
                return !globalRet.asBoolean();
            }
        }

        for (Map.Entry<String, String> context : contextSet.toSet()) {
            Tristate ret = sender.getPermissionValue(base.getPermission() + ".usecontext." + context.getKey() + "." + context.getValue());
            if (ret != Tristate.UNDEFINED) {
                if (ret == Tristate.FALSE) {
                    return true;
                }
            } else {
                // fallback to the global perm if the one for the specific command is undefined
                Tristate globalRet = sender.getPermissionValue(CONTEXT_USE.apply(context.getKey(), context.getValue()));
                if (globalRet == Tristate.FALSE) {
                    return true;
                }
            }
        }

        return false;
    }
    
}
