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

package me.lucko.luckperms.sponge.managers;

import lombok.NonNull;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.UncheckedExecutionException;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.core.UserIdentifier;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.managers.UserManager;
import me.lucko.luckperms.common.managers.impl.GenericUserManager;
import me.lucko.luckperms.common.utils.ImmutableCollectors;
import me.lucko.luckperms.sponge.LPSpongePlugin;
import me.lucko.luckperms.sponge.model.SpongeUser;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.service.base.LPSubject;
import me.lucko.luckperms.sponge.service.base.LPSubjectCollection;
import me.lucko.luckperms.sponge.service.references.SubjectReference;
import me.lucko.luckperms.sponge.timings.LPTiming;

import org.spongepowered.api.service.permission.PermissionService;

import co.aikar.timings.Timing;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SpongeUserManager implements UserManager, LPSubjectCollection {
    private final LPSpongePlugin plugin;
    
    private final LoadingCache<UserIdentifier, SpongeUser> objects = CacheBuilder.newBuilder()
            .build(new CacheLoader<UserIdentifier, SpongeUser>() {
                @Override
                public SpongeUser load(UserIdentifier i) {
                    return apply(i);
                }

                @Override
                public ListenableFuture<SpongeUser> reload(UserIdentifier i, SpongeUser t) {
                    return Futures.immediateFuture(t); // Never needs to be refreshed.
                }
            });

    private final LoadingCache<UUID, LPSubject> subjectLoadingCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build(new CacheLoader<UUID, LPSubject>() {
                @Override
                public LPSubject load(UUID u) throws Exception {
                    if (isLoaded(UserIdentifier.of(u, null))) {

                        SpongeUser user = get(u);
                        if (user.getUserData() == null) {
                            user.setupData(false);
                        }

                        return get(u).getSpongeData();
                    }

                    // Request load
                    plugin.getStorage().loadUser(u, "null").join();

                    SpongeUser user = get(u);
                    if (user == null) {
                        plugin.getLog().severe("Error whilst loading user '" + u + "'.");
                        throw new RuntimeException();
                    }

                    user.setupData(false);

                    if (user.getUserData() == null) {
                        plugin.getLog().warn("User data not present for requested user id: " + u);
                    }

                    return user.getSpongeData();
                }
            });

    public SpongeUserManager(LPSpongePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public SpongeUser apply(UserIdentifier id) {
        return id.getUsername() == null ?
                new SpongeUser(id.getUuid(), plugin) :
                new SpongeUser(id.getUuid(), id.getUsername(), plugin);
    }

    public void performCleanup() {
        Set<UserIdentifier> set = new HashSet<>();
        for (Map.Entry<UserIdentifier, SpongeUser> user : objects.asMap().entrySet()) {
            if (user.getValue().getSpongeData().shouldCleanup()) {
                user.getValue().unregisterData();
                set.add(user.getKey());
            }
        }

        objects.invalidateAll(set);
    }

    /* ------------------------------------------
     * Manager methods
     * ------------------------------------------ */

    @Override
    public Map<UserIdentifier, SpongeUser> getAll() {
        return ImmutableMap.copyOf(objects.asMap());
    }

    @Override
    public SpongeUser getOrMake(UserIdentifier id) {
        return objects.getUnchecked(id);
    }

    @Override
    public SpongeUser getIfLoaded(UserIdentifier id) {
        return objects.getIfPresent(id);
    }

    @Override
    public boolean isLoaded(UserIdentifier id) {
        return objects.asMap().containsKey(id);
    }

    @Override
    public void unload(UserIdentifier id) {
        if (id != null) {
            objects.invalidate(id);
        }
    }

    @Override
    public void unload(User t) {
        if (t != null) {
            unload(t.getId());
        }
    }

    @Override
    public void unloadAll() {
        objects.invalidateAll();
    }

    /* ------------------------------------------
     * UserManager methods
     * ------------------------------------------ */

    @Override
    public SpongeUser getByUsername(String name) {
        for (SpongeUser user : getAll().values()) {
            if (user.getName().equalsIgnoreCase(name)) {
                return user;
            }
        }
        return null;
    }

    @Override
    public SpongeUser get(UUID uuid) {
        return getIfLoaded(UserIdentifier.of(uuid, null));
    }

    @Override
    public boolean giveDefaultIfNeeded(User user, boolean save) {
        return GenericUserManager.giveDefaultIfNeeded(user, save, plugin);
    }

    @Override
    public void cleanup(User user) {
        // Do nothing - this instance uses other means in order to cleanup
    }

    @Override
    public void updateAllUsers() {
        plugin.doSync(() -> {
            Set<UUID> players = plugin.getOnlinePlayers();
            plugin.doAsync(() -> {
                for (UUID uuid : players) {
                    UUID internal = plugin.getUuidCache().getUUID(uuid);
                    plugin.getStorage().loadUser(internal, "null").join();
                }
            });
        });
    }

    /* ------------------------------------------
     * SubjectCollection methods
     * ------------------------------------------ */

    @Override
    public String getIdentifier() {
        return PermissionService.SUBJECTS_USER;
    }

    @Override
    public LuckPermsService getService() {
        return plugin.getService();
    }

    @Override
    public LPSubject get(@NonNull String id) {
        // Special Sponge method. This call will actually load the user from the datastore if not already present.

        try (Timing ignored = plugin.getTimings().time(LPTiming.USER_COLLECTION_GET)) {
            UUID uuid = Util.parseUuid(id);
            if (uuid == null) {
                plugin.getLog().warn("Couldn't get user subject for id: " + id + " (not a uuid)");
                return plugin.getService().getFallbackUserSubjects().get(id); // fallback to the transient collection
            }

            UUID u = plugin.getUuidCache().getUUID(uuid);
            try {
                return subjectLoadingCache.get(u);
            } catch (UncheckedExecutionException | ExecutionException e) {
                e.printStackTrace();
                plugin.getLog().warn("Couldn't get user subject for id: " + id);
                return plugin.getService().getFallbackUserSubjects().get(id); // fallback to the transient collection
            }
        }
    }

    @Override
    public boolean hasRegistered(@NonNull String id) {
        UUID uuid = Util.parseUuid(id);
        if (uuid == null) {
            return false;
        }

        UUID internal = plugin.getUuidCache().getUUID(uuid);
        return isLoaded(UserIdentifier.of(internal, null));
    }

    @Override
    public Collection<LPSubject> getSubjects() {
        return objects.asMap().values().stream().map(SpongeUser::getSpongeData).collect(ImmutableCollectors.toImmutableList());
    }

    @Override
    public Map<LPSubject, Boolean> getWithPermission(@NonNull ContextSet contexts, @NonNull String node) {
        return objects.asMap().values().stream()
                .map(SpongeUser::getSpongeData)
                .filter(sub -> sub.getPermissionValue(contexts, node) != Tristate.UNDEFINED)
                .collect(ImmutableCollectors.toImmutableMap(sub -> sub, sub -> sub.getPermissionValue(contexts, node).asBoolean()));
    }

    @Override
    public SubjectReference getDefaultSubject() {
        return SubjectReference.of("defaults", getIdentifier());
    }

    @Override
    public boolean getTransientHasPriority() {
        return true;
    }
}
