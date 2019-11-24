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

package me.lucko.luckperms.common.locale.command;

import me.lucko.luckperms.common.locale.LocaleManager;

import java.util.List;

/**
 * An enumeration of the command defintion/usage messages used in the plugin.
 *
 * <p>The values in this enum are only defaults, and are only returned if no value for the key is present in the
 * {@link LocaleManager}.</p>
 */
@SuppressWarnings("SpellCheckingInspection")
public enum CommandSpec {

    USER("A set of commands for managing users within LuckPerms. " +
            "(A 'user' in LuckPerms is just a player, and can refer to a UUID or username)",
            "/%s user <user>"
    ),
    GROUP("A set of commands for managing groups within LuckPerms. " +
            "Groups are just collections of permission assignments that can be given to users. " +
            "New groups are made using the 'creategroup' command.",
            "/%s group <group>"
    ),
    TRACK("A set of commands for managing tracks within LuckPerms. " +
            "Tracks are a ordered collection of groups which can be used for defining " +
            "promotions and demotions.",
            "/%s track <track>"
    ),
    LOG("A set of commands for managing the logging functionality within LuckPerms.", "/%s log"),

    SYNC("Reloads all data from the plugins storage into memory, and applies any changes that are detected.", "/%s sync"),
    INFO("Prints general information about the active plugin instance.", "/%s info"),
    EDITOR("Creates a new web editor session", "/%s editor [type]",
            Argument.list(
                    Argument.create("type", false, "the types to load into the editor. ('all', 'users' or 'groups')")
            )
    ),
    DEBUG("Produces a set of internal debugging output", "/%s debug"),
    VERBOSE("Controls the plugins verbose permission check monitoring system.", "/%s verbose <on|record|off|upload> [filter]",
            Argument.list(
                    Argument.create("on|record|off|upload", true, "whether to enable/disable logging, or to upload the logged output"),
                    Argument.create("filter", false, "the filter to match entries against")
            )
    ),
    TREE("Generates a tree view (ordered list hierarchy) of all permissions known to LuckPerms.", "/%s tree [scope] [player]",
            Argument.list(
                    Argument.create("scope", false, "the root of the tree. specify \".\" to include all permissions"),
                    Argument.create("player", false, "the name of an online player to check against")
            )
    ),
    SEARCH("Searchs for all of the users/groups with a specific permission", "/%s search <permission>",
            Argument.list(
                    Argument.create("permission", true, "the permission to search for"),
                    Argument.create("page", false, "the page to view")
            )
    ),
    CHECK("Performs a 'mock' permission check for an online player", "/%s check <user> <permission>",
            Argument.list(
                    Argument.create("user", true, "the user to check"),
                    Argument.create("permission", true, "the permission to check for")
            )
    ),
    NETWORK_SYNC("Sync changes with the storage and request that all other servers on the network do the same", "/%s networksync"),
    IMPORT("Imports data from a (previously created) export file", "/%s import <file>",
            Argument.list(
                    Argument.create("file", true, "the file to import from")
            )
    ),
    EXPORT("Exports all permissions data to an 'export' file. Can be re-imported at a later time.", "/%s export <file>",
            Argument.list(
                    Argument.create("file", true, "the file to export to")
            )
    ),
    RELOAD_CONFIG("Reload some of the config options", "/%s reloadconfig"),
    BULK_UPDATE("Execute bulk change queries on all data", "/%s bulkupdate",
            Argument.list(
                    Argument.create("data type", true, "the type of data being changed. ('all', 'users' or 'groups')"),
                    Argument.create("action", true, "the action to perform on the data. ('update' or 'delete')"),
                    Argument.create("action field", false, "the field to act upon. only required for 'update'. ('permission', 'server' or 'world')"),
                    Argument.create("action value", false, "the value to replace with. only required for 'update'."),
                    Argument.create("constraint...", false, "the constraints required for the update")
            )
    ),
    MIGRATION("Migration commands", "/%s migration"),
    APPLY_EDITS("Applies permission changes made from the web editor", "/%s applyedits <code> [target]",
            Argument.list(
                    Argument.create("code", true, "the unique code for the data"),
                    Argument.create("target", false, "who to apply the data to")
            )
    ),

    CREATE_GROUP("Create a new group", "/%s creategroup <group>",
            Argument.list(
                    Argument.create("name", true, "the name of the group")
            )
    ),
    DELETE_GROUP("Delete a group", "/%s deletegroup <group>",
            Argument.list(
                    Argument.create("name", true, "the name of the group")
            )
    ),
    LIST_GROUPS("List all groups on the platform", "/%s listgroups"),

    CREATE_TRACK("Create a new track", "/%s createtrack <track>",
            Argument.list(
                    Argument.create("name", true, "the name of the track")
            )
    ),
    DELETE_TRACK("Delete a track", "/%s deletetrack <track>",
            Argument.list(
                    Argument.create("name", true, "the name of the track")
            )
    ),
    LIST_TRACKS("List all tracks on the platform", "/%s listtracks"),

    USER_INFO("Shows info about the user"),
    USER_SWITCHPRIMARYGROUP("Switches the user's primary group",
            Argument.list(
                    Argument.create("group", true, "the group to switch to")
            )
    ),
    USER_PROMOTE("Promotes the user up a track",
            Argument.list(
                    Argument.create("track", true, "the track to promote the user up"),
                    Argument.create("context...", false, "the contexts to promote the user in"),
                    Argument.create("--dont-add-to-first", false, "only promote the user if they're already on the track")
            )
    ),
    USER_DEMOTE("Demotes the user down a track",
            Argument.list(
                    Argument.create("track", true, "the track to demote the user down"),
                    Argument.create("context...", false, "the contexts to demote the user in"),
                    Argument.create("--dont-remove-from-first", false, "prevent the user from being removed from the first group")
            )
    ),
    USER_CLONE("Clone the user",
            Argument.list(
                    Argument.create("user", true, "the name/uuid of the user to clone onto")
            )
    ),

    GROUP_INFO("Gives info about the group"),
    GROUP_LISTMEMBERS("Show the users/groups who inherit from this group",
            Argument.list(
                    Argument.create("page", false, "the page to view")
            )
    ),
    GROUP_SETWEIGHT("Set the groups weight",
            Argument.list(
                    Argument.create("weight", true, "the weight to set")
            )
    ),
    GROUP_SET_DISPLAY_NAME("Set the groups display name",
            Argument.list(
                    Argument.create("name", true, "the name to set"),
                    Argument.create("context...", false, "the contexts to set the name in")
            )
    ),
    GROUP_RENAME("Rename the group",
            Argument.list(
                    Argument.create("name", true, "the new name")
            )
    ),
    GROUP_CLONE("Clone the group",
            Argument.list(
                    Argument.create("name", true, "the name of the group to clone onto")
            )
    ),

    HOLDER_EDITOR("Opens the web permission editor"),
    HOLDER_SHOWTRACKS("Lists the tracks that the object is on"),
    HOLDER_CLEAR("Removes all permissions, parents and meta",
            Argument.list(
                    Argument.create("context...", false, "the contexts to filter by")
            )
    ),

    PERMISSION("Edit permissions"),
    PARENT("Edit inheritances"),
    META("Edit metadata values"),

    PERMISSION_INFO("Lists the permission nodes the object has",
            Argument.list(
                    Argument.create("page", false, "the page to view"),
                    Argument.create("sort mode", false, "how to sort the entries")
            )
    ),
    PERMISSION_SET("Sets a permission for the object",
            Argument.list(
                    Argument.create("node", true, "the permission node to set"),
                    Argument.create("true|false", false, "the value of the node"),
                    Argument.create("context...", false, "the contexts to add the permission in")
            )
    ),
    PERMISSION_UNSET("Unsets a permission for the object",
            Argument.list(
                    Argument.create("node", true, "the permission node to unset"),
                    Argument.create("context...", false, "the contexts to remove the permission in")
            )
    ),
    PERMISSION_SETTEMP("Sets a permission for the object temporarily",
            Argument.list(
                    Argument.create("node", true, "the permission node to set"),
                    Argument.create("true|false", false, "the value of the node"),
                    Argument.create("duration", true, "the duration until the permission node expires"),
                    Argument.create("temporary modifier", false, "how the temporary permission should be applied"),
                    Argument.create("context...", false, "the contexts to add the permission in")
            )
    ),
    PERMISSION_UNSETTEMP("Unsets a temporary permission for the object",
            Argument.list(
                    Argument.create("node", true, "the permission node to unset"),
                    Argument.create("context...", false, "the contexts to remove the permission in")
            )
    ),
    PERMISSION_CHECK("Checks to see if the object has a certain permission node",
            Argument.list(
                    Argument.create("node", true, "the permission node to check for"),
                    Argument.create("context...", false, "the contexts to check in")
            )
    ),
    PERMISSION_CHECK_INHERITS("Checks to see if the object inherits a certain permission node",
            Argument.list(
                    Argument.create("node", true, "the permission node to check for"),
                    Argument.create("context...", false, "the contexts to check in")
            )
    ),
    PERMISSION_CLEAR("Clears all permissions",
            Argument.list(
                    Argument.create("context...", false, "the contexts to filter by")
            )
    ),

    PARENT_INFO("Lists the groups that this object inherits from",
            Argument.list(
                    Argument.create("page", false, "the page to view"),
                    Argument.create("sort mode", false, "how to sort the entries")
            )
    ),
    PARENT_SET("Removes all other groups the object inherits already and adds them to the one given",
            Argument.list(
                    Argument.create("group", true, "the group to set to"),
                    Argument.create("context...", false, "the contexts to set the group in")
            )
    ),
    PARENT_ADD("Sets another group for the object to inherit permissions from",
            Argument.list(
                    Argument.create("group", true, "the group to inherit from"),
                    Argument.create("context...", false, "the contexts to inherit the group in")
            )
    ),
    PARENT_REMOVE("Removes a previously set inheritance rule",
            Argument.list(
                    Argument.create("group", true, "the group to remove"),
                    Argument.create("context...", false, "the contexts to remove the group in")
            )
    ),
    PARENT_SET_TRACK("Removes all other groups the object inherits from already on the given track and adds them to the one given",
            Argument.list(
                    Argument.create("track", true, "the track to set on"),
                    Argument.create("group", true, "the group to set to, or a number relating to the position of the group on the given track"),
                    Argument.create("context...", false, "the contexts to set the group in")
            )
    ),
    PARENT_ADD_TEMP("Sets another group for the object to inherit permissions from temporarily",
            Argument.list(
                    Argument.create("group", true, "the group to inherit from"),
                    Argument.create("duration", true, "the duration of the group membership"),
                    Argument.create("temporary modifier", false, "how the temporary permission should be applied"),
                    Argument.create("context...", false, "the contexts to inherit the group in")
            )
    ),
    PARENT_REMOVE_TEMP("Removes a previously set temporary inheritance rule",
            Argument.list(
                    Argument.create("group", true, "the group to remove"),
                    Argument.create("context...", false, "the contexts to remove the group in")
            )
    ),
    PARENT_CLEAR("Clears all parents",
            Argument.list(
                    Argument.create("context...", false, "the contexts to filter by")
            )
    ),
    PARENT_CLEAR_TRACK("Clears all parents on a given track",
            Argument.list(
                    Argument.create("track", true, "the track to remove on"),
                    Argument.create("context...", false, "the contexts to filter by")
            )
    ),

    META_INFO("Shows all chat meta"),
    META_SET("Sets a meta value",
            Argument.list(
                    Argument.create("key", true, "the key to set"),
                    Argument.create("value", true, "the value to set"),
                    Argument.create("context...", false, "the contexts to add the meta pair in")
            )
    ),
    META_UNSET("Unsets a meta value",
            Argument.list(
                    Argument.create("key", true, "the key to unset"),
                    Argument.create("context...", false, "the contexts to remove the meta pair in")
            )
    ),
    META_SETTEMP("Sets a meta value temporarily",
            Argument.list(
                    Argument.create("key", true, "the key to set"),
                    Argument.create("value", true, "the value to set"),
                    Argument.create("duration", true, "the duration until the meta value expires"),
                    Argument.create("context...", false, "the contexts to add the meta pair in")
            )
    ),
    META_UNSETTEMP("Unsets a temporary meta value",
            Argument.list(
                    Argument.create("key", true, "the key to unset"),
                    Argument.create("context...", false, "the contexts to remove the meta pair in")
            )
    ),
    META_ADDPREFIX("Adds a prefix",
            Argument.list(
                    Argument.create("priority", true, "the priority to add the prefix at"),
                    Argument.create("prefix", true, "the prefix string"),
                    Argument.create("context...", false, "the contexts to add the prefix in")
            )
    ),
    META_ADDSUFFIX("Adds a suffix",
            Argument.list(
                    Argument.create("priority", true, "the priority to add the suffix at"),
                    Argument.create("suffix", true, "the suffix string"),
                    Argument.create("context...", false, "the contexts to add the suffix in")
            )
    ),
    META_SETPREFIX("Sets a prefix",
            Argument.list(
                    Argument.create("priority", false, "the priority to set the prefix at"),
                    Argument.create("prefix", true, "the prefix string"),
                    Argument.create("context...", false, "the contexts to set the prefix in")
            )
    ),
    META_SETSUFFIX("Sets a suffix",
            Argument.list(
                    Argument.create("priority", false, "the priority to set the suffix at"),
                    Argument.create("suffix", true, "the suffix string"),
                    Argument.create("context...", false, "the contexts to set the suffix in")
            )
    ),
    META_REMOVEPREFIX("Removes a prefix",
            Argument.list(
                    Argument.create("priority", true, "the priority to remove the prefix at"),
                    Argument.create("prefix", false, "the prefix string"),
                    Argument.create("context...", false, "the contexts to remove the prefix in")
            )
    ),
    META_REMOVESUFFIX("Removes a suffix",
            Argument.list(
                    Argument.create("priority", true, "the priority to remove the suffix at"),
                    Argument.create("suffix", false, "the suffix string"),
                    Argument.create("context...", false, "the contexts to remove the suffix in")
            )
    ),
    META_ADDTEMP_PREFIX("Adds a prefix temporarily",
            Argument.list(
                    Argument.create("priority", true, "the priority to add the prefix at"),
                    Argument.create("prefix", true, "the prefix string"),
                    Argument.create("duration", true, "the duration until the prefix expires"),
                    Argument.create("context...", false, "the contexts to add the prefix in")
            )
    ),
    META_ADDTEMP_SUFFIX("Adds a suffix temporarily",
            Argument.list(
                    Argument.create("priority", true, "the priority to add the suffix at"),
                    Argument.create("suffix", true, "the suffix string"),
                    Argument.create("duration", true, "the duration until the suffix expires"),
                    Argument.create("context...", false, "the contexts to add the suffix in")
            )
    ),
    META_SETTEMP_PREFIX("Sets a prefix temporarily",
            Argument.list(
                    Argument.create("priority", true, "the priority to set the prefix at"),
                    Argument.create("prefix", true, "the prefix string"),
                    Argument.create("duration", true, "the duration until the prefix expires"),
                    Argument.create("context...", false, "the contexts to set the prefix in")
            )
    ),
    META_SETTEMP_SUFFIX("Sets a suffix temporarily",
            Argument.list(
                    Argument.create("priority", true, "the priority to set the suffix at"),
                    Argument.create("suffix", true, "the suffix string"),
                    Argument.create("duration", true, "the duration until the suffix expires"),
                    Argument.create("context...", false, "the contexts to set the suffix in")
            )
    ),
    META_REMOVETEMP_PREFIX("Removes a temporary prefix",
            Argument.list(
                    Argument.create("priority", true, "the priority to remove the prefix at"),
                    Argument.create("prefix", false, "the prefix string"),
                    Argument.create("context...", false, "the contexts to remove the prefix in")
            )
    ),
    META_REMOVETEMP_SUFFIX("Removes a temporary suffix",
            Argument.list(
                    Argument.create("priority", true, "the priority to remove the suffix at"),
                    Argument.create("suffix", false, "the suffix string"),
                    Argument.create("context...", false, "the contexts to remove the suffix in")
            )
    ),
    META_CLEAR("Clears all meta",
            Argument.list(
                    Argument.create("type", false, "the type of meta to remove"),
                    Argument.create("context...", false, "the contexts to filter by")
            )
    ),

    TRACK_INFO("Gives info about the track"),
    TRACK_APPEND("Appends a group onto the end of the track",
            Argument.list(
                    Argument.create("group", true, "the group to append")
            )
    ),
    TRACK_INSERT("Inserts a group at a given position along the track",
            Argument.list(
                    Argument.create("group", true, "the group to insert"),
                    Argument.create("position", true, "the position to insert the group at (the first position on the track is 1)")
            )
    ),
    TRACK_REMOVE("Removes a group from the track",
            Argument.list(
                    Argument.create("group", true, "the group to remove")
            )
    ),
    TRACK_CLEAR("Clears the groups on the track"),
    TRACK_RENAME("Rename the track",
            Argument.list(
                    Argument.create("name", true, "the new name")
            )
    ),
    TRACK_CLONE("Clone the track",
            Argument.list(
                    Argument.create("name", true, "the name of the track to clone onto")
            )
    ),

    LOG_RECENT("View recent actions",
            Argument.list(
                    Argument.create("user", false, "the name/uuid of the user to filter by"),
                    Argument.create("page", false, "the page number to view")
            )
    ),
    LOG_SEARCH("Search the log for an entry",
            Argument.list(
                    Argument.create("query", true, "the query to search by"),
                    Argument.create("page", false, "the page number to view")
            )
    ),
    LOG_NOTIFY("Toggle log notifications",
            Argument.list(
                    Argument.create("on|off", false, "whether to toggle on or off")
            )
    ),
    LOG_USER_HISTORY("View a user's history",
            Argument.list(
                    Argument.create("user", true, "the name/uuid of the user"),
                    Argument.create("page", false, "the page number to view")
            )
    ),
    LOG_GROUP_HISTORY("View an group's history",
            Argument.list(
                    Argument.create("group", true, "the name of the group"),
                    Argument.create("page", false, "the page number to view")
            )
    ),
    LOG_TRACK_HISTORY("View a track's history",
            Argument.list(
                    Argument.create("track", true, "the name of the track"),
                    Argument.create("page", false, "the page number to view")
            )
    ),

    SPONGE("Edit extra Sponge data", "/%s sponge <collection> <subject>",
            Argument.list(
                    Argument.create("collection", true, "the collection to query"),
                    Argument.create("subject", true, "the subject to modify")
            )
    ),
    SPONGE_PERMISSION_INFO("Shows info about the subject's permissions",
            Argument.list(
                    Argument.create("contexts...", false, "the contexts to filter by")
            )
    ),
    SPONGE_PERMISSION_SET("Sets a permission for the Subject",
            Argument.list(
                    Argument.create("node", true, "the permission node to set"),
                    Argument.create("tristate", true, "the value to set the permission to"),
                    Argument.create("contexts...", false, "the contexts to set the permission in")
            )
    ),
    SPONGE_PERMISSION_CLEAR("Clears the Subjects permissions",
            Argument.list(
                    Argument.create("contexts...", false, "the contexts to clear permissions in")
            )
    ),
    SPONGE_PARENT_INFO("Shows info about the subject's parents",
            Argument.list(
                    Argument.create("contexts...", false, "the contexts to filter by")
            )
    ),
    SPONGE_PARENT_ADD("Adds a parent to the Subject",
            Argument.list(
                    Argument.create("collection", true, "the subject collection where the parent Subject is"),
                    Argument.create("subject", true, "the name of the parent Subject"),
                    Argument.create("contexts...", false, "the contexts to add the parent in")
            )
    ),
    SPONGE_PARENT_REMOVE("Removes a parent from the Subject",
            Argument.list(
                    Argument.create("collection", true, "the subject collection where the parent Subject is"),
                    Argument.create("subject", true, "the name of the parent Subject"),
                    Argument.create("contexts...", false, "the contexts to remove the parent in")
            )
    ),
    SPONGE_PARENT_CLEAR("Clears the Subjects parents",
            Argument.list(
                    Argument.create("contexts...", false, "the contexts to clear parents in")
            )
    ),
    SPONGE_OPTION_INFO("Shows info about the subject's options",
            Argument.list(
                    Argument.create("contexts...", false, "the contexts to filter by")
            )
    ),
    SPONGE_OPTION_SET("Sets an option for the Subject",
            Argument.list(
                    Argument.create("key", true, "the key to set"),
                    Argument.create("value", true, "the value to set the key to"),
                    Argument.create("contexts...", false, "the contexts to set the option in")
            )
    ),
    SPONGE_OPTION_UNSET("Unsets an option for the Subject",
            Argument.list(
                    Argument.create("key", true, "the key to unset"),
                    Argument.create("contexts...", false, "the contexts to unset the key in")
            )
    ),
    SPONGE_OPTION_CLEAR("Clears the Subjects options",
            Argument.list(
                    Argument.create("contexts...", false, "the contexts to clear options in")
            )
    ),

    MIGRATION_COMMAND("Migration command"),
    MIGRATION_GROUPMANAGER("Migration command",
            Argument.list(
                    Argument.create("migrate as global", true, "if world permissions should be ignored, and just migrated as global")
            )
    ),
    MIGRATION_POWERFULPERMS("Migration command",
            Argument.list(
                    Argument.create("address", true, "the address of the PP database"),
                    Argument.create("database", true, "the name of the PP database"),
                    Argument.create("username", true, "the username to log into the DB"),
                    Argument.create("password", true, "the password to log into the DB"),
                    Argument.create("db table", true, "the name of the PP table where player data is stored")
            )
    );

    private final String description;
    private final String usage;
    private final List<Argument> args;

    CommandSpec(String description, String usage, List<Argument> args) {
        this.description = description;
        this.usage = usage;
        this.args = args;
    }

    CommandSpec(String description, String usage) {
        this(description, usage, null);
    }

    CommandSpec(String description) {
        this(description, null, null);
    }

    CommandSpec(String description, List<Argument> args) {
        this(description, null, args);
    }

    public String getDescription() {
        return this.description;
    }

    public String getUsage() {
        return this.usage;
    }

    public List<Argument> getArgs() {
        return this.args;
    }

    /**
     * Creates a {@link LocalizedCommandSpec} for the spec using the platforms locale manager.
     *
     * @param localeManager the locale manager to use for the spec
     * @return a localized spec instance
     */
    public LocalizedCommandSpec localize(LocaleManager localeManager) {
        return new LocalizedCommandSpec(this, localeManager);
    }

    /**
     * Prints this CommandSpec enum in a yml format, for reading by the {@link me.lucko.luckperms.common.locale.LocaleManager}
     * @param args not needed
     */
    public static void main(String[] args) {
        System.out.println("command-specs:");

        for (CommandSpec spec : values()) {
            String key = spec.name().replace('_', '-').toLowerCase();

            System.out.println("  " + key + ":");

            if (spec.description != null) {
                System.out.println("    description: \"" + spec.description.replace("\"", "\\\"") + "\"");
            }
            if (spec.usage != null) {
                System.out.println("    usage: \"" + spec.usage.replace("\"", "\\\"") + "\"");
            }

            if (spec.args != null && !spec.args.isEmpty()) {
                System.out.println("    args:");
                for (Argument arg : spec.args) {
                    System.out.println("      \"" + arg.getName() + "\": \"" + arg.getDescription().replace("\"", "\\\"") + "\"");
                }
            }
        }
    }

}
