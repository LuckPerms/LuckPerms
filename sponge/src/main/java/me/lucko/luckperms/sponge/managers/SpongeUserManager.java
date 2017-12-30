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

package me.lucko.luckperms.sponge.managers;

import lombok.Getter;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import me.lucko.luckperms.api.HeldPermission;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.managers.GenericUserManager;
import me.lucko.luckperms.common.managers.UserManager;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.references.UserIdentifier;
import me.lucko.luckperms.common.utils.ImmutableCollectors;
import me.lucko.luckperms.sponge.LPSpongePlugin;
import me.lucko.luckperms.sponge.model.SpongeUser;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.service.ProxyFactory;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectCollection;
import me.lucko.luckperms.sponge.service.model.SubjectReference;
import me.lucko.luckperms.sponge.service.model.SubjectReferenceFactory;

import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.SubjectCollection;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class SpongeUserManager implements UserManager, LPSubjectCollection {

    @Getter
    private final LPSpongePlugin plugin;

    private SubjectCollection spongeProxy = null;

    private final LoadingCache<UserIdentifier, SpongeUser> objects = Caffeine.newBuilder()
            .build(this::apply);

    private final LoadingCache<UUID, LPSubject> subjectLoadingCache = Caffeine.<UUID, LPSubject>newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build(u -> {
                // check if the user instance is already loaded.
                SpongeUser user = getIfLoaded(u);
                if (user != null) {
                    // they're already loaded, but the data might not actually be there yet
                    // if stuff is being loaded, then the user's i/o lock will be locked by the storage impl
                    user.getIoLock().lock();
                    user.getIoLock().unlock();

                    // ok, data is here, let's do the pre-calculation stuff.
                    user.preCalculateData();
                    return user.sponge();
                }

                // Request load
                getPlugin().getStorage().loadUser(u, null).join();
                user = getIfLoaded(u);
                if (user == null) {
                    getPlugin().getLog().severe("Error whilst loading user '" + u + "'.");
                    throw new RuntimeException();
                }

                user.preCalculateData();
                return user.sponge();
            });

    public SpongeUserManager(LPSpongePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public SpongeUser apply(UserIdentifier id) {
        return !id.getUsername().isPresent() ?
                new SpongeUser(id.getUuid(), plugin) :
                new SpongeUser(id.getUuid(), id.getUsername().get(), plugin);
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
        SpongeUser ret = objects.get(id);
        if (id.getUsername().isPresent()) {
            ret.setName(id.getUsername().get(), false);
        }
        return ret;
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
            Optional<String> n = user.getName();
            if (n.isPresent() && n.get().equalsIgnoreCase(name)) {
                return user;
            }
        }
        return null;
    }

    @Override
    public SpongeUser getIfLoaded(UUID uuid) {
        return getIfLoaded(UserIdentifier.of(uuid, null));
    }

    @Override
    public boolean giveDefaultIfNeeded(User user, boolean save) {
        return GenericUserManager.giveDefaultIfNeeded(user, save, plugin);
    }

    @Override
    public boolean cleanup(User user) {
        // Do nothing - this instance uses other means in order to cleanup
        return false;
    }

    @Override
    public void scheduleUnload(UUID uuid) {
        // Do nothing - this instance uses other means in order to cleanup
    }

    @Override
    public CompletableFuture<Void> updateAllUsers() {
        return CompletableFuture.runAsync(
                () -> plugin.getOnlinePlayers()
                        .map(u -> plugin.getUuidCache().getUUID(u))
                        .forEach(u -> plugin.getStorage().loadUser(u, null).join()),
                plugin.getScheduler().async()
        );
    }

    /* ------------------------------------------
     * SubjectCollection methods
     * ------------------------------------------ */

    @Override
    public synchronized SubjectCollection sponge() {
        if (spongeProxy == null) {
            Preconditions.checkNotNull(plugin.getService(), "service");
            spongeProxy = ProxyFactory.toSponge(this);
        }
        return spongeProxy;
    }

    @Override
    public LuckPermsService getService() {
        return plugin.getService();
    }

    @Override
    public String getIdentifier() {
        return PermissionService.SUBJECTS_USER;
    }

    @Override
    public Predicate<String> getIdentifierValidityPredicate() {
        return s -> {
            try {
                //noinspection ResultOfMethodCallIgnored
                UUID.fromString(s);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        };
    }

    @Override
    public CompletableFuture<LPSubject> loadSubject(String identifier) {
        UUID uuid;
        try {
            uuid = UUID.fromString(identifier);
        } catch (IllegalArgumentException e) {
            CompletableFuture<LPSubject> fut = new CompletableFuture<>();
            fut.completeExceptionally(e);
            return fut;
        }

        LPSubject present = subjectLoadingCache.getIfPresent(uuid);
        if (present != null) {
            return CompletableFuture.completedFuture(present);
        }

        return CompletableFuture.supplyAsync(() -> subjectLoadingCache.get(uuid), plugin.getScheduler().async());
    }

    @Override
    public Optional<LPSubject> getSubject(String identifier) {
        UUID uuid = UUID.fromString(identifier);
        return Optional.ofNullable(getIfLoaded(uuid)).map(SpongeUser::sponge);
    }

    @Override
    public CompletableFuture<Boolean> hasRegistered(String identifier) {
        UUID uuid = null;
        IllegalArgumentException ex = null;
        try {
            uuid = UUID.fromString(identifier);
        } catch (IllegalArgumentException e) {
            ex = e;
        }

        if (uuid != null && isLoaded(UserIdentifier.of(uuid, null))) {
            return CompletableFuture.completedFuture(true);
        }

        if (uuid == null) {
            CompletableFuture<Boolean> fut = new CompletableFuture<>();
            fut.completeExceptionally(ex);
            return fut;
        }

        UUID finalUuid = uuid;
        return plugin.getStorage().getUniqueUsers().thenApply(set -> set.contains(finalUuid));
    }

    @Override
    public CompletableFuture<ImmutableCollection<LPSubject>> loadSubjects(Set<String> identifiers) {
        return CompletableFuture.supplyAsync(() -> {
            ImmutableSet.Builder<LPSubject> ret = ImmutableSet.builder();
            for (String id : identifiers) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(id);
                } catch (IllegalArgumentException e) {
                    continue;
                }

                ret.add(loadSubject(uuid.toString()).join());
            }

            return ret.build();
        }, plugin.getScheduler().async());
    }

    @Override
    public ImmutableCollection<LPSubject> getLoadedSubjects() {
        return getAll().values().stream().map(SpongeUser::sponge).collect(ImmutableCollectors.toSet());
    }

    @Override
    public CompletableFuture<ImmutableSet<String>> getAllIdentifiers() {
        return CompletableFuture.supplyAsync(() -> {
            ImmutableSet.Builder<String> ids = ImmutableSet.builder();

            getAll().keySet().forEach(uuid -> ids.add(uuid.getUuid().toString()));
            plugin.getStorage().getUniqueUsers().join().forEach(uuid -> ids.add(uuid.toString()));

            return ids.build();
        }, plugin.getScheduler().async());
    }

    @Override
    public CompletableFuture<ImmutableMap<SubjectReference, Boolean>> getAllWithPermission(String permission) {
        return CompletableFuture.supplyAsync(() -> {
            ImmutableMap.Builder<SubjectReference, Boolean> ret = ImmutableMap.builder();

            List<HeldPermission<UUID>> lookup = plugin.getStorage().getUsersWithPermission(permission).join();
            for (HeldPermission<UUID> holder : lookup) {
                if (holder.asNode().getFullContexts().equals(ImmutableContextSet.empty())) {
                    ret.put(SubjectReferenceFactory.obtain(getService(), getIdentifier(), holder.getHolder().toString()), holder.getValue());
                }
            }

            return ret.build();
        }, plugin.getScheduler().async());
    }

    @Override
    public CompletableFuture<ImmutableMap<SubjectReference, Boolean>> getAllWithPermission(ImmutableContextSet contexts, String permission) {
        return CompletableFuture.supplyAsync(() -> {
            ImmutableMap.Builder<SubjectReference, Boolean> ret = ImmutableMap.builder();

            List<HeldPermission<UUID>> lookup = plugin.getStorage().getUsersWithPermission(permission).join();
            for (HeldPermission<UUID> holder : lookup) {
                if (holder.asNode().getFullContexts().equals(contexts)) {
                    ret.put(SubjectReferenceFactory.obtain(getService(), getIdentifier(), holder.getHolder().toString()), holder.getValue());
                }
            }

            return ret.build();
        }, plugin.getScheduler().async());
    }

    @Override
    public ImmutableMap<LPSubject, Boolean> getLoadedWithPermission(String permission) {
        return objects.asMap().values().stream()
                .map(SpongeUser::sponge)
                .map(sub -> Maps.immutableEntry(sub, sub.getPermissionValue(ImmutableContextSet.empty(), permission)))
                .filter(pair -> pair.getValue() != Tristate.UNDEFINED)
                .collect(ImmutableCollectors.toMap(Map.Entry::getKey, sub -> sub.getValue().asBoolean()));
    }

    @Override
    public ImmutableMap<LPSubject, Boolean> getLoadedWithPermission(ImmutableContextSet contexts, String permission) {
        return objects.asMap().values().stream()
                .map(SpongeUser::sponge)
                .map(sub -> Maps.immutableEntry(sub, sub.getPermissionValue(contexts, permission)))
                .filter(pair -> pair.getValue() != Tristate.UNDEFINED)
                .collect(ImmutableCollectors.toMap(Map.Entry::getKey, sub -> sub.getValue().asBoolean()));
    }

    @Override
    public LPSubject getDefaults() {
        return getService().getDefaultSubjects().loadSubject(getIdentifier()).join();
    }

}
