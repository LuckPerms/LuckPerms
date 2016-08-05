package me.lucko.luckperms.api.implementation.internal;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import me.lucko.luckperms.api.PermissionHolder;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.utils.DateUtil;
import me.lucko.luckperms.utils.Patterns;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Provides a link between {@link PermissionHolder} and {@link me.lucko.luckperms.utils.PermissionHolder}
 */
@SuppressWarnings("unused")
@AllArgsConstructor(access = AccessLevel.PACKAGE)
class PermissionHolderLink implements PermissionHolder {

    @NonNull
    private final me.lucko.luckperms.utils.PermissionHolder master;

    static String checkServer(String s) {
        if (Patterns.NON_ALPHA_NUMERIC.matcher(s).find()) {
            throw new IllegalArgumentException("Invalid server entry '" + s + "'. Server names can only contain alphanumeric characters.");
        }
        return s;
    }

    private static String checkNode(String s) {
        if (s.contains("/") || s.contains("$")) {
            throw new IllegalArgumentException("Invalid node entry '" + s + "'. Nodes cannot contain '/' or '$' characters.");
        }
        return s;
    }

    static long checkTime(long l) {
        if (DateUtil.shouldExpire(l)) {
            throw new IllegalArgumentException("Unix time '" + l + "' is invalid, as it has already passed.");
        }
        return l;
    }

    @Override
    public String getObjectName() {
        return master.getObjectName();
    }

    @Override
    public Map<String, Boolean> getNodes() {
        return Collections.unmodifiableMap(master.getNodes());
    }

    @Override
    public boolean hasPermission(@NonNull String node, @NonNull boolean b) {
        return master.hasPermission(node, b);
    }

    @Override
    public boolean hasPermission(@NonNull String node, @NonNull boolean b, @NonNull String server) {
        return master.hasPermission(node, b, checkServer(server));
    }

    @Override
    public boolean hasPermission(@NonNull String node, @NonNull boolean b, @NonNull String server, @NonNull String world) {
        return master.hasPermission(node, b, checkServer(server), world);
    }

    @Override
    public boolean hasPermission(@NonNull String node, @NonNull boolean b, @NonNull boolean temporary) {
        return master.hasPermission(node, b, temporary);
    }

    @Override
    public boolean hasPermission(@NonNull String node, @NonNull boolean b, @NonNull String server, @NonNull boolean temporary) {
        return master.hasPermission(node, b, checkServer(server), temporary);
    }

    @Override
    public boolean hasPermission(@NonNull String node, @NonNull boolean b, @NonNull String server, @NonNull String world, @NonNull boolean temporary) {
        return master.hasPermission(node, b, checkServer(server), world, temporary);
    }

    @Override
    public boolean inheritsPermission(@NonNull String node, @NonNull boolean b) {
        return master.inheritsPermission(node, b);
    }

    @Override
    public boolean inheritsPermission(@NonNull String node, @NonNull boolean b, @NonNull String server) {
        return master.inheritsPermission(node, b, checkServer(server));
    }

    @Override
    public boolean inheritsPermission(@NonNull String node, @NonNull boolean b, @NonNull String server, @NonNull String world) {
        return master.inheritsPermission(node, b, checkServer(server), world);
    }

    @Override
    public boolean inheritsPermission(@NonNull String node, @NonNull boolean b, @NonNull boolean temporary) {
        return master.inheritsPermission(node, b, temporary);
    }

    @Override
    public boolean inheritsPermission(@NonNull String node, @NonNull boolean b, @NonNull String server, @NonNull boolean temporary) {
        return master.inheritsPermission(node, b, checkServer(server), temporary);
    }

    @Override
    public boolean inheritsPermission(@NonNull String node, @NonNull boolean b, @NonNull String server, @NonNull String world, @NonNull boolean temporary) {
        return master.inheritsPermission(node, b, checkServer(server), world, temporary);
    }

    @Override
    public void setPermission(@NonNull String node, @NonNull boolean value) throws ObjectAlreadyHasException {
        master.setPermission(checkNode(node), value);
    }

    @Override
    public void setPermission(@NonNull String node, @NonNull boolean value, @NonNull String server) throws ObjectAlreadyHasException {
        master.setPermission(checkNode(node), value, checkServer(server));
    }

    @Override
    public void setPermission(@NonNull String node, @NonNull boolean value, @NonNull String server, @NonNull String world) throws ObjectAlreadyHasException {
        master.setPermission(checkNode(node), value, checkServer(server), world);
    }

    @Override
    public void setPermission(@NonNull String node, @NonNull boolean value, @NonNull long expireAt) throws ObjectAlreadyHasException {
        master.setPermission(checkNode(node), value, checkTime(expireAt));
    }

    @Override
    public void setPermission(@NonNull String node, @NonNull boolean value, @NonNull String server, @NonNull long expireAt) throws ObjectAlreadyHasException {
        master.setPermission(checkNode(node), value, checkServer(server), checkTime(expireAt));
    }

    @Override
    public void setPermission(@NonNull String node, @NonNull boolean value, @NonNull String server, @NonNull String world, @NonNull long expireAt) throws ObjectAlreadyHasException {
        master.setPermission(checkNode(node), value, checkServer(server), world, checkTime(expireAt));
    }

    @Override
    public void unsetPermission(@NonNull String node, @NonNull boolean temporary) throws ObjectLacksException {
        master.unsetPermission(checkNode(node), temporary);
    }

    @Override
    public void unsetPermission(@NonNull String node) throws ObjectLacksException {
        master.unsetPermission(checkNode(node));
    }

    @Override
    public void unsetPermission(@NonNull String node, @NonNull String server) throws ObjectLacksException {
        master.unsetPermission(checkNode(node), checkServer(server));
    }

    @Override
    public void unsetPermission(@NonNull String node, @NonNull String server, @NonNull String world) throws ObjectLacksException {
        master.unsetPermission(checkNode(node), checkServer(server), world);
    }

    @Override
    public void unsetPermission(@NonNull String node, @NonNull String server, @NonNull boolean temporary) throws ObjectLacksException {
        master.unsetPermission(checkNode(node), checkServer(server), temporary);
    }

    @Override
    public void unsetPermission(@NonNull String node, @NonNull String server, @NonNull String world, @NonNull boolean temporary) throws ObjectLacksException {
        master.unsetPermission(checkNode(node), checkServer(server), world, temporary);
    }

    @Override
    public Map<String, Boolean> getLocalPermissions(String server, String world, List<String> excludedGroups) {
        return master.getLocalPermissions(server, world, excludedGroups);
    }

    @Override
    public Map<String, Boolean> getLocalPermissions(String server, List<String> excludedGroups) {
        return master.getLocalPermissions(server, excludedGroups);
    }

    @Override
    public Map<Map.Entry<String, Boolean>, Long> getTemporaryNodes() {
        return master.getTemporaryNodes();
    }

    @Override
    public Map<String, Boolean> getPermanentNodes() {
        return master.getPermanentNodes();
    }

    @Override
    public void auditTemporaryPermissions() {
        master.auditTemporaryPermissions();
    }

}
