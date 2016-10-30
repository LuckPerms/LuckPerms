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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.NonNull;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.commands.Util;
import me.lucko.luckperms.common.users.User;
import me.lucko.luckperms.common.users.UserManager;
import me.lucko.luckperms.common.utils.ImmutableCollectors;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.service.LuckPermsUserSubject;
import me.lucko.luckperms.sponge.service.simple.SimpleCollection;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.util.Tristate;

import java.util.Map;
import java.util.Optional;
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
            /*
            .removalListener((RemovalListener<UUID, LuckPermsUserSubject>) r -> {
                if (r.getValue() != null) {
                    r.getValue().deprovision();
                }
            })
            */
            .build(new CacheLoader<UUID, LuckPermsUserSubject>() {
                @Override
                public LuckPermsUserSubject load(UUID uuid) throws Exception {
                    User user = manager.get(uuid);
                    if (user == null) {
                        throw new IllegalStateException("user not loaded");
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
        Optional<Subject> s = getIfLoaded(id);
        if (s.isPresent()) {
            return s.get();
        }

        // Fallback to the other collection. This Subject instance will never be persisted.
        return fallback.get(id);
    }

    private Optional<Subject> getIfLoaded(String id) {
        UUID uuid = Util.parseUuid(id);

        find:
        if (uuid == null) {
            for (LuckPermsUserSubject subject : users.asMap().values()) {
                if (subject.getUser().getName().equals(id)) {
                    return Optional.of(subject);
                }
            }

            for (User user : manager.getAll().values()) {
                if (user.getName().equalsIgnoreCase(id)) {
                    uuid = user.getUuid();
                    break find;
                }
            }
        }

        if (uuid == null) {
            return Optional.empty();
        }

        UUID internal = service.getPlugin().getUuidCache().getUUID(uuid);
        try {
            return Optional.of(users.get(internal));
        } catch (ExecutionException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean hasRegistered(@NonNull String id) {
        return getIfLoaded(id).isPresent();
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
