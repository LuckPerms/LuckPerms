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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import me.lucko.luckperms.common.contexts.ContextManager;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.sponge.LPSpongePlugin;
import me.lucko.luckperms.sponge.contexts.SpongeProxiedContextCalculator;
import me.lucko.luckperms.sponge.managers.SpongeGroupManager;
import me.lucko.luckperms.sponge.managers.SpongeUserManager;
import me.lucko.luckperms.sponge.service.model.LPPermissionDescription;
import me.lucko.luckperms.sponge.service.model.LPPermissionService;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectCollection;
import me.lucko.luckperms.sponge.service.persisted.PersistedCollection;
import me.lucko.luckperms.sponge.service.reference.SubjectReferenceFactory;
import me.lucko.luckperms.sponge.service.storage.SubjectStorage;

import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;

import java.io.File;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * LuckPerms implementation of the Sponge Permission Service
 */
public class LuckPermsService implements LPPermissionService {

    private final LPSpongePlugin plugin;

    private final PermissionService spongeProxy;

    private final SubjectReferenceFactory referenceFactory;
    private final SubjectStorage storage;
    private final SpongeUserManager userSubjects;
    private final SpongeGroupManager groupSubjects;
    private final PersistedCollection defaultSubjects;
    private final Set<LPPermissionDescription> descriptionSet;

    private final LoadingCache<String, LPSubjectCollection> collections = Caffeine.newBuilder()
            .build(s -> new PersistedCollection(this, s));

    public LuckPermsService(LPSpongePlugin plugin) {
        this.plugin = plugin;
        this.referenceFactory = new SubjectReferenceFactory(this);
        this.spongeProxy = ProxyFactory.toSponge(this);

        this.storage = new SubjectStorage(this, new File(plugin.getBootstrap().getDataDirectory(), "sponge-data"));

        this.userSubjects = plugin.getUserManager();
        this.groupSubjects = plugin.getGroupManager();
        this.defaultSubjects = new PersistedCollection(this, "defaults");
        this.defaultSubjects.loadAll();

        this.collections.put("user", this.userSubjects);
        this.collections.put("group", this.groupSubjects);
        this.collections.put("defaults", this.defaultSubjects);

        for (String collection : this.storage.getSavedCollections()) {
            if (this.collections.asMap().containsKey(collection.toLowerCase())) {
                continue;
            }

            PersistedCollection c = new PersistedCollection(this, collection.toLowerCase());
            c.loadAll();
            this.collections.put(c.getIdentifier(), c);
        }

        this.descriptionSet = ConcurrentHashMap.newKeySet();
    }

    @Override
    public PermissionService sponge() {
        return this.spongeProxy;
    }

    @Override
    public LPSpongePlugin getPlugin() {
        return this.plugin;
    }

    @Override
    public ContextManager<Subject> getContextManager() {
        return this.plugin.getContextManager();
    }

    @Override
    public SubjectReferenceFactory getReferenceFactory() {
        return this.referenceFactory;
    }

    public SubjectStorage getStorage() {
        return this.storage;
    }

    @Override
    public SpongeUserManager getUserSubjects() {
        return this.userSubjects;
    }

    @Override
    public SpongeGroupManager getGroupSubjects() {
        return this.groupSubjects;
    }

    @Override
    public PersistedCollection getDefaultSubjects() {
        return this.defaultSubjects;
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
        Objects.requireNonNull(s);
        return this.collections.get(s.toLowerCase());
    }

    @Override
    public ImmutableMap<String, LPSubjectCollection> getLoadedCollections() {
        return ImmutableMap.copyOf(this.collections.asMap());
    }

    @Override
    public LPPermissionDescription registerPermissionDescription(String id, Text description, PluginContainer owner) {
        LuckPermsPermissionDescription desc = new LuckPermsPermissionDescription(this, id, description, owner);
        this.descriptionSet.add(desc);
        return desc;
    }

    @Override
    public Optional<LPPermissionDescription> getDescription(String s) {
        Objects.requireNonNull(s);
        for (LPPermissionDescription d : this.descriptionSet) {
            if (d.getId().equals(s)) {
                return Optional.of(d);
            }
        }

        return Optional.empty();
    }

    @Override
    public ImmutableSet<LPPermissionDescription> getDescriptions() {
        Set<LPPermissionDescription> descriptions = new HashSet<>(this.descriptionSet);

        // collect known values from the permission vault
        for (String knownPermission : this.plugin.getPermissionVault().getKnownPermissions()) {
            LPPermissionDescription desc = new LuckPermsPermissionDescription(this, knownPermission, null, null);

            // don't override plugin defined values
            if (!descriptions.contains(desc)) {
                descriptions.add(desc);
            }
        }

        return ImmutableSet.copyOf(descriptions);
    }

    @Override
    public void registerContextCalculator(ContextCalculator<Subject> calculator) {
        Objects.requireNonNull(calculator);
        this.plugin.getContextManager().registerCalculator(new SpongeProxiedContextCalculator(calculator));
    }

    @Override
    public void invalidateAllCaches() {
        for (LPSubjectCollection collection : this.collections.asMap().values()) {
            for (LPSubject subject : collection.getLoadedSubjects()) {
                subject.invalidateCaches();
            }
        }

        this.plugin.getCalculatorFactory().invalidateAll();
    }

}
