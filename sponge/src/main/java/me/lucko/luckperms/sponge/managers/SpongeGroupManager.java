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

import co.aikar.timings.Timing;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.NonNull;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.managers.GroupManager;
import me.lucko.luckperms.common.utils.ArgumentChecker;
import me.lucko.luckperms.common.utils.ImmutableCollectors;
import me.lucko.luckperms.sponge.LPSpongePlugin;
import me.lucko.luckperms.sponge.model.SpongeGroup;
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

public class SpongeGroupManager implements GroupManager, SubjectCollection {
    private final LPSpongePlugin plugin;
    private final SimpleCollection fallback;

    private final LoadingCache<String, SpongeGroup> objects = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, SpongeGroup>() {
                @Override
                public SpongeGroup load(String i) {
                    return apply(i);
                }

                @Override
                public ListenableFuture<SpongeGroup> reload(String i, SpongeGroup t) {
                    return Futures.immediateFuture(t); // Never needs to be refreshed.
                }
            });

    public SpongeGroupManager(LPSpongePlugin plugin) {
        this.plugin = plugin;
        this.fallback = plugin.getService().getFallbackGroupSubjects();
    }

    @Override
    public SpongeGroup apply(String name) {
        return new SpongeGroup(name, plugin);
    }

    /* ------------------------------------------
     * Manager methods
     * ------------------------------------------ */

    @Override
    public Map<String, SpongeGroup> getAll() {
        return ImmutableMap.copyOf(objects.asMap());
    }

    @Override
    public SpongeGroup getOrMake(String id) {
        return objects.getUnchecked(id);
    }

    @Override
    public SpongeGroup getIfLoaded(String id) {
        return objects.getIfPresent(id);
    }

    @Override
    public boolean isLoaded(String id) {
        return objects.asMap().containsKey(id);
    }

    @Override
    public void unload(Group t) {
        if (t != null) {
            objects.invalidate(t.getId());
        }
    }

    @Override
    public void unloadAll() {
        objects.invalidateAll();
    }

    /* ------------------------------------------
     * SubjectCollection methods
     * ------------------------------------------ */

    @Override
    public String getIdentifier() {
        return PermissionService.SUBJECTS_GROUP;
    }

    @Override
    public Subject get(@NonNull String id) {
        // Special Sponge method. This call will actually load the group from the datastore if not already present.

        try (Timing ignored = plugin.getTimings().time(LPTiming.GROUP_COLLECTION_GET)) {
            id = id.toLowerCase();
            if (ArgumentChecker.checkName(id)) {
                plugin.getLog().warn("Couldn't get group subject for id: " + id + " (invalid name)");
                return fallback.get(id); // fallback to transient collection
            }

            // check if the group is loaded in memory.
            if (isLoaded(id)) {
                return getIfLoaded(id).getSpongeData();
            } else {

                // Group isn't already loaded. hopefully this call is not on the main thread. :(
                plugin.getLog().warn("Group Subject '" + id + "' was requested, but is not loaded in memory. Loading it from storage now.");
                long startTime = System.currentTimeMillis();
                plugin.getStorage().createAndLoadGroup(id).join();
                SpongeGroup group = getIfLoaded(id);

                if (group == null) {
                    plugin.getLog().severe("Error whilst loading group '" + id + "'.");
                    return fallback.get(id);
                }

                plugin.getLog().warn("Loading '" + id + "' took " + (System.currentTimeMillis() - startTime) + " ms.");
                return group.getSpongeData();
            }
        }
    }

    @Override
    public boolean hasRegistered(@NonNull String id) {
        id = id.toLowerCase();
        return !ArgumentChecker.checkName(id) && isLoaded(id);
    }

    @Override
    public Iterable<Subject> getAllSubjects() {
        return objects.asMap().values().stream().map(SpongeGroup::getSpongeData).collect(ImmutableCollectors.toImmutableList());
    }

    @Override
    public Map<Subject, Boolean> getAllWithPermission(@NonNull String id) {
        return getAllWithPermission(SubjectData.GLOBAL_CONTEXT, id);
    }

    @Override
    public Map<Subject, Boolean> getAllWithPermission(@NonNull Set<Context> contexts, @NonNull String node) {
        ContextSet cs = LuckPermsService.convertContexts(contexts);
        return objects.asMap().values().stream()
                .map(SpongeGroup::getSpongeData)
                .filter(sub -> sub.getPermissionValue(cs, node) != Tristate.UNDEFINED)
                .collect(ImmutableCollectors.toImmutableMap(sub -> sub, sub -> sub.getPermissionValue(cs, node).asBoolean()));
    }

    @Override
    public Subject getDefaults() {
        return plugin.getService().getDefaultSubjects().get(getIdentifier());
    }
}
