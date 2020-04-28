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

package me.lucko.luckperms.common.locale.message;

import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.TextUtils;

import net.kyori.text.TextComponent;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An enumeration of some of the messages used within the plugin.
 *
 * <p>The values in this enum are only defaults, and are only returned if no value for the key is present in the
 * {@link LocaleManager}.</p>
 */
@SuppressWarnings("SpellCheckingInspection")
public enum Message {

    /*
     * General & Commands
     */
    PREFIX("&7&l[&b&lL&3&lP&7&l] ", false),

    VIEW_AVAILABLE_COMMANDS_PROMPT("&3Use &a/{} help &3to view available commands.", true),
    NO_PERMISSION_FOR_SUBCOMMANDS("&3You do not have permission to use any sub commands.", true),
    FIRST_TIME_SETUP(
            "{PREFIX}&3It seems that no permissions have been setup yet!" + "\n" +
            "{PREFIX}&3Before you can use any of the LuckPerms commands in-game, you need to use the console to give yourself access." + "\n" +
            "{PREFIX}&3Open your console and run:" + "\n" +
            "{PREFIX} &3&l> &a{} user {} permission set luckperms.* true" + "\n\n" +
            "{PREFIX}&3After you've done this, you can begin to define your permission assignments and groups." + "\n" +
            "{PREFIX}&3Don't know where to start? Check here: &7https://github.com/lucko/LuckPerms/wiki/Usage",
            false
    ),

    BLANK("{}", true),
    PLAYER_ONLINE("&aOnline", false),
    PLAYER_OFFLINE("&cOffline", false),
    LOADING_DATABASE_ERROR("&cA database error occurred whilst loading permissions data. Please try again later. If you are a server admin, please check the console for any errors.", true),
    LOADING_STATE_ERROR("&cPermissions data for your user was not loaded during the pre-login stage - unable to continue. Please try again later. If you are a server admin, please check the console for any errors.", true),
    LOADING_STATE_ERROR_CB_OFFLINE_MODE("&cPermissions data for your user was not loaded during the pre-login stage - this is likely due to a conflict between CraftBukkit and the online-mode setting. Please check the server console for more information.", true),
    LOADING_SETUP_ERROR("&cAn unexpected error occurred whilst setting up your permissions data. Please try again later.", true),
    OP_DISABLED("&bThe vanilla OP system is disabled on this server.", false),
    OP_DISABLED_SPONGE("&2Please note that Server Operator status has no effect on Sponge permission checks when a permission plugin is installed. Please edit user data directly.", true),


    /*
     * Logging
     */
    LOG(
            "{PREFIX}&3LOG &3&l> &8(&e{}&8) [&a{}&8] (&b{}&8)" + "\n" +
            "{PREFIX}&3LOG &3&l> &f{}",
            false
    ),
    VERBOSE_LOG_PERMISSION("&3VB &3&l> &a{}&7 - &a{}&7 - {}{}", true),
    VERBOSE_LOG_META("&3VB &3&l> &a{}&7 - &bmeta: &a{}&7 - &7{}", true),
    EXPORT_LOG("&3EXPORT &3&l> &f{}", true),
    EXPORT_LOG_PROGRESS("&3EXPORT &3&l> &7{}", true),
    MIGRATION_LOG("&3MIGRATION &7[&3{}&7] &3&l> &f{}", true),
    MIGRATION_LOG_PROGRESS("&3MIGRATION &7[&3{}&7] &3&l> &7{}", true),


    /*
     * Misc commands
     */
    COMMAND_NOT_RECOGNISED("&cCommand not recognised.", true),
    COMMAND_NO_PERMISSION("&cYou do not have permission to use this command!", true),

    MAIN_COMMAND_USAGE_HEADER("&b{} Sub Commands: &7({} ...)", true),
    COMMAND_USAGE_ARGUMENT_JOIN("&3 - &7", false),
    COMMAND_USAGE_BRIEF("&3> &a{}{}", false),
    COMMAND_USAGE_DETAILED_HEADER(
            "{PREFIX}&3&lCommand Usage &3- &b{}" + "\n" +
            "{PREFIX}&b> &7{}",
            false
    ),
    COMMAND_USAGE_DETAILED_ARGS_HEADER("&3Arguments:", true),
    COMMAND_USAGE_DETAILED_ARG("&b- {}&3 -> &7{}", true),
    REQUIRED_ARGUMENT("&8<&7{}&8>", false),
    OPTIONAL_ARGUMENT("&8[&7{}&8]", false),


    /*
     * Loading / Saving
     */
    USER_NOT_FOUND("&cA user for &4{}&c could not be found.", true),
    USER_NOT_ONLINE("&aUser &b{}&a is not online.", true),
    USER_SAVE_ERROR("&cThere was an error whilst saving user data for &4{}&c.", true),

    GROUP_NOT_FOUND("&cA group named &4{}&c could not be found.", true),
    GROUP_SAVE_ERROR("&cThere was an error whilst saving group data for &4{}&c.", true),

    TRACK_NOT_FOUND("&cA track named &4{}&c could not be found.", true),
    TRACK_SAVE_ERROR("&cThere was an error whilst saving track data for &4{}&c.", true),

    USER_INVALID_ENTRY("&4{}&c is not a valid username/uuid.", true),
    GROUP_INVALID_ENTRY("&4{}&c is not a valid group name.", true),
    TRACK_INVALID_ENTRY("&4{}&c is not a valid track name.", true),
    SERVER_WORLD_INVALID_ENTRY("&cServer/world names can only contain alphanumeric characters and cannot exceed 36 characters in length.", true),


    /*
     * Commands
     */
    VERBOSE_INVALID_FILTER("&4{}&c is not a valid verbose filter. &7({})", true),
    VERBOSE_ON("&bVerbose logging &aenabled &bfor checks matching &aANY&b.", true),
    VERBOSE_ON_QUERY("&bVerbose logging &aenabled &bfor checks matching &a{}&b.", true),
    VERBOSE_OFF("&bVerbose logging &cdisabled&b.", true),

    VERBOSE_RECORDING_ON("&bVerbose recording &aenabled &bfor checks matching &aANY&b.", true),
    VERBOSE_RECORDING_ON_QUERY("&bVerbose recording &aenabled &bfor checks matching &a{}&b.", true),
    VERBOSE_UPLOAD_START("&bVerbose logging &cdisabled&b. Uploading results...", true),
    VERBOSE_RESULTS_URL("&aVerbose results URL:", true),

    TREE_UPLOAD_START("&bGenerating permission tree...", true),
    TREE_EMPTY("&cUnable to generate tree. No results were found.", true),
    TREE_URL("&aPermission tree URL:", true),

    GENERIC_HTTP_REQUEST_FAILURE("&cUnable to communicate with the web app. (response code &4{}&c, message='{}')", true),
    GENERIC_HTTP_UNKNOWN_FAILURE("&cUnable to communicate with the web app. Check the console for errors.", true),

    SEARCH_SEARCHING("&aSearching for users and groups with &bpermissions {}&a...", true),
    SEARCH_SEARCHING_MEMBERS("&aSearching for users and groups who inherit from &b{}&a...", true),
    SEARCH_RESULT("&aFound &b{}&a entries from &b{}&a users and &b{}&a groups.", true),

    SEARCH_SHOWING_USERS("&bShowing user entries:    &7(page &f{}&7 of &f{}&7 - &f{}&7 entries)", true),
    SEARCH_SHOWING_GROUPS("&bShowing group entries:    &7(page &f{}&7 of &f{}&7 - &f{}&7 entries)", true),

    APPLY_EDITS_INVALID_CODE("&cInvalid code. &7({})", true),
    APPLY_EDITS_UNABLE_TO_READ("&cUnable to read data using the given code. &7({})", true),
    APPLY_EDITS_UNKNOWN_TYPE("&cUnable to apply edit to the specified object type. &7({})", true),
    APPLY_EDITS_TARGET_USER_NOT_UUID("&cTarget user &4{}&c is not a valid uuid.", true),
    APPLY_EDITS_TARGET_USER_UNABLE_TO_LOAD("&cUnable to load target user &4{}&c.", true),
    APPLY_EDITS_TARGET_UNKNOWN("&cInvalid target. &7({})", true),
    APPLY_EDITS_TARGET_NO_CHANGES_PRESENT("&aNo changes were applied from the web editor. The returned data didn't contain any edits.", true),
    APPLY_EDITS_SUCCESS("&aWeb editor data was applied to {} &b{}&a successfully.", true),
    APPLY_EDITS_SUCCESS_SUMMARY("&7(&a{} &7{} and &c{} &7{})", true),
    APPLY_EDITS_DIFF_ADDED("&a+  &f{}", false),
    APPLY_EDITS_DIFF_REMOVED("&c-  &f{}", false),

    EDITOR_NO_MATCH("&cUnable to open editor. No objects matched the desired type.", true),
    EDITOR_START("&7Preparing a new editor session. Please wait...", true),
    EDITOR_URL("&aClick the link below to open the editor:", true),

    EDITOR_HTTP_REQUEST_FAILURE("&cUnable to communicate with the editor. (response code &4{}&c, message='{}')", true),
    EDITOR_HTTP_UNKNOWN_FAILURE("&cUnable to communicate with the editor. Check the console for errors.", true),

    CHECK_RESULT("&aPermission check result on user &b{}&a for permission &b{}&a: &f{}", true),

    CREATE_SUCCESS("&b{}&a was successfully created.", true),
    DELETE_SUCCESS("&b{}&a was successfully deleted.", true),
    RENAME_SUCCESS("&b{}&a was successfully renamed to &b{}&a.", true),
    CLONE_SUCCESS("&b{}&a was successfully cloned onto &b{}&a.", true),

    ALREADY_INHERITS("&b{}&a already inherits from &b{}&a in context {}&a.", true),
    DOES_NOT_INHERIT("&b{}&a does not inherit from &b{}&a in context {}&a.", true),
    ALREADY_TEMP_INHERITS("&b{}&a already temporarily inherits from &b{}&a in context {}&a.", true),
    DOES_NOT_TEMP_INHERIT("&b{}&a does not temporarily inherit from &b{}&a in context {}&a.", true),

    TRACK_ALREADY_CONTAINS("&b{}&a already contains &b{}&a.", true),
    TRACK_DOES_NOT_CONTAIN("&b{}&a doesn't contain &b{}&a.", true),
    TRACK_AMBIGUOUS_CALL("&4{}&c is a member of multiple groups on this track. Unable to determine their location.", true),

    ALREADY_EXISTS("&4{}&c already exists!", true),
    DOES_NOT_EXIST("&4{}&c does not exist!", true),

    USER_LOAD_ERROR("&cAn unexpected error occurred. User not loaded.", true),
    GROUP_LOAD_ERROR("&cAn unexpected error occurred. Group not loaded.", true),
    GROUPS_LOAD_ERROR("&cAn unexpected error occurred. Unable to load all groups.", true),

    TRACK_LOAD_ERROR("&cAn unexpected error occurred. Track not loaded.", true),
    TRACKS_LOAD_ERROR("&cAn unexpected error occurred. Unable to load all tracks.", true),
    TRACK_EMPTY("&4{}&c cannot be used as it is empty or contains only one group.", true),

    UPDATE_TASK_REQUEST("&bAn update task has been requested. Please wait...", true),
    UPDATE_TASK_COMPLETE("&aUpdate task complete.", true),
    UPDATE_TASK_COMPLETE_NETWORK("&aUpdate task complete. Now attempting to push to other servers.", true),
    UPDATE_TASK_PUSH_SUCCESS("&aOther servers were notified via &b{} Messaging &asuccessfully.", true),
    UPDATE_TASK_PUSH_FAILURE("&cError whilst pushing changes to other servers.", true),
    UPDATE_TASK_PUSH_FAILURE_NOT_SETUP("&cError whilst pushing changes to other servers. &7(a messaging service has not been configured)", true),
    RELOAD_CONFIG_SUCCESS("&aThe configuration file was reloaded. &7(some options will only apply after the server has restarted)", true),
    INFO_TOP(
            "{PREFIX}&2Running &bLuckPerms v{}&2 by &bLuck&2." + "\n" +
            "{PREFIX}&f-  &3Platform: &f{}" + "\n" +
            "{PREFIX}&f-  &3Server Brand: &f{}" + "\n" +
            "{PREFIX}&f-  &3Server Version:" + "\n" +
            "{PREFIX}&f-  {}",
            false
    ),

    INFO_STORAGE(
            "{PREFIX}&f-  &bStorage:" + "\n" +
            "{PREFIX}&f-     &3Type: &f{}",
            false
    ),

    INFO_STORAGE_META("&f-     &3{}: {}", true),

    INFO_EXTENSIONS("{PREFIX}&f-  &bExtensions:", true),
    INFO_EXTENSION_ENTRY("&f-     &3{}", true),

    INFO_MIDDLE(
            "{PREFIX}&f-  &bMessaging: &f{}" + "\n" +
            "{PREFIX}&f-  &bInstance:" + "\n" +
            "{PREFIX}&f-     &3Static contexts: &f{}" + "\n" +
            "{PREFIX}&f-     &3Online Players: &a{} &7(&a{}&7 unique)" + "\n" +
            "{PREFIX}&f-     &3Uptime: &7{}" + "\n" +
            "{PREFIX}&f-     &3Local Data: &a{} &7users, &a{} &7groups, &a{} &7tracks",
            false
    ),

    DEBUG_START("&bGenerating debugging output...", true),
    DEBUG_URL("&aDebug data URL:", true),

    CREATE_ERROR("&cThere was an error whilst creating &4{}&c.", true),
    DELETE_ERROR("&cThere was an error whilst deleting &4{}&c.", true),

    DELETE_GROUP_ERROR_DEFAULT("&cYou cannot delete the default group.", true),

    GROUPS_LIST("&aGroups: &7(name, weight, tracks)", true),
    GROUPS_LIST_ENTRY("&f-  &3{} &7- &b{}", true),
    GROUPS_LIST_ENTRY_WITH_TRACKS("&f-  &3{} &7- &b{} &7- [&3{}&7]", true),

    TRACKS_LIST("&aTracks: {}", true),

    PERMISSION_INFO("&b{}'s Permissions:  &7(page &f{}&7 of &f{}&7 - &f{}&7 entries)", true),
    PERMISSION_INFO_NO_DATA("&b{}&a does not have any permissions set.", true),

    PARENT_INFO("&b{}'s Parents:  &7(page &f{}&7 of &f{}&7 - &f{}&7 entries)", true),
    PARENT_INFO_NO_DATA("&b{}&a does not have any parents defined.", true),

    LIST_TRACKS("&b{}'s Tracks:", true),
    LIST_TRACKS_ENTRY("&a{}: {}", false),
    LIST_TRACKS_EMPTY("&b{}&a is not on any tracks.", true),

    CONTEXT_PAIR_INLINE("&3{}=&b{}", false),
    CONTEXT_PAIR_GLOBAL_INLINE("&eglobal", false),
    CONTEXT_PAIR_SEP("&a, ", false),

    CONTEXT_PAIR("&8(&7{}=&f{}&8)", false),

    CHECK_PERMISSION("&b{}&a has permission &b{}&a set to {}&a in context {}&a.", true),
    CHECK_INHERITS_PERMISSION("&b{}&a has permission &b{}&a set to {}&a in context {}&a. &7(inherited from &a{}&7)", true),

    SETPERMISSION_SUCCESS("&aSet &b{}&a to &b{}&a for &b{}&a in context {}&a.", true),
    ALREADY_HASPERMISSION("&b{}&a already has &b{}&a set in context {}&a.", true),

    SETPERMISSION_TEMP_SUCCESS("&aSet &b{}&a to &b{}&a for &b{}&a for a duration of &b{}&a in context {}&a.", true),
    ALREADY_HAS_TEMP_PERMISSION("&b{}&a already has &b{}&a set temporarily in context {}&a.", true),

    UNSETPERMISSION_SUCCESS("&aUnset &b{}&a for &b{}&a in context {}&a.", true),
    DOES_NOT_HAVE_PERMISSION("&b{}&a does not have &b{}&a set in context {}&a.", true),

    UNSET_TEMP_PERMISSION_SUCCESS("&aUnset temporary permission &b{}&a for &b{}&a in context {}&a.", true),
    DOES_NOT_HAVE_TEMP_PERMISSION("&b{}&a does not have &b{}&a set temporarily in context {}&a.", true),

    SET_INHERIT_SUCCESS("&b{}&a now inherits permissions from &b{}&a in context {}&a.", true),
    SET_TEMP_INHERIT_SUCCESS("&b{}&a now inherits permissions from &b{}&a for a duration of &b{}&a in context {}&a.", true),
    SET_PARENT_SUCCESS("&b{}&a had their existing parent groups cleared, and now only inherits &b{}&a in context {}&a.", true),
    SET_TRACK_PARENT_SUCCESS("&b{}&a had their existing parent groups on track &b{}&a cleared, and now only inherits &b{}&a in context {}&a.", true),
    UNSET_INHERIT_SUCCESS("&b{}&a no longer inherits permissions from &b{}&a in context {}&a.", true),
    UNSET_TEMP_INHERIT_SUCCESS("&b{}&a no longer temporarily inherits permissions from &b{}&a in context {}&a.", true),

    CLEAR_SUCCESS("&b{}&a's nodes were cleared in context {}&a. (&b{}&a nodes were removed.)", true),
    CLEAR_SUCCESS_SINGULAR("&b{}&a's nodes were cleared in context {}&a. (&b{}&a node was removed.)", true),
    PERMISSION_CLEAR_SUCCESS("&b{}&a's permissions were cleared in context {}&a. (&b{}&a nodes were removed.)", true),
    PERMISSION_CLEAR_SUCCESS_SINGULAR("&b{}&a's permissions were cleared in context {}&a. (&b{}&a node was removed.)", true),
    PARENT_CLEAR_SUCCESS("&b{}&a's parents were cleared in context {}&a. (&b{}&a nodes were removed.)", true),
    PARENT_CLEAR_SUCCESS_SINGULAR("&b{}&a's parents were cleared in context {}&a. (&b{}&a node was removed.)", true),

    PARENT_CLEAR_TRACK_SUCCESS("&b{}&a's parents on track &b{}&a were cleared in context {}&a. (&b{}&a nodes were removed.)", true),
    PARENT_CLEAR_TRACK_SUCCESS_SINGULAR("&b{}&a's parents on track &b{}&a were cleared in context {}&a. (&b{}&a node was removed.)", true),

    META_CLEAR_SUCCESS("&b{}&a's meta matching type &b{}&a was cleared in context {}&a. (&b{}&a nodes were removed.)", true),
    META_CLEAR_SUCCESS_SINGULAR("&b{}&a's meta matching type &b{}&a was cleared in context {}&a. (&b{}&a node was removed.)", true),

    ILLEGAL_DATE_ERROR("&cCould not parse date &4{}&c.", true),
    PAST_DATE_ERROR("&cYou cannot set a date in the past!", true),

    CHAT_META_PREFIX_HEADER("&b{}'s Prefixes", true),
    CHAT_META_SUFFIX_HEADER("&b{}'s Suffixes", true),
    META_HEADER("&b{}'s Meta", true),
    CHAT_META_ENTRY("&b-> {} &f- &f'{}&f' &8(&7inherited from &a{}&8)", true),
    CHAT_META_ENTRY_WITH_CONTEXT("&b-> {} &f- &f'{}&f' &8(&7inherited from &a{}&8){}", true),
    META_ENTRY("&b-> &a{} &f= &f'{}&f' &8(&7inherited from &a{}&8)", true),
    META_ENTRY_WITH_CONTEXT("&b-> &a{} &f= &f'{}&f' &8(&7inherited from &a{}&8){}", true),
    CHAT_META_PREFIX_NONE("&b{} has no prefixes.", true),
    CHAT_META_SUFFIX_NONE("&b{} has no suffixes.", true),
    META_NONE("&b{} has no meta.", true),

    META_INVALID_PRIORITY("&cInvalid priority &4{}&c. Expected a number.", true),

    ALREADY_HAS_CHAT_META("&b{}&a already has {} &f'{}&f'&a set at a priority of &b{}&a in context {}&a.", true),
    ALREADY_HAS_TEMP_CHAT_META("&b{}&a already has {} &f'{}&f'&a set temporarily at a priority of &b{}&a in context {}&a.", true),

    DOES_NOT_HAVE_CHAT_META("&b{}&a doesn't have {} &f'{}&f'&a set at a priority of &b{}&a in context {}&a.", true),
    DOES_NOT_HAVE_TEMP_CHAT_META("&b{}&a doesn't have {} &f'{}&f'&a set temporarily at a priority of &b{}&a in context {}&a.", true),

    ADD_CHATMETA_SUCCESS("&b{}&a had {} &f'{}&f'&a set at a priority of &b{}&a in context {}&a.", true),
    ADD_TEMP_CHATMETA_SUCCESS("&b{}&a had {} &f'{}&f'&a set at a priority of &b{}&a for a duration of &b{}&a in context {}&a.", true),
    REMOVE_CHATMETA_SUCCESS("&b{}&a had {} &f'{}&f'&a at priority &b{}&a removed in context {}&a.", true),
    BULK_REMOVE_CHATMETA_SUCCESS("&b{}&a had all {}es at priority &b{}&a removed in context {}&a.", true),
    REMOVE_TEMP_CHATMETA_SUCCESS("&b{}&a had temporary {} &f'{}&f'&a at priority &b{}&a removed in context {}&a.", true),
    BULK_REMOVE_TEMP_CHATMETA_SUCCESS("&b{}&a had all temporary {}es at priority &b{}&a removed in context {}&a.", true),

    ALREADY_HAS_META("&b{}&a already has meta key &f'{}&f'&a set to &f'{}&f'&a in context {}&a.", true),
    ALREADY_HAS_TEMP_META("&b{}&a already has meta key &f'{}&f'&a temporarily set to &f'{}&f'&a in context {}&a.", true),

    DOESNT_HAVE_META("&b{}&a doesn't have meta key &f'{}&f'&a set in context {}&a.", true),
    DOESNT_HAVE_TEMP_META("&b{}&a doesn't have meta key &f'{}&f'&a set temporarily in context {}&a.", true),

    SET_META_SUCCESS("&aSet meta key &f'{}&f'&a to &f'{}&f'&a for &b{}&a in context {}&a.", true),
    SET_META_TEMP_SUCCESS("&aSet meta key &f'{}&f'&a to &f'{}&f'&a for &b{}&a for a duration of &b{}&a in context {}&a.", true),
    UNSET_META_SUCCESS("&aUnset meta key &f'{}&f'&a for &b{}&a in context {}&a.", true),
    UNSET_META_TEMP_SUCCESS("&aUnset temporary meta key &f'{}&f'&a for &b{}&a in context {}&a.", true),

    BULK_UPDATE_MUST_USE_CONSOLE("&cThe bulk update command can only be used from the console.", true),
    BULK_UPDATE_INVALID_DATA_TYPE("&cInvalid type. Was expecting 'all', 'users' or 'groups'.", true),
    BULK_UPDATE_INVALID_CONSTRAINT("&cInvalid constraint &4{}&c. Constraints should be in the format '&f<field> <comparison operator> <value>&c'.", true),
    BULK_UPDATE_INVALID_COMPARISON("&cInvalid comparison operator '&4{}&c'. Expected one of the following: &f==  !=  ~~  ~!", true),
    BULK_UPDATE_QUEUED("&aBulk update operation was queued. &7(&f{}&7)", true),
    BULK_UPDATE_CONFIRM("&aRun &b/{} bulkupdate confirm {} &ato execute the update.", true),
    BULK_UPDATE_UNKNOWN_ID("&aOperation with id &b{}&a does not exist or has expired.", true),

    BULK_UPDATE_STARTING("&aRunning bulk update.", true),
    BULK_UPDATE_SUCCESS("&bBulk update completed successfully.", true),
    BULK_UPDATE_FAILURE("&cBulk update failed. Check the console for errors.", true),

    USER_INFO_GENERAL(
            "{PREFIX}&b&l> &bUser Info: &f{}" + "\n" +
            "{PREFIX}&f- &3UUID: &f{} &7(type: {}&7)" + "\n" +
            "{PREFIX}&f- &3Status: {}" + "\n" +
            "{PREFIX}&f- &3Primary Group: &f{}",
            false
    ),

    USER_INFO_DATA(
            "{PREFIX}&f- &aContextual Data:" + "\n" +
            "{PREFIX}&f-    &3Has contextual data: {}" + "\n" +
            "{PREFIX}&f-    &3Applicable contexts: {}" + "\n" +
            "{PREFIX}&f-    &3Prefix: {}" + "\n" +
            "{PREFIX}&f-    &3Suffix: {}" + "\n" +
            "{PREFIX}&f-    &3Meta: {}",
            false
    ),

    INFO_PARENT_HEADER("&f- &aParent Groups:", true),
    INFO_TEMP_PARENT_HEADER("&f- &aTemporary Parent Groups:", true),
    INFO_PARENT_ENTRY("&f-    &3> &f{}{}", true),
    INFO_PARENT_ENTRY_EXPIRY("&f-    &2-    expires in {}", true),
    USER_REMOVEGROUP_ERROR_PRIMARY("&aYou cannot remove a user from their primary group.", true),
    USER_PRIMARYGROUP_SUCCESS("&b{}&a's primary group was set to &b{}&a.", true),
    USER_PRIMARYGROUP_WARN_OPTION("&aWarning: The primary group calculation method being used by this server &7({}) &amay not reflect this change.", true),
    USER_PRIMARYGROUP_ERROR_ALREADYHAS("&b{}&a already has &b{}&a set as their primary group.", true),
    USER_PRIMARYGROUP_ERROR_NOTMEMBER("&b{}&a was not already a member of &b{}&a, adding them now.", true),
    USER_TRACK_ERROR_NOT_CONTAIN_GROUP("&b{}&a isn't already in any groups on &b{}&a.", true),

    USER_TRACK_ADDED_TO_FIRST("&b{}&a isn't in any groups on this track, so they were added to the first group, &b{}&a in context {}&a.", true),
    USER_PROMOTE_NOT_ON_TRACK("&b{}&a isn't in any groups on this track, so was not promoted.", true),
    USER_PROMOTE_SUCCESS("&aPromoting &b{}&a along track &b{}&a from &b{}&a to &b{}&a in context {}&a.", true),

    USER_PROMOTE_ERROR_ENDOFTRACK("&aThe end of track &b{}&a was reached. Unable to promote &b{}&a.", true),
    USER_PROMOTE_ERROR_MALFORMED(
            "{PREFIX}&aThe next group on the track, &b{}&a, no longer exists. Unable to promote user." + "\n" +
            "{PREFIX}&aEither create the group, or remove it from the track and try again.",
            false
    ),
    USER_DEMOTE_SUCCESS("&aDemoting &b{}&a along track &b{}&a from &b{}&a to &b{}&a in context {}&a.", true),
    USER_DEMOTE_ENDOFTRACK("&aThe end of track &b{}&a was reached, so &b{}&a was removed from &b{}&a.", true),
    USER_DEMOTE_ENDOFTRACK_NOT_REMOVED("&aThe end of track &b{}&a was reached, but &b{}&a was not removed from the first group.", true),
    USER_DEMOTE_ERROR_MALFORMED(
            "{PREFIX}&aThe previous group on the track, &b{}&a, no longer exists. Unable to demote user." + "\n" +
            "{PREFIX}&aEither create the group, or remove it from the track and try again.",
            false
    ),

    GROUP_INFO_GENERAL(
            "{PREFIX}&b&l> &bGroup Info: &f{}" + "\n" +
            "{PREFIX}&f- &3Display Name: &f{}" + "\n" +
            "{PREFIX}&f- &3Weight: &f{}",
            false
    ),
    GROUP_SET_WEIGHT("&aSet weight to &b{}&a for group &b{}&a.", true),

    GROUP_SET_DISPLAY_NAME_DOESNT_HAVE("&b{}&a doesn't have a display name set.", true),
    GROUP_SET_DISPLAY_NAME_ALREADY_HAS("&b{}&a already has a display name of &b{}&a.", true),
    GROUP_SET_DISPLAY_NAME_ALREADY_IN_USE("&aThe display name &b{}&a is already being used by &b{}&a.", true),
    GROUP_SET_DISPLAY_NAME("&aSet display name to &b{}&a for group &b{}&a in context {}&a.", true),
    GROUP_SET_DISPLAY_NAME_REMOVED("&aRemoved display name for group &b{}&a in context {}&a.", true),

    TRACK_INFO(
            "{PREFIX}&b&l> &bShowing Track: &f{}" + "\n" +
            "{PREFIX}&f- &7Path: &f{}",
            false
    ),
    TRACK_CLEAR("&b{}&a's groups track was cleared.", true),
    TRACK_APPEND_SUCCESS("&aGroup &b{}&a was appended to track &b{}&a.", true),
    TRACK_INSERT_SUCCESS("&aGroup &b{}&a was inserted into track &b{}&a at position &b{}&a.", true),
    TRACK_INSERT_ERROR_NUMBER("&cExpected number but instead received: {}", true),
    TRACK_INSERT_ERROR_INVALID_POS("&cUnable to insert at position &4{}&c. &7(invalid position)", true),
    TRACK_REMOVE_SUCCESS("&aGroup &b{}&a was removed from track &b{}&a.", true),

    LOG_LOAD_ERROR("&cThe log could not be loaded.", true),
    LOG_INVALID_PAGE("&cInvalid page number.", true),
    LOG_INVALID_PAGE_RANGE("&cInvalid page number. Please enter a value between &41&c and &4{}&c.", true),
    LOG_NO_ENTRIES("&bNo log entries to show.", true),

    LOG_ENTRY(
            "{PREFIX}&b#{} &8(&7{} ago&8) &8(&e{}&8) [&a{}&8] (&b{}&8)" + "\n" +
            "{PREFIX}&7> &f{}",
            false
    ),

    LOG_NOTIFY_CONSOLE("&cCannot toggle notifications for console.", true),
    LOG_NOTIFY_TOGGLE_ON("&aEnabled&b logging output.", true),
    LOG_NOTIFY_TOGGLE_OFF("&cDisabled&b logging output.", true),
    LOG_NOTIFY_ALREADY_ON("&cYou are already receiving notifications.", true),
    LOG_NOTIFY_ALREADY_OFF("&cYou aren't currently receiving notifications.", true),
    LOG_NOTIFY_UNKNOWN("&cState unknown. Expecting \"on\" or \"off\".", true),

    LOG_SEARCH_HEADER("&aShowing recent actions for query &b{}  &7(page &f{}&7 of &f{}&7)", true),
    LOG_RECENT_HEADER("&aShowing recent actions  &7(page &f{}&7 of &f{}&7)", true),
    LOG_RECENT_BY_HEADER("&aShowing recent actions by &b{}  &7(page &f{}&7 of &f{}&7)", true),
    LOG_HISTORY_USER_HEADER("&aShowing history for user &b{}  &7(page &f{}&7 of &f{}&7)", true),
    LOG_HISTORY_GROUP_HEADER("&aShowing history for group &b{}  &7(page &f{}&7 of &f{}&7)", true),
    LOG_HISTORY_TRACK_HEADER("&aShowing history for track &b{}  &7(page &f{}&7 of &f{}&7)", true),

    LOG_EXPORT_ALREADY_EXISTS("&cError: File &4{}&c already exists.", true),
    LOG_EXPORT_NOT_WRITABLE("&cError: File &4{}&c is not writable.", true),
    LOG_EXPORT_EMPTY("&cThe log is empty and therefore cannot be exported.", true),
    LOG_EXPORT_FAILURE("&cAn unexpected error occured whilst writing to the file.", true),
    LOG_EXPORT_SUCCESS("&aSuccessfully exported to &b{}&a.", true),

    IMPORT_ALREADY_RUNNING("&cAnother import process is already running. Please wait for it to finish and try again.", true),
    EXPORT_ALREADY_RUNNING("&cAnother export process is already running. Please wait for it to finish and try again.", true),
    FILE_NOT_WITHIN_DIRECTORY("&cError: File &4{}&c must be a direct child of the data directory.", true),
    IMPORT_FILE_DOESNT_EXIST("&cError: File &4{}&c does not exist.", true),
    IMPORT_FILE_NOT_READABLE("&cError: File &4{}&c is not readable.", true),
    IMPORT_FILE_READ_FAILURE("&cAn unexpected error occured whilst reading from the import file. (is it the correct format?)", true),

    IMPORT_PROGRESS("&b(Import) &b-> &f{}&f% complete &7- &b{}&f/&b{} &foperations complete with &c{} &ferrors.", true),
    IMPORT_PROGRESS_SIN("&b(Import) &b-> &f{}&f% complete &7- &b{}&f/&b{} &foperations complete with &c{} &ferror.", true),
    IMPORT_START("&b(Import) &b-> &fStarting import process.", true),
    IMPORT_INFO("&b(Import) &b-> &f{}.", true),

    IMPORT_END_COMPLETE("&b(Import) &a&lCOMPLETED &7- took &b{} &7seconds - &7No errors.", true),
    IMPORT_END_COMPLETE_ERR("&b(Import) &a&lCOMPLETED &7- took &b{} &7seconds - &c{} errors.", true),
    IMPORT_END_COMPLETE_ERR_SIN("&b(Import) &a&lCOMPLETED &7- took &b{} &7seconds - &c{} error.", true),
    IMPORT_END_ERROR_HEADER(
            "{PREFIX}&b(Import) &7------------> &fShowing Error #&b{} &7<------------" + "\n" +
            "{PREFIX}&b(Import) &fWhilst executing: &3Command #{}" + "\n" +
            "{PREFIX}&b(Import) &fCommand: &7{}" + "\n" +
            "{PREFIX}&b(Import) &fType: &3{}" + "\n" +
            "{PREFIX}&b(Import) &fOutput:",
            false
    ),

    IMPORT_END_ERROR_CONTENT("&b(Import) &b-> &c{}", true),
    IMPORT_END_ERROR_FOOTER("&b(Import) &7<------------------------------------------>", true);

    private final String message;
    private final boolean showPrefix;

    Message(String message, boolean showPrefix) {
        // rewrite hardcoded placeholders according to their position
        this.message = TextUtils.rewritePlaceholders(message);
        this.showPrefix = showPrefix;
    }

    public String getMessage() {
        return this.message;
    }

    private String getTranslatedMessage(@Nullable LocaleManager localeManager) {
        String message = null;
        if (localeManager != null) {
            message = localeManager.getTranslation(this);
        }
        if (message == null) {
            message = this.getMessage();
        }
        return message;
    }

    private String format(@Nullable LocaleManager localeManager, Object... objects) {
        String prefix = PREFIX.getTranslatedMessage(localeManager);
        String msg = format(
                this.getTranslatedMessage(localeManager)
                        .replace("{PREFIX}", prefix)
                        .replace("\\n", "\n"),
                objects
        );
        return this.showPrefix ? prefix + msg : msg;
    }

    public String asString(@Nullable LocaleManager localeManager, Object... objects) {
        return colorize(format(localeManager, objects));
    }

    public TextComponent asComponent(@Nullable LocaleManager localeManager, Object... objects) {
        return TextUtils.fromLegacy(format(localeManager, objects), TextUtils.AMPERSAND_CHAR);
    }

    public void send(Sender sender, Object... objects) {
        sender.sendMessage(asString(sender.getPlugin().getLocaleManager(), objects));
    }

    private static String format(String s, Object... objects) {
        for (int i = 0; i < objects.length; i++) {
            Object o = objects[i];
            s = s.replace("{" + i + "}", String.valueOf(o));
        }
        return s;
    }

    /**
     * Colorizes a message.
     *
     * @param s the message to colorize
     * @return a colored message
     */
    public static String colorize(String s) {
        char[] b = s.toCharArray();

        for (int i = 0; i < b.length - 1; ++i) {
            if (b[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(b[i + 1]) > -1) {
                b[i] = 167;
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }

        return new String(b);
    }

    /*
    public static void main(String[] args) {
        for (Message message : values()) {
            String key = message.name().replace('_', '-').toLowerCase();
            String value = message.message;

            if (!value.contains("\n")) {
                System.out.println(key + ": \"" + value.replace("\"", "\\\"") + "\"");
            } else {
                System.out.println(key + ": >");
                String[] parts = value.split("\n");

                for (int i = 0; i < parts.length; i++) {
                    String s = parts[i].replace("\"", "\\\"");
                    System.out.println("  " + s + (i == (parts.length - 1) ? "" : "\\n"));
                }
            }
        }
    }
    */

}
