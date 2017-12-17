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

import lombok.Getter;

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.common.commands.Arg;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * An enumeration of the command defintion/usage messages used in the plugin.
 *
 * <p>The values in this enum are only defaults, and are only returned if no value for the key is present in the
 * {@link LocaleManager}.</p>
 */
@SuppressWarnings("SpellCheckingInspection")
public enum CommandSpec {

    USER("User commands", "/%s user <user>"),
    GROUP("Group commands", "/%s group <group>"),
    TRACK("Track commands", "/%s track <track>"),
    LOG("Log commands", "/%s log"),

    SYNC("Sync changes with the storage", "/%s sync"),
    INFO("Print general plugin info", "/%s info"),
    VERBOSE("Manage verbose permission checking", "/%s verbose <true|false> [filter]",
            Arg.list(
                    Arg.create("on|record|off|paste", true, "whether to enable/disable logging, or to paste the logged output"),
                    Arg.create("filter", false, "the filter to match entries against"),
                    Arg.create("--slim", false, "add \"--slim\" to exclude trace data from the pasted output")
            )
    ),
    TREE("Generate a tree view of permissions", "/%s tree [selection] [max level] [player]",
            Arg.list(
                    Arg.create("selection", false, "the root of the tree. specify \".\" to include all permissions"),
                    Arg.create("max level", false, "how many branch levels should be returned"),
                    Arg.create("player", false, "the name of an online player to check against")
            )
    ),
    SEARCH("Search for users/groups with a specific permission", "/%s search <permission>",
            Arg.list(
                    Arg.create("permission", true, "the permission to search for"),
                    Arg.create("page", false, "the page to view")
            )
    ),
    CHECK("Perform a standard permission check on an online player", "/%s check <user> <permission>",
            Arg.list(
                    Arg.create("user", true, "the user to check"),
                    Arg.create("permission", true, "the permission to check for")
            )
    ),
    NETWORK_SYNC("Sync changes with the storage and request that all other servers on the network do the same", "/%s networksync"),
    IMPORT("Import data from a file", "/%s import <file>",
            Arg.list(
                    Arg.create("file", true, "the file to import from")
            )
    ),
    EXPORT("Export data to a file", "/%s export <file>",
            Arg.list(
                    Arg.create("file", true, "the file to export to")
            )
    ),
    RELOAD_CONFIG("Reload some of the config options", "/%s reloadconfig"),
    BULK_UPDATE("Execute bulk change queries on all data", "/%s bulkupdate",
            Arg.list(
                    Arg.create("data type", true, "the type of data being changed. ('all', 'users' or 'groups')"),
                    Arg.create("action", true, "the action to perform on the data. ('update' or 'delete')"),
                    Arg.create("action field", false, "the field to act upon. only required for 'update'. ('permission', 'server' or 'world')"),
                    Arg.create("action value", false, "the value to replace with. only required for 'update'."),
                    Arg.create("constraint...", false, "the constraints required for the update")
            )
    ),
    MIGRATION("Migration commands", "/%s migration"),
    APPLY_EDITS("Applies permission changes made from the web editor", "/%s applyedits <code> [target]",
            Arg.list(
                    Arg.create("code", true, "the unique code for the data"),
                    Arg.create("target", false, "who to apply the data to")
            )
    ),

    CREATE_GROUP("Create a new group", "/%s creategroup <group>",
            Arg.list(
                    Arg.create("name", true, "the name of the group")
            )
    ),
    DELETE_GROUP("Delete a group", "/%s deletegroup <group>",
            Arg.list(
                    Arg.create("name", true, "the name of the group")
            )
    ),
    LIST_GROUPS("List all groups on the platform", "/%s listgroups"),

    CREATE_TRACK("Create a new track", "/%s createtrack <track>",
            Arg.list(
                    Arg.create("name", true, "the name of the track")
            )
    ),
    DELETE_TRACK("Delete a track", "/%s deletetrack <track>",
            Arg.list(
                    Arg.create("name", true, "the name of the track")
            )
    ),
    LIST_TRACKS("List all tracks on the platform", "/%s listtracks"),

    USER_INFO("Shows info about the user"),
    USER_SWITCHPRIMARYGROUP("Switches the user's primary group",
            Arg.list(
                    Arg.create("group", true, "the group to switch to")
            )
    ),
    USER_PROMOTE("Promotes the user up a track",
            Arg.list(
                    Arg.create("track", true, "the track to promote the user up"),
                    Arg.create("context...", false, "the contexts to promote the user in")
            )
    ),
    USER_DEMOTE("Demotes the user down a track",
            Arg.list(
                    Arg.create("track", true, "the track to demote the user down"),
                    Arg.create("context...", false, "the contexts to demote the user in")
            )
    ),
    USER_CLONE("Clone the user",
            Arg.list(
                    Arg.create("user", true, "the name/uuid of the user to clone onto")
            )
    ),

    GROUP_INFO("Gives info about the group"),
    GROUP_LISTMEMBERS("Show the users/groups who inherit from this group",
            Arg.list(
                    Arg.create("page", false, "the page to view")
            )
    ),
    GROUP_SETWEIGHT("Set the groups weight",
            Arg.list(
                    Arg.create("weight", true, "the weight to set")
            )
    ),
    GROUP_SET_DISPLAY_NAME("Set the groups display name",
            Arg.list(
                    Arg.create("name", true, "the name to set")
            )
    ),
    GROUP_RENAME("Rename the group",
            Arg.list(
                    Arg.create("name", true, "the new name")
            )
    ),
    GROUP_CLONE("Clone the group",
            Arg.list(
                    Arg.create("name", true, "the name of the group to clone onto")
            )
    ),

    HOLDER_EDITOR("Opens the web permission editor"),
    HOLDER_SHOWTRACKS("Lists the tracks that the object is on"),
    HOLDER_CLEAR("Removes all permissions, parents and meta",
            Arg.list(
                    Arg.create("context...", false, "the contexts to filter by")
            )
    ),

    PERMISSION("Edit permissions"),
    PARENT("Edit inheritances"),
    META("Edit metadata values"),

    PERMISSION_INFO("Lists the permission nodes the object has",
            Arg.list(
                    Arg.create("page", false, "the page to view"),
                    Arg.create("sort mode", false, "how to sort the entries")
            )
    ),
    PERMISSION_SET("Sets a permission for the object",
            Arg.list(
                    Arg.create("node", true, "the permission node to set"),
                    Arg.create("true|false", false, "the value of the node"),
                    Arg.create("context...", false, "the contexts to add the permission in")
            )
    ),
    PERMISSION_UNSET("Unsets a permission for the object",
            Arg.list(
                    Arg.create("node", true, "the permission node to unset"),
                    Arg.create("context...", false, "the contexts to remove the permission in")
            )
    ),
    PERMISSION_SETTEMP("Sets a permission for the object temporarily",
            Arg.list(
                    Arg.create("node", true, "the permission node to set"),
                    Arg.create("true|false", false, "the value of the node"),
                    Arg.create("duration", true, "the duration until the permission node expires"),
                    Arg.create("context...", false, "the contexts to add the permission in")
            )
    ),
    PERMISSION_UNSETTEMP("Unsets a temporary permission for the object",
            Arg.list(
                    Arg.create("node", true, "the permission node to unset"),
                    Arg.create("context...", false, "the contexts to remove the permission in")
            )
    ),
    PERMISSION_CHECK("Checks to see if the object has a certain permission node",
            Arg.list(
                    Arg.create("node", true, "the permission node to check for"),
                    Arg.create("context...", false, "the contexts to check in")
            )
    ),
    PERMISSION_CHECK_INHERITS("Checks to see if the object inherits a certain permission node",
            Arg.list(
                    Arg.create("node", true, "the permission node to check for"),
                    Arg.create("context...", false, "the contexts to check in")
            )
    ),

    PARENT_INFO("Lists the groups that this object inherits from",
            Arg.list(
                    Arg.create("page", false, "the page to view"),
                    Arg.create("sort mode", false, "how to sort the entries")
            )
    ),
    PARENT_SET("Removes all other groups the object inherits already and adds them to the one given",
            Arg.list(
                    Arg.create("group", true, "the group to set to"),
                    Arg.create("context...", false, "the contexts to set the group in")
            )
    ),
    PARENT_ADD("Sets another group for the object to inherit permissions from",
            Arg.list(
                    Arg.create("group", true, "the group to inherit from"),
                    Arg.create("context...", false, "the contexts to inherit the group in")
            )
    ),
    PARENT_REMOVE("Removes a previously set inheritance rule",
            Arg.list(
                    Arg.create("group", true, "the group to remove"),
                    Arg.create("context...", false, "the contexts to remove the group in")
            )
    ),
    PARENT_SET_TRACK("Removes all other groups the object inherits from already on the given track and adds them to the one given",
            Arg.list(
                    Arg.create("track", true, "the track to set on"),
                    Arg.create("group", true, "the group to set to, or a number relating to the position of the group on the given track"),
                    Arg.create("context...", false, "the contexts to set the group in")
            )
    ),
    PARENT_ADD_TEMP("Sets another group for the object to inherit permissions from temporarily",
            Arg.list(
                    Arg.create("group", true, "the group to inherit from"),
                    Arg.create("duration", true, "the duration of the group membership"),
                    Arg.create("context...", false, "the contexts to inherit the group in")
            )
    ),
    PARENT_REMOVE_TEMP("Removes a previously set temporary inheritance rule",
            Arg.list(
                    Arg.create("group", true, "the group to remove"),
                    Arg.create("context...", false, "the contexts to remove the group in")
            )
    ),
    PARENT_CLEAR("Clears all parents",
            Arg.list(
                    Arg.create("context...", false, "the contexts to filter by")
            )
    ),
    PARENT_CLEAR_TRACK("Clears all parents on a given track",
            Arg.list(
                    Arg.create("track", true, "the track to remove on"),
                    Arg.create("context...", false, "the contexts to filter by")
            )
    ),

    META_INFO("Shows all chat meta"),
    META_SET("Sets a meta value",
            Arg.list(
                    Arg.create("key", true, "the key to set"),
                    Arg.create("value", true, "the value to set"),
                    Arg.create("context...", false, "the contexts to add the meta pair in")
            )
    ),
    META_UNSET("Unsets a meta value",
            Arg.list(
                    Arg.create("key", true, "the key to unset"),
                    Arg.create("context...", false, "the contexts to remove the meta pair in")
            )
    ),
    META_SETTEMP("Sets a meta value temporarily",
            Arg.list(
                    Arg.create("key", true, "the key to set"),
                    Arg.create("value", true, "the value to set"),
                    Arg.create("duration", true, "the duration until the meta value expires"),
                    Arg.create("context...", false, "the contexts to add the meta pair in")
            )
    ),
    META_UNSETTEMP("Unsets a temporary meta value",
            Arg.list(
                    Arg.create("key", true, "the key to unset"),
                    Arg.create("context...", false, "the contexts to remove the meta pair in")
            )
    ),
    META_ADDPREFIX("Adds a prefix",
            Arg.list(
                    Arg.create("priority", true, "the priority to add the prefix at"),
                    Arg.create("prefix", true, "the prefix string"),
                    Arg.create("context...", false, "the contexts to add the prefix in")
            )
    ),
    META_ADDSUFFIX("Adds a suffix",
            Arg.list(
                    Arg.create("priority", true, "the priority to add the suffix at"),
                    Arg.create("suffix", true, "the suffix string"),
                    Arg.create("context...", false, "the contexts to add the suffix in")
            )
    ),
    META_REMOVEPREFIX("Removes a prefix",
            Arg.list(
                    Arg.create("priority", true, "the priority to remove the prefix at"),
                    Arg.create("prefix", false, "the prefix string"),
                    Arg.create("context...", false, "the contexts to remove the prefix in")
            )
    ),
    META_REMOVESUFFIX("Removes a suffix",
            Arg.list(
                    Arg.create("priority", true, "the priority to remove the suffix at"),
                    Arg.create("suffix", false, "the suffix string"),
                    Arg.create("context...", false, "the contexts to remove the suffix in")
            )
    ),
    META_ADDTEMP_PREFIX("Adds a prefix temporarily",
            Arg.list(
                    Arg.create("priority", true, "the priority to add the prefix at"),
                    Arg.create("prefix", true, "the prefix string"),
                    Arg.create("duration", true, "the duration until the prefix expires"),
                    Arg.create("context...", false, "the contexts to add the prefix in")
            )
    ),
    META_ADDTEMP_SUFFIX("Adds a suffix temporarily",
            Arg.list(
                    Arg.create("priority", true, "the priority to add the suffix at"),
                    Arg.create("suffix", true, "the suffix string"),
                    Arg.create("duration", true, "the duration until the suffix expires"),
                    Arg.create("context...", false, "the contexts to add the suffix in")
            )
    ),
    META_REMOVETEMP_PREFIX("Removes a temporary prefix",
            Arg.list(
                    Arg.create("priority", true, "the priority to remove the prefix at"),
                    Arg.create("prefix", false, "the prefix string"),
                    Arg.create("context...", false, "the contexts to remove the prefix in")
            )
    ),
    META_REMOVETEMP_SUFFIX("Removes a temporary suffix",
            Arg.list(
                    Arg.create("priority", true, "the priority to remove the suffix at"),
                    Arg.create("suffix", false, "the suffix string"),
                    Arg.create("context...", false, "the contexts to remove the suffix in")
            )
    ),
    META_CLEAR("Clears all meta",
            Arg.list(
                    Arg.create("type", false, "the type of meta to remove"),
                    Arg.create("context...", false, "the contexts to filter by")
            )
    ),

    TRACK_INFO("Gives info about the track"),
    TRACK_APPEND("Appends a group onto the end of the track",
            Arg.list(
                    Arg.create("group", true, "the group to append")
            )
    ),
    TRACK_INSERT("Inserts a group at a given position along the track",
            Arg.list(
                    Arg.create("group", true, "the group to insert"),
                    Arg.create("position", true, "the position to insert the group at (the first position on the track is 1)")
            )
    ),
    TRACK_REMOVE("Removes a group from the track",
            Arg.list(
                    Arg.create("group", true, "the group to remove")
            )
    ),
    TRACK_CLEAR("Clears the groups on the track"),
    TRACK_RENAME("Rename the track",
            Arg.list(
                    Arg.create("name", true, "the new name")
            )
    ),
    TRACK_CLONE("Clone the track",
            Arg.list(
                    Arg.create("name", true, "the name of the track to clone onto")
            )
    ),

    LOG_RECENT("View recent actions",
            Arg.list(
                    Arg.create("user", false, "the name/uuid of the user to filter by"),
                    Arg.create("page", false, "the page number to view")
            )
    ),
    LOG_SEARCH("Search the log for an entry",
            Arg.list(
                    Arg.create("query", true, "the query to search by"),
                    Arg.create("page", false, "the page number to view")
            )
    ),
    LOG_NOTIFY("Toggle log notifications",
            Arg.list(
                    Arg.create("on|off", false, "whether to toggle on or off")
            )
    ),
    LOG_USER_HISTORY("View a user's history",
            Arg.list(
                    Arg.create("user", true, "the name/uuid of the user"),
                    Arg.create("page", false, "the page number to view")
            )
    ),
    LOG_GROUP_HISTORY("View an group's history",
            Arg.list(
                    Arg.create("group", true, "the name of the group"),
                    Arg.create("page", false, "the page number to view")
            )
    ),
    LOG_TRACK_HISTORY("View a track's history",
            Arg.list(
                    Arg.create("track", true, "the name of the track"),
                    Arg.create("page", false, "the page number to view")
            )
    ),

    SPONGE("Edit extra Sponge data", "/%s sponge <collection> <subject>",
            Arg.list(
                    Arg.create("collection", true, "the collection to query"),
                    Arg.create("subject", true, "the subject to modify")
            )
    ),
    SPONGE_PERMISSION_INFO("Shows info about the subject's permissions",
            Arg.list(
                    Arg.create("contexts...", false, "the contexts to filter by")
            )
    ),
    SPONGE_PERMISSION_SET("Sets a permission for the Subject",
            Arg.list(
                    Arg.create("node", true, "the permission node to set"),
                    Arg.create("tristate", true, "the value to set the permission to"),
                    Arg.create("contexts...", false, "the contexts to set the permission in")
            )
    ),
    SPONGE_PERMISSION_CLEAR("Clears the Subjects permissions",
            Arg.list(
                    Arg.create("contexts...", false, "the contexts to clear permissions in")
            )
    ),
    SPONGE_PARENT_INFO("Shows info about the subject's parents",
            Arg.list(
                    Arg.create("contexts...", false, "the contexts to filter by")
            )
    ),
    SPONGE_PARENT_ADD("Adds a parent to the Subject",
            Arg.list(
                    Arg.create("collection", true, "the subject collection where the parent Subject is"),
                    Arg.create("subject", true, "the name of the parent Subject"),
                    Arg.create("contexts...", false, "the contexts to add the parent in")
            )
    ),
    SPONGE_PARENT_REMOVE("Removes a parent from the Subject",
            Arg.list(
                    Arg.create("collection", true, "the subject collection where the parent Subject is"),
                    Arg.create("subject", true, "the name of the parent Subject"),
                    Arg.create("contexts...", false, "the contexts to remove the parent in")
            )
    ),
    SPONGE_PARENT_CLEAR("Clears the Subjects parents",
            Arg.list(
                    Arg.create("contexts...", false, "the contexts to clear parents in")
            )
    ),
    SPONGE_OPTION_INFO("Shows info about the subject's options",
            Arg.list(
                    Arg.create("contexts...", false, "the contexts to filter by")
            )
    ),
    SPONGE_OPTION_SET("Sets an option for the Subject",
            Arg.list(
                    Arg.create("key", true, "the key to set"),
                    Arg.create("value", true, "the value to set the key to"),
                    Arg.create("contexts...", false, "the contexts to set the option in")
            )
    ),
    SPONGE_OPTION_UNSET("Unsets an option for the Subject",
            Arg.list(
                    Arg.create("key", true, "the key to unset"),
                    Arg.create("contexts...", false, "the contexts to unset the key in")
            )
    ),
    SPONGE_OPTION_CLEAR("Clears the Subjects options",
            Arg.list(
                    Arg.create("contexts...", false, "the contexts to clear options in")
            )
    ),

    MIGRATION_COMMAND("Migration command"),
    MIGRATION_GROUPMANAGER("Migration command",
            Arg.list(
                    Arg.create("migrate as global", true, "if world permissions should be ignored, and just migrated as global")
            )
    ),
    MIGRATION_POWERFULPERMS("Migration command",
            Arg.list(
                    Arg.create("address", true, "the address of the PP database"),
                    Arg.create("database", true, "the name of the PP database"),
                    Arg.create("username", true, "the username to log into the DB"),
                    Arg.create("password", true, "the password to log into the DB"),
                    Arg.create("db table", true, "the name of the PP table where player data is stored")
            )
    );

    private final String description;
    private final String usage;
    private final List<Arg> args;

    CommandSpec(String description, String usage, List<Arg> args) {
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

    CommandSpec(String description, List<Arg> args) {
        this(description, null, args);
    }

    /**
     * Creates a {@link LocalizedSpec} for the spec using the platforms locale manager.
     *
     * @param localeManager the locale manager to use for the spec
     * @return a localized spec instance
     */
    public LocalizedSpec spec(LocaleManager localeManager) {
        return new SimpleLocalizedSpec(this, localeManager);
    }

    private static final class SimpleLocalizedSpec implements LocalizedSpec {

        @Getter
        private final LocaleManager localeManager;

        private final CommandSpec spec;

        public SimpleLocalizedSpec(CommandSpec spec, LocaleManager localeManager) {
            this.localeManager = localeManager;
            this.spec = spec;
        }

        public String description() {
            CommandSpecData translation = localeManager.getTranslation(spec);
            if (translation != null && translation.getDescription() != null) {
                return translation.getDescription();
            }

            // fallback
            return spec.description;
        }

        public String usage() {
            CommandSpecData translation = localeManager.getTranslation(spec);
            if (translation != null && translation.getUsage() != null) {
                return translation.getUsage();
            }

            // fallback
            return spec.usage;
        }

        public List<Arg> args() {
            CommandSpecData translation = localeManager.getTranslation(spec);
            if (translation == null || translation.getArgs() == null) {
                // fallback
                return spec.args;
            }

            List<Arg> args = new ArrayList<>(spec.args);
            ListIterator<Arg> it = args.listIterator();
            while (it.hasNext()) {
                Arg next = it.next();
                String s = translation.getArgs().get(next.getName());

                // if a translation for the given arg key is present, apply the new description.
                if (s != null) {
                    it.set(Arg.create(next.getName(), next.isRequired(), s));
                }
            }

            return ImmutableList.copyOf(args);
        }
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
                for (Arg arg : spec.args) {
                    System.out.println("      \"" + arg.getName() + "\": \"" + arg.getDescription().replace("\"", "\\\"") + "\"");
                }
            }
        }
    }

}
