package me.lucko.luckperms.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.lucko.luckperms.commands.Sender;

@SuppressWarnings("SpellCheckingInspection")
@AllArgsConstructor
public enum Permission {

    SYNC("sync", null),
    INFO("info", null),
    DEBUG("debug", null),

    CREATE_GROUP("creategroup", null),
    DELETE_GROUP("deletegroup", null),
    LIST_GROUPS("listgroups", null),

    CREATE_TRACK("createtrack", null),
    DELETE_TRACK("deletetrack", null),
    LIST_TRACKS("listtracks", null),

    USER_INFO("info", PermissionGroup.USER),
    USER_GETUUID("getuuid", PermissionGroup.USER),
    USER_LISTNODES("listnodes", PermissionGroup.USER),
    USER_HASPERMISSION("haspermission", PermissionGroup.USER),
    USER_INHERITSPERMISSION("inheritspermission", PermissionGroup.USER),
    USER_SETPERMISSION("setpermission", PermissionGroup.USER),
    USER_UNSETPERMISSION("unsetpermission", PermissionGroup.USER),
    USER_ADDGROUP("addgroup", PermissionGroup.USER),
    USER_REMOVEGROUP("removegroup", PermissionGroup.USER),
    USER_SETPRIMARYGROUP("setprimarygroup", PermissionGroup.USER),
    USER_SHOWTRACKS("showtracks", PermissionGroup.USER),
    USER_PROMOTE("promote", PermissionGroup.USER),
    USER_DEMOTE("demote", PermissionGroup.USER),
    USER_SHOWPOS("showpos", PermissionGroup.USER),
    USER_CLEAR("clear", PermissionGroup.USER),

    GROUP_INFO("info", PermissionGroup.GROUP),
    GROUP_LISTNODES("listnodes", PermissionGroup.GROUP),
    GROUP_HASPERMISSION("haspermission", PermissionGroup.GROUP),
    GROUP_INHERITSPERMISSION("inheritspermission", PermissionGroup.GROUP),
    GROUP_SETPERMISSION("setpermission", PermissionGroup.GROUP),
    GROUP_UNSETPERMISSION("unsetpermission", PermissionGroup.GROUP),
    GROUP_SETINHERIT("setinherit", PermissionGroup.GROUP),
    GROUP_UNSETINHERIT("unsetinherit", PermissionGroup.GROUP),
    GROUP_SHOWTRACKS("showtracks", PermissionGroup.GROUP),
    GROUP_CLEAR("clear", PermissionGroup.GROUP),

    TRACK_INFO("info", PermissionGroup.TRACK),
    TRACK_APPEND("append", PermissionGroup.TRACK),
    TRACK_INSERT("insert", PermissionGroup.TRACK),
    TRACK_REMOVE("remove", PermissionGroup.TRACK),
    TRACK_CLEAR("clear", PermissionGroup.TRACK);

    private String node;
    private PermissionGroup group;

    public boolean isAuthorized(Sender sender) {
        if (sender.hasPermission("luckperms.*")) {
            return true;
        }

        if (group != null) {
            return group.isAuthorized(sender) || sender.hasPermission("luckperms." + group.getNode() + "." + node);
        }

        return sender.hasPermission("luckperms." + node);
    }

    @Getter
    @AllArgsConstructor
    private enum PermissionGroup {
        USER("user"),
        GROUP("group"),
        TRACK("track");

        private String node;

        private boolean isAuthorized(Sender sender) {
            return sender.hasPermission("luckperms." + node + ".*");
        }
    }

}
