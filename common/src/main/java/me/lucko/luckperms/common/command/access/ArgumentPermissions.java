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

package me.lucko.luckperms.common.command.access;

import me.lucko.luckperms.common.cacheddata.result.TristateResult;
import me.lucko.luckperms.common.cacheddata.type.PermissionCache;
import me.lucko.luckperms.common.calculator.processor.DirectProcessor;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.query.QueryOptionsImpl;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;
import net.luckperms.api.context.Context;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.util.Tristate;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Implements argument based permission checks for use in command implementations.
 */
public final class ArgumentPermissions {
    private ArgumentPermissions() {}

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

    /**
     * Checks if the sender has permission to use the given arguments
     *
     * @param plugin the plugin instance
     * @param sender the sender to check
     * @param base the base permission for the command
     * @param args the arguments the sender is trying to use
     * @return true if the sender should NOT be allowed to use the arguments, true if they should
     */
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

    /**
     * Checks if the sender has permission to modify the given target
     *
     * @param plugin the plugin instance
     * @param sender the sender to check
     * @param base the base permission for the command
     * @param target the object the sender is truing to modify
     * @return true if the sender should NOT be allowed to modify the target, true if they should
     */
    public static boolean checkModifyPerms(LuckPermsPlugin plugin, Sender sender, CommandPermission base, Object target) {
        if (!plugin.getConfiguration().get(ConfigKeys.USE_ARGUMENT_BASED_COMMAND_PERMISSIONS)) {
            return false;
        }
        
        if (target instanceof User) {
            User targetUser = (User) target;
            
            if (targetUser.getUniqueId().equals(sender.getUniqueId())) {
                // the sender is trying to edit themselves
                Tristate state = sender.getPermissionValue(base.getPermission() + ".modify.self");
                if (state != Tristate.UNDEFINED) {
                    return !state.asBoolean();
                } else {
                    // fallback to the global perm if the one for the specific command is undefined
                    Tristate globalState = sender.getPermissionValue(USER_MODIFY_SELF);
                    return !globalState.asBoolean();
                }
            } else {
                // they're trying to edit another user
                Tristate state = sender.getPermissionValue(base.getPermission() + ".modify.others");
                if (state != Tristate.UNDEFINED) {
                    return !state.asBoolean();
                } else {
                    // fallback to the global perm if the one for the specific command is undefined
                    Tristate globalState = sender.getPermissionValue(USER_MODIFY_OTHERS);
                    return !globalState.asBoolean();
                }
            }
        } else if (target instanceof Group) {
            Group targetGroup = (Group) target;

            Tristate state = sender.getPermissionValue(base.getPermission() + ".modify." + targetGroup.getName());
            if (state != Tristate.UNDEFINED) {
                return !state.asBoolean();
            } else {
                // fallback to the global perm if the one for the specific command is undefined
                Tristate globalState = sender.getPermissionValue(GROUP_MODIFY.apply(targetGroup.getName()));
                return !globalState.asBoolean();
            }
        } else if (target instanceof Track) {
            Track targetTrack = (Track) target;

            Tristate state = sender.getPermissionValue(base.getPermission() + ".modify." + targetTrack.getName());
            if (state != Tristate.UNDEFINED) {
                return !state.asBoolean();
            } else {
                // fallback to the global perm if the one for the specific command is undefined
                Tristate globalState = sender.getPermissionValue(TRACK_MODIFY.apply(targetTrack.getName()));
                return !globalState.asBoolean();
            }
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Checks if the sender has permission to view the given target
     *
     * @param plugin the plugin instance
     * @param sender the sender to check
     * @param base the base permission for the command
     * @param target the object the sender is truing to view
     * @return true if the sender should NOT be allowed to view the target, true if they should
     */
    public static boolean checkViewPerms(LuckPermsPlugin plugin, Sender sender, CommandPermission base, Object target) {
        if (!plugin.getConfiguration().get(ConfigKeys.USE_ARGUMENT_BASED_COMMAND_PERMISSIONS)) {
            return false;
        }

        if (target instanceof User) {
            User targetUser = (User) target;

            if (targetUser.getUniqueId().equals(sender.getUniqueId())) {
                // the sender is trying to view themselves
                Tristate state = sender.getPermissionValue(base.getPermission() + ".view.self");
                if (state != Tristate.UNDEFINED) {
                    return !state.asBoolean();
                } else {
                    // fallback to the global perm if the one for the specific command is undefined
                    Tristate globalState = sender.getPermissionValue(USER_VIEW_SELF);
                    return !globalState.asBoolean();
                }
            } else {
                // they're trying to view another user
                Tristate state = sender.getPermissionValue(base.getPermission() + ".view.others");
                if (state != Tristate.UNDEFINED) {
                    return !state.asBoolean();
                } else {
                    // fallback to the global perm if the one for the specific command is undefined
                    Tristate globalState = sender.getPermissionValue(USER_VIEW_OTHERS);
                    return !globalState.asBoolean();
                }
            }
        } else if (target instanceof Group) {
            Group targetGroup = (Group) target;

            Tristate state = sender.getPermissionValue(base.getPermission() + ".view." + targetGroup.getName());
            if (state != Tristate.UNDEFINED) {
                return !state.asBoolean();
            } else {
                // fallback to the global perm if the one for the specific command is undefined
                Tristate globalState = sender.getPermissionValue(GROUP_VIEW.apply(targetGroup.getName()));
                return !globalState.asBoolean();
            }
        } else if (target instanceof Track) {
            Track targetTrack = (Track) target;

            Tristate state = sender.getPermissionValue(base.getPermission() + ".view." + targetTrack.getName());
            if (state != Tristate.UNDEFINED) {
                return !state.asBoolean();
            } else {
                // fallback to the global perm if the one for the specific command is undefined
                Tristate globalState = sender.getPermissionValue(TRACK_VIEW.apply(targetTrack.getName()));
                return !globalState.asBoolean();
            }
        }

        return false;
    }

    /**
     * Checks if the sender has permission to act within a given set of contexts
     *
     * @param plugin the plugin instance
     * @param sender the sender to check
     * @param base the base permission for the command
     * @param contextSet the contexts the sender is trying to act within
     * @return true if the sender should NOT be allowed to act, true if they should
     */
    public static boolean checkContext(LuckPermsPlugin plugin, Sender sender, CommandPermission base, ContextSet contextSet) {
        if (!plugin.getConfiguration().get(ConfigKeys.USE_ARGUMENT_BASED_COMMAND_PERMISSIONS)) {
            return false;
        }

        if (contextSet.isEmpty()) {
            Tristate state = sender.getPermissionValue(base.getPermission() + ".usecontext.global");
            if (state != Tristate.UNDEFINED) {
                return !state.asBoolean();
            } else {
                // fallback to the global perm if the one for the specific command is undefined
                Tristate globalState = sender.getPermissionValue(CONTEXT_USE_GLOBAL);
                return !globalState.asBoolean();
            }
        }

        for (Context context : contextSet) {
            Tristate state = sender.getPermissionValue(base.getPermission() + ".usecontext." + context.getKey() + "." + context.getValue());
            if (state != Tristate.UNDEFINED) {
                if (state == Tristate.FALSE) {
                    return true;
                }
            } else {
                // fallback to the global perm if the one for the specific command is undefined
                Tristate globalState = sender.getPermissionValue(CONTEXT_USE.apply(context.getKey(), context.getValue()));
                if (globalState == Tristate.FALSE) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if the sender has permission to act using a given group, if holder is a group.
     *
     * @param plugin the plugin instance
     * @param sender the sender to check
     * @param holder the target group (doesn't have to be a group instance - this method checks that)
     * @param contextSet the contexts the sender is trying to act within
     * @return true if the sender should NOT be allowed to act, true if they should
     */
    public static boolean checkGroup(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, ContextSet contextSet) {
        if (holder.getType() == HolderType.GROUP) {
            return checkGroup(plugin, sender, ((Group) holder).getName(), contextSet);
        }
        return false;
    }

    /**
     * Checks if the sender has permission to act using a given group
     *
     * @param plugin the plugin instance
     * @param sender the sender to check
     * @param targetGroupName the target group
     * @param contextSet the contexts the sender is trying to act within
     * @return true if the sender should NOT be allowed to act, true if they should
     */
    public static boolean checkGroup(LuckPermsPlugin plugin, Sender sender, String targetGroupName, ContextSet contextSet) {
        if (!plugin.getConfiguration().get(ConfigKeys.REQUIRE_SENDER_GROUP_MEMBERSHIP_TO_MODIFY)) {
            return false;
        }

        if (sender.isConsole()) {
            return false;
        }

        User user = plugin.getUserManager().getIfLoaded(sender.getUniqueId());
        if (user == null) {
            throw new IllegalStateException("Unable to get a User for " + sender.getUniqueId() + " - " + sender.getName());
        }

        PermissionCache permissionData = user.getCachedData().getPermissionData(QueryOptionsImpl.DEFAULT_CONTEXTUAL.toBuilder().context(contextSet).build());
        TristateResult result = permissionData.checkPermission(Inheritance.key(targetGroupName), CheckOrigin.INTERNAL);
        return result.result() != Tristate.TRUE || result.processorClass() != DirectProcessor.class;
    }
    
}
