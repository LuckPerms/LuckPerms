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

import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.lucko.luckperms.common.commands.Sender;
import me.lucko.luckperms.common.utils.ImmutableCollectors;

import java.util.Set;

@SuppressWarnings("SpellCheckingInspection")
@Getter
public enum Permission {

    SYNC(set("sync"), Type.NONE),
    INFO(set("info"), Type.NONE),
    DEBUG(set("debug"), Type.NONE),
    VERBOSE(set("verbose"), Type.NONE),
    IMPORT(set("import"), Type.NONE),

    CREATE_GROUP(set("creategroup"), Type.NONE),
    DELETE_GROUP(set("deletegroup"), Type.NONE),
    LIST_GROUPS(set("listgroups"), Type.NONE),

    CREATE_TRACK(set("createtrack"), Type.NONE),
    DELETE_TRACK(set("deletetrack"), Type.NONE),
    LIST_TRACKS(set("listtracks"), Type.NONE),

    USER_INFO(set("info"), Type.USER),
    USER_PERM_INFO(set("permission.info", "listnodes"), Type.USER),
    USER_PERM_SET(set("permission.set", "setpermission"), Type.USER),
    USER_PERM_UNSET(set("permission.unset", "unsetpermission"), Type.USER),
    USER_PERM_SETTEMP(set("permission.settemp", "settemppermission"), Type.USER),
    USER_PERM_UNSETTEMP(set("permission.unsettemp", "unsettemppermission"), Type.USER),
    USER_PERM_CHECK(set("permission.check", "haspermission"), Type.USER),
    USER_PERM_CHECK_INHERITS(set("permission.checkinherits", "inheritspermission"), Type.USER),
    USER_PARENT_INFO(set("parent.info", "listgroups"), Type.USER),
    USER_PARENT_SET(set("parent.set"), Type.USER),
    USER_PARENT_ADD(set("parent.add", "addgroup"), Type.USER),
    USER_PARENT_REMOVE(set("parent.remove", "removegroup"), Type.USER),
    USER_PARENT_ADDTEMP(set("parent.addtemp", "addtempgroup"), Type.USER),
    USER_PARENT_REMOVETEMP(set("parent.removetemp", "removetempgroup"), Type.USER),
    USER_META_INFO(set("meta.info", "chatmeta"), Type.USER),
    USER_META_SET(set("meta.set", "setmeta"), Type.USER),
    USER_META_UNSET(set("meta.unset", "unsetmeta"), Type.USER),
    USER_META_SETTEMP(set("meta.settemp", "settempmeta"), Type.USER),
    USER_META_UNSETTEMP(set("meta.unsettemp", "unsettempmeta"), Type.USER),
    USER_META_ADDPREFIX(set("meta.addprefix", "addprefix"), Type.USER),
    USER_META_ADDSUFFIX(set("meta.addsuffix", "addsuffix"), Type.USER),
    USER_META_REMOVEPREFIX(set("meta.removeprefix", "removeprefix"), Type.USER),
    USER_META_REMOVESUFFIX(set("meta.removesuffix", "removesuffix"), Type.USER),
    USER_META_ADDTEMP_PREFIX(set("meta.addtempprefix", "addtempprefix"), Type.USER),
    USER_META_ADDTEMP_SUFFIX(set("meta.addtempsuffix", "addtempsuffix"), Type.USER),
    USER_META_REMOVETEMP_PREFIX(set("meta.removetempprefix", "removetempprefix"), Type.USER),
    USER_META_REMOVETEMP_SUFFIX(set("meta.removetempsuffix", "removetempsuffix"), Type.USER),
    USER_META_CLEAR(set("meta.clear", "clearmeta"), Type.USER),
    USER_GETUUID(set("getuuid"), Type.USER),
    USER_SETPRIMARYGROUP(set("setprimarygroup"), Type.USER),
    USER_SHOWTRACKS(set("showtracks"), Type.USER),
    USER_PROMOTE(set("promote"), Type.USER),
    USER_DEMOTE(set("demote"), Type.USER),
    USER_BULKCHANGE(set("bulkchange"), Type.USER),
    USER_CLEAR(set("clear"), Type.USER),

    GROUP_INFO(set("info"), Type.GROUP),
    GROUP_PERM_INFO(set("permission.info", "listnodes"), Type.GROUP),
    GROUP_PERM_SET(set("permission.set", "setpermission"), Type.GROUP),
    GROUP_PERM_UNSET(set("permission.unset", "unsetpermission"), Type.GROUP),
    GROUP_PERM_SETTEMP(set("permission.settemp", "settemppermission"), Type.GROUP),
    GROUP_PERM_UNSETTEMP(set("permission.unsettemp", "unsettemppermission"), Type.GROUP),
    GROUP_PERM_CHECK(set("permission.check", "haspermission"), Type.GROUP),
    GROUP_PERM_CHECK_INHERITS(set("permission.checkinherits", "inheritspermission"), Type.GROUP),
    GROUP_PARENT_INFO(set("parent.info", "listparents"), Type.GROUP),
    GROUP_PARENT_SET(set("parent.set"), Type.GROUP),
    GROUP_PARENT_ADD(set("parent.add", "setinherit"), Type.GROUP),
    GROUP_PARENT_REMOVE(set("parent.remove", "unsetinherit"), Type.GROUP),
    GROUP_PARENT_ADDTEMP(set("parent.addtemp", "settempinherit"), Type.GROUP),
    GROUP_PARENT_REMOVETEMP(set("parent.removetemp", "unsettempinherit"), Type.GROUP),
    GROUP_META_INFO(set("meta.info", "chatmeta"), Type.GROUP),
    GROUP_META_SET(set("meta.set", "setmeta"), Type.GROUP),
    GROUP_META_UNSET(set("meta.unset", "unsetmeta"), Type.GROUP),
    GROUP_META_SETTEMP(set("meta.settemp", "settempmeta"), Type.GROUP),
    GROUP_META_UNSETTEMP(set("meta.unsettemp", "unsettempmeta"), Type.GROUP),
    GROUP_META_ADDPREFIX(set("meta.addprefix", "addprefix"), Type.GROUP),
    GROUP_META_ADDSUFFIX(set("meta.addsuffix", "addsuffix"), Type.GROUP),
    GROUP_META_REMOVEPREFIX(set("meta.removeprefix", "removeprefix"), Type.GROUP),
    GROUP_META_REMOVESUFFIX(set("meta.removesuffix", "removesuffix"), Type.GROUP),
    GROUP_META_ADDTEMP_PREFIX(set("meta.addtempprefix", "addtempprefix"), Type.GROUP),
    GROUP_META_ADDTEMP_SUFFIX(set("meta.addtempsuffix", "addtempsuffix"), Type.GROUP),
    GROUP_META_REMOVETEMP_PREFIX(set("meta.removetempprefix", "removetempprefix"), Type.GROUP),
    GROUP_META_REMOVETEMP_SUFFIX(set("meta.removetempsuffix", "removetempsuffix"), Type.GROUP),
    GROUP_META_CLEAR(set("meta.clear", "clearmeta"), Type.GROUP),
    GROUP_SHOWTRACKS(set("showtracks"), Type.GROUP),
    GROUP_BULKCHANGE(set("bulkchange"), Type.GROUP),
    GROUP_CLEAR(set("clear"), Type.GROUP),
    GROUP_RENAME(set("rename"), Type.GROUP),
    GROUP_CLONE(set("clone"), Type.GROUP),

    TRACK_INFO(set("info"), Type.TRACK),
    TRACK_APPEND(set("append"), Type.TRACK),
    TRACK_INSERT(set("insert"), Type.TRACK),
    TRACK_REMOVE(set("remove"), Type.TRACK),
    TRACK_CLEAR(set("clear"), Type.TRACK),
    TRACK_RENAME(set("rename"), Type.TRACK),
    TRACK_CLONE(set("clone"), Type.TRACK),

    LOG_RECENT(set("recent"), Type.LOG),
    LOG_USER_HISTORY(set("userhistory"), Type.LOG),
    LOG_GROUP_HISTORY(set("grouphistory"), Type.LOG),
    LOG_TRACK_HISTORY(set("trackhistory"), Type.LOG),
    LOG_SEARCH(set("search"), Type.LOG),
    LOG_NOTIFY(set("notify"), Type.LOG),
    LOG_EXPORT(set("export"), Type.LOG),

    MIGRATION(set("migration"), Type.NONE);

    private static final String IDENTIFIER = "luckperms.";

    private Set<String> nodes;
    private Type type;

    Permission(Set<String> tags, Type type) {
        this.type = type;

        if (type == Type.NONE) {
            this.nodes = tags.stream().map(t -> IDENTIFIER + t).collect(ImmutableCollectors.toImmutableSet());
        } else {
            this.nodes = tags.stream().map(t -> IDENTIFIER + type.getTag() + "." + t).collect(ImmutableCollectors.toImmutableSet());
        }
    }

    public boolean isAuthorized(Sender sender) {
        return sender.hasPermission(this);
    }

    private static Set<String> set(String... args) {
        return Sets.newHashSet(args);
    }
    
    @Getter
    @AllArgsConstructor
    public enum Type {
        
        NONE(null),
        USER("user"),
        GROUP("group"),
        TRACK("track"),
        LOG("log");
        
        private final String tag;

    }

}
