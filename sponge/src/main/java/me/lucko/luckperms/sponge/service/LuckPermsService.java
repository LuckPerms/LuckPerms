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

package me.lucko.luckperms.sponge.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.caching.UserCache;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.utils.ImmutableCollectors;
import me.lucko.luckperms.sponge.LPSpongePlugin;
import me.lucko.luckperms.sponge.contexts.SpongeCalculatorLink;
import me.lucko.luckperms.sponge.managers.SpongeGroupManager;
import me.lucko.luckperms.sponge.managers.SpongeUserManager;
import me.lucko.luckperms.sponge.model.SpongeGroup;
import me.lucko.luckperms.sponge.service.calculated.CalculatedSubjectData;
import me.lucko.luckperms.sponge.service.calculated.OptionLookup;
import me.lucko.luckperms.sponge.service.calculated.PermissionLookup;
import me.lucko.luckperms.sponge.service.persisted.PersistedCollection;
import me.lucko.luckperms.sponge.service.persisted.SubjectStorage;
import me.lucko.luckperms.sponge.service.proxy.LPSubject;
import me.lucko.luckperms.sponge.service.proxy.LPSubjectCollection;
import me.lucko.luckperms.sponge.service.proxy.LPSubjectData;
import me.lucko.luckperms.sponge.service.references.SubjectReference;
import me.lucko.luckperms.sponge.timings.LPTiming;

import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.text.Text;

import co.aikar.timings.Timing;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The LuckPerms implementation of the Sponge Permission Service
 */
@Getter
public class LuckPermsService implements PermissionService {
    public static final String SERVER_CONTEXT = "server";

    private final LPSpongePlugin plugin;
    private final SubjectStorage storage;
    private final SpongeUserManager userSubjects;
    private final PersistedCollection fallbackUserSubjects;
    private final SpongeGroupManager groupSubjects;
    private final PersistedCollection fallbackGroupSubjects;
    private final PersistedCollection defaultSubjects;
    private final Set<PermissionDescription> descriptionSet;

    private final Set<LoadingCache<PermissionLookup, Tristate>> localPermissionCaches;
    private final Set<LoadingCache<ImmutableContextSet, Set<SubjectReference>>> localParentCaches;
    private final Set<LoadingCache<OptionLookup, Optional<String>>> localOptionCaches;
    private final Set<CalculatedSubjectData> localDataCaches;

    @Getter(value = AccessLevel.NONE)
    private final LoadingCache<String, LPSubjectCollection> collections = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, LPSubjectCollection>() {
                @Override
                public LPSubjectCollection load(String s) {
                    return new PersistedCollection(LuckPermsService.this, s, true);
                }

                @Override
                public ListenableFuture<LPSubjectCollection> reload(String s, LPSubjectCollection collection) {
                    return Futures.immediateFuture(collection); // Never needs to be refreshed.
                }
            });

    public LuckPermsService(LPSpongePlugin plugin) {
        this.plugin = plugin;

        localPermissionCaches = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());
        localParentCaches = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());
        localOptionCaches = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());
        localDataCaches = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());

        storage = new SubjectStorage(new File(plugin.getDataDirectory(), "local"));

        userSubjects = plugin.getUserManager();
        fallbackUserSubjects = new PersistedCollection(this, "fallback-users", true);
        groupSubjects = plugin.getGroupManager();
        fallbackGroupSubjects = new PersistedCollection(this, "fallback-groups", true);
        defaultSubjects = new PersistedCollection(this, "defaults", false);
        defaultSubjects.loadAll();

        collections.put(PermissionService.SUBJECTS_USER, userSubjects);
        collections.put("fallback-users", fallbackUserSubjects);
        collections.put(PermissionService.SUBJECTS_GROUP, groupSubjects);
        collections.put("fallback-groups", fallbackGroupSubjects);
        collections.put("defaults", defaultSubjects);

        for (String collection : storage.getSavedCollections()) {
            if (collections.asMap().containsKey(collection.toLowerCase())) {
                continue;
            }

            PersistedCollection c = new PersistedCollection(this, collection.toLowerCase(), true);
            c.loadAll();
            collections.put(c.getIdentifier(), c);
        }

        descriptionSet = ConcurrentHashMap.newKeySet();
    }

    public LPSubjectData getDefaultData() {
        return getDefaults().getSubjectData();
    }

    @Override
    public LPSubject getDefaults() {
        return getDefaultSubjects().get("default");
    }

    @Override
    public LPSubjectCollection getSubjects(String s) {
        try (Timing ignored = plugin.getTimings().time(LPTiming.GET_SUBJECTS)) {
            return collections.getUnchecked(s.toLowerCase());
        }
    }

    public Map<String, LPSubjectCollection> getCollections() {
        return ImmutableMap.copyOf(collections.asMap());
    }

    @Deprecated
    @Override
    public Map<String, SubjectCollection> getKnownSubjects() {
        return getCollections().entrySet().stream().collect(ImmutableCollectors.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Optional<PermissionDescription.Builder> newDescriptionBuilder(@NonNull Object o) {
        Optional<PluginContainer> container = plugin.getGame().getPluginManager().fromInstance(o);
        if (!container.isPresent()) {
            throw new IllegalArgumentException("Couldn't find a plugin container for " + o.getClass().getSimpleName());
        }

        return Optional.of(new DescriptionBuilder(this, container.get()));
    }

    @Override
    public Optional<PermissionDescription> getDescription(@NonNull String s) {
        for (PermissionDescription d : descriptionSet) {
            if (d.getId().equals(s)) {
                return Optional.of(d);
            }
        }

        return Optional.empty();
    }

    @Override
    public Collection<PermissionDescription> getDescriptions() {
        return ImmutableSet.copyOf(descriptionSet);
    }

    @Override
    public void registerContextCalculator(@NonNull ContextCalculator<Subject> contextCalculator) {
        plugin.getContextManager().registerCalculator(new SpongeCalculatorLink(contextCalculator));
    }

    public List<Subject> sortSubjects(List<Subject> s) {
        List<Subject> ret = new ArrayList<>(s);
        ret.sort(Collections.reverseOrder((o1, o2) -> {
            if (o1.equals(o2)) {
                return 0;
            }

            boolean o1isGroup = o1.getContainingCollection().getIdentifier().equals(PermissionService.SUBJECTS_GROUP);
            boolean o2isGroup = o2.getContainingCollection().getIdentifier().equals(PermissionService.SUBJECTS_GROUP);

            if (o1isGroup != o2isGroup) {
                return o1isGroup ? 1 : -1;
            }

            // Neither are groups
            if (!o1isGroup) {
                return 1;
            }

            Group g1 = plugin.getGroupManager().getIfLoaded(o1.getIdentifier());
            Group g2 = plugin.getGroupManager().getIfLoaded(o2.getIdentifier());

            boolean g1Null = g1 == null;
            boolean g2Null = g2 == null;

            if (g1Null != g2Null) {
                return g1Null ? -1 : 1;
            }

            // Both are null
            if (g1Null) {
                return 1;
            }

            return Integer.compare(g1.getWeight().orElse(0), g2.getWeight().orElse(0)) == 1 ? 1 : -1;
        }));
        return ImmutableList.copyOf(ret);
    }

    public Contexts calculateContexts(ContextSet contextSet) {
        return new Contexts(
                contextSet,
                plugin.getConfiguration().get(ConfigKeys.INCLUDING_GLOBAL_PERMS),
                plugin.getConfiguration().get(ConfigKeys.INCLUDING_GLOBAL_WORLD_PERMS),
                true,
                plugin.getConfiguration().get(ConfigKeys.APPLYING_GLOBAL_GROUPS),
                plugin.getConfiguration().get(ConfigKeys.APPLYING_GLOBAL_WORLD_GROUPS),
                false
        );
    }

    public void invalidatePermissionCaches() {
        for (LoadingCache<PermissionLookup, Tristate> c : localPermissionCaches) {
            c.invalidateAll();
        }
        for (CalculatedSubjectData subjectData : localDataCaches) {
            subjectData.invalidateLookupCache();
        }

        plugin.getCalculatorFactory().invalidateAll();

        for (User user : plugin.getUserManager().getAll().values()) {
            UserCache userCache = user.getUserData();
            if (userCache == null) continue;

            userCache.invalidateCache();
        }

        for (SpongeGroup group : plugin.getGroupManager().getAll().values()) {
            group.getSpongeData().invalidateCaches();
        }
    }

    public void invalidateParentCaches() {
        for (LoadingCache<ImmutableContextSet, Set<SubjectReference>> c : localParentCaches) {
            c.invalidateAll();
        }
        invalidateOptionCaches();
        invalidatePermissionCaches();
    }

    public void invalidateOptionCaches() {
        for (LoadingCache<OptionLookup, Optional<String>> c : localOptionCaches) {
            c.invalidateAll();
        }

        for (User user : plugin.getUserManager().getAll().values()) {
            UserCache userCache = user.getUserData();
            if (userCache == null) continue;

            userCache.invalidateCache();
        }
    }

    @RequiredArgsConstructor
    @EqualsAndHashCode
    @ToString
    public static class DescriptionBuilder implements PermissionDescription.Builder {
        private final LuckPermsService service;
        private final PluginContainer container;
        private final Map<String, Tristate> roles = new HashMap<>();
        private String id = null;
        private Text description = null;

        @Override
        public PermissionDescription.Builder id(@NonNull String s) {
            id = s;
            return this;
        }

        @Override
        public PermissionDescription.Builder description(@NonNull Text text) {
            description = text;
            return this;
        }

        @Override
        public PermissionDescription.Builder assign(@NonNull String s, boolean b) {
            roles.put(s, Tristate.fromBoolean(b));
            return this;
        }

        @Override
        public PermissionDescription register() throws IllegalStateException {
            if (id == null) {
                throw new IllegalStateException("id cannot be null");
            }
            if (description == null) {
                throw new IllegalStateException("description cannot be null");
            }

            Description d = new Description(service, container, id, description);
            service.getDescriptionSet().add(d);

            // Set role-templates
            LPSubjectCollection subjects = service.getSubjects(PermissionService.SUBJECTS_ROLE_TEMPLATE);
            for (Map.Entry<String, Tristate> assignment : roles.entrySet()) {
                LPSubject subject = subjects.get(assignment.getKey());
                subject.getTransientSubjectData().setPermission(ContextSet.empty(), id, assignment.getValue());
            }

            service.getPlugin().getPermissionVault().offer(id);

            return d;
        }
    }

    @Getter
    @AllArgsConstructor
    @EqualsAndHashCode
    @ToString
    public static class Description implements PermissionDescription {
        private final LuckPermsService service;
        private final PluginContainer owner;
        private final String id;
        private final Text description;

        @Override
        public Map<Subject, Boolean> getAssignedSubjects(String id) {
            SubjectCollection subjects = service.getSubjects(id);
            return subjects.getAllWithPermission(this.id);
        }
    }
}
