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

package me.lucko.luckperms.sponge.service.persisted;

import lombok.AccessLevel;
import lombok.Getter;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.utils.ImmutableCollectors;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.service.ProxyFactory;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectCollection;
import me.lucko.luckperms.sponge.service.model.SubjectReference;
import me.lucko.luckperms.sponge.service.storage.SubjectStorageModel;

import org.spongepowered.api.service.permission.SubjectCollection;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * A simple persistable subject collection
 */
@Getter
public class PersistedCollection implements LPSubjectCollection {
    private final LuckPermsService service;
    private final String identifier;

    @Getter(AccessLevel.NONE)
    private final SubjectCollection spongeProxy;

    @Getter(AccessLevel.NONE)
    private final LoadingCache<String, PersistedSubject> subjects = Caffeine.newBuilder()
            .build(s -> new PersistedSubject(s, getService(), PersistedCollection.this));

    public PersistedCollection(LuckPermsService service, String identifier) {
        this.service = service;
        this.identifier = identifier;
        this.spongeProxy = ProxyFactory.toSponge(this);
    }

    public void loadAll() {
        Map<String, SubjectStorageModel> holders = service.getStorage().loadAllFromFile(identifier);
        for (Map.Entry<String, SubjectStorageModel> e : holders.entrySet()) {
            PersistedSubject subject = subjects.get(e.getKey().toLowerCase());
            subject.loadData(e.getValue());
        }
    }

    @Override
    public SubjectCollection sponge() {
        return spongeProxy;
    }

    @Override
    public Predicate<String> getIdentifierValidityPredicate() {
        return Predicates.alwaysTrue();
    }

    @Override
    public CompletableFuture<LPSubject> loadSubject(String identifier) {
        return CompletableFuture.completedFuture(subjects.get(identifier.toLowerCase()));
    }

    @Override
    public Optional<LPSubject> getSubject(String identifier) {
        return Optional.of(subjects.get(identifier.toLowerCase()));
    }

    @Override
    public CompletableFuture<Boolean> hasRegistered(String identifier) {
        return CompletableFuture.completedFuture(subjects.asMap().containsKey(identifier.toLowerCase()));
    }

    @Override
    public CompletableFuture<ImmutableCollection<LPSubject>> loadSubjects(Set<String> identifiers) {
        ImmutableSet.Builder<LPSubject> ret = ImmutableSet.builder();
        for (String id : identifiers) {
            ret.add(subjects.get(id.toLowerCase()));
        }
        return CompletableFuture.completedFuture(ret.build());
    }

    @Override
    public ImmutableCollection<LPSubject> getLoadedSubjects() {
        return ImmutableList.copyOf(subjects.asMap().values());
    }

    @Override
    public CompletableFuture<ImmutableSet<String>> getAllIdentifiers() {
        return CompletableFuture.completedFuture(ImmutableSet.copyOf(subjects.asMap().keySet()));
    }

    @Override
    public CompletableFuture<ImmutableMap<SubjectReference, Boolean>> getAllWithPermission(String permission) {
        return CompletableFuture.completedFuture(getLoadedWithPermission(permission).entrySet().stream()
                .collect(ImmutableCollectors.toMap(e -> e.getKey().toReference(), Map.Entry::getValue)));
    }

    @Override
    public CompletableFuture<ImmutableMap<SubjectReference, Boolean>> getAllWithPermission(ImmutableContextSet contexts, String permission) {
        return CompletableFuture.completedFuture(getLoadedWithPermission(contexts, permission).entrySet().stream()
                .collect(ImmutableCollectors.toMap(e -> e.getKey().toReference(), Map.Entry::getValue)));
    }

    @Override
    public ImmutableMap<LPSubject, Boolean> getLoadedWithPermission(String permission) {
        ImmutableMap.Builder<LPSubject, Boolean> m = ImmutableMap.builder();
        for (LPSubject subject : subjects.asMap().values()) {
            Tristate ts = subject.getPermissionValue(ImmutableContextSet.empty(), permission);
            if (ts != Tristate.UNDEFINED) {
                m.put(subject, ts.asBoolean());
            }

        }
        return m.build();
    }

    @Override
    public ImmutableMap<LPSubject, Boolean> getLoadedWithPermission(ImmutableContextSet contexts, String permission) {
        ImmutableMap.Builder<LPSubject, Boolean> m = ImmutableMap.builder();
        for (LPSubject subject : subjects.asMap().values()) {
            Tristate ts = subject.getPermissionValue(contexts, permission);
            if (ts != Tristate.UNDEFINED) {
                m.put(subject, ts.asBoolean());
            }

        }
        return m.build();
    }

    @Override
    public LPSubject getDefaults() {
        return service.getDefaultSubjects().loadSubject(getIdentifier()).join();
    }

}
