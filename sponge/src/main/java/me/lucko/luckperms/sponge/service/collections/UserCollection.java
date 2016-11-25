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

package me.lucko.luckperms.sponge.service.collections;

import co.aikar.timings.Timing;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.UncheckedExecutionException;
import lombok.NonNull;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.core.UserIdentifier;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.managers.UserManager;
import me.lucko.luckperms.common.utils.ImmutableCollectors;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.service.LuckPermsUserSubject;
import me.lucko.luckperms.sponge.service.simple.SimpleCollection;
import me.lucko.luckperms.sponge.timings.LPTiming;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.util.Tristate;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Manages low level Subject instances for the PermissionService.
 * Most calls are cached.
 */
public class UserCollection implements SubjectCollection {
    private final LuckPermsService service;
    private final UserManager manager;
    private final SimpleCollection fallback;

    private final LoadingCache<UUID, LuckPermsUserSubject> users = CacheBuilder.newBuilder()
            .build(new CacheLoader<UUID, LuckPermsUserSubject>() {
                @Override
                public LuckPermsUserSubject load(UUID uuid) throws Exception {
                    User user = manager.get(uuid);
                    if (user == null) {
                        throw new IllegalStateException("User not loaded");
                    }

                    return LuckPermsUserSubject.wrapUser(user, service);
                }

                @Override
                public ListenableFuture<LuckPermsUserSubject> reload(UUID uuid, LuckPermsUserSubject s) {
                    return Futures.immediateFuture(s); // Never needs to be refreshed.
                }
            });

    public UserCollection(LuckPermsService service, UserManager manager) {
        this.service = service;
        this.manager = manager;
        this.fallback = new SimpleCollection(service, "fallback-users");
    }

    @Override
    public String getIdentifier() {
        return PermissionService.SUBJECTS_USER;
    }

    public void unload(UUID uuid) {
        users.invalidate(uuid);
    }

    @Override
    public Subject get(@NonNull String id) {
        try (Timing ignored = service.getPlugin().getTimings().time(LPTiming.USER_COLLECTION_GET)) {
            UUID uuid = Util.parseUuid(id);
            if (uuid == null) {
                service.getPlugin().getLog().warn("Couldn't get user subject for id: " + id + " (not a uuid)");
                return fallback.get(id); // fallback to the transient collection
            }

            UUID u = service.getPlugin().getUuidCache().getUUID(uuid);

            // check if the user is loaded in memory. hopefully this call is not on the main thread. :(
            if (!manager.isLoaded(UserIdentifier.of(u, null))) {
                service.getPlugin().getLog().warn("User Subject '" + u + "' was requested, but is not loaded in memory. Loading them from storage now.");
                long startTime = System.currentTimeMillis();
                service.getPlugin().getStorage().loadUser(u, "null").join();
                User user = service.getPlugin().getUserManager().get(u);
                if (user != null) {
                    user.setupData(false);
                }
                service.getPlugin().getLog().warn("Loading '" + u + "' took " + (System.currentTimeMillis() - startTime) + " ms.");
            }

            try {
                return users.get(u);
            } catch (ExecutionException | UncheckedExecutionException e) {
                service.getPlugin().getLog().warn("Unable to get user subject '" + u + "' from memory.");
                e.printStackTrace();
                return fallback.get(u.toString());
            }
        }
    }

    @Override
    public boolean hasRegistered(@NonNull String id) {
        UUID uuid = Util.parseUuid(id);
        if (uuid == null) {
            return false;
        }

        UUID internal = service.getPlugin().getUuidCache().getUUID(uuid);
        return users.asMap().containsKey(internal) || manager.isLoaded(UserIdentifier.of(internal, null));
    }

    @Override
    public Iterable<Subject> getAllSubjects() {
        return users.asMap().values().stream().collect(ImmutableCollectors.toImmutableList());
    }

    @Override
    public Map<Subject, Boolean> getAllWithPermission(@NonNull String id) {
        return getAllWithPermission(SubjectData.GLOBAL_CONTEXT, id);
    }

    @Override
    public Map<Subject, Boolean> getAllWithPermission(@NonNull Set<Context> contexts, @NonNull String node) {
        ContextSet cs = LuckPermsService.convertContexts(contexts);
        return users.asMap().values().stream()
                .filter(sub -> sub.getPermissionValue(cs, node) != Tristate.UNDEFINED)
                .collect(ImmutableCollectors.toImmutableMap(sub -> sub, sub -> sub.getPermissionValue(cs, node).asBoolean()));
    }

    @Override
    public Subject getDefaults() {
        return service.getDefaultSubjects().get(getIdentifier());
    }
}
