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

package me.lucko.luckperms.common.commands;

import lombok.AllArgsConstructor;
import lombok.Getter;

import me.lucko.luckperms.common.commands.sender.Sender;

import static me.lucko.luckperms.common.commands.CommandPermission.Type.GROUP;
import static me.lucko.luckperms.common.commands.CommandPermission.Type.LOG;
import static me.lucko.luckperms.common.commands.CommandPermission.Type.NONE;
import static me.lucko.luckperms.common.commands.CommandPermission.Type.SPONGE;
import static me.lucko.luckperms.common.commands.CommandPermission.Type.TRACK;
import static me.lucko.luckperms.common.commands.CommandPermission.Type.USER;

/**
 * An enumeration of the permissions required to execute built in LuckPerms commands.
 */
public enum CommandPermission {

    SYNC("sync", NONE),
    INFO("info", NONE),
    VERBOSE("verbose", NONE),
    TREE("tree", NONE),
    SEARCH("search", NONE),
    CHECK("check", NONE),
    IMPORT("import", NONE),
    EXPORT("export", NONE),
    RELOAD_CONFIG("reloadconfig", NONE),
    BULK_UPDATE("bulkupdate", NONE),
    APPLY_EDITS("applyedits", NONE),
    MIGRATION("migration", NONE),

    CREATE_GROUP("creategroup", NONE),
    DELETE_GROUP("deletegroup", NONE),
    LIST_GROUPS("listgroups", NONE),

    CREATE_TRACK("createtrack", NONE),
    DELETE_TRACK("deletetrack", NONE),
    LIST_TRACKS("listtracks", NONE),

    USER_INFO("info", USER),
    USER_PERM_INFO("permission.info", USER),
    USER_PERM_SET("permission.set", USER),
    USER_PERM_UNSET("permission.unset", USER),
    USER_PERM_SET_TEMP("permission.settemp", USER),
    USER_PERM_UNSET_TEMP("permission.unsettemp", USER),
    USER_PERM_CHECK("permission.check", USER),
    USER_PERM_CHECK_INHERITS("permission.checkinherits", USER),
    USER_PARENT_INFO("parent.info", USER),
    USER_PARENT_SET("parent.set", USER),
    USER_PARENT_SET_TRACK("parent.settrack", USER),
    USER_PARENT_ADD("parent.add", USER),
    USER_PARENT_REMOVE("parent.remove", USER),
    USER_PARENT_ADD_TEMP("parent.addtemp", USER),
    USER_PARENT_REMOVE_TEMP("parent.removetemp", USER),
    USER_PARENT_CLEAR("parent.clear", USER),
    USER_PARENT_CLEAR_TRACK("parent.cleartrack", USER),
    USER_META_INFO("meta.info", USER),
    USER_META_SET("meta.set", USER),
    USER_META_UNSET("meta.unset", USER),
    USER_META_SET_TEMP("meta.settemp", USER),
    USER_META_UNSET_TEMP("meta.unsettemp", USER),
    USER_META_ADD_PREFIX("meta.addprefix", USER),
    USER_META_ADD_SUFFIX("meta.addsuffix", USER),
    USER_META_REMOVE_PREFIX("meta.removeprefix", USER),
    USER_META_REMOVE_SUFFIX("meta.removesuffix", USER),
    USER_META_ADD_TEMP_PREFIX("meta.addtempprefix", USER),
    USER_META_ADD_TEMP_SUFFIX("meta.addtempsuffix", USER),
    USER_META_REMOVE_TEMP_PREFIX("meta.removetempprefix", USER),
    USER_META_REMOVE_TEMP_SUFFIX("meta.removetempsuffix", USER),
    USER_META_CLEAR("meta.clear", USER),
    USER_EDITOR("editor", USER),
    USER_SWITCHPRIMARYGROUP("switchprimarygroup", USER),
    USER_SHOW_TRACKS("showtracks", USER),
    USER_PROMOTE("promote", USER),
    USER_DEMOTE("demote", USER),
    USER_CLEAR("clear", USER),
    USER_CLONE("clone", USER),

    GROUP_INFO("info", GROUP),
    GROUP_PERM_INFO("permission.info", GROUP),
    GROUP_PERM_SET("permission.set", GROUP),
    GROUP_PERM_UNSET("permission.unset", GROUP),
    GROUP_PERM_SET_TEMP("permission.settemp", GROUP),
    GROUP_PERM_UNSET_TEMP("permission.unsettemp", GROUP),
    GROUP_PERM_CHECK("permission.check", GROUP),
    GROUP_PERM_CHECK_INHERITS("permission.checkinherits", GROUP),
    GROUP_PARENT_INFO("parent.info", GROUP),
    GROUP_PARENT_SET("parent.set", GROUP),
    GROUP_PARENT_SET_TRACK("parent.settrack", GROUP),
    GROUP_PARENT_ADD("parent.add", GROUP),
    GROUP_PARENT_REMOVE("parent.remove", GROUP),
    GROUP_PARENT_ADD_TEMP("parent.addtemp", GROUP),
    GROUP_PARENT_REMOVE_TEMP("parent.removetemp", GROUP),
    GROUP_PARENT_CLEAR("parent.clear", GROUP),
    GROUP_PARENT_CLEAR_TRACK("parent.cleartrack", GROUP),
    GROUP_META_INFO("meta.info", GROUP),
    GROUP_META_SET("meta.set", GROUP),
    GROUP_META_UNSET("meta.unset", GROUP),
    GROUP_META_SET_TEMP("meta.settemp", GROUP),
    GROUP_META_UNSET_TEMP("meta.unsettemp", GROUP),
    GROUP_META_ADD_PREFIX("meta.addprefix", GROUP),
    GROUP_META_ADD_SUFFIX("meta.addsuffix", GROUP),
    GROUP_META_REMOVE_PREFIX("meta.removeprefix", GROUP),
    GROUP_META_REMOVE_SUFFIX("meta.removesuffix", GROUP),
    GROUP_META_ADD_TEMP_PREFIX("meta.addtempprefix", GROUP),
    GROUP_META_ADD_TEMP_SUFFIX("meta.addtempsuffix", GROUP),
    GROUP_META_REMOVE_TEMP_PREFIX("meta.removetempprefix", GROUP),
    GROUP_META_REMOVE_TEMP_SUFFIX("meta.removetempsuffix", GROUP),
    GROUP_META_CLEAR("meta.clear", GROUP),
    GROUP_EDITOR("editor", GROUP),
    GROUP_LIST_MEMBERS("listmembers", GROUP),
    GROUP_SHOW_TRACKS("showtracks", GROUP),
    GROUP_SET_WEIGHT("setweight", GROUP),
    GROUP_SET_DISPLAY_NAME("setdisplayname", GROUP),
    GROUP_CLEAR("clear", GROUP),
    GROUP_RENAME("rename", GROUP),
    GROUP_CLONE("clone", GROUP),

    TRACK_INFO("info", TRACK),
    TRACK_APPEND("append", TRACK),
    TRACK_INSERT("insert", TRACK),
    TRACK_REMOVE("remove", TRACK),
    TRACK_CLEAR("clear", TRACK),
    TRACK_RENAME("rename", TRACK),
    TRACK_CLONE("clone", TRACK),

    LOG_RECENT("recent", LOG),
    LOG_USER_HISTORY("userhistory", LOG),
    LOG_GROUP_HISTORY("grouphistory", LOG),
    LOG_TRACK_HISTORY("trackhistory", LOG),
    LOG_SEARCH("search", LOG),
    LOG_NOTIFY("notify", LOG),

    SPONGE_PERMISSION_INFO("permission.info", SPONGE),
    SPONGE_PERMISSION_SET("permission.set", SPONGE),
    SPONGE_PERMISSION_CLEAR("permission.clear", SPONGE),
    SPONGE_PARENT_INFO("parent.info", SPONGE),
    SPONGE_PARENT_ADD("parent.add", SPONGE),
    SPONGE_PARENT_REMOVE("parent.remove", SPONGE),
    SPONGE_PARENT_CLEAR("parent.clear", SPONGE),
    SPONGE_OPTION_INFO("option.info", SPONGE),
    SPONGE_OPTION_SET("option.set", SPONGE),
    SPONGE_OPTION_UNSET("option.unset", SPONGE),
    SPONGE_OPTION_CLEAR("option.clear", SPONGE);

    public static final String ROOT = "luckperms.";

    private final String node;

    @Getter
    private final Type type;

    CommandPermission(String node, Type type) {
        this.type = type;

        if (type == NONE) {
            this.node = ROOT + node;
        } else {
            this.node = ROOT + type.getTag() + "." + node;
        }
    }

    public String getPermission() {
        return node;
    }

    public boolean isAuthorized(Sender sender) {
        return sender.hasPermission(this);
    }

    @Getter
    @AllArgsConstructor
    public enum Type {

        NONE(null),
        USER("user"),
        GROUP("group"),
        TRACK("track"),
        LOG("log"),
        SPONGE("sponge");

        private final String tag;

    }

}
