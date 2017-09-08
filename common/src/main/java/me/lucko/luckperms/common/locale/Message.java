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

package me.lucko.luckperms.common.locale;

import lombok.AllArgsConstructor;
import lombok.Getter;

import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.Util;

/**
 * An enumeration of some of the messages used within the plugin.
 *
 * <p>The values in this enum are only defaults, and are only returned if no value for the key is present in the
 * {@link LocaleManager}.</p>
 */
@SuppressWarnings("SpellCheckingInspection")
@AllArgsConstructor
public enum Message {

    /*
     * General & Commands
     */
    PREFIX("&7&l[&b&lL&3&lP&7&l] &c", false),

    LOG_INFO("&7&l[&bL&3P&7&l] &3{0}", false),
    LOG_WARN("&7&l[&bLuck&3Perms&7&l] &c[WARN] {0}", false),
    LOG_ERROR("&7&l[&bLuck&3Perms&7&l] &4[ERROR] {0}", false),

    EMPTY("{0}", true),
    PLAYER_ONLINE("&aOnline", false),
    PLAYER_OFFLINE("&cOffline", false),
    LOADING_ERROR("Permissions data could not be loaded. Please try again later.", true),
    OP_DISABLED("&bThe vanilla OP system is disabled on this server.", false),
    OP_DISABLED_SPONGE("&2Server Operator status has no effect when a permission plugin is installed. Please edit user data directly.", true),
    LOG("&3LOG &3&l> {0}", true),
    VERBOSE_LOG("&3VERBOSE &3&l> {0}", true),

    EXPORT_LOG("&3EXPORT &3&l> &f{0}", true),
    EXPORT_LOG_PROGRESS("&3EXPORT &3&l> &7{0}", true),

    MIGRATION_LOG("&3MIGRATION &7[&3{0}&7] &3&l> &f{1}", true),
    MIGRATION_LOG_PROGRESS("&3MIGRATION &7[&3{0}&7] &3&l> &7{1}", true),

    COMMAND_NOT_RECOGNISED("Command not recognised.", true),
    COMMAND_NO_PERMISSION("You do not have permission to use this command!", true),

    ALREADY_HASPERMISSION("{0} already has this permission!", true),
    DOES_NOT_HAVEPERMISSION("{0} does not have this permission set.", true),
    ALREADY_HAS_TEMP_PERMISSION("{0} already has this permission set temporarily!", true),
    DOES_NOT_HAVE_TEMP_PERMISSION("{0} does not have this permission set temporarily.", true),


    /*
     * Loading / Saving
     */
    USER_NOT_FOUND("&bUser could not be found.", true),
    USER_NOT_ONLINE("&bUser &a{0}&b is not online.", true),
    USER_SAVE_ERROR("There was an error whilst saving the user.", true),

    GROUP_NOT_FOUND("&bGroup could not be found.", true),
    GROUP_SAVE_ERROR("There was an error whilst saving the group.", true),

    TRACK_NOT_FOUND("&bTrack could not be found.", true),
    TRACK_SAVE_ERROR("There was an error whilst saving the track.", true),


    /*
     * Command Syntax
     */
    USER_INVALID_ENTRY("&d{0}&c is not a valid username/uuid.", true),
    GROUP_INVALID_ENTRY("Group names can only contain alphanumeric characters.", true),
    TRACK_INVALID_ENTRY("Track names can only contain alphanumeric characters.", true),
    SERVER_WORLD_INVALID_ENTRY("Server/world names can only contain alphanumeric characters and cannot exceed 36 characters in length.", true),
    USE_INHERIT_COMMAND("Use the 'parent add' and 'parent remove' commands instead of specifying the node.", true),


    /*
     * Commands
     */
    VERBOSE_INVALID_FILTER("&cInvalid verbose filter: &f{0}", true),
    VERBOSE_ON("&bVerbose checking output set to &aTRUE &bfor all permissions.", true),
    VERBOSE_ON_QUERY("&bVerbose checking output set to &aTRUE &bfor permissions matching filter: &f{0}", true),
    VERBOSE_OFF("&bVerbose checking output set to &cFALSE&b.", true),

    VERBOSE_RECORDING_ON("&bVerbose recording set to &aTRUE &bfor all permissions.", true),
    VERBOSE_RECORDING_ON_QUERY("&bVerbose recording set to &aTRUE &bfor permissions matching filter: &f{0}", true),
    VERBOSE_RECORDING_UPLOAD_START("&bVerbose recording was disabled. Uploading results...", true),
    VERBOSE_RECORDING_URL("&aVerbose results URL:", true),

    TREE_UPLOAD_START("&bGenerating permission tree...", true),
    TREE_EMPTY("&aUnable to generate tree. No results were found.", true),
    TREE_URL("&aPermission Tree URL:", true),

    SEARCH_SEARCHING("&aSearching for users and groups with &b{0}&a...", true),
    SEARCH_SEARCHING_MEMBERS("&aSearching for users and groups who inherit from &b{0}&a...", true),
    SEARCH_RESULT("&aFound &b{0}&a entries from &b{1}&a users and &b{2}&a groups.", true),
    SEARCH_SHOWING_USERS("&bShowing user entries:", true),
    SEARCH_SHOWING_GROUPS("&bShowing group entries:", true),
    SEARCH_SHOWING_USERS_WITH_PAGE("&bShowing user entries:  {0}", true),
    SEARCH_SHOWING_GROUPS_WITH_PAGE("&bShowing group entries:  {0}", true),

    APPLY_EDITS_INVALID_CODE("&aInvalid code. &7({0})", true),
    APPLY_EDITS_UNABLE_TO_READ("&aUnable to read data using the given code. &7({0})", true),
    APPLY_EDITS_NO_TARGET("&aUnable to parse the target of the edit. Please supply it as an extra argument.", true),
    APPLY_EDITS_TARGET_GROUP_NOT_EXISTS("&aTarget group &b{0}&a does not exist.", true),
    APPLY_EDITS_TARGET_USER_NOT_UUID("&aTarget user &b{0}&a is not a valid uuid.", true),
    APPLY_EDITS_TARGET_USER_UNABLE_TO_LOAD("&aUnable to load target user &b{0}&a.", true),
    APPLY_EDITS_TARGET_UNKNOWN("&aInvalid target. &7({0})", true),
    APPLY_EDITS_SUCCESS("&aSuccessfully applied &b{0}&a nodes to &b{1}&a.", true),

    EDITOR_UPLOAD_FAILURE("&cUnable to upload permission data to the editor.", true),
    EDITOR_URL("&aEditor URL:", true),

    CHECK_RESULT("&aPermission check result on user &b{0}&a for permission &b{1}&a: &f{2}", true),

    CREATE_SUCCESS("&b{0}&a was successfully created.", true),
    DELETE_SUCCESS("&b{0}&a was successfully deleted.", true),
    RENAME_SUCCESS("&b{0}&a was successfully renamed to &b{1}&a.", true),
    CLONE_SUCCESS("&b{0}&a was successfully cloned onto &b{1}&a.", true),

    ALREADY_INHERITS("{0} already inherits '{1}'.", true),
    DOES_NOT_INHERIT("{0} does not inherit '{1}'.", true),
    ALREADY_TEMP_INHERITS("{0} already temporarily inherits '{1}'.", true),
    DOES_NOT_TEMP_INHERIT("{0} does not temporarily inherit '{1}'.", true),

    TRACK_ALREADY_CONTAINS("Track {0} already contains the group '{1}'.", true),
    TRACK_DOES_NOT_CONTAIN("Track {0} does not contain the group '{1}'.", true),
    TRACK_AMBIGUOUS_CALL("The user specified is a member of multiple groups on this track. Unable to determine their location.", true),

    GROUP_ALREADY_EXISTS("That group already exists!", true),
    GROUP_DOES_NOT_EXIST("That group does not exist!", true),
    GROUP_LOAD_ERROR("An unexpected error occurred. Group not loaded.", true),
    GROUPS_LOAD_ERROR("An unexpected error occurred. Unable to load all groups.", true),

    TRACK_ALREADY_EXISTS("That track already exists!", true),
    TRACK_DOES_NOT_EXIST("That track does not exist!", true),
    TRACK_LOAD_ERROR("An unexpected error occurred. Track not loaded.", true),
    TRACKS_LOAD_ERROR("An unexpected error occurred. Unable to load all tracks.", true),
    TRACK_EMPTY("The track cannot be used as it is empty or contains only one group.", true),

    UPDATE_TASK_REQUEST("&bUpdate task scheduled.", true),
    UPDATE_TASK_COMPLETE("&aUpdate task finished.", true),
    UPDATE_TASK_COMPLETE_NETWORK("&aUpdate task finished. Now attempting to push to other servers.", true),
    UPDATE_TASK_PUSH_SUCCESS("&aOther servers were notified via &b{0} Messaging &asuccessfully.", true),
    UPDATE_TASK_PUSH_FAILURE("&cError whilst pushing changes to other servers.", true),
    UPDATE_TASK_PUSH_FAILURE_NOT_SETUP("&cError whilst pushing changes to other servers. A messaging service has not been configured.", true),
    RELOAD_CONFIG_SUCCESS("&aThe configuration file was reloaded. &7(some options will only apply after the server has restarted.)", true),
    INFO_TOP(
            "{PREFIX}&2Running &bLuckPerms v{0}&2 by &bLuck&2." + "\n" +
            "{PREFIX}&f-  &3Platform: &f{1}" + "\n" +
            "{PREFIX}&f-  &3Server Brand: &f{2}" + "\n" +
            "{PREFIX}&f-  &3Server Version: &f{3}",
            false
    ),
    INFO_MIDDLE(
            "{PREFIX}&f-  &bMessaging Type: &f{0}" + "\n" +
            "{PREFIX}&f-  &bInstance:" + "\n" +
            "{PREFIX}&f-     &3Server Name: &f{1}" + "\n" +
            "{PREFIX}&f-     &3Online Players: &a{2}" + "\n" +
            "{PREFIX}&f-     &3Unique Connections: &a{3}" + "\n" +
            "{PREFIX}&f-     &3Uptime: &7{4}" + "\n" +
            "{PREFIX}&f-     &3Local Data: &a{5} &7users, &a{6} &7groups, &a{7} &7tracks" + "\n" +
            "{PREFIX}&f-     &3Context Calculators: &a{8}" + "\n" +
            "{PREFIX}&f-     &3Known permissions: &a{9}" + "\n" +
            "{PREFIX}&f-     &3Active processors: &7{10}",
            false
    ),
    CREATE_GROUP_ERROR("There was an error whilst creating the group.", true),
    DELETE_GROUP_ERROR("There was an error whilst deleting the group.", true),
    DELETE_GROUP_ERROR_DEFAULT("You cannot delete the default group.", true),
    GROUPS_LIST("&aGroups: &7(name, weight, tracks)", true),
    GROUPS_LIST_ENTRY("&f-  &3{0} &7- &b{1}", true),
    GROUPS_LIST_ENTRY_WITH_TRACKS("&f-  &3{0} &7- &b{1} &7- [&3{2}&7]", true),

    CREATE_TRACK_ERROR("There was an error whilst creating the track.", true),
    DELETE_TRACK_ERROR("There was an error whilst deleting the track.", true),
    TRACKS_LIST("&aTracks: {0}", true),

    LISTNODES("&b{0}'s Nodes:", true),
    LISTNODES_WITH_PAGE("&b{0}'s Nodes:  {1}", true),
    LISTNODES_TEMP("&b{0}'s Temporary Nodes:", true),
    LISTNODES_TEMP_WITH_PAGE("&b{0}'s Temporary Nodes:  {1}", true),
    LISTPARENTS("&b{0}'s Parent Groups:", true),
    LISTPARENTS_TEMP("&b{0}'s Temporary Parent Groups:", true),
    LIST_TRACKS("&b{0}'s Tracks:" + "\n" + "{1}", true),
    LIST_TRACKS_EMPTY("{0} is not on any tracks.", true),

    CONTEXT_PAIR_INLINE("&3{0}=&b{1}", false),
    CONTEXT_PAIR__GLOBAL_INLINE("&eglobal", false),
    CONTEXT_PAIR_SEP("&a, ", false),

    CONTEXT_PAIR("&8(&7{0}=&f{1}&8)", false),

    CHECK_PERMISSION("&b{0}&a has permission &b{1}&a set to {2}&a in context {3}&a.", true),
    CHECK_INHERITS_PERMISSION("&b{0}&a has permission &b{1}&a set to {2}&a in context {3}&a. &7(inherited from &a{4}&7)", true),
    SETPERMISSION_SUCCESS("&aSet &b{0}&a to &b{1}&a for &b{2}&a in context {3}&a.", true),
    SETPERMISSION_TEMP_SUCCESS("&aSet &b{0}&a to &b{1}&a for &b{2}&a for a duration of &b{3}&a in context {4}&a.", true),
    UNSETPERMISSION_SUCCESS("&aUnset &b{0}&a for &b{1}&a in context {2}&a.", true),
    UNSET_TEMP_PERMISSION_SUCCESS("&aUnset temporary permission &b{0}&a for &b{1}&a in context {2}&a.", true),
    SET_INHERIT_SUCCESS("&b{0}&a now inherits permissions from &b{1}&a in context {2}&a.", true),
    SET_TEMP_INHERIT_SUCCESS("&b{0}&a now inherits permissions from &b{1}&a for a duration of &b{2}&a in context {3}&a.", true),
    SET_PARENT_SUCCESS("&b{0}&a had their existing parent groups cleared, and now only inherits &b{1}&a in context {2}&a.", true),
    SET_TRACK_PARENT_SUCCESS("&b{0}&a had their existing parent groups on track &b{1}&a cleared, and now only inherits &b{2}&a in context {3}&a.", true),
    UNSET_INHERIT_SUCCESS("&b{0}&a no longer inherits permissions from &b{1}&a in context {2}&a.", true),
    UNSET_TEMP_INHERIT_SUCCESS("&b{0}&a no longer temporarily inherits permissions from &b{1}&a in context {2}&a.", true),

    CLEAR_SUCCESS("&b{0}&a's permissions were cleared in context {1}&a. (&b{2}&a nodes were removed.)", true),
    CLEAR_SUCCESS_SINGULAR("&b{0}&a's permissions were cleared in context {1}&a. (&b{2}&a node was removed.)", true),
    PARENT_CLEAR_SUCCESS("&b{0}&a's parents were cleared in context {1}&a. (&b{2}&a nodes were removed.)", true),
    PARENT_CLEAR_SUCCESS_SINGULAR("&b{0}&a's parents were cleared in context {1}&a. (&b{2}&a node was removed.)", true),

    PARENT_CLEAR_TRACK_SUCCESS("&b{0}&a's parents on track &b{1}&a were cleared in context {2}&a. (&b{3}&a nodes were removed.)", true),
    PARENT_CLEAR_TRACK_SUCCESS_SINGULAR("&b{0}&a's parents on track &b{1}&a were cleared in context {2}&a. (&b{3}&a node was removed.)", true),

    META_CLEAR_SUCCESS("&b{0}&a's meta matching type &b{1}&a was cleared in context {2}&a. (&b{3}&a nodes were removed.)", true),
    META_CLEAR_SUCCESS_SINGULAR("&b{0}&a's meta matching type &b{1}&a was cleared in context {2}&a. (&b{3}&a node was removed.)", true),

    ILLEGAL_DATE_ERROR("Could not parse date '{0}'.", true),
    PAST_DATE_ERROR("You cannot set a date in the past!", true),

    CHAT_META_PREFIX_HEADER("&b{0}'s Prefixes", true),
    CHAT_META_SUFFIX_HEADER("&b{0}'s Suffixes", true),
    META_HEADER("&b{0}'s Meta", true),
    CHAT_META_ENTRY("&b-> {0} &f- &f\"{1}&f\" &8(&7inherited from &a{2}&8)", true),
    CHAT_META_ENTRY_WITH_CONTEXT("&b-> {0} &f- &f\"{1}&f\" &8(&7inherited from &a{2}&8){3}", true),
    META_ENTRY("&b-> &a{0} &f= &f\"{1}&f\" &8(&7inherited from &a{2}&8)", true),
    META_ENTRY_WITH_CONTEXT("&b-> &a{0} &f= &f\"{1}&f\" &8(&7inherited from &a{2}&8){3}", true),
    CHAT_META_PREFIX_NONE("&b{0} has no prefixes.", true),
    CHAT_META_SUFFIX_NONE("&b{0} has no suffixes.", true),
    META_NONE("&b{0} has no meta.", true),

    META_INVALID_PRIORITY("Invalid priority '{0}'. Expected a number.", true),

    ALREADY_HAS_CHAT_META("{0} already has that {1} set.", true),
    DOES_NOT_HAVE_CHAT_META("{0} doesn't have that {1} set.", true),

    ADD_CHATMETA_SUCCESS("&b{0}&a had {1} &f\"{2}&f\"&a set at a priority of &b{3}&a in context {4}&a.", true),
    ADD_TEMP_CHATMETA_SUCCESS("&b{0}&a had {1} &f\"{2}&f\"&a set at a priority of &b{3}&a for a duration of &b{4}&a in context {5}&a.", true),
    REMOVE_CHATMETA_SUCCESS("&b{0}&a had {1} &f\"{2}&f\"&a at priority &b{3}&a removed in context {4}&a.", true),
    BULK_REMOVE_CHATMETA_SUCCESS("&b{0}&a had all {1}es at priority &b{2}&a removed in context {3}&a.", true),
    REMOVE_TEMP_CHATMETA_SUCCESS("&b{0}&a had temporary {1} &f\"{2}&f\"&a at priority &b{3}&a removed in context {4}&a.", true),
    BULK_REMOVE_TEMP_CHATMETA_SUCCESS("&b{0}&a had all temporary {1}es at priority &b{2}&a removed in context {3}&a.", true),

    ALREADY_HAS_META("{0} already has that meta pair set.", true),

    SET_META_SUCCESS("&aSet meta value for key &f\"{0}&f\"&a to &f\"{1}&f\"&a for &b{2}&a in context {3}&a.", true),
    SET_META_TEMP_SUCCESS("&aSet meta value for key &f\"{0}&f\"&a to &f\"{1}&f\"&a for &b{2}&a for a duration of &b{3}&a in context {4}&a.", true),
    UNSET_META_SUCCESS("&aUnset meta value with key &f\"{0}&f\"&a for &b{1}&a in context {2}&a.", true),
    UNSET_META_TEMP_SUCCESS("&aUnset temporary meta value with key &f\"{0}&f\"&a for &b{1}&a in context {2}&a.", true),

    DOESNT_HAVE_META("{0} does not have that meta pair set.", true),

    BULK_UPDATE_INVALID_DATA_TYPE("Invalid type. Was expecting 'all', 'users' or 'groups'.", true),
    BULK_UPDATE_INVALID_CONSTRAINT("Invalid constraint &4{0}&c. Constraints should be in the format '&f<field> <comparison operator> <value>&c'.", true),
    BULK_UPDATE_INVALID_COMPARISON("Invalid comparison operator '&4{0}&c'. Expected one of the following: &f==  !=  ~~  ~!", true),
    BULK_UPDATE_QUEUED("&aBulk update operation was queued. &7(&f{0}&7)", true),
    BULK_UPDATE_CONFIRM("&aRun &b/{0} bulkupdate confirm {1} &ato execute the update.", true),
    BULK_UPDATE_UNKNOWN_ID("&aOperation with id &b{0}&a does not exist or has expired.", true),

    BULK_UPDATE_STARTING("&aRunning bulk update.", true),
    BULK_UPDATE_SUCCESS("&bBulk update completed successfully.", true),
    BULK_UPDATE_FAILURE("&cBulk update failed. Check the console for errors.", true),

    USER_INFO_GENERAL(
            "{PREFIX}&b&l> &bUser Info: &f{0}" + "\n" +
            "{PREFIX}&f- &3UUID: &f{1}" + "\n" +
            "{PREFIX}&f- &3Status: {2}" + "\n" +
            "{PREFIX}&f- &3Primary Group: &f{3}" + "\n" +
            "{PREFIX}&f- &aCounts:" + "\n" +
            "{PREFIX}&f-    &3Permissions: &a{4}" + "\n" +
            "{PREFIX}&f-    &3Temporary Permissions: &a{5}" + "\n" +
            "{PREFIX}&f-    &3Prefixes: &a{6}" + "\n" +
            "{PREFIX}&f-    &3Suffixes: &a{7}" + "\n" +
            "{PREFIX}&f-    &3Meta: &a{8}",
            false
    ),

    USER_INFO_DATA(
            "{PREFIX}&f- &aCached Data:" + "\n" +
            "{PREFIX}&f-    &3Has contextual data: {0}" + "\n" +
            "{PREFIX}&f-    &3Current Contexts: {1}" + "\n" +
            "{PREFIX}&f-    &3Current Prefix: {2}" + "\n" +
            "{PREFIX}&f-    &3Current Suffix: {3}",
            false
    ),

    INFO_PARENT_HEADER("&f- &aParent Groups:", true),
    INFO_TEMP_PARENT_HEADER("&f- &aTemporary Parent Groups:", true),
    USER_REMOVEGROUP_ERROR_PRIMARY("You cannot remove a user from their primary group.", true),
    USER_PRIMARYGROUP_SUCCESS("&b{0}&a's primary group was set to &b{1}&a.", true),
    USER_PRIMARYGROUP_WARN_OPTION("&cWarning: The primary group calculation method being used by this server &7({0}) &cmay not reflect this change.", true),
    USER_PRIMARYGROUP_ERROR_ALREADYHAS("The user already has this group set as their primary group.", true),
    USER_PRIMARYGROUP_ERROR_NOTMEMBER("&b{0}&a was not already a member of &b{1}&a, adding them now.", true),
    USER_TRACK_ERROR_NOT_CONTAIN_GROUP("The user specified isn't already in any groups on this track.", true),

    USER_TRACK_ADDED_TO_FIRST("&b{0}&a isn't in any groups on this track, so they were added to the first group, &b{1}&a in context {2}&a.", true),
    USER_PROMOTE_SUCCESS("&aPromoting user along track &b{0}&a from &b{1}&a to &b{2}&a in context {3}&a.", true),

    USER_PROMOTE_ERROR_ENDOFTRACK("The end of track &4{0}&c was reached. Unable to promote user.", true),
    USER_PROMOTE_ERROR_MALFORMED(
            "{PREFIX}The next group on the track, {0}, no longer exists. Unable to promote user." + "\n" +
            "{PREFIX}Either create the group, or remove it from the track and try again.",
            false
    ),
    USER_DEMOTE_SUCCESS("&aDemoting user along track &b{0}&a from &b{1}&a to &b{2}&a in context {3}&a.", true),
    USER_DEMOTE_ENDOFTRACK("The end of track &4{0}&c was reached, so &4{1}&c was removed from &4{2}&c.", true),
    USER_DEMOTE_ERROR_MALFORMED(
            "{PREFIX}The previous group on the track, {0}, no longer exists. Unable to demote user." + "\n" +
            "{PREFIX}Either create the group, or remove it from the track and try again.",
            false
    ),

    GROUP_INFO_GENERAL(
            "{PREFIX}&b&l> &bGroup Info: &f{0}" + "\n" +
            "{PREFIX}&f- &3Display Name: &f{1}" + "\n" +
            "{PREFIX}&f- &3Weight: &f{2}" + "\n" +
            "{PREFIX}&f- &aCounts:" + "\n" +
            "{PREFIX}&f-    &3Permissions: &a{3}" + "\n" +
            "{PREFIX}&f-    &3Temporary Permissions: &a{4}" + "\n" +
            "{PREFIX}&f-    &3Prefixes: &a{5}" + "\n" +
            "{PREFIX}&f-    &3Suffixes: &a{6}" + "\n" +
            "{PREFIX}&f-    &3Meta: &a{7}",
            false
    ),
    GROUP_SET_WEIGHT("&aSet weight to &b{0}&a for group &b{1}&a.", true),

    TRACK_INFO(
            "{PREFIX}&b&l> &bShowing Track: &f{0}" + "\n" +
            "{PREFIX}&f- &7Path: &f{1}",
            false
    ),
    TRACK_CLEAR("&b{0}&a's groups track was cleared.", true),
    TRACK_APPEND_SUCCESS("&aGroup &b{0}&a was successfully appended to track &b{1}&a.", true),
    TRACK_INSERT_SUCCESS("&aGroup &b{0}&a was successfully inserted into track &b{1}&a at position &b{2}&a.", true),
    TRACK_INSERT_ERROR_NUMBER("Expected number but instead received: {0}", true),
    TRACK_INSERT_ERROR_INVALID_POS("Unable to insert at position {0}. Index out of bounds.", true),
    TRACK_REMOVE_SUCCESS("&aGroup &b{0}&a was successfully removed from track &b{1}&a.", true),

    LOG_LOAD_ERROR("The log could not be loaded.", true),
    LOG_INVALID_PAGE("Invalid page number.", true),
    LOG_INVALID_PAGE_RANGE("Invalid page number. Please enter a value between 1 and {0}.", true),
    LOG_NO_ENTRIES("&bNo log entries to show.", true),
    LOG_ENTRY("&b#{0} -> &8(&7{1} ago&8) {2}", true),

    LOG_NOTIFY_CONSOLE("&cCannot toggle notifications for console.", true),
    LOG_NOTIFY_TOGGLE_ON("&aEnabled&b logging output.", true),
    LOG_NOTIFY_TOGGLE_OFF("&cDisabled&b logging output.", true),
    LOG_NOTIFY_ALREADY_ON("You are already receiving notifications.", true),
    LOG_NOTIFY_ALREADY_OFF("You aren't currently receiving notifications.", true),
    LOG_NOTIFY_UNKNOWN("State unknown. Expecting \"on\" or \"off\".", true),

    LOG_SEARCH_HEADER("&aShowing recent actions for query &b{0} &a(page &f{1}&a of &f{2}&a)", true),

    LOG_RECENT_HEADER("&aShowing recent actions (page &f{0}&a of &f{1}&a)", true),
    LOG_RECENT_BY_HEADER("&aShowing recent actions by &b{0} &a(page &f{1}&a of &f{2}&a)", true),

    LOG_HISTORY_USER_HEADER("&aShowing history for user &b{0} &a(page &f{1}&a of &f{2}&a)", true),
    LOG_HISTORY_GROUP_HEADER("&aShowing history for group &b{0} &a(page &f{1}&a of &f{2}&a)", true),
    LOG_HISTORY_TRACK_HEADER("&aShowing history for track &b{0} &a(page &f{1}&a of &f{2}&a)", true),

    LOG_EXPORT_ALREADY_EXISTS("Error: File {0} already exists.", true),
    LOG_EXPORT_NOT_WRITABLE("Error: File {0} is not writable.", true),
    LOG_EXPORT_EMPTY("The log is empty and therefore cannot be exported.", true),
    LOG_EXPORT_FAILURE("An unexpected error occured whilst writing to the file.", true),
    LOG_EXPORT_SUCCESS("&aSuccessfully exported the log to &b{0}&a.", true),

    IMPORT_ALREADY_RUNNING("Another import process is already running. Please wait for it to finish and try again.", true),
    EXPORT_ALREADY_RUNNING("Another export process is already running. Please wait for it to finish and try again.", true),
    IMPORT_LOG_DOESNT_EXIST("Error: File {0} does not exist.", true),
    IMPORT_LOG_NOT_READABLE("Error: File {0} is not readable.", true),
    IMPORT_LOG_FAILURE("An unexpected error occured whilst reading from the log file.", true),

    IMPORT_PROGRESS("&b(Import) &b-> &f{0}&f% complete &7- &b{1}&f/&b{2} &foperations complete with &c{3} &ferrors.", true),
    IMPORT_PROGRESS_SIN("&b(Import) &b-> &f{0}&f% complete &7- &b{1}&f/&b{2} &foperations complete with &c{3} &ferror.", true),
    IMPORT_START("&b(Import) &b-> &fStarting import process.", true),

    IMPORT_END_COMPLETE("&b(Import) &a&lCOMPLETED &7- took &b{0} &7seconds - &7No errors.", true),
    IMPORT_END_COMPLETE_ERR("&b(Import) &a&lCOMPLETED &7- took &b{0} &7seconds - &c{1} errors.", true),
    IMPORT_END_COMPLETE_ERR_SIN("&b(Import) &a&lCOMPLETED &7- took &b{0} &7seconds - &c{1} error.", true),
    IMPORT_END_ERROR_HEADER(
            "{PREFIX}&b(Import) &7------------> &fShowing Error #&b{0} &7<------------" + "\n" +
            "{PREFIX}&b(Import) &fWhilst executing: &3Command #{1}" + "\n" +
            "{PREFIX}&b(Import) &fCommand: &7{2}" + "\n" +
            "{PREFIX}&b(Import) &fType: &3{3}" + "\n" +
            "{PREFIX}&b(Import) &fOutput:",
            false
    ),

    IMPORT_END_ERROR_CONTENT("&b(Import) &b-> &c{0}", true),
    IMPORT_END_ERROR_FOOTER("&b(Import) &7<------------------------------------------>", true);

    public static final Object SKIP_ELEMENT = new Object();

    private static String format(String s, Object... objects) {
        for (int i = 0; i < objects.length; i++) {
            Object o = objects[i];

            if (o != SKIP_ELEMENT) {
                s = s.replace("{" + i + "}", String.valueOf(o));
            }
        }
        return s;
    }

    @Getter
    private String message;
    private boolean showPrefix;

    public String asString(LocaleManager localeManager, Object... objects) {
        String prefix = null;
        if (localeManager != null) {
            prefix = localeManager.getTranslation(PREFIX);
        }
        if (prefix == null) {
            prefix = PREFIX.getMessage();
        }

        String s = null;
        if (localeManager != null) {
            s = localeManager.getTranslation(this);
        }
        if (s == null) {
            s = message;
        }

        if (s.startsWith("&")) {
            prefix = prefix.substring(0, prefix.length() - 2);
        }

        s = format(s.replace("{PREFIX}", prefix).replace("\\n", "\n"), objects);
        return Util.color(showPrefix ? prefix + s : s);
    }

    public void send(Sender sender, Object... objects) {
        sender.sendMessage(asString(sender.getPlatform().getLocaleManager(), objects));
    }

    /**
     * Prints this Message enum in a yml format, for reading by the {@link me.lucko.luckperms.common.locale.LocaleManager}
     * @param args not needed
     */
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

}
