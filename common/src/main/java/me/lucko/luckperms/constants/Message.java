package me.lucko.luckperms.constants;

import lombok.AllArgsConstructor;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;

@SuppressWarnings("SpellCheckingInspection")
@AllArgsConstructor
public enum Message {

    /**
     * General & Commands
     */
    PREFIX("&7&l[&b&lL&a&lP&7&l] &c", false),
    EMPTY("%s", true),

    COMMAND_NOT_RECOGNISED("Command not recognised.", true),
    COMMAND_NO_PERMISSION("You do not have permission to use this command!", true),
    INFO_BRIEF("&6Running &bLuckPerms %s&6.", true),

    ALREADY_HASPERMISSION("%s already has this permission!", true),
    DOES_NOT_HAVEPERMISSION("%s does not have this permission set.", true),



    /**
     * Loading / Saving
     */
    USER_NOT_FOUND("&eUser could not be found.", true),
    USER_SAVE_SUCCESS("&7(User data was saved to the datastore)", true),
    USER_SAVE_ERROR("There was an error whilst saving the user.", true),
    USER_ATTEMPTING_LOOKUP("&7(Attempting UUID lookup, since you specified a username)", true),

    GROUP_NOT_FOUND("&eGroup could not be found.", true),
    GROUP_SAVE_SUCCESS("&7(Group data was saved to the datastore)", true),
    GROUP_SAVE_ERROR("There was an error whilst saving the group.", true),

    TRACK_NOT_FOUND("&eTrack could not be found.", true),
    TRACK_SAVE_SUCCESS("&7(Track data was saved to the datastore)", true),
    TRACK_SAVE_ERROR("There was an error whilst saving the track.", true),



    /**
     * Command Syntax
     */
    USER_USE_ADDGROUP("Use the addgroup command instead of specifying the node.", true),
    USER_USE_REMOVEGROUP("Use the removegroup command instead of specifying the node.", true),
    USER_INVALID_ENTRY("&d%s&c is not a valid username/uuid.", true),

    GROUP_USE_INHERIT("Use the setinherit command instead of specifying the node.", true),
    GROUP_USE_UNINHERIT("Use the unsetinherit command instead of specifying the node.", true),
    GROUP_INVALID_ENTRY("Group names can only contain alphanumeric charcters.", true),

    TRACK_INVALID_ENTRY("Track names can only contain alphanumeric charcters.", true),


    /**
     * Commands
     */
    CREATE_SUCCESS("&b%s&a was successfully created.", true),
    DELETE_SUCCESS("&b%s&a was successfully deleted.", true),

    USER_ALREADY_MEMBER_OF("%s is already a member of '%s'.", true),
    USER_NOT_MEMBER_OF("%s is not a member of '%s'.", true),
    GROUP_ALREADY_INHERITS("%s already inherits '%s'.", true),
    GROUP_DOES_NOT_INHERIT("%s does not inherit '%s'.", true),
    TRACK_ALREADY_CONTAINS("Track %s already contains the group '%s'.", true),
    TRACK_DOES_NOT_CONTAIN("Track %s does not contain the group '%s'.", true),

    GROUP_ALREADY_EXISTS("That group already exists!", true),
    GROUP_DOES_NOT_EXIST("That group does not exist!", true),
    GROUP_NAME_TOO_LONG("Group name '%s' exceeds the maximum length of 36 characters.", true),
    GROUP_LOAD_ERROR("An unexpected error occurred. Group not loaded.", true),
    GROUPS_LOAD_ERROR("An unexpected error occurred. Unable to load all groups.", true),

    TRACK_ALREADY_EXISTS("That track already exists!", true),
    TRACK_DOES_NOT_EXIST("That track does not exist!", true),
    TRACK_NAME_TOO_LONG("Track name '%s' exceeds the maximum length of 36 characters.", true),
    TRACK_LOAD_ERROR("An unexpected error occurred. Track not loaded.", true),
    TRACKS_LOAD_ERROR("An unexpected error occurred. Unable to load all tracks.", true),
    TRACK_EMPTY("The track cannot be used as it is empty or contains only one group.", true),

    UPDATE_TASK_RUN("&bRunning update task for all online users.", true),
    INFO(
            PREFIX + "&6Running &bLuckPerms %s&6." + "\n" +
            PREFIX + "&eAuthor: &6Luck" + "\n" +
            PREFIX + "&eStorage Method: &6%s",
            false
    ),
    DEBUG(
            PREFIX + "&d&l> &dDebug Info" + "\n" +
            PREFIX + "&eOnline Players: &6%s" + "\n" +
            PREFIX + "&eLoaded Users: &6%s" + "\n" +
            PREFIX + "&eLoaded Groups: &6%s" + "\n" +
            PREFIX + "&eLoaded Tracks: &6%s",
            false
    ),

    CREATE_GROUP_ERROR("There was an error whilst creating the group.", true),
    DELETE_GROUP_ERROR("There was an error whilst deleting the group.", true),
    DELETE_GROUP_ERROR_DEFAULT("You cannot delete the default group.", true),
    GROUPS_LIST("&aGroups: %s", true),

    CREATE_TRACK_ERROR("There was an error whilst creating the track.", true),
    DELETE_TRACK_ERROR("There was an error whilst deleting the track.", true),
    TRACKS_LIST("&aTracks: %s", true),

    LISTNODES("&e%s's Nodes:" + "\n" + "%s", true),
    SETPERMISSION_SUCCESS("&aSet &b%s&a to &b%s&a for &b%s&a.", true),
    SETPERMISSION_SERVER_SUCCESS("&aSet &b%s&a to &b%s&a for &b%s&a on server &b%s&a.", true),
    UNSETPERMISSION_SUCCESS("&aUnset &b%s&a for &b%s&a.", true),
    UNSETPERMISSION_SERVER_SUCCESS("&aUnset &b%s&a for &b%s&a on server &b%$s&a.", true),
    CLEAR_SUCCESS("&b%s&a's permissions were cleared.", true),

    USER_INFO(
            PREFIX + "&d-> &eUser: &6%s" + "\n" +
            PREFIX + "&d-> &eUUID: &6%s" + "\n" +
            PREFIX + "&d-> &eStatus: %s" + "\n" +
            PREFIX + "&d-> &eGroups: &6%s" + "\n" +
            PREFIX + "&d-> &ePrimary Group: &6%s" + "\n" +
            PREFIX + "&d-> &ePermissions: &6%s" + "\n" +
            PREFIX + "&d-> &bUse &a/%s user %s listnodes &bto see all permissions.",
            false
    ),
    USER_GETUUID("&bThe UUID of &e%s&b is &e%s&b.", true),
    USER_ADDGROUP_SUCCESS("&b%s&a successfully added to group &b%s&a.", true),
    USER_ADDGROUP_SERVER_SUCCESS("&b%s&a successfully added to group &b%s&a on server &b%s&a.", true),
    USER_REMOVEGROUP_SUCCESS("&b%s&a was removed from group &b%s&a.", true),
    USER_REMOVEGROUP_SERVER_SUCCESS("&b%s&a was removed from group &b%s&a on server &b%s&a.", true),
    USER_REMOVEGROUP_ERROR_PRIMARY("You cannot remove a user from their primary group.", true),
    USER_PRIMARYGROUP_SUCCESS("&b%s&a's primary group was set to &b%s&a.", true),
    USER_PRIMARYGROUP_ERROR_ALREADYHAS("The user already has this group set as their primary group.", true),
    USER_PRIMARYGROUP_ERROR_NOTMEMBER("The user must be a member of the group first! Use &4/%s user <user> addgroup <group>", true),
    USER_SHOWTRACKS_INFO("&aShowing tracks that contain the group '&b%s&a' (%s's primary group)", true),
    USER_PROMOTE_SUCCESS_PROMOTE("&aPromoting user along track &b%s&a from &b%s&a to &b%s&a.", true),
    USER_PROMOTE_SUCCESS_REMOVE("&b%s&a was removed from &b%s&a, added to &b%s&a, and their primary group was set to &b%s&a.", true),
    USER_PROMOTE_ERROR_ENDOFTRACK("The end of track &4%s&c was reached. Unable to promote user.", true),
    USER_PROMOTE_ERROR_MALFORMED(
            PREFIX + "The next group on the track, %s, no longer exists. Unable to promote user." + "\n" +
            PREFIX + "Either create the group, or remove it from the track and try again.",
            false
    ),
    USER_PROMOTE_ERROR_NOT_CONTAIN_GROUP("Promotions are done based on primary groups. The users primary group is not on the track specified.", true),
    USER_DEMOTE_SUCCESS_PROMOTE("&aDemoting user along track &b%s&a from &b%s&a to &b%s&a.", true),
    USER_DEMOTE_SUCCESS_REMOVE("&b%s&a was removed from &b%s&a, added to &b%s&a, and their primary group was set to &b%s&a.", true),
    USER_DEMOTE_ERROR_ENDOFTRACK("The end of track &4%s&c was reached. Unable to demote user.", true),
    USER_DEMOTE_ERROR_MALFORMED(
            PREFIX + "The previous group on the track, %s, no longer exists. Unable to demote user." + "\n" +
            PREFIX + "Either create the group, or remove it from the track and try again.",
            false
    ),
    USER_DEMOTE_ERROR_NOT_CONTAIN_GROUP("Demotions are done based on primary groups. The users primary group is not on the track specified.", true),
    USER_SHOWPOS("&aShowing &b%s&a's position on track &b%s&a.\n%s", true),

    GROUP_INFO(
            PREFIX + "&d-> &eGroup: &6%s" + "\n" +
            PREFIX + "&d-> &ePermissions: &6%s" + "\n" +
            PREFIX + "&d-> &bUse &a/%s group %s listnodes &bto see all permissions.",
            false
    ),
    GROUP_SETINHERIT_SUCCESS("&b%s&a now inherits permissions from &b%s&a.", true),
    GROUP_SETINHERIT_SERVER_SUCCESS("&b%s&a now inherits permissions from &b%s&a on server &b%s&a.", true),
    GROUP_UNSETINHERIT_SUCCESS("&b%s&a no longer inherits permissions from &b%s&a.", true),
    GROUP_UNSETINHERIT_SERVER_SUCCESS("&b%s&a no longer inherits permissions from &b%s&a on server &b%s&a.", true),

    TRACK_INFO(
            PREFIX + "&d-> &eTrack: &6%s" + "\n" +
            PREFIX + "&d-> &ePath: &6%s",
            false
    ),
    TRACK_CLEAR("&b%s&a's groups track was cleared.", true),
    TRACK_APPEND_SUCCESS("&aGroup &b%s&a was successfully appended to track &b%s&a.", true),
    TRACK_INSERT_SUCCESS("&aGroup &b%s&a was successfully inserted into to track &b%s&a at position &b%s&a.", true),
    TRACK_INSERT_ERROR_NUMBER("Expected number but instead received: %s", true),
    TRACK_INSERT_ERROR_INVALID_POS("Unable to insert at position %s. Index out of bounds.", true),
    TRACK_REMOVE_SUCCESS("&aGroup &b%s&a was successfully removed from track &b%s&a.", true);

    private String message;
    private boolean showPrefix;

    @Override
    public String toString() {
        return message;
    }

    public void send(Sender sender, Object... objects) {
        if (showPrefix) {
            sender.sendMessage(Util.color(PREFIX + String.format(message, objects)));
        } else {
            sender.sendMessage(Util.color(String.format(message, objects)));
        }
    }
}
