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

package me.lucko.luckperms.sponge.service;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.caching.UserCache;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.sponge.LPSpongePlugin;
import me.lucko.luckperms.sponge.contexts.SpongeCalculatorLink;
import me.lucko.luckperms.sponge.managers.SpongeGroupManager;
import me.lucko.luckperms.sponge.managers.SpongeUserManager;
import me.lucko.luckperms.sponge.model.SpongeGroup;
import me.lucko.luckperms.sponge.service.calculated.CalculatedSubjectData;
import me.lucko.luckperms.sponge.service.calculated.OptionLookup;
import me.lucko.luckperms.sponge.service.calculated.PermissionLookup;
import me.lucko.luckperms.sponge.service.legacy.LegacyDataMigrator;
import me.lucko.luckperms.sponge.service.model.LPPermissionDescription;
import me.lucko.luckperms.sponge.service.model.LPPermissionService;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectCollection;
import me.lucko.luckperms.sponge.service.model.SubjectReference;
import me.lucko.luckperms.sponge.service.persisted.PersistedCollection;
import me.lucko.luckperms.sponge.service.storage.SubjectStorage;
import me.lucko.luckperms.sponge.timings.LPTiming;

import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;

import co.aikar.timings.Timing;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * The LuckPerms implementation of the Sponge Permission Service
 */
@Getter
public class LuckPermsService implements LPPermissionService {
    public static final String SERVER_CONTEXT = "server";

    private final LPSpongePlugin plugin;

    @Getter(AccessLevel.NONE)
    private final PermissionService spongeProxy;

    private final SubjectStorage storage;
    private final SpongeUserManager userSubjects;
    private final SpongeGroupManager groupSubjects;
    private final PersistedCollection defaultSubjects;
    private final Set<LPPermissionDescription> descriptionSet;

    private final Set<LoadingCache<PermissionLookup, Tristate>> localPermissionCaches;
    private final Set<LoadingCache<ImmutableContextSet, ImmutableList<SubjectReference>>> localParentCaches;
    private final Set<LoadingCache<OptionLookup, Optional<String>>> localOptionCaches;
    private final Set<CalculatedSubjectData> localDataCaches;

    @Getter(value = AccessLevel.NONE)
    private final LoadingCache<String, LPSubjectCollection> collections = Caffeine.newBuilder()
            .build(new CacheLoader<String, LPSubjectCollection>() {
                @Override
                public LPSubjectCollection load(String s) {
                    return new PersistedCollection(LuckPermsService.this, s);
                }

                @Override
                public LPSubjectCollection reload(String s, LPSubjectCollection collection) {
                    return collection; // Never needs to be refreshed.
                }
            });

    public LuckPermsService(LPSpongePlugin plugin) {
        this.plugin = plugin;
        this.spongeProxy = ProxyFactory.toSponge(this);

        localPermissionCaches = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());
        localParentCaches = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());
        localOptionCaches = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());
        localDataCaches = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());

        storage = new SubjectStorage(this, new File(plugin.getDataDirectory(), "sponge-data"));
        new LegacyDataMigrator(plugin, new File(plugin.getDataDirectory(), "local"), storage).run();

        userSubjects = plugin.getUserManager();
        groupSubjects = plugin.getGroupManager();
        defaultSubjects = new PersistedCollection(this, "defaults");
        defaultSubjects.loadAll();

        collections.put(PermissionService.SUBJECTS_USER, userSubjects);
        collections.put(PermissionService.SUBJECTS_GROUP, groupSubjects);
        collections.put("defaults", defaultSubjects);

        for (String collection : storage.getSavedCollections()) {
            if (collections.asMap().containsKey(collection.toLowerCase())) {
                continue;
            }

            PersistedCollection c = new PersistedCollection(this, collection.toLowerCase());
            c.loadAll();
            collections.put(c.getIdentifier(), c);
        }

        descriptionSet = ConcurrentHashMap.newKeySet();
    }

    @Override
    public PermissionService sponge() {
        return spongeProxy;
    }

    @Override
    public LPSubject getDefaults() {
        return getDefaultSubjects().loadSubject("default").join();
    }

    @Override
    public Predicate<String> getIdentifierValidityPredicate() {
        return Predicates.alwaysTrue();
    }

    @Override
    public LPSubjectCollection getCollection(String s) {
        try (Timing ignored = plugin.getTimings().time(LPTiming.GET_SUBJECTS)) {
            return collections.get(s.toLowerCase());
        }
    }

    @Override
    public ImmutableMap<String, LPSubjectCollection> getLoadedCollections() {
        return ImmutableMap.copyOf(collections.asMap());
    }

    @Override
    public SubjectReference newSubjectReference(String collectionIdentifier, String subjectIdentifier) {
        return SubjectReference.of(this, collectionIdentifier, subjectIdentifier);
    }

    @Override
    public LPPermissionDescription registerPermissionDescription(String id, Text description, PluginContainer owner) {
        LuckPermsPermissionDescription desc = new LuckPermsPermissionDescription(this, id, description, owner);
        descriptionSet.add(desc);
        return desc;
    }

    @Override
    public Optional<LPPermissionDescription> getDescription(@NonNull String s) {
        for (LPPermissionDescription d : descriptionSet) {
            if (d.getId().equals(s)) {
                return Optional.of(d);
            }
        }

        return Optional.empty();
    }

    @Override
    public ImmutableSet<LPPermissionDescription> getDescriptions() {
        Set<LPPermissionDescription> descriptions = new HashSet<>(descriptionSet);

        // collect known values from the permission vault
        for (String knownPermission : plugin.getPermissionVault().getKnownPermissions()) {
            LPPermissionDescription desc = new LuckPermsPermissionDescription(this, knownPermission, null, null);

            // don't override plugin defined values
            if (!descriptions.contains(desc)) {
                descriptions.add(desc);
            }
        }

        return ImmutableSet.copyOf(descriptions);
    }

    @Override
    public void registerContextCalculator(org.spongepowered.api.service.context.ContextCalculator<Subject> calculator) {
        plugin.getContextManager().registerCalculator(new SpongeCalculatorLink(calculator));
    }

    @Override
    public ImmutableList<SubjectReference> sortSubjects(Collection<SubjectReference> s) {
        List<SubjectReference> ret = new ArrayList<>(s);
        ret.sort(Collections.reverseOrder((o1, o2) -> {
            if (o1.equals(o2)) {
                return 0;
            }

            boolean o1isGroup = o1.getCollectionIdentifier().equals(PermissionService.SUBJECTS_GROUP);
            boolean o2isGroup = o2.getCollectionIdentifier().equals(PermissionService.SUBJECTS_GROUP);

            if (o1isGroup != o2isGroup) {
                return o1isGroup ? 1 : -1;
            }

            // Neither are groups
            if (!o1isGroup) {
                return 1;
            }

            Group g1 = plugin.getGroupManager().getIfLoaded(o1.getSubjectIdentifier());
            Group g2 = plugin.getGroupManager().getIfLoaded(o2.getSubjectIdentifier());

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

    @Override
    public Contexts calculateContexts(ImmutableContextSet contextSet) {
        return plugin.getContextManager().formContexts(null, contextSet);
    }

    @Override
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
            userCache.invalidateCache();
        }

        for (SpongeGroup group : plugin.getGroupManager().getAll().values()) {
            group.sponge().invalidateCaches();
        }
    }

    @Override
    public void invalidateParentCaches() {
        for (LoadingCache<ImmutableContextSet, ImmutableList<SubjectReference>> c : localParentCaches) {
            c.invalidateAll();
        }
        invalidateOptionCaches();
        invalidatePermissionCaches();
    }

    @Override
    public void invalidateOptionCaches() {
        for (LoadingCache<OptionLookup, Optional<String>> c : localOptionCaches) {
            c.invalidateAll();
        }

        for (User user : plugin.getUserManager().getAll().values()) {
            UserCache userCache = user.getUserData();
            userCache.invalidateCache();
        }
    }
}
