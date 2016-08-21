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
import me.lucko.luckperms.commands.Sender;

@SuppressWarnings("SpellCheckingInspection")
@AllArgsConstructor
public enum Permission {

    SYNC("sync", null),
    INFO("info", null),
    DEBUG("debug", null),
    IMPORT("import", null),

    CREATE_GROUP("creategroup", null),
    DELETE_GROUP("deletegroup", null),
    LIST_GROUPS("listgroups", null),

    CREATE_TRACK("createtrack", null),
    DELETE_TRACK("deletetrack", null),
    LIST_TRACKS("listtracks", null),

    USER_INFO("info", "user"),
    USER_GETUUID("getuuid", "user"),
    USER_LISTNODES("listnodes", "user"),
    USER_HASPERMISSION("haspermission", "user"),
    USER_INHERITSPERMISSION("inheritspermission", "user"),
    USER_SETPERMISSION("setpermission", "user"),
    USER_UNSETPERMISSION("unsetpermission", "user"),
    USER_ADDGROUP("addgroup", "user"),
    USER_REMOVEGROUP("removegroup", "user"),
    USER_SET_TEMP_PERMISSION("settemppermission", "user"),
    USER_UNSET_TEMP_PERMISSION("unsettemppermission", "user"),
    USER_ADDTEMPGROUP("addtempgroup", "user"),
    USER_REMOVETEMPGROUP("removetempgroup", "user"),
    USER_SETPRIMARYGROUP("setprimarygroup", "user"),
    USER_SHOWTRACKS("showtracks", "user"),
    USER_PROMOTE("promote", "user"),
    USER_DEMOTE("demote", "user"),
    USER_SHOWPOS("showpos", "user"),
    USER_CLEAR("clear", "user"),

    GROUP_INFO("info", "group"),
    GROUP_LISTNODES("listnodes", "group"),
    GROUP_HASPERMISSION("haspermission", "group"),
    GROUP_INHERITSPERMISSION("inheritspermission", "group"),
    GROUP_SETPERMISSION("setpermission", "group"),
    GROUP_UNSETPERMISSION("unsetpermission", "group"),
    GROUP_SETINHERIT("setinherit", "group"),
    GROUP_UNSETINHERIT("unsetinherit", "group"),
    GROUP_SET_TEMP_PERMISSION("settemppermission", "group"),
    GROUP_UNSET_TEMP_PERMISSION("unsettemppermission", "group"),
    GROUP_SET_TEMP_INHERIT("settempinherit", "group"),
    GROUP_UNSET_TEMP_INHERIT("unsettempinherit", "group"),
    GROUP_SHOWTRACKS("showtracks", "group"),
    GROUP_CLEAR("clear", "group"),

    TRACK_INFO("info", "track"),
    TRACK_APPEND("append", "track"),
    TRACK_INSERT("insert", "track"),
    TRACK_REMOVE("remove", "track"),
    TRACK_CLEAR("clear", "track"),

    LOG_RECENT("recent", "log"),
    LOG_USER_HISTORY("userhistory", "log"),
    LOG_GROUP_HISTORY("grouphistory", "log"),
    LOG_TRACK_HISTORY("trackhistory", "log"),
    LOG_SEARCH("search", "log"),
    LOG_NOTIFY("notify", "log"),
    LOG_EXPORT("export", "log"),

    MIGRATION("migration", null);

    private String node;
    private String group;

    public String getNode() {
        if (group != null) {
            return "luckperms." + group + "." + node;
        }

        return "luckperms." + node;
    }

    public boolean isAuthorized(Sender sender) {
        return sender.hasPermission(this);
    }

}
