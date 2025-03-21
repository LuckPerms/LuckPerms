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

import me.lucko.luckperms.common.sender.Sender;

/**
 * An enumeration of the permissions required to execute built in LuckPerms commands.
 */
public enum CommandPermission {

    SYNC("sync", Type.NONE, true),
    INFO("info", Type.NONE, true),
    EDITOR("editor", Type.NONE, true),
    VERBOSE("verbose", Type.NONE, true),
    VERBOSE_COMMAND_OTHERS("verbose.command.others", Type.NONE, false),
    TREE("tree", Type.NONE, true),
    SEARCH("search", Type.NONE, true),
    IMPORT("import", Type.NONE, false),
    EXPORT("export", Type.NONE, true),
    RELOAD_CONFIG("reloadconfig", Type.NONE, true),
    BULK_UPDATE("bulkupdate", Type.NONE, false),
    APPLY_EDITS("applyedits", Type.NONE, false),
    TRUST_EDITOR("trusteditor", Type.NONE, false),
    TRANSLATIONS("translations", Type.NONE, true),

    CREATE_GROUP("creategroup", Type.NONE, false),
    DELETE_GROUP("deletegroup", Type.NONE, false),
    LIST_GROUPS("listgroups", Type.NONE, true),

    CREATE_TRACK("createtrack", Type.NONE, false),
    DELETE_TRACK("deletetrack", Type.NONE, false),
    LIST_TRACKS("listtracks", Type.NONE, true),

    USER_INFO("info", Type.USER, true),
    USER_PERM_INFO("permission.info", Type.USER, true),
    USER_PERM_SET("permission.set", Type.USER, false),
    USER_PERM_UNSET("permission.unset", Type.USER, false),
    USER_PERM_SET_TEMP("permission.settemp", Type.USER, false),
    USER_PERM_UNSET_TEMP("permission.unsettemp", Type.USER, false),
    USER_PERM_CHECK("permission.check", Type.USER, true),
    USER_PERM_CLEAR("permission.clear", Type.USER, false),
    USER_PARENT_INFO("parent.info", Type.USER, true),
    USER_PARENT_SET("parent.set", Type.USER, false),
    USER_PARENT_SET_TRACK("parent.settrack", Type.USER, false),
    USER_PARENT_ADD("parent.add", Type.USER, false),
    USER_PARENT_REMOVE("parent.remove", Type.USER, false),
    USER_PARENT_ADD_TEMP("parent.addtemp", Type.USER, false),
    USER_PARENT_REMOVE_TEMP("parent.removetemp", Type.USER, false),
    USER_PARENT_CLEAR("parent.clear", Type.USER, false),
    USER_PARENT_CLEAR_TRACK("parent.cleartrack", Type.USER, false),
    USER_PARENT_SWITCHPRIMARYGROUP("parent.switchprimarygroup", Type.USER, false),
    USER_META_INFO("meta.info", Type.USER, true),
    USER_META_SET("meta.set", Type.USER, false),
    USER_META_UNSET("meta.unset", Type.USER, false),
    USER_META_SET_TEMP("meta.settemp", Type.USER, false),
    USER_META_UNSET_TEMP("meta.unsettemp", Type.USER, false),
    USER_META_ADD_PREFIX("meta.addprefix", Type.USER, false),
    USER_META_ADD_SUFFIX("meta.addsuffix", Type.USER, false),
    USER_META_SET_PREFIX("meta.setprefix", Type.USER, false),
    USER_META_SET_SUFFIX("meta.setsuffix", Type.USER, false),
    USER_META_REMOVE_PREFIX("meta.removeprefix", Type.USER, false),
    USER_META_REMOVE_SUFFIX("meta.removesuffix", Type.USER, false),
    USER_META_ADD_TEMP_PREFIX("meta.addtempprefix", Type.USER, false),
    USER_META_ADD_TEMP_SUFFIX("meta.addtempsuffix", Type.USER, false),
    USER_META_SET_TEMP_PREFIX("meta.settempprefix", Type.USER, false),
    USER_META_SET_TEMP_SUFFIX("meta.settempsuffix", Type.USER, false),
    USER_META_REMOVE_TEMP_PREFIX("meta.removetempprefix", Type.USER, false),
    USER_META_REMOVE_TEMP_SUFFIX("meta.removetempsuffix", Type.USER, false),
    USER_META_CLEAR("meta.clear", Type.USER, false),
    USER_EDITOR("editor", Type.USER, true),
    USER_SHOW_TRACKS("showtracks", Type.USER, true),
    USER_PROMOTE("promote", Type.USER, false),
    USER_DEMOTE("demote", Type.USER, false),
    USER_CLEAR("clear", Type.USER, false),
    USER_CLONE("clone", Type.USER, false),

    GROUP_INFO("info", Type.GROUP, true),
    GROUP_PERM_INFO("permission.info", Type.GROUP, true),
    GROUP_PERM_SET("permission.set", Type.GROUP, false),
    GROUP_PERM_UNSET("permission.unset", Type.GROUP, false),
    GROUP_PERM_SET_TEMP("permission.settemp", Type.GROUP, false),
    GROUP_PERM_UNSET_TEMP("permission.unsettemp", Type.GROUP, false),
    GROUP_PERM_CHECK("permission.check", Type.GROUP, true),
    GROUP_PERM_CLEAR("permission.clear", Type.GROUP, false),
    GROUP_PARENT_INFO("parent.info", Type.GROUP, true),
    GROUP_PARENT_SET("parent.set", Type.GROUP, false),
    GROUP_PARENT_SET_TRACK("parent.settrack", Type.GROUP, false),
    GROUP_PARENT_ADD("parent.add", Type.GROUP, false),
    GROUP_PARENT_REMOVE("parent.remove", Type.GROUP, false),
    GROUP_PARENT_ADD_TEMP("parent.addtemp", Type.GROUP, false),
    GROUP_PARENT_REMOVE_TEMP("parent.removetemp", Type.GROUP, false),
    GROUP_PARENT_CLEAR("parent.clear", Type.GROUP, false),
    GROUP_PARENT_CLEAR_TRACK("parent.cleartrack", Type.GROUP, false),
    GROUP_META_INFO("meta.info", Type.GROUP, true),
    GROUP_META_SET("meta.set", Type.GROUP, false),
    GROUP_META_UNSET("meta.unset", Type.GROUP, false),
    GROUP_META_SET_TEMP("meta.settemp", Type.GROUP, false),
    GROUP_META_UNSET_TEMP("meta.unsettemp", Type.GROUP, false),
    GROUP_META_ADD_PREFIX("meta.addprefix", Type.GROUP, false),
    GROUP_META_ADD_SUFFIX("meta.addsuffix", Type.GROUP, false),
    GROUP_META_SET_PREFIX("meta.setprefix", Type.GROUP, false),
    GROUP_META_SET_SUFFIX("meta.setsuffix", Type.GROUP, false),
    GROUP_META_REMOVE_PREFIX("meta.removeprefix", Type.GROUP, false),
    GROUP_META_REMOVE_SUFFIX("meta.removesuffix", Type.GROUP, false),
    GROUP_META_ADD_TEMP_PREFIX("meta.addtempprefix", Type.GROUP, false),
    GROUP_META_ADD_TEMP_SUFFIX("meta.addtempsuffix", Type.GROUP, false),
    GROUP_META_SET_TEMP_PREFIX("meta.settempprefix", Type.GROUP, false),
    GROUP_META_SET_TEMP_SUFFIX("meta.settempsuffix", Type.GROUP, false),
    GROUP_META_REMOVE_TEMP_PREFIX("meta.removetempprefix", Type.GROUP, false),
    GROUP_META_REMOVE_TEMP_SUFFIX("meta.removetempsuffix", Type.GROUP, false),
    GROUP_META_CLEAR("meta.clear", Type.GROUP, false),
    GROUP_EDITOR("editor", Type.GROUP, true),
    GROUP_LIST_MEMBERS("listmembers", Type.GROUP, true),
    GROUP_SHOW_TRACKS("showtracks", Type.GROUP, true),
    GROUP_SET_WEIGHT("setweight", Type.GROUP, false),
    GROUP_SET_DISPLAY_NAME("setdisplayname", Type.GROUP, false),
    GROUP_CLEAR("clear", Type.GROUP, false),
    GROUP_RENAME("rename", Type.GROUP, false),
    GROUP_CLONE("clone", Type.GROUP, false),

    TRACK_INFO("info", Type.TRACK, true),
    TRACK_EDITOR("editor", Type.TRACK, true),
    TRACK_APPEND("append", Type.TRACK, false),
    TRACK_INSERT("insert", Type.TRACK, false),
    TRACK_REMOVE("remove", Type.TRACK, false),
    TRACK_CLEAR("clear", Type.TRACK, false),
    TRACK_RENAME("rename", Type.TRACK, false),
    TRACK_CLONE("clone", Type.TRACK, false),

    LOG_RECENT("recent", Type.LOG, true),
    LOG_USER_HISTORY("userhistory", Type.LOG, true),
    LOG_GROUP_HISTORY("grouphistory", Type.LOG, true),
    LOG_TRACK_HISTORY("trackhistory", Type.LOG, true),
    LOG_SEARCH("search", Type.LOG, true),
    LOG_NOTIFY("notify", Type.LOG, true),

    SPONGE_PERMISSION_INFO("permission.info", Type.SPONGE, true),
    SPONGE_PERMISSION_SET("permission.set", Type.SPONGE, false),
    SPONGE_PERMISSION_CLEAR("permission.clear", Type.SPONGE, false),
    SPONGE_PARENT_INFO("parent.info", Type.SPONGE, true),
    SPONGE_PARENT_ADD("parent.add", Type.SPONGE, false),
    SPONGE_PARENT_REMOVE("parent.remove", Type.SPONGE, false),
    SPONGE_PARENT_CLEAR("parent.clear", Type.SPONGE, false),
    SPONGE_OPTION_INFO("option.info", Type.SPONGE, true),
    SPONGE_OPTION_SET("option.set", Type.SPONGE, false),
    SPONGE_OPTION_UNSET("option.unset", Type.SPONGE, false),
    SPONGE_OPTION_CLEAR("option.clear", Type.SPONGE, false);

    public static final String ROOT = "luckperms.";

    private final String node;
    private final String permission;

    private final Type type;
    private final boolean readOnly;

    CommandPermission(String node, Type type, boolean readOnly) {
        this.type = type;
        this.readOnly = readOnly;

        if (type == Type.NONE) {
            this.node = node;
        } else {
            this.node = type.getTag() + "." + node;
        }

        this.permission = ROOT + this.node;
    }

    public String getNode() {
        return this.node;
    }

    public String getPermission() {
        return this.permission;
    }

    public boolean isAuthorized(Sender sender) {
        return sender.hasPermission(this);
    }

    public Type getType() {
        return this.type;
    }

    public boolean isReadOnly() {
        return this.readOnly;
    }

    public enum Type {

        NONE(null),
        USER("user"),
        GROUP("group"),
        TRACK("track"),
        LOG("log"),
        SPONGE("sponge");

        private final String tag;

        Type(String tag) {
            this.tag = tag;
        }

        public String getTag() {
            return this.tag;
        }
    }

}
