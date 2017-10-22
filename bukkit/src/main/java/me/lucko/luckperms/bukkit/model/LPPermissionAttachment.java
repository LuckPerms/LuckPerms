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

package me.lucko.luckperms.bukkit.model;

import lombok.Getter;
import lombok.Setter;

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.node.ImmutableTransientNode;
import me.lucko.luckperms.common.node.NodeFactory;

import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionRemovedExecutor;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * PermissionAttachment for LuckPerms.
 *
 * Applies all permissions directly to the backing user instance via transient nodes.
 */
public class LPPermissionAttachment extends PermissionAttachment {

    /**
     * The parent LPPermissible
     */
    @Getter
    private final LPPermissible permissible;

    /**
     * The plugin which "owns" this attachment, may be null
     */
    private final Plugin owner;

    /**
     * The permissions being applied by this attachment
     */
    private final Map<String, Boolean> perms = Collections.synchronizedMap(new HashMap<>());

    /**
     * If the attachment has been applied to the user
     */
    private boolean hooked = false;

    /**
     * Callback to run when the attachment is removed
     */
    @Getter
    @Setter
    private PermissionRemovedExecutor removalCallback = null;

    public LPPermissionAttachment(LPPermissible permissible, Plugin owner) {
        super(DummyPlugin.INSTANCE, null);
        this.permissible = permissible;
        this.owner = owner;
    }

    public LPPermissionAttachment(LPPermissible permissible, PermissionAttachment bukkit) {
        super(DummyPlugin.INSTANCE, null);
        this.permissible = permissible;
        this.owner = null;

        // copy
        perms.putAll(bukkit.getPermissions());
    }

    public void hook() {
        hooked = true;
        permissible.attachments.add(this);
        for (Map.Entry<String, Boolean> entry : perms.entrySet()) {
            setPermissionInternal(entry.getKey(), entry.getValue());
        }
    }

    private void setPermissionInternal(String name, boolean value) {
        if (!permissible.getPlugin().getConfiguration().get(ConfigKeys.APPLY_BUKKIT_ATTACHMENT_PERMISSIONS)) {
            return;
        }

        ImmutableTransientNode node = ImmutableTransientNode.of(NodeFactory.make(name, value), this);
        if (permissible.getUser().setTransientPermission(node).asBoolean()) {
            permissible.getUser().getRefreshBuffer().request();
        }
    }

    private void unsetPermissionInternal(String name) {
        if (!permissible.getPlugin().getConfiguration().get(ConfigKeys.APPLY_BUKKIT_ATTACHMENT_PERMISSIONS)) {
            return;
        }

        if (permissible.getUser().removeIfTransient(n -> n instanceof ImmutableTransientNode && ((ImmutableTransientNode) n).getOwner() == this && n.getPermission().equals(name))) {
            permissible.getUser().getRefreshBuffer().request();
        }
    }

    @Override
    public boolean remove() {
        if (!hooked) {
            return false;
        }

        if (permissible.getUser().removeIfTransient(n -> n instanceof ImmutableTransientNode && ((ImmutableTransientNode) n).getOwner() == this)) {
            permissible.getUser().getRefreshBuffer().request();
        }

        if (removalCallback != null) {
            removalCallback.attachmentRemoved(this);
        }

        hooked = false;
        permissible.attachments.remove(this);
        return true;
    }

    @Override
    public void setPermission(String name, boolean value) {
        Boolean previous = perms.put(name, value);
        if (previous != null && previous == value) {
            return;
        }

        if (!hooked) {
            return;
        }

        if (previous != null) {
            unsetPermissionInternal(name);
        }

        setPermissionInternal(name, value);
    }

    @Override
    public void unsetPermission(String name) {
        Boolean previous = perms.remove(name);
        if (previous == null) {
            return;
        }

        if (!hooked) {
            return;
        }

        unsetPermissionInternal(name);
    }

    @Override
    public Map<String, Boolean> getPermissions() {
        return perms;
    }

    @Override
    public Plugin getPlugin() {
        return owner != null ? owner : permissible.getPlugin();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
