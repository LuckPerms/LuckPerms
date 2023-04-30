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

package me.lucko.luckperms.nukkit.inject.permissible;

import cn.nukkit.Player;
import cn.nukkit.permission.PermissibleBase;
import cn.nukkit.permission.Permission;
import cn.nukkit.permission.PermissionAttachment;
import cn.nukkit.permission.PermissionAttachmentInfo;
import cn.nukkit.plugin.Plugin;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import me.lucko.luckperms.common.cacheddata.result.TristateResult;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.context.manager.QueryOptionsCache;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;
import me.lucko.luckperms.nukkit.LPNukkitPlugin;
import me.lucko.luckperms.nukkit.calculator.OpProcessor;
import me.lucko.luckperms.nukkit.calculator.PermissionMapProcessor;
import me.lucko.luckperms.nukkit.inject.PermissionDefault;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

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
public class LuckPermsPermissible extends PermissibleBase {

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
    private final QueryOptionsCache<Player> queryOptionsSupplier;

    // the players previous permissible. (the one they had before this one was injected)
    private PermissibleBase oldPermissible = null;

    // if the permissible is currently active.
    private final AtomicBoolean active = new AtomicBoolean(false);

    // the attachments hooked onto the permissible.
    // this collection is only modified by the attachments themselves
    final Set<LuckPermsPermissionAttachment> hookedAttachments = ConcurrentHashMap.newKeySet();

    public LuckPermsPermissible(Player player, User user, LPNukkitPlugin plugin) {
        super(player);
        this.user = Objects.requireNonNull(user, "user");
        this.player = Objects.requireNonNull(player, "player");
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.queryOptionsSupplier = plugin.getContextManager().getCacheFor(player);

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

        QueryOptions queryOptions = this.queryOptionsSupplier.getQueryOptions();
        TristateResult result = this.user.getCachedData().getPermissionData(queryOptions).checkPermission(permission, CheckOrigin.PLATFORM_API_HAS_PERMISSION_SET);
        if (result.result() == Tristate.UNDEFINED) {
            return false;
        }

        // ignore matches made from looking up in the permission map (replicate nukkit behaviour)
        if (result.processorClass() == PermissionMapProcessor.class) {
            return false;
        }

        // ignore the op processor
        return result.processorClass() != OpProcessor.class;
    }

    @Override
    public boolean isPermissionSet(Permission permission) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }

        return isPermissionSet(permission.getName());
    }

    @Override
    public boolean hasPermission(String permission) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }

        QueryOptions queryOptions = this.queryOptionsSupplier.getQueryOptions();
        return this.user.getCachedData().getPermissionData(queryOptions).checkPermission(permission, CheckOrigin.PLATFORM_API_HAS_PERMISSION).result().asBoolean();
    }

    @Override
    public boolean hasPermission(Permission permission) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }

        QueryOptions queryOptions = this.queryOptionsSupplier.getQueryOptions();
        TristateResult result = this.user.getCachedData().getPermissionData(queryOptions).checkPermission(permission.getName(), CheckOrigin.PLATFORM_API_HAS_PERMISSION);

        // override default op handling using the Permission class we have
        if (result.processorClass() == OpProcessor.class && this.plugin.getConfiguration().get(ConfigKeys.APPLY_BUKKIT_DEFAULT_PERMISSIONS)) {
            // 'op == true' is implied by the presence of the OpProcessor class
            PermissionDefault def = PermissionDefault.fromPermission(permission);
            if (def != null) {
                return def.getValue(true);
            }
        }

        return result.result().asBoolean();
    }

    /**
     * Adds attachments to this permissible.
     *
     * @param attachments the attachments to add
     */
    void convertAndAddAttachments(Collection<PermissionAttachment> attachments) {
        for (PermissionAttachment attachment : attachments) {
            new LuckPermsPermissionAttachment(this, attachment).hook();
        }
    }

    @Override
    public void setOp(boolean value) {
        this.player.setOp(value);
    }

    @Override
    public Map<String, PermissionAttachmentInfo> getEffectivePermissions() {
        Map<String, Boolean> permissionMap = this.user.getCachedData().getPermissionData(this.queryOptionsSupplier.getQueryOptions()).getPermissionMap();

        ImmutableMap.Builder<String, PermissionAttachmentInfo> builder = ImmutableMap.builder();
        permissionMap.forEach((key, value) -> builder.put(key, new PermissionAttachmentInfo(this.player, key, null, value)));
        return builder.build();
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin) {
        return addAttachment(plugin, null, null);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, @Nullable String permission) {
        return addAttachment(plugin, permission, null);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, @Nullable String permission, @Nullable Boolean value) {
        Objects.requireNonNull(plugin, "plugin");

        LuckPermsPermissionAttachment attachment = new LuckPermsPermissionAttachment(this, plugin);
        attachment.hook();

        if (permission != null) { // ffs nukkit, why
            boolean val = value == null || value;
            attachment.setPermission(permission, val);
        }

        return attachment;
    }

    @Override
    public void removeAttachment(PermissionAttachment attachment) {
        Objects.requireNonNull(attachment, "attachment");

        LuckPermsPermissionAttachment luckPermsAttachment;

        if (!(attachment instanceof LuckPermsPermissionAttachment)) {
            // try to find a match
            LuckPermsPermissionAttachment match = this.hookedAttachments.stream().filter(at -> at.getSource() == attachment).findFirst().orElse(null);
            if (match != null) {
                luckPermsAttachment = match;
            } else {
                throw new IllegalArgumentException("Given attachment is not a LPPermissionAttachment.");
            }
        } else {
            luckPermsAttachment = (LuckPermsPermissionAttachment) attachment;
        }

        if (luckPermsAttachment.getPermissible() != this) {
            throw new IllegalArgumentException("Attachment does not belong to this permissible.");
        }

        luckPermsAttachment.remove();
    }

    @Override
    public void recalculatePermissions() {
        // this method is called (among other times) when op status is updated.
        // because we encapsulate op status within QueryOptions, we need to invalidate
        // the query options cache when op status changes.
        // (#invalidate is a fast call)
        if (this.queryOptionsSupplier != null) { // this method is called by the super class constructor, before this class has fully initialised
            this.queryOptionsSupplier.invalidate();
        }

        // but we don't need to do anything else in this method, unlike the Nukkit impl.
    }

    @Override
    public void clearPermissions() {
        this.hookedAttachments.forEach(LuckPermsPermissionAttachment::remove);
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
            if (LuckPermsPermissible.this.hookedAttachments.stream().anyMatch(at -> at.getSource() == attachment)) {
                return false;
            }

            new LuckPermsPermissionAttachment(LuckPermsPermissible.this, attachment).hook();
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
        public boolean addAll(@NonNull Collection<? extends PermissionAttachment> c) {
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
            return LuckPermsPermissible.this.hookedAttachments.stream().anyMatch(at -> at.getSource() == attachment);
        }

        @Override
        public Iterator<PermissionAttachment> iterator() {
            return ImmutableList.<PermissionAttachment>copyOf(LuckPermsPermissible.this.hookedAttachments).iterator();
        }

        @Override
        public @NonNull Object[] toArray() {
            return ImmutableList.<PermissionAttachment>copyOf(LuckPermsPermissible.this.hookedAttachments).toArray();
        }

        @Override
        public <T> @NonNull T[] toArray(@NonNull T[] a) {
            return ImmutableList.<PermissionAttachment>copyOf(LuckPermsPermissible.this.hookedAttachments).toArray(a);
        }

        @Override public int size() { throw new UnsupportedOperationException(); }
        @Override public boolean isEmpty() { throw new UnsupportedOperationException(); }
        @Override public boolean containsAll(@NonNull Collection<?> c) { throw new UnsupportedOperationException(); }
        @Override public boolean removeAll(@NonNull Collection<?> c) { throw new UnsupportedOperationException(); }
        @Override public boolean retainAll(@NonNull Collection<?> c) { throw new UnsupportedOperationException(); }
    }
}
