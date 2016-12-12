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

package me.lucko.luckperms.common.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.utils.ImmutableCollectors;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("SpellCheckingInspection")
@Getter
public enum Permission {

    SYNC(l("sync"), Type.NONE),
    INFO(l("info"), Type.NONE),
    VERBOSE(l("verbose"), Type.NONE),
    IMPORT(l("import"), Type.NONE),

    CREATE_GROUP(l("creategroup"), Type.NONE),
    DELETE_GROUP(l("deletegroup"), Type.NONE),
    LIST_GROUPS(l("listgroups"), Type.NONE),

    CREATE_TRACK(l("createtrack"), Type.NONE),
    DELETE_TRACK(l("deletetrack"), Type.NONE),
    LIST_TRACKS(l("listtracks"), Type.NONE),

    USER_INFO(l("info"), Type.USER),
    USER_PERM_INFO(l("permission.info", "listnodes"), Type.USER),
    USER_PERM_SET(l("permission.set", "setpermission"), Type.USER),
    USER_PERM_UNSET(l("permission.unset", "unsetpermission"), Type.USER),
    USER_PERM_SETTEMP(l("permission.settemp", "settemppermission"), Type.USER),
    USER_PERM_UNSETTEMP(l("permission.unsettemp", "unsettemppermission"), Type.USER),
    USER_PERM_CHECK(l("permission.check", "haspermission"), Type.USER),
    USER_PERM_CHECK_INHERITS(l("permission.checkinherits", "inheritspermission"), Type.USER),
    USER_PARENT_INFO(l("parent.info", "listgroups"), Type.USER),
    USER_PARENT_SET(l("parent.set"), Type.USER),
    USER_PARENT_ADD(l("parent.add", "addgroup"), Type.USER),
    USER_PARENT_REMOVE(l("parent.remove", "removegroup"), Type.USER),
    USER_PARENT_ADDTEMP(l("parent.addtemp", "addtempgroup"), Type.USER),
    USER_PARENT_REMOVETEMP(l("parent.removetemp", "removetempgroup"), Type.USER),
    USER_PARENT_CLEAR(l("parent.clear"), Type.USER),
    USER_META_INFO(l("meta.info", "chatmeta"), Type.USER),
    USER_META_SET(l("meta.set", "setmeta"), Type.USER),
    USER_META_UNSET(l("meta.unset", "unsetmeta"), Type.USER),
    USER_META_SETTEMP(l("meta.settemp", "settempmeta"), Type.USER),
    USER_META_UNSETTEMP(l("meta.unsettemp", "unsettempmeta"), Type.USER),
    USER_META_ADDPREFIX(l("meta.addprefix", "addprefix"), Type.USER),
    USER_META_ADDSUFFIX(l("meta.addsuffix", "addsuffix"), Type.USER),
    USER_META_REMOVEPREFIX(l("meta.removeprefix", "removeprefix"), Type.USER),
    USER_META_REMOVESUFFIX(l("meta.removesuffix", "removesuffix"), Type.USER),
    USER_META_ADDTEMP_PREFIX(l("meta.addtempprefix", "addtempprefix"), Type.USER),
    USER_META_ADDTEMP_SUFFIX(l("meta.addtempsuffix", "addtempsuffix"), Type.USER),
    USER_META_REMOVETEMP_PREFIX(l("meta.removetempprefix", "removetempprefix"), Type.USER),
    USER_META_REMOVETEMP_SUFFIX(l("meta.removetempsuffix", "removetempsuffix"), Type.USER),
    USER_META_CLEAR(l("meta.clear", "clearmeta"), Type.USER),
    USER_GETUUID(l("getuuid"), Type.USER),
    USER_SWITCHPRIMARYGROUP(l("switchprimarygroup", "setprimarygroup"), Type.USER),
    USER_SHOWTRACKS(l("showtracks"), Type.USER),
    USER_PROMOTE(l("promote"), Type.USER),
    USER_DEMOTE(l("demote"), Type.USER),
    USER_BULKCHANGE(l("bulkchange"), Type.USER),
    USER_CLEAR(l("clear"), Type.USER),

    GROUP_INFO(l("info"), Type.GROUP),
    GROUP_PERM_INFO(l("permission.info", "listnodes"), Type.GROUP),
    GROUP_PERM_SET(l("permission.set", "setpermission"), Type.GROUP),
    GROUP_PERM_UNSET(l("permission.unset", "unsetpermission"), Type.GROUP),
    GROUP_PERM_SETTEMP(l("permission.settemp", "settemppermission"), Type.GROUP),
    GROUP_PERM_UNSETTEMP(l("permission.unsettemp", "unsettemppermission"), Type.GROUP),
    GROUP_PERM_CHECK(l("permission.check", "haspermission"), Type.GROUP),
    GROUP_PERM_CHECK_INHERITS(l("permission.checkinherits", "inheritspermission"), Type.GROUP),
    GROUP_PARENT_INFO(l("parent.info", "listparents"), Type.GROUP),
    GROUP_PARENT_SET(l("parent.set"), Type.GROUP),
    GROUP_PARENT_ADD(l("parent.add", "setinherit"), Type.GROUP),
    GROUP_PARENT_REMOVE(l("parent.remove", "unsetinherit"), Type.GROUP),
    GROUP_PARENT_ADDTEMP(l("parent.addtemp", "settempinherit"), Type.GROUP),
    GROUP_PARENT_REMOVETEMP(l("parent.removetemp", "unsettempinherit"), Type.GROUP),
    GROUP_PARENT_CLEAR(l("parent.clear"), Type.GROUP),
    GROUP_META_INFO(l("meta.info", "chatmeta"), Type.GROUP),
    GROUP_META_SET(l("meta.set", "setmeta"), Type.GROUP),
    GROUP_META_UNSET(l("meta.unset", "unsetmeta"), Type.GROUP),
    GROUP_META_SETTEMP(l("meta.settemp", "settempmeta"), Type.GROUP),
    GROUP_META_UNSETTEMP(l("meta.unsettemp", "unsettempmeta"), Type.GROUP),
    GROUP_META_ADDPREFIX(l("meta.addprefix", "addprefix"), Type.GROUP),
    GROUP_META_ADDSUFFIX(l("meta.addsuffix", "addsuffix"), Type.GROUP),
    GROUP_META_REMOVEPREFIX(l("meta.removeprefix", "removeprefix"), Type.GROUP),
    GROUP_META_REMOVESUFFIX(l("meta.removesuffix", "removesuffix"), Type.GROUP),
    GROUP_META_ADDTEMP_PREFIX(l("meta.addtempprefix", "addtempprefix"), Type.GROUP),
    GROUP_META_ADDTEMP_SUFFIX(l("meta.addtempsuffix", "addtempsuffix"), Type.GROUP),
    GROUP_META_REMOVETEMP_PREFIX(l("meta.removetempprefix", "removetempprefix"), Type.GROUP),
    GROUP_META_REMOVETEMP_SUFFIX(l("meta.removetempsuffix", "removetempsuffix"), Type.GROUP),
    GROUP_META_CLEAR(l("meta.clear", "clearmeta"), Type.GROUP),
    GROUP_SHOWTRACKS(l("showtracks"), Type.GROUP),
    GROUP_BULKCHANGE(l("bulkchange"), Type.GROUP),
    GROUP_CLEAR(l("clear"), Type.GROUP),
    GROUP_RENAME(l("rename"), Type.GROUP),
    GROUP_CLONE(l("clone"), Type.GROUP),

    TRACK_INFO(l("info"), Type.TRACK),
    TRACK_APPEND(l("append"), Type.TRACK),
    TRACK_INSERT(l("insert"), Type.TRACK),
    TRACK_REMOVE(l("remove"), Type.TRACK),
    TRACK_CLEAR(l("clear"), Type.TRACK),
    TRACK_RENAME(l("rename"), Type.TRACK),
    TRACK_CLONE(l("clone"), Type.TRACK),

    LOG_RECENT(l("recent"), Type.LOG),
    LOG_USER_HISTORY(l("userhistory"), Type.LOG),
    LOG_GROUP_HISTORY(l("grouphistory"), Type.LOG),
    LOG_TRACK_HISTORY(l("trackhistory"), Type.LOG),
    LOG_SEARCH(l("search"), Type.LOG),
    LOG_NOTIFY(l("notify"), Type.LOG),
    LOG_EXPORT(l("export"), Type.LOG),

    SPONGE_PERMISSION_INFO(l("permission.info"), Type.SPONGE),
    SPONGE_PERMISSION_SET(l("permission.set"), Type.SPONGE),
    SPONGE_PERMISSION_CLEAR(l("permission.clear"), Type.SPONGE),
    SPONGE_PARENT_INFO(l("parent.info"), Type.SPONGE),
    SPONGE_PARENT_ADD(l("parent.add"), Type.SPONGE),
    SPONGE_PARENT_REMOVE(l("parent.remove"), Type.SPONGE),
    SPONGE_PARENT_CLEAR(l("parent.clear"), Type.SPONGE),
    SPONGE_OPTION_INFO(l("option.info"), Type.SPONGE),
    SPONGE_OPTION_SET(l("option.set"), Type.SPONGE),
    SPONGE_OPTION_UNSET(l("option.unset"), Type.SPONGE),
    SPONGE_OPTION_CLEAR(l("option.clear"), Type.SPONGE),

    MIGRATION(l("migration"), Type.NONE);

    private static final String IDENTIFIER = "luckperms.";

    private static List<String> l(String... args) {
        return Arrays.asList(args);
    }

    private List<String> nodes;
    private Type type;

    Permission(List<String> tags, Type type) {
        this.type = type;

        if (type == Type.NONE) {
            this.nodes = tags.stream().map(t -> IDENTIFIER + t).collect(ImmutableCollectors.toImmutableList());
        } else {
            this.nodes = tags.stream().map(t -> IDENTIFIER + type.getTag() + "." + t).collect(ImmutableCollectors.toImmutableList());
        }
    }

    public String getExample() {
        return nodes.get(0);
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
