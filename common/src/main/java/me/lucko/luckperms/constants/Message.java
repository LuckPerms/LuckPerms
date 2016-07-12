package me.lucko.luckperms.constants;

import lombok.AllArgsConstructor;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;

@SuppressWarnings("SpellCheckingInspection")
@AllArgsConstructor
public enum Message {

    PREFIX("&7&l[&b&lL&a&lP&7&l] &c", false),

    COMMAND_NOT_RECOGNISED("Command not recognised.", true),
    COMMAND_NO_PERMISSION("You do not have permission to use this command!", true),
    INFO_BRIEF("&6Running &bLuckPerms %s&6.", true),


    ALREADY_HASPERMISSION("%s already has this permission!", true),
    DOES_NOT_HAVEPERMISSION("%s does not have this permission set.", true),

    USER_NOT_FOUND("&eUser could not be found.", true),
    USER_SAVE_SUCCESS("&7(User data was saved to the datastore)", true),
    USER_SAVE_ERROR("There was an error whilst saving the user.", true),
    USER_ALREADY_MEMBER_OF("%s is already a member of '%s'.", true),
    USER_NOT_MEMBER_OF("%s is not a member of '%s'.", true),
    USER_USE_ADDGROUP("Use the addgroup command instead of specifying the node.", true),
    USER_USE_REMOVEGROUP("Use the removegroup command instead of specifying the node.", true),

    GROUP_NOT_FOUND("&eGroup could not be found.", true),
    GROUP_SAVE_SUCCESS("&7(Group data was saved to the datastore)", true),
    GROUP_SAVE_ERROR("There was an error whilst saving the group.", true),
    GROUP_ALREADY_INHERITS("%s already inherits '%s'.", true),
    GROUP_DOES_NOT_INHERIT("%s does not inherit '%s'.", true),
    GROUP_USE_INHERIT("Use the setinherit command instead of specifying the node.", true),
    GROUP_USE_UNINHERIT("Use the unsetinherit command instead of specifying the node.", true),


    USER_ATTEMPTING_LOOKUP("&7(Attempting UUID lookup, since you specified a user)", true),
    USER_INVALID_ENTRY("&d%s&c is not a valid username/uuid.", true),

    GROUP_ALREADY_EXISTS("That group already exists!", true),
    GROUP_DOES_NOT_EXIST("That group does not exist!", true),

    GROUP_LOAD_ERROR("An unexpected error occurred. Group not loaded.", true),
    GROUPS_LOAD_ERROR("An unexpected error occurred. Unable to load all groups.", true),


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
            PREFIX + "&eLoaded Groups: &6%s",
            false
    ),

    CREATE_GROUP_ERROR("There was an error whilst creating the group.", true),
    CREATE_GROUP_SUCCESS("&b%s&a was successfully created.", true),
    DELETE_GROUP_ERROR("There was an error whilst deleting the group.", true),
    DELETE_GROUP_ERROR_DEFAULT("You cannot delete the default group.", true),
    DELETE_GROUP_SUCCESS("&b%s&a was successfully deleted.", true),
    GROUPS_LIST("&aGroups: %s", true),

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
            PREFIX + "&d-> &bUse &a/perms user %s listnodes &bto see all permissions.",
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
    USER_PRIMARYGROUP_ERROR_NOTMEMBER("The user must be a member of the group first! Use &4/perms user <user> addgroup <group>", true),

    GROUP_INFO(
            PREFIX + "&d-> &eGroup: &6%s" + "\n" +
            PREFIX + "&d-> &ePermissions: &6%s" + "\n" +
            PREFIX + "&d-> &bUse &a/perms group %s listnodes &bto see all permissions.",
            false
    ),
    GROUP_SETINHERIT_SUCCESS("&b%s&a now inherits permissions from &b%s&a.", true),
    GROUP_SETINHERIT_SERVER_SUCCESS("&b%s&a now inherits permissions from &b%s&a on server &b%s&a.", true),
    GROUP_UNSETINHERIT_SUCCESS("&b%s&a no longer inherits permissions from &b%s&a.", true),
    GROUP_UNSETINHERIT_SERVER_SUCCESS("&b%s&a no longer inherits permissions from &b%s&a on server &b%s&a.", true);

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
