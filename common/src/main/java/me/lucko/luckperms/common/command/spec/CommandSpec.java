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

package me.lucko.luckperms.common.command.spec;

import me.lucko.luckperms.common.util.ImmutableCollectors;

import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.List;

/**
 * An enumeration of the command defintion/usage messages used in the plugin.
 */
@SuppressWarnings("SpellCheckingInspection")
public enum CommandSpec {

    USER("/%s user <user>"),
    GROUP("/%s group <group>"),
    TRACK("/%s track <track>"),
    LOG("/%s log"),

    SYNC("/%s sync"),
    INFO("/%s info"),
    EDITOR("/%s editor [type]",
            arg("type", false),
            arg("filter", false)
    ),
    VERBOSE("/%s verbose <on|record|off|upload> [filter]",
            arg("action", "on|record|off|upload|command", true),
            arg("filter", false),
            arg("commandas", "<me|player> <command>", false)
    ),
    TREE("/%s tree [scope] [player]",
            arg("scope", false),
            arg("player", false)
    ),
    SEARCH("/%s search <permission>",
            arg("permission", true),
            arg("page", false)
    ),
    CHECK("/%s check <user> <permission>",
            arg("user", true),
            arg("permission", true)
    ),
    NETWORK_SYNC("/%s networksync"),
    IMPORT("/%s import <file>",
            arg("file", true),
            arg("replace", "--replace", false),
            arg("upload", "--upload", false)
    ),
    EXPORT("/%s export <file>",
            arg("file", true),
            arg("without-users", "--without-users", false),
            arg("without-groups", "--without-groups", false),
            arg("upload", "--upload", false)
    ),
    RELOAD_CONFIG("/%s reloadconfig"),
    BULK_UPDATE("/%s bulkupdate",
            arg("data type", true),
            arg("action", true),
            arg("action field", false),
            arg("action value", false),
            arg("constraint...", false)
    ),
    TRANSLATIONS("/%s translations",
            arg("install", false)
    ),
    APPLY_EDITS("/%s applyedits <code> [target]",
            arg("code", true),
            arg("target", false)
    ),

    CREATE_GROUP("/%s creategroup <group>",
            arg("name", true),
            arg("weight", false),
            arg("display-name", "displayname", false)
    ),
    DELETE_GROUP("/%s deletegroup <group>",
                 arg("name", true)
    ),
    LIST_GROUPS("/%s listgroups"),

    CREATE_TRACK("/%s createtrack <track>",
            arg("name", true)
    ),
    DELETE_TRACK("/%s deletetrack <track>",
                 arg("name", true)
    ),
    LIST_TRACKS("/%s listtracks"),

    USER_INFO,
    USER_SWITCHPRIMARYGROUP(
            (arg("group", true))
    ),
    USER_PROMOTE(
            arg("track", false),
            arg("context...", false),
            arg("dont-add-to-first", "--dont-add-to-first", false)
    ),
    USER_DEMOTE(
            arg("track", false),
            arg("context...", false),
            arg("dont-remove-from-first", "--dont-remove-from-first", false)
    ),
    USER_CLONE(
            arg("user", true)
    ),

    GROUP_INFO,
    GROUP_LISTMEMBERS(
            arg("page", false)
    ),
    GROUP_SETWEIGHT(
            arg("weight", true)
    ),
    GROUP_SET_DISPLAY_NAME(
            arg("name", true),
            arg("context...", false)
    ),
    GROUP_RENAME(
            arg("name", true)
    ),
    GROUP_CLONE(
            arg("name", true)
    ),

    HOLDER_EDITOR,
    HOLDER_SHOWTRACKS,
    HOLDER_CLEAR(
            arg("context...", false)
    ),

    PERMISSION,
    PARENT,
    META,

    PERMISSION_INFO(
            arg("page", false),
            arg("sort mode", false)
    ),
    PERMISSION_SET(
            arg("node", true),
            arg("value", "true|false", false),
            arg("context...", false)
    ),
    PERMISSION_UNSET(
            arg("node", true),
            arg("context...", false)
    ),
    PERMISSION_SETTEMP(
            arg("node", true),
            arg("value", "true|false", false),
            arg("duration", true),
            arg("temporary modifier", false),
            arg("context...", false)
    ),
    PERMISSION_UNSETTEMP(
            arg("node", true),
            arg("duration", false),
            arg("context...", false)
    ),
    PERMISSION_CHECK(
            arg("node", true),
            arg("context...", false)
    ),
    PERMISSION_CHECK_INHERITS(
            arg("node", true),
            arg("context...", false)
    ),
    PERMISSION_CLEAR(
            arg("context...", false)
    ),

    PARENT_INFO(
            arg("page", false),
            arg("sort mode", false)
    ),
    PARENT_SET(
            arg("group", true),
            arg("context...", false)
    ),
    PARENT_ADD(
            arg("group", true),
            arg("context...", false)
    ),
    PARENT_REMOVE(
            arg("group", true),
            arg("context...", false)
    ),
    PARENT_SET_TRACK(
            arg("track", true),
            arg("group", true),
            arg("context...", false)
    ),
    PARENT_ADD_TEMP(
            arg("group", true),
            arg("duration", true),
            arg("temporary modifier", false),
            arg("context...", false)
    ),
    PARENT_REMOVE_TEMP(
            arg("group", true),
            arg("duration", false),
            arg("context...", false)
    ),
    PARENT_CLEAR(
            arg("context...", false)
    ),
    PARENT_CLEAR_TRACK(
            arg("track", true),
            arg("context...", false)
    ),

    META_INFO,
    META_SET(
            arg("key", true),
            arg("value", true),
            arg("context...", false)
    ),
    META_UNSET(
            arg("key", true),
            arg("context...", false)
    ),
    META_SETTEMP(
            arg("key", true),
            arg("value", true),
            arg("duration", true),
            arg("context...", false)
    ),
    META_UNSETTEMP(
            arg("key", true),
            arg("context...", false)
    ),
    META_ADDPREFIX(
            arg("priority", true),
            arg("prefix", true),
            arg("context...", false)
    ),
    META_ADDSUFFIX(
            arg("priority", true),
            arg("suffix", true),
            arg("context...", false)
    ),
    META_SETPREFIX(
            arg("priority", false),
            arg("prefix", true),
            arg("context...", false)
    ),
    META_SETSUFFIX(
            arg("priority", false),
            arg("suffix", true),
            arg("context...", false)
    ),
    META_REMOVEPREFIX(
            arg("priority", true),
            arg("prefix", false),
            arg("context...", false)
    ),
    META_REMOVESUFFIX(
            arg("priority", true),
            arg("suffix", false),
            arg("context...", false)
    ),
    META_ADDTEMP_PREFIX(
            arg("priority", true),
            arg("prefix", true),
            arg("duration", true),
            arg("context...", false)
    ),
    META_ADDTEMP_SUFFIX(
            arg("priority", true),
            arg("suffix", true),
            arg("duration", true),
            arg("context...", false)
    ),
    META_SETTEMP_PREFIX(
            arg("priority", true),
            arg("prefix", true),
            arg("duration", true),
            arg("context...", false)
    ),
    META_SETTEMP_SUFFIX(
            arg("priority", true),
            arg("suffix", true),
            arg("duration", true),
            arg("context...", false)
    ),
    META_REMOVETEMP_PREFIX(
            arg("priority", true),
            arg("prefix", false),
            arg("context...", false)
    ),
    META_REMOVETEMP_SUFFIX(
            arg("priority", true),
            arg("suffix", false),
            arg("context...", false)
    ),
    META_CLEAR(
            arg("type", false),
            arg("context...", false)
    ),

    TRACK_INFO,
    TRACK_APPEND(
            arg("group", true)
    ),
    TRACK_INSERT(
            arg("group", true),
            arg("position", true)
    ),
    TRACK_REMOVE(
            arg("group", true)
    ),
    TRACK_CLEAR,
    TRACK_RENAME(
            arg("name", true)
    ),
    TRACK_CLONE(
            arg("name", true)
    ),

    LOG_RECENT(
            arg("user", false),
            arg("page", false)
    ),
    LOG_SEARCH(
            arg("query", true),
            arg("page", false)
    ),
    LOG_NOTIFY(
            arg("toggle", "on|off", false)
    ),
    LOG_USER_HISTORY(
            arg("user", true),
            arg("page", false)
    ),
    LOG_GROUP_HISTORY(
            arg("group", true),
            arg("page", false)
    ),
    LOG_TRACK_HISTORY(
            arg("track", true),
            arg("page", false)
    ),

    SPONGE("/%s sponge <collection> <subject>",
            arg("collection", true),
            arg("subject", true)
    ),
    SPONGE_PERMISSION_INFO(
            arg("contexts...", false)
    ),
    SPONGE_PERMISSION_SET(
            arg("node", true),
            arg("tristate", true),
            arg("contexts...", false)
    ),
    SPONGE_PERMISSION_CLEAR(
            arg("contexts...", false)
    ),
    SPONGE_PARENT_INFO(
            arg("contexts...", false)
    ),
    SPONGE_PARENT_ADD(
            arg("collection", true),
            arg("subject", true),
            arg("contexts...", false)
    ),
    SPONGE_PARENT_REMOVE(
            arg("collection", true),
            arg("subject", true),
            arg("contexts...", false)
    ),
    SPONGE_PARENT_CLEAR(
            arg("contexts...", false)
    ),
    SPONGE_OPTION_INFO(
            arg("contexts...", false)
    ),
    SPONGE_OPTION_SET(
            arg("key", true),
            arg("value", true),
            arg("contexts...", false)
    ),
    SPONGE_OPTION_UNSET(
            arg("key", true),
            arg("contexts...", false)
    ),
    SPONGE_OPTION_CLEAR(
            arg("contexts...", false)
    );

    private final String usage;
    private final List<Argument> args;

    CommandSpec(String usage, PartialArgument... args) {
        this.usage = usage;
        this.args = args.length == 0 ? null : Arrays.stream(args)
                .map(builder -> {
                    String key = builder.id.replace(".", "").replace(' ', '-');
                    Component description = Component.translatable("luckperms.usage." + key() + ".argument." + key);
                    return new Argument(builder.name, builder.required, description);
                })
                .collect(ImmutableCollectors.toList());
    }

    CommandSpec(PartialArgument... args) {
        this(null, args);
    }

    public Component description() {
        return Component.translatable("luckperms.usage." + this.key() + ".description");
    }

    public String usage() {
        return this.usage;
    }

    public List<Argument> args() {
        return this.args;
    }

    public String key() {
        return name().toLowerCase().replace('_', '-');
    }

    private static PartialArgument arg(String id, String name, boolean required) {
        return new PartialArgument(id, name, required);
    }

    private static PartialArgument arg(String name, boolean required) {
        return new PartialArgument(name, name, required);
    }

    private static final class PartialArgument {
        private final String id;
        private final String name;
        private final boolean required;

        private PartialArgument(String id, String name, boolean required) {
            this.id = id;
            this.name = name;
            this.required = required;
        }
    }

}
