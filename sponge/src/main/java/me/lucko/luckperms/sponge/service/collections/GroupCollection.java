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
import me.lucko.luckperms.common.groups.Group;
import me.lucko.luckperms.common.groups.GroupManager;
import me.lucko.luckperms.common.utils.ArgumentChecker;
import me.lucko.luckperms.common.utils.ImmutableCollectors;
import me.lucko.luckperms.sponge.service.LuckPermsGroupSubject;
import me.lucko.luckperms.sponge.service.LuckPermsService;
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
import java.util.concurrent.ExecutionException;

public class GroupCollection implements SubjectCollection {
    private final LuckPermsService service;
    private final GroupManager manager;
    private final SimpleCollection fallback;

    private final LoadingCache<String, LuckPermsGroupSubject> groups = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, LuckPermsGroupSubject>() {
                @Override
                public LuckPermsGroupSubject load(String id) throws Exception {
                    Group group = manager.get(id);
                    if (group == null) {
                        throw new IllegalStateException("User not loaded");
                    }

                    return LuckPermsGroupSubject.wrapGroup(group, service);
                }

                @Override
                public ListenableFuture<LuckPermsGroupSubject> reload(String str, LuckPermsGroupSubject s) {
                    return Futures.immediateFuture(s); // Never needs to be refreshed.
                }
            });

    public GroupCollection(LuckPermsService service, GroupManager manager) {
        this.service = service;
        this.manager = manager;
        this.fallback = new SimpleCollection(service, "fallback-groups");
    }

    @Override
    public String getIdentifier() {
        return PermissionService.SUBJECTS_GROUP;
    }

    @Override
    public Subject get(@NonNull String id) {
        try (Timing ignored = service.getPlugin().getTimings().time(LPTiming.GROUP_COLLECTION_GET)) {
            id = id.toLowerCase();
            if (ArgumentChecker.checkName(id)) {
                service.getPlugin().getLog().warn("Couldn't get group subject for id: " + id + " (invalid name)");
                return fallback.get(id); // fallback to transient collection
            }

            // check if the user is loaded in memory. hopefully this call is not on the main thread. :(
            if (!manager.isLoaded(id)) {
                service.getPlugin().getLog().warn("Group Subject '" + id + "' was requested, but is not loaded in memory. Loading it from storage now.");
                long startTime = System.currentTimeMillis();
                service.getPlugin().getStorage().createAndLoadGroup(id).join();
                service.getPlugin().getLog().warn("Loading '" + id + "' took " + (System.currentTimeMillis() - startTime) + " ms.");
            }

            try {
                return groups.get(id);
            } catch (ExecutionException | UncheckedExecutionException e) {
                service.getPlugin().getLog().warn("Unable to get group subject '" + id + "' from memory.");
                e.printStackTrace();
                return fallback.get(id);
            }
        }
    }

    @Override
    public boolean hasRegistered(@NonNull String id) {
        id = id.toLowerCase();
        return !ArgumentChecker.checkName(id) && (groups.asMap().containsKey(id) || manager.isLoaded(id));
    }

    @Override
    public Iterable<Subject> getAllSubjects() {
        return groups.asMap().values().stream().collect(ImmutableCollectors.toImmutableList());
    }

    @Override
    public Map<Subject, Boolean> getAllWithPermission(@NonNull String node) {
        return getAllWithPermission(SubjectData.GLOBAL_CONTEXT, node);
    }

    @Override
    public Map<Subject, Boolean> getAllWithPermission(@NonNull Set<Context> contexts, @NonNull String node) {
        ContextSet cs = LuckPermsService.convertContexts(contexts);
        return groups.asMap().values().stream()
                .filter(sub -> sub.getPermissionValue(cs, node) != Tristate.UNDEFINED)
                .collect(ImmutableCollectors.toImmutableMap(sub -> sub, sub -> sub.getPermissionValue(cs, node).asBoolean()));
    }

    @Override
    public Subject getDefaults() {
        return service.getDefaultSubjects().get(getIdentifier());
    }
}
