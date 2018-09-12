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

package me.lucko.luckperms.nukkit.model.permissible;

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.contexts.ContextsSupplier;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.utils.ImmutableCollectors;
import me.lucko.luckperms.common.verbose.CheckOrigin;
import me.lucko.luckperms.nukkit.LPNukkitPlugin;
import me.lucko.luckperms.nukkit.model.PermissionDefault;

import cn.nukkit.Player;
import cn.nukkit.permission.PermissibleBase;
import cn.nukkit.permission.Permission;
import cn.nukkit.permission.PermissionAttachment;
import cn.nukkit.permission.PermissionAttachmentInfo;
import cn.nukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;

/**
 * PermissibleBase for LuckPerms.
 *
 * This class overrides all methods defined in PermissibleBase, and provides custom handling
 * from LuckPerms.
 *
 * This means that all permission checks made for a player are handled directly by the plugin.
 * Method behaviour is retained, but alternate implementation is used.
 *
 * "Hot" method calls, (namely #hasPermission) are significantly faster than the base implementation.
 *
 * This class is **thread safe**. This means that when LuckPerms is installed on the server,
 * is is safe to call Player#hasPermission asynchronously.
 */
public class LPPermissible extends PermissibleBase {

    private static final Field ATTACHMENTS_FIELD;

    static {
        try {
            ATTACHMENTS_FIELD = PermissibleBase.class.getDeclaredField("attachments");
            ATTACHMENTS_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // the LuckPerms user this permissible references.
    private final User user;

    // the player this permissible is injected into.
    private final Player player;

    // the luckperms plugin instance
    private final LPNukkitPlugin plugin;

    // caches context lookups for the player
    private final ContextsSupplier contextsSupplier;

    // the players previous permissible. (the one they had before this one was injected)
    private PermissibleBase oldPermissible = null;

    // if the permissible is currently active.
    private final AtomicBoolean active = new AtomicBoolean(false);

    // the attachments hooked onto the permissible.
    // this collection is only modified by the attachments themselves
    final Set<LPPermissionAttachment> lpAttachments = ConcurrentHashMap.newKeySet();

    public LPPermissible(Player player, User user, LPNukkitPlugin plugin) {
        super(player);
        this.user = Objects.requireNonNull(user, "user");
        this.player = Objects.requireNonNull(player, "player");
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.contextsSupplier = plugin.getContextManager().getCacheFor(player);

        injectFakeAttachmentsList();
    }

    /**
     * Injects a fake 'attachments' set into the superclass, for dumb plugins
     * which for some reason decided to add attachments via reflection.
     *
     * The fake list proxies (some) calls back to the proper methods on this permissible.
     */
    private void injectFakeAttachmentsList() {
        FakeAttachmentSet fakeSet = new FakeAttachmentSet();

        try {
            // the field we need to modify is in the superclass - it has private
            // and final modifiers so we have to use reflection to modify it.
            ATTACHMENTS_FIELD.set(this, fakeSet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isPermissionSet(String permission) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }

        Tristate ts = this.user.getCachedData().getPermissionData(this.contextsSupplier.getContexts()).getPermissionValue(permission, CheckOrigin.PLATFORM_LOOKUP_CHECK);
        return ts != Tristate.UNDEFINED || PermissionDefault.OP.getValue(isOp());
    }

    @Override
    public boolean isPermissionSet(Permission permission) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }

        Tristate ts = this.user.getCachedData().getPermissionData(this.contextsSupplier.getContexts()).getPermissionValue(permission.getName(), CheckOrigin.PLATFORM_LOOKUP_CHECK);
        if (ts != Tristate.UNDEFINED) {
            return true;
        }

        if (!this.plugin.getConfiguration().get(ConfigKeys.APPLY_NUKKIT_DEFAULT_PERMISSIONS)) {
            return PermissionDefault.OP.getValue(isOp());
        } else {
            PermissionDefault def = PermissionDefault.fromPermission(permission);
            return def != null && def.getValue(isOp());
        }
    }

    @Override
    public boolean hasPermission(String permission) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }

        Tristate ts = this.user.getCachedData().getPermissionData(this.contextsSupplier.getContexts()).getPermissionValue(permission, CheckOrigin.PLATFORM_PERMISSION_CHECK);
        return ts != Tristate.UNDEFINED ? ts.asBoolean() : PermissionDefault.OP.getValue(isOp());
    }

    @Override
    public boolean hasPermission(Permission permission) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }

        Tristate ts = this.user.getCachedData().getPermissionData(this.contextsSupplier.getContexts()).getPermissionValue(permission.getName(), CheckOrigin.PLATFORM_PERMISSION_CHECK);
        if (ts != Tristate.UNDEFINED) {
            return ts.asBoolean();
        }

        if (!this.plugin.getConfiguration().get(ConfigKeys.APPLY_NUKKIT_DEFAULT_PERMISSIONS)) {
            return PermissionDefault.OP.getValue(isOp());
        } else {
            PermissionDefault def = PermissionDefault.fromPermission(permission);
            return def != null && def.getValue(isOp());
        }
    }

    /**
     * Adds attachments to this permissible.
     *
     * @param attachments the attachments to add
     */
    void convertAndAddAttachments(Collection<PermissionAttachment> attachments) {
        for (PermissionAttachment attachment : attachments) {
            new LPPermissionAttachment(this, attachment).hook();
        }
    }

    @Override
    public void setOp(boolean value) {
        this.player.setOp(value);
    }

    @Override
    public Map<String, PermissionAttachmentInfo> getEffectivePermissions() {
        return this.user.getCachedData().getPermissionData(this.contextsSupplier.getContexts()).getImmutableBacking().entrySet().stream()
                .collect(ImmutableCollectors.toMap(Map.Entry::getKey, entry -> new PermissionAttachmentInfo(this.player, entry.getKey(), null, entry.getValue())));
    }

    @Override
    public LPPermissionAttachment addAttachment(Plugin plugin) {
        if (plugin == null) {
            throw new NullPointerException("plugin");
        }

        LPPermissionAttachment ret = new LPPermissionAttachment(this, plugin);
        ret.hook();
        return ret;
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String permission) {
        if (plugin == null) {
            throw new NullPointerException("plugin");
        }
        if (permission == null) {
            return addAttachment(plugin);
        }

        PermissionAttachment ret = addAttachment(plugin);
        ret.setPermission(permission, true);
        return ret;
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String permission, Boolean value) {
        if (plugin == null) {
            throw new NullPointerException("plugin");
        }
        if (permission == null) {
            return addAttachment(plugin);
        }
        if (value == null) {
            return addAttachment(plugin, permission);   
        }

        PermissionAttachment ret = addAttachment(plugin);
        ret.setPermission(permission, value);
        return ret;
    }

    @Override
    public void removeAttachment(PermissionAttachment attachment) {
        if (attachment == null) {
            throw new NullPointerException("attachment");
        }

        LPPermissionAttachment a;

        if (!(attachment instanceof LPPermissionAttachment)) {
            // try to find a match
            LPPermissionAttachment match = this.lpAttachments.stream().filter(at -> at.getSource() == attachment).findFirst().orElse(null);
            if (match != null) {
                a = match;
            } else {
                throw new IllegalArgumentException("Given attachment is not a LPPermissionAttachment.");
            }
        } else {
            a = (LPPermissionAttachment) attachment;
        }

        if (a.getPermissible() != this) {
            throw new IllegalArgumentException("Attachment does not belong to this permissible.");
        }

        a.remove();
    }

    @Override
    public void recalculatePermissions() {
        // do nothing
    }

    @Override
    public void clearPermissions() {
        this.lpAttachments.forEach(LPPermissionAttachment::remove);
    }

    public User getUser() {
        return this.user;
    }

    public Player getPlayer() {
        return this.player;
    }

    public LPNukkitPlugin getPlugin() {
        return this.plugin;
    }

    PermissibleBase getOldPermissible() {
        return this.oldPermissible;
    }

    AtomicBoolean getActive() {
        return this.active;
    }

    void setOldPermissible(PermissibleBase oldPermissible) {
        this.oldPermissible = oldPermissible;
    }

    /**
     * A fake set to be injected into the superclass. This implementation simply
     * proxies calls back to this permissible instance.
     *
     * Some (clever/dumb??) plugins attempt to add/remove/query attachments using reflection.
     *
     * An instance of this map is injected into the super instance so these plugins continue
     * to work with LuckPerms.
     */
    private final class FakeAttachmentSet implements Set<PermissionAttachment> {

        @Override
        public boolean add(PermissionAttachment attachment) {
            if (LPPermissible.this.lpAttachments.stream().anyMatch(at -> at.getSource() == attachment)) {
                return false;
            }

            new LPPermissionAttachment(LPPermissible.this, attachment).hook();
            return true;
        }

        @Override
        public boolean remove(Object o) {
            removeAttachment((PermissionAttachment) o);
            return true;
        }

        @Override
        public void clear() {
            clearPermissions();
        }

        @Override
        public boolean addAll(@Nonnull Collection<? extends PermissionAttachment> c) {
            boolean modified = false;
            for (PermissionAttachment e : c) {
                if (add(e)) {
                    modified = true;
                }
            }
            return modified;
        }

        @Override
        public boolean contains(Object o) {
            PermissionAttachment attachment = (PermissionAttachment) o;
            return LPPermissible.this.lpAttachments.stream().anyMatch(at -> at.getSource() == attachment);
        }

        @Override
        public Iterator<PermissionAttachment> iterator() {
            return ImmutableList.<PermissionAttachment>copyOf(LPPermissible.this.lpAttachments).iterator();
        }

        @Nonnull
        @Override
        public Object[] toArray() {
            return ImmutableList.<PermissionAttachment>copyOf(LPPermissible.this.lpAttachments).toArray();
        }

        @Nonnull
        @Override
        public <T> T[] toArray(@Nonnull T[] a) {
            return ImmutableList.<PermissionAttachment>copyOf(LPPermissible.this.lpAttachments).toArray(a);
        }

        @Override public int size() { throw new UnsupportedOperationException(); }
        @Override public boolean isEmpty() { throw new UnsupportedOperationException(); }
        @Override public boolean containsAll(@Nonnull Collection<?> c) { throw new UnsupportedOperationException(); }
        @Override public boolean removeAll(@Nonnull Collection<?> c) { throw new UnsupportedOperationException(); }
        @Override public boolean retainAll(@Nonnull Collection<?> c) { throw new UnsupportedOperationException(); }
    }
}
