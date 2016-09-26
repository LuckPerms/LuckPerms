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

package me.lucko.luckperms.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;

@SuppressWarnings("SpellCheckingInspection")
@AllArgsConstructor
public enum Message {

    /*
     * General & Commands
     */
    PREFIX("&7&l[&b&lL&3&lP&7&l] &c", false),
    EMPTY("{0}", true),
    PLAYER_ONLINE("&aOnline", false),
    PLAYER_OFFLINE("&cOffline", false),
    LOADING_ERROR("Permissions data could not be loaded. Please contact an administrator.", true),
    OP_DISABLED("&eThe vanilla OP system is disabled on this server.", false),
    LOG("&3LOG &3&l> {0}", true),

    COMMAND_NOT_RECOGNISED("Command not recognised.", true),
    COMMAND_NO_PERMISSION("You do not have permission to use this command!", true),

    ALREADY_HASPERMISSION("{0} already has this permission!", true),
    DOES_NOT_HAVEPERMISSION("{0} does not have this permission set.", true),
    ALREADY_HAS_TEMP_PERMISSION("{0} already has this permission set temporarily!", true),
    DOES_NOT_HAVE_TEMP_PERMISSION("{0} does not have this permission set temporarily.", true),



    /*
     * Loading / Saving
     */
    USER_NOT_FOUND("&eUser could not be found.", true),
    USER_SAVE_SUCCESS("&7(User data was saved to the datastore)", true),
    USER_SAVE_ERROR("There was an error whilst saving the user.", true),
    USER_ATTEMPTING_LOOKUP("&7(Attempting UUID lookup, since you specified a username)", true),
    USER_CREATE_FAIL("There was an error whilst creating a new user.", true),

    GROUP_NOT_FOUND("&eGroup could not be found.", true),
    GROUP_SAVE_SUCCESS("&7(Group data was saved to the datastore)", true),
    GROUP_SAVE_ERROR("There was an error whilst saving the group.", true),

    TRACK_NOT_FOUND("&eTrack could not be found.", true),
    TRACK_SAVE_SUCCESS("&7(Track data was saved to the datastore)", true),
    TRACK_SAVE_ERROR("There was an error whilst saving the track.", true),



    /*
     * Command Syntax
     */
    USER_USE_ADDGROUP("Use the addgroup command instead of specifying the node.", true),
    USER_USE_REMOVEGROUP("Use the removegroup command instead of specifying the node.", true),
    USER_INVALID_ENTRY("&d{0}&c is not a valid username/uuid.", true),

    GROUP_USE_INHERIT("Use the setinherit command instead of specifying the node.", true),
    GROUP_USE_UNINHERIT("Use the unsetinherit command instead of specifying the node.", true),
    GROUP_INVALID_ENTRY("Group names can only contain alphanumeric characters.", true),

    TRACK_INVALID_ENTRY("Track names can only contain alphanumeric characters.", true),

    SERVER_INVALID_ENTRY("Server names can only contain alphanumeric characters.", true),


    /*
     * Commands
     */
    CREATE_SUCCESS("&b{0}&a was successfully created.", true),
    DELETE_SUCCESS("&b{0}&a was successfully deleted.", true),
    RENAME_SUCCESS("&b{0}&a was successfully renamed to &b{1}&a.", true),

    USER_ALREADY_MEMBER_OF("{0} is already a member of '{1}'.", true),
    USER_NOT_MEMBER_OF("{0} is not a member of '{1}'.", true),
    GROUP_ALREADY_INHERITS("{0} already inherits '{1}'.", true),
    GROUP_DOES_NOT_INHERIT("{0} does not inherit '{1}'.", true),

    USER_ALREADY_TEMP_MEMBER_OF("{0} is already a temporary member of '{1}'.", true),
    USER_NOT_TEMP_MEMBER_OF("{0} is not a temporary member of '{1}'.", true),
    GROUP_ALREADY_TEMP_INHERITS("{0} already temporarily inherits '{1}'.", true),
    GROUP_DOES_NOT_TEMP_INHERIT("{0} does not temporarily inherit '{1}'.", true),

    TRACK_ALREADY_CONTAINS("Track {0} already contains the group '{1}'.", true),
    TRACK_DOES_NOT_CONTAIN("Track {0} does not contain the group '{1}'.", true),

    GROUP_ALREADY_EXISTS("That group already exists!", true),
    GROUP_DOES_NOT_EXIST("That group does not exist!", true),
    GROUP_LOAD_ERROR("An unexpected error occurred. Group not loaded.", true),
    GROUPS_LOAD_ERROR("An unexpected error occurred. Unable to load all groups.", true),

    TRACK_ALREADY_EXISTS("That track already exists!", true),
    TRACK_DOES_NOT_EXIST("That track does not exist!", true),
    TRACK_LOAD_ERROR("An unexpected error occurred. Track not loaded.", true),
    TRACKS_LOAD_ERROR("An unexpected error occurred. Unable to load all tracks.", true),
    TRACK_EMPTY("The track cannot be used as it is empty or contains only one group.", true),

    UPDATE_TASK_RUN("&bRunning update task for all online users.", true),
    INFO(
            PREFIX + "&2Running &bLuckPerms v{0}&2 by &bLuck&2." + "\n" +
            PREFIX + "&f-> &ePlatform: &6{1}" + "\n" +
            PREFIX + "&f-> &eStorage Method: &6{2}" + "\n" +
            PREFIX + "&f-> &eServer Name: &6{3}" + "\n" +
            PREFIX + "&f-> &eSync Interval: &6{4} minutes" + "\n" +
            PREFIX + "&f-> &eInclude Global: &6{5}" + "\n" +
            PREFIX + "&f-> &eInclude Global World: &6{6}" + "\n" +
            PREFIX + "&f-> &eApply Global Groups: &6{7}" + "\n" +
            PREFIX + "&f-> &eApply Global World Groups: &6{8}" + "\n" +
            PREFIX + "&f-> &eOnline Mode: &6{9}" + "\n" +
            PREFIX + "&f-> &eApply Wildcards: &6{10}" + "\n" +
            PREFIX + "&f-> &eApply Regex: &6{11}" + "\n" +
            PREFIX + "&f-> &eApply Shorthand: &6{12}",
            false
    ),
    DEBUG(
            PREFIX + "&d&l> &dDebug Info" + "\n" +
            PREFIX + "&f> &eOnline Players: &6{0}" + "\n" +
            PREFIX + "&f> &eLoaded Users: &6{1}" + "\n" +
            PREFIX + "&f> &eLoaded Groups: &6{2}" + "\n" +
            PREFIX + "&f> &eLoaded Tracks: &6{3}",
            false
    ),

    CREATE_GROUP_ERROR("There was an error whilst creating the group.", true),
    DELETE_GROUP_ERROR("There was an error whilst deleting the group.", true),
    DELETE_GROUP_ERROR_DEFAULT("You cannot delete the default group.", true),
    GROUPS_LIST("&aGroups: {0}", true),

    CREATE_TRACK_ERROR("There was an error whilst creating the track.", true),
    DELETE_TRACK_ERROR("There was an error whilst deleting the track.", true),
    TRACKS_LIST("&aTracks: {0}", true),

    LISTNODES("&e{0}'s Nodes:" + "\n" + "{1}", true),
    LISTNODES_TEMP("&e{0}'s Temporary Nodes:" + "\n" + "{1}", true),
    LISTPARENTS("&e{0}'s Parent Groups:" + "\n" + "{1}", true),
    LISTPARENTS_TEMP("&e{0}'s Temporary Parent Groups:" + "\n" + "{1}", true),
    LISTGROUPS("&e{0}'s Groups:" + "\n" + "{1}", true),
    LISTGROUPS_TEMP("&e{0}'s Temporary Groups:" + "\n" + "{1}", true),
    SETPERMISSION_SUCCESS("&aSet &b{0}&a to &b{1}&a for &b{2}&a.", true),
    SETPERMISSION_SERVER_SUCCESS("&aSet &b{0}&a to &b{1}&a for &b{2}&a on server &b{3}&a.", true),
    SETPERMISSION_SERVER_WORLD_SUCCESS("&aSet &b{0}&a to &b{1}&a for &b{2}&a on server &b{3}&a, world &b{4}&a.", true),
    SETPERMISSION_TEMP_SUCCESS("&aSet &b{0}&a to &b{1}&a for &b{2}&a for a duration of &b{3}&a.", true),
    SETPERMISSION_TEMP_SERVER_SUCCESS("&aSet &b{0}&a to &b{1}&a for &b{2}&a on server &b{3}&a for a duration of &b{4}&a.", true),
    SETPERMISSION_TEMP_SERVER_WORLD_SUCCESS("&aSet &b{0}&a to &b{1}&a for &b{2}&a on server &b{3}&a, world &b{4}&a, for a duration of &b{5}&a.", true),
    UNSETPERMISSION_SUCCESS("&aUnset &b{0}&a for &b{1}&a.", true),
    UNSETPERMISSION_SERVER_SUCCESS("&aUnset &b{0}&a for &b{1}&a on server &b{2}&a.", true),
    UNSETPERMISSION_SERVER_WORLD_SUCCESS("&aUnset &b{0}&a for &b{1}&a on server &b{2}&a, world &b{3}&a.", true),
    UNSET_TEMP_PERMISSION_SUCCESS("&aUnset temporary permission &b{0}&a for &b{1}&a.", true),
    UNSET_TEMP_PERMISSION_SERVER_SUCCESS("&aUnset temporary permission &b{0}&a for &b{1}&a on server &b{2}&a.", true),
    UNSET_TEMP_PERMISSION_SERVER_WORLD_SUCCESS("&aUnset temporary permission &b{0}&a for &b{1}&a on server &b{2}&a, world &b{3}&a.", true),
    CLEAR_SUCCESS("&b{0}&a's permissions were cleared.", true),
    ILLEGAL_DATE_ERROR("Could not parse date '{0}'.", true),
    PAST_DATE_ERROR("You cannot set a date in the past!", true),

    CHAT_META_PREFIX_HEADER("&e{0}'s Prefixes", true),
    CHAT_META_SUFFIX_HEADER("&e{0}'s Suffixes", true),
    CHAT_META_ENTRY("&d-> &e{0} &6- &f\"{1}&f\"", true),
    CHAT_META_PREFIX_NONE("&e{0} has no prefixes.", true),
    CHAT_META_SUFFIX_NONE("&e{0} has no suffixes.", true),

    META_INVALID_PRIORITY("Invalid priority '{0}'. Expected a number.", true),
    ALREADY_HAS_PREFIX("{0} already has that prefix set.", true),
    ALREADY_HAS_SUFFIX("{0} already has that suffix set.", true),
    DOES_NOT_HAVE_PREFIX("{0} doesn't have that prefix set.", true),
    DOES_NOT_HAVE_SUFFIX("{0} doesn't have that suffix set.", true),

    ADDPREFIX_SUCCESS("&b{0}&a had prefix &f\"{1}&f\"&a set at a priority of &b{2}&a.", true),
    ADDPREFIX_SERVER_SUCCESS("&b{0}&a had prefix &f\"{1}&f\"&a set at a priority of &b{2}&a on server &b{3}&a.", true),
    ADDPREFIX_SERVER_WORLD_SUCCESS("&b{0}&a had prefix &f\"{1}&f\"&a set at a priority of &b{2}&a on server &b{3}&a, world &b{4}&a.", true),
    REMOVEPREFIX_SUCCESS("&b{0}&a had prefix &f\"{1}&f\"&a at priority &b{2}&a removed.", true),
    REMOVEPREFIX_SERVER_SUCCESS("&b{0}&a had prefix &f\"{1}&f\"&a at priority &b{2}&a removed on server &b{3}&a.", true),
    REMOVEPREFIX_SERVER_WORLD_SUCCESS("&b{0}&a had prefix &f\"{1}&f\"&a at priority &b{2}&a removed on server &b{3}&a, world &b{4}&a.", true),

    ADDSUFFIX_SUCCESS("&b{0}&a had suffix &f\"{1}&f\"&a set at a priority of &b{2}&a.", true),
    ADDSUFFIX_SERVER_SUCCESS("&b{0}&a had suffix &f\"{1}&f\"&a set at a priority of &b{2}&a on server &b{3}&a.", true),
    ADDSUFFIX_SERVER_WORLD_SUCCESS("&b{0}&a had suffix &f\"{1}&f\"&a set at a priority of &b{2}&a on server &b{3}&a, world &b{4}&a.", true),
    REMOVESUFFIX_SUCCESS("&b{0}&a had suffix &f\"{1}&f\"&a at priority &b{2}&a removed.", true),
    REMOVESUFFIX_SERVER_SUCCESS("&b{0}&a had suffix &f\"{1}&f\"&a at priority &b{2}&a removed on server &b{3}&a.", true),
    REMOVESUFFIX_SERVER_WORLD_SUCCESS("&b{0}&a had suffix &f\"{1}&f\"&a at priority &b{2}&a removed on server &b{3}&a, world &b{4}&a.", true),

    ADD_TEMP_PREFIX_SUCCESS("&b{0}&a had prefix &f\"{1}&f\"&a set at a priority of &b{2}&a for a duration of &b{3}&a.", true),
    ADD_TEMP_PREFIX_SERVER_SUCCESS("&b{0}&a had prefix &f\"{1}&f\"&a set at a priority of &b{2}&a on server &b{3}&a, for a duration of &b{4}&a.", true),
    ADD_TEMP_PREFIX_SERVER_WORLD_SUCCESS("&b{0}&a had prefix &f\"{1}&f\"&a set at a priority of &b{2}&a on server &b{3}&a, world &b{4}&a, for a duration of &b{5}&a.", true),
    REMOVE_TEMP_PREFIX_SUCCESS("&b{0}&a had temporary prefix &f\"{1}&f\"&a at priority &b{2}&a removed.", true),
    REMOVE_TEMP_PREFIX_SERVER_SUCCESS("&b{0}&a had temporary prefix &f\"{1}&f\"&a at priority &b{2}&a removed on server &b{3}&a.", true),
    REMOVE_TEMP_PREFIX_SERVER_WORLD_SUCCESS("&b{0}&a had temporary prefix &f\"{1}&f\"&a at priority &b{2}&a removed on server &b{3}&a, world &b{4}&a.", true),

    ADD_TEMP_SUFFIX_SUCCESS("&b{0}&a had suffix &f\"{1}&f\"&a set at a priority of &b{2}&a for a duration of &b{3}&a.", true),
    ADD_TEMP_SUFFIX_SERVER_SUCCESS("&b{0}&a had suffix &f\"{1}&f\"&a set at a priority of &b{2}&a on server &b{3}&a, for a duration of &b{4}&a.", true),
    ADD_TEMP_SUFFIX_SERVER_WORLD_SUCCESS("&b{0}&a had suffix &f\"{1}&f\"&a set at a priority of &b{2}&a on server &b{3}&a, world &b{4}&a, for a duration of &b{5}&a.", true),
    REMOVE_TEMP_SUFFIX_SUCCESS("&b{0}&a had temporary suffix &f\"{1}&f\"&a at priority &b{1}&a removed.", true),
    REMOVE_TEMP_SUFFIX_SERVER_SUCCESS("&b{0}&a had temporary suffix &f\"{1}&f\"&a at priority &b{2}&a removed on server &b{3}&a.", true),
    REMOVE_TEMP_SUFFIX_SERVER_WORLD_SUCCESS("&b{0}&a had temporary suffix &f\"{1}&f\"&a at priority &b{2}&a removed on server &b{3}&a, world &b{4}&a.", true),

    BULK_CHANGE_TYPE_ERROR("Invalid type. Was expecting 'server' or 'world'.", true),
    BULK_CHANGE_SUCCESS("&aApplied bulk change successfully. {0} records were changed.", true),

    USER_INFO(
            PREFIX + "&d-> &eUser: &6{0}" + "\n" +
            PREFIX + "&d-> &eUUID: &6{1}" + "\n" +
            PREFIX + "&d-> &eStatus: {2}" + "\n" +
            PREFIX + "&d-> &ePrimary Group: &6{3}" + "\n" +
            PREFIX + "&d-> &ePermissions: &6{4}" + "\n" +
            PREFIX + "&d-> &eTemporary Permissions: &6{5}" + "\n" +
            PREFIX + "&d-> &bUse &a/{6} user {7} listnodes &bto see all permissions.",
            false
    ),
    USER_GETUUID("&bThe UUID of &e{0}&b is &e{1}&b.", true),
    USER_ADDGROUP_SUCCESS("&b{0}&a successfully added to group &b{1}&a.", true),
    USER_ADDGROUP_SERVER_SUCCESS("&b{0}&a successfully added to group &b{1}&a on server &b{2}&a.", true),
    USER_ADDGROUP_SERVER_WORLD_SUCCESS("&b{0}&a successfully added to group &b{1}&a on server &b{2}&a, world &b{3}&a.", true),
    USER_ADDTEMPGROUP_SUCCESS("&b{0}&a successfully added to group &b{1}&a for a duration of &b{2}&a.", true),
    USER_ADDTEMPGROUP_SERVER_SUCCESS("&b{0}&a successfully added to group &b{1}&a on server &b{2}&a for a duration of &b{3}&a.", true),
    USER_ADDTEMPGROUP_SERVER_WORLD_SUCCESS("&b{0}&a successfully added to group &b{1}&a on server &b{2}&a, world &b{3}&a, for a duration of &b{4}&a.", true),
    USER_REMOVEGROUP_SUCCESS("&b{0}&a was removed from group &b{1}&a.", true),
    USER_REMOVEGROUP_SERVER_SUCCESS("&b{0}&a was removed from group &b{1}&a on server &b{2}&a.", true),
    USER_REMOVEGROUP_SERVER_WORLD_SUCCESS("&b{0}&a was removed from group &b{1}&a on server &b{2}&a, world &b{3}&a.", true),
    USER_REMOVETEMPGROUP_SUCCESS("&b{0}&a was removed from temproary group &b{1}&a.", true),
    USER_REMOVETEMPGROUP_SERVER_SUCCESS("&b{0}&a was removed from temporary group &b{1}&a on server &b{2}&a.", true),
    USER_REMOVETEMPGROUP_SERVER_WORLD_SUCCESS("&b{0}&a was removed from temporary group &b{1}&a on server &b{2}&a, world &b{3}&a.", true),
    USER_REMOVEGROUP_ERROR_PRIMARY("You cannot remove a user from their primary group.", true),
    USER_PRIMARYGROUP_SUCCESS("&b{0}&a's primary group was set to &b{1}&a.", true),
    USER_PRIMARYGROUP_ERROR_ALREADYHAS("The user already has this group set as their primary group.", true),
    USER_PRIMARYGROUP_ERROR_NOTMEMBER("&b{0}&a was not already a member of &b{1}&a, adding them now.", true),
    USER_SHOWTRACKS_INFO("&aShowing tracks that contain the group '&b{0}&a' ({1}'s primary group)", true),
    USER_PROMOTE_SUCCESS_PROMOTE("&aPromoting user along track &b{0}&a from &b{1}&a to &b{2}&a.", true),
    USER_PROMOTE_SUCCESS_REMOVE("&b{0}&a was removed from &b{1}&a, added to &b{2}&a, and their primary group was set to &b{3}&a.", true),
    USER_PROMOTE_ERROR_ENDOFTRACK("The end of track &4{0}&c was reached. Unable to promote user.", true),
    USER_PROMOTE_ERROR_MALFORMED(
            PREFIX + "The next group on the track, {0}, no longer exists. Unable to promote user." + "\n" +
            PREFIX + "Either create the group, or remove it from the track and try again.",
            false
    ),
    USER_PROMOTE_ERROR_NOT_CONTAIN_GROUP("Promotions are done based on primary groups. The users primary group is not on the track specified.", true),
    USER_DEMOTE_SUCCESS_PROMOTE("&aDemoting user along track &b{0}&a from &b{1}&a to &b{2}&a.", true),
    USER_DEMOTE_SUCCESS_REMOVE("&b{0}&a was removed from &b{1}&a, added to &b{2}&a, and their primary group was set to &b{3}&a.", true),
    USER_DEMOTE_ERROR_ENDOFTRACK("The end of track &4{0}&c was reached. Unable to demote user.", true),
    USER_DEMOTE_ERROR_MALFORMED(
            PREFIX + "The previous group on the track, {0}, no longer exists. Unable to demote user." + "\n" +
            PREFIX + "Either create the group, or remove it from the track and try again.",
            false
    ),
    USER_DEMOTE_ERROR_NOT_CONTAIN_GROUP("Demotions are done based on primary groups. The users primary group is not on the track specified.", true),
    USER_SHOWPOS("&aShowing &b{0}&a's position on track &b{1}&a.\n{2}", true),

    GROUP_INFO(
            PREFIX + "&d-> &eGroup: &6{0}" + "\n" +
            PREFIX + "&d-> &ePermissions: &6{1}" + "\n" +
            PREFIX + "&d-> &eTemporary Permissions: &6{2}" + "\n" +
            PREFIX + "&d-> &bUse &a/{3} group {4} listnodes &bto see all permissions.",
            false
    ),
    GROUP_SETINHERIT_SUCCESS("&b{0}&a now inherits permissions from &b{1}&a.", true),
    GROUP_SETINHERIT_SERVER_SUCCESS("&b{0}&a now inherits permissions from &b{2}&a on server &b{3}&a.", true),
    GROUP_SETINHERIT_SERVER_WORLD_SUCCESS("&b{0}&a now inherits permissions from &b{1}&a on server &b{2}&a, world &b{3}&a.", true),
    GROUP_SET_TEMP_INHERIT_SUCCESS("&b{0}&a now inherits permissions from &b{1}&a for a duration of &b{2}&a.", true),
    GROUP_SET_TEMP_INHERIT_SERVER_SUCCESS("&b{0}&a now inherits permissions from &b{1}&a on server &b{2}&a for a duration of &b{3}&a.", true),
    GROUP_SET_TEMP_INHERIT_SERVER_WORLD_SUCCESS("&b{0}&a now inherits permissions from &b{1}&a on server &b{2}&a, world &b{3}&a, for a duration of &b{4}&a.", true),
    GROUP_UNSETINHERIT_SUCCESS("&b{0}&a no longer inherits permissions from &b{1}&a.", true),
    GROUP_UNSETINHERIT_SERVER_SUCCESS("&b{0}&a no longer inherits permissions from &b{1}&a on server &b{2}&a.", true),
    GROUP_UNSETINHERIT_SERVER_WORLD_SUCCESS("&b{0}&a no longer inherits permissions from &b{1}&a on server &b{2}&a, world &b{3}&a.", true),
    GROUP_UNSET_TEMP_INHERIT_SUCCESS("&b{0}&a no longer temporarily inherits permissions from &b{1}&a.", true),
    GROUP_UNSET_TEMP_INHERIT_SERVER_SUCCESS("&b{0}&a no longer temporarily inherits permissions from &b{1}&a on server &b{2}&a.", true),
    GROUP_UNSET_TEMP_INHERIT_SERVER_WORLD_SUCCESS("&b{0}&a no longer temporarily inherits permissions from &b{1}&a on server &b{2}&a, world &b{3}&a.", true),

    TRACK_INFO(
            PREFIX + "&d-> &eTrack: &6{0}" + "\n" +
            PREFIX + "&d-> &ePath: &6{1}",
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
    LOG_NO_ENTRIES("&eNo log entries to show.", true),
    LOG_ENTRY("&e#{0} -> &8(&7{1} ago&8) {2}", true),

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
    IMPORT_LOG_DOESNT_EXIST("Error: File {0} does not exist.", true),
    IMPORT_LOG_NOT_READABLE("Error: File {0} is not readable.", true),
    IMPORT_LOG_FAILURE("An unexpected error occured whilst reading from the log file.", true),

    IMPORT_PROGRESS("&e(Import) &d-> &f{0} &6percent complete &7- &e{1}&6/&e{2} &6operations complete with &c{3} &6errors.", true),
    IMPORT_PROGRESS_SIN("&e(Import) &d-> &f{0} &6percent complete &7- &e{1}&6/&e{2} &6operations complete with &c{3} &6error.", true),
    IMPORT_START("&e(Import) &d-> &6Starting import process.", true),

    IMPORT_END_COMPLETE("&e(Import) &a&lCOMPLETED &7- took &e{0} &7seconds - &7No errors.", true),
    IMPORT_END_COMPLETE_ERR("&e(Import) &a&lCOMPLETED &7- took &e{0} &7seconds - &c{1} errors.", true),
    IMPORT_END_COMPLETE_ERR_SIN("&e(Import) &a&lCOMPLETED &7- took &e{0} &7seconds - &c{1} error.", true),
    IMPORT_END_ERROR_HEADER(
            PREFIX + "&e(Import) &7------------> &6Showing Error #&e{0} &7<------------" + "\n" +
            PREFIX + "&e(Import) &6Whilst executing: &fCommand #{1}" + "\n" +
            PREFIX + "&e(Import) &6Command: &7{2}" + "\n" +
            PREFIX + "&e(Import) &6Type: &f{3}" + "\n" +
            PREFIX + "&e(Import) &6Output:",
            false
    ),

    IMPORT_END_ERROR_CONTENT("&e(Import) &d-> &c{0}", true),
    IMPORT_END_ERROR_FOOTER("&e(Import) &7<------------------------------------------>", true),

    MIGRATION_NOT_CONSOLE("Migration must be performed from the Console.", true);

    @Getter
    private String message;
    private boolean showPrefix;

    @Override
    public String toString() {
        return Util.color(showPrefix ? PREFIX + message : message);
    }

    public void send(Sender sender, Object... objects) {
        String s = sender.getPlatform().getLocaleManager().getTranslation(this);
        if (s == null) {
            s = message;
        } else {
            s = s.replace("{PREFIX}", PREFIX.getMessage()).replace("\\n", "\n");
        }

        if (showPrefix) {
            sender.sendMessage(Util.color(PREFIX + format(s, objects)));
        } else {
            sender.sendMessage(Util.color(format(s, objects)));
        }
    }

    private static String format(String s, Object... objects) {
        for (int i = 0, objsLength = objects.length; i < objsLength; i++) {
            Object o = objects[i];
            s = s.replace("{" + i + "}", o.toString());
        }
        return s;
    }
}
