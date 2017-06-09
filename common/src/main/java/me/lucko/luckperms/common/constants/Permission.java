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

    SYNC(list("sync"), Type.NONE),
    INFO(list("info"), Type.NONE),
    VERBOSE(list("verbose"), Type.NONE),
    TREE(list("tree"), Type.NONE),
    SEARCH(list("search"), Type.NONE),
    CHECK(list("check"), Type.NONE),
    IMPORT(list("import"), Type.NONE),
    EXPORT(list("export"), Type.NONE),
    RELOAD_CONFIG(list("reloadconfig"), Type.NONE),
    BULK_UPDATE(list("bulkupdate"), Type.NONE),
    APPLY_EDITS(list("applyedits"), Type.NONE),
    MIGRATION(list("migration"), Type.NONE),

    CREATE_GROUP(list("creategroup"), Type.NONE),
    DELETE_GROUP(list("deletegroup"), Type.NONE),
    LIST_GROUPS(list("listgroups"), Type.NONE),

    CREATE_TRACK(list("createtrack"), Type.NONE),
    DELETE_TRACK(list("deletetrack"), Type.NONE),
    LIST_TRACKS(list("listtracks"), Type.NONE),

    USER_INFO(list("info"), Type.USER),
    USER_PERM_INFO(list("permission.info", "listnodes"), Type.USER),
    USER_PERM_SET(list("permission.set", "setpermission"), Type.USER),
    USER_PERM_UNSET(list("permission.unset", "unsetpermission"), Type.USER),
    USER_PERM_SETTEMP(list("permission.settemp", "settemppermission"), Type.USER),
    USER_PERM_UNSETTEMP(list("permission.unsettemp", "unsettemppermission"), Type.USER),
    USER_PERM_CHECK(list("permission.check", "haspermission"), Type.USER),
    USER_PERM_CHECK_INHERITS(list("permission.checkinherits", "inheritspermission"), Type.USER),
    USER_PARENT_INFO(list("parent.info", "listgroups"), Type.USER),
    USER_PARENT_SET(list("parent.set"), Type.USER),
    USER_PARENT_SET_TRACK(list("parent.settrack"), Type.USER),
    USER_PARENT_ADD(list("parent.add", "addgroup"), Type.USER),
    USER_PARENT_REMOVE(list("parent.remove", "removegroup"), Type.USER),
    USER_PARENT_ADDTEMP(list("parent.addtemp", "addtempgroup"), Type.USER),
    USER_PARENT_REMOVETEMP(list("parent.removetemp", "removetempgroup"), Type.USER),
    USER_PARENT_CLEAR(list("parent.clear"), Type.USER),
    USER_PARENT_CLEAR_TRACK(list("parent.cleartrack"), Type.USER),
    USER_META_INFO(list("meta.info", "chatmeta"), Type.USER),
    USER_META_SET(list("meta.set", "setmeta"), Type.USER),
    USER_META_UNSET(list("meta.unset", "unsetmeta"), Type.USER),
    USER_META_SETTEMP(list("meta.settemp", "settempmeta"), Type.USER),
    USER_META_UNSETTEMP(list("meta.unsettemp", "unsettempmeta"), Type.USER),
    USER_META_ADDPREFIX(list("meta.addprefix", "addprefix"), Type.USER),
    USER_META_ADDSUFFIX(list("meta.addsuffix", "addsuffix"), Type.USER),
    USER_META_REMOVEPREFIX(list("meta.removeprefix", "removeprefix"), Type.USER),
    USER_META_REMOVESUFFIX(list("meta.removesuffix", "removesuffix"), Type.USER),
    USER_META_ADDTEMP_PREFIX(list("meta.addtempprefix", "addtempprefix"), Type.USER),
    USER_META_ADDTEMP_SUFFIX(list("meta.addtempsuffix", "addtempsuffix"), Type.USER),
    USER_META_REMOVETEMP_PREFIX(list("meta.removetempprefix", "removetempprefix"), Type.USER),
    USER_META_REMOVETEMP_SUFFIX(list("meta.removetempsuffix", "removetempsuffix"), Type.USER),
    USER_META_CLEAR(list("meta.clear", "clearmeta"), Type.USER),
    USER_EDITOR(list("editor"), Type.USER),
    USER_SWITCHPRIMARYGROUP(list("switchprimarygroup", "setprimarygroup"), Type.USER),
    USER_SHOWTRACKS(list("showtracks"), Type.USER),
    USER_PROMOTE(list("promote"), Type.USER),
    USER_DEMOTE(list("demote"), Type.USER),
    USER_CLEAR(list("clear"), Type.USER),

    GROUP_INFO(list("info"), Type.GROUP),
    GROUP_PERM_INFO(list("permission.info", "listnodes"), Type.GROUP),
    GROUP_PERM_SET(list("permission.set", "setpermission"), Type.GROUP),
    GROUP_PERM_UNSET(list("permission.unset", "unsetpermission"), Type.GROUP),
    GROUP_PERM_SETTEMP(list("permission.settemp", "settemppermission"), Type.GROUP),
    GROUP_PERM_UNSETTEMP(list("permission.unsettemp", "unsettemppermission"), Type.GROUP),
    GROUP_PERM_CHECK(list("permission.check", "haspermission"), Type.GROUP),
    GROUP_PERM_CHECK_INHERITS(list("permission.checkinherits", "inheritspermission"), Type.GROUP),
    GROUP_PARENT_INFO(list("parent.info", "listparents"), Type.GROUP),
    GROUP_PARENT_SET(list("parent.set"), Type.GROUP),
    GROUP_PARENT_SET_TRACK(list("parent.settrack"), Type.GROUP),
    GROUP_PARENT_ADD(list("parent.add", "setinherit"), Type.GROUP),
    GROUP_PARENT_REMOVE(list("parent.remove", "unsetinherit"), Type.GROUP),
    GROUP_PARENT_ADDTEMP(list("parent.addtemp", "settempinherit"), Type.GROUP),
    GROUP_PARENT_REMOVETEMP(list("parent.removetemp", "unsettempinherit"), Type.GROUP),
    GROUP_PARENT_CLEAR(list("parent.clear"), Type.GROUP),
    GROUP_PARENT_CLEAR_TRACK(list("parent.cleartrack"), Type.GROUP),
    GROUP_META_INFO(list("meta.info", "chatmeta"), Type.GROUP),
    GROUP_META_SET(list("meta.set", "setmeta"), Type.GROUP),
    GROUP_META_UNSET(list("meta.unset", "unsetmeta"), Type.GROUP),
    GROUP_META_SETTEMP(list("meta.settemp", "settempmeta"), Type.GROUP),
    GROUP_META_UNSETTEMP(list("meta.unsettemp", "unsettempmeta"), Type.GROUP),
    GROUP_META_ADDPREFIX(list("meta.addprefix", "addprefix"), Type.GROUP),
    GROUP_META_ADDSUFFIX(list("meta.addsuffix", "addsuffix"), Type.GROUP),
    GROUP_META_REMOVEPREFIX(list("meta.removeprefix", "removeprefix"), Type.GROUP),
    GROUP_META_REMOVESUFFIX(list("meta.removesuffix", "removesuffix"), Type.GROUP),
    GROUP_META_ADDTEMP_PREFIX(list("meta.addtempprefix", "addtempprefix"), Type.GROUP),
    GROUP_META_ADDTEMP_SUFFIX(list("meta.addtempsuffix", "addtempsuffix"), Type.GROUP),
    GROUP_META_REMOVETEMP_PREFIX(list("meta.removetempprefix", "removetempprefix"), Type.GROUP),
    GROUP_META_REMOVETEMP_SUFFIX(list("meta.removetempsuffix", "removetempsuffix"), Type.GROUP),
    GROUP_META_CLEAR(list("meta.clear", "clearmeta"), Type.GROUP),
    GROUP_EDITOR(list("editor"), Type.GROUP),
    GROUP_LISTMEMBERS(list("listmembers"), Type.GROUP),
    GROUP_SHOWTRACKS(list("showtracks"), Type.GROUP),
    GROUP_SETWEIGHT(list("setweight"), Type.GROUP),
    GROUP_CLEAR(list("clear"), Type.GROUP),
    GROUP_RENAME(list("rename"), Type.GROUP),
    GROUP_CLONE(list("clone"), Type.GROUP),

    TRACK_INFO(list("info"), Type.TRACK),
    TRACK_APPEND(list("append"), Type.TRACK),
    TRACK_INSERT(list("insert"), Type.TRACK),
    TRACK_REMOVE(list("remove"), Type.TRACK),
    TRACK_CLEAR(list("clear"), Type.TRACK),
    TRACK_RENAME(list("rename"), Type.TRACK),
    TRACK_CLONE(list("clone"), Type.TRACK),

    LOG_RECENT(list("recent"), Type.LOG),
    LOG_USER_HISTORY(list("userhistory"), Type.LOG),
    LOG_GROUP_HISTORY(list("grouphistory"), Type.LOG),
    LOG_TRACK_HISTORY(list("trackhistory"), Type.LOG),
    LOG_SEARCH(list("search"), Type.LOG),
    LOG_NOTIFY(list("notify"), Type.LOG),

    SPONGE_PERMISSION_INFO(list("permission.info"), Type.SPONGE),
    SPONGE_PERMISSION_SET(list("permission.set"), Type.SPONGE),
    SPONGE_PERMISSION_CLEAR(list("permission.clear"), Type.SPONGE),
    SPONGE_PARENT_INFO(list("parent.info"), Type.SPONGE),
    SPONGE_PARENT_ADD(list("parent.add"), Type.SPONGE),
    SPONGE_PARENT_REMOVE(list("parent.remove"), Type.SPONGE),
    SPONGE_PARENT_CLEAR(list("parent.clear"), Type.SPONGE),
    SPONGE_OPTION_INFO(list("option.info"), Type.SPONGE),
    SPONGE_OPTION_SET(list("option.set"), Type.SPONGE),
    SPONGE_OPTION_UNSET(list("option.unset"), Type.SPONGE),
    SPONGE_OPTION_CLEAR(list("option.clear"), Type.SPONGE);

    private static final String IDENTIFIER = "luckperms.";

    private static List<String> list(String... args) {
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
