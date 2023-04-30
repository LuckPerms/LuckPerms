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

package me.lucko.luckperms.sponge.service.model.persisted;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import me.lucko.luckperms.common.cache.LoadingMap;
import me.lucko.luckperms.common.context.ImmutableContextSetImpl;
import me.lucko.luckperms.common.util.ImmutableCollectors;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.service.ProxyFactory;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectCollection;
import me.lucko.luckperms.sponge.service.model.LPSubjectReference;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.util.Tristate;
import org.spongepowered.api.service.permission.SubjectCollection;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * A simple persistable subject collection
 */
public class PersistedCollection implements LPSubjectCollection {
    private final LuckPermsService service;

    /**
     * The collection identifier
     */
    private final String identifier;

    /**
     * If the collection is the defaults collection
     */
    private final boolean isDefaultsCollection;

    /**
     * Cached sponge proxy instance
     */
    private final SubjectCollection spongeProxy;

    /**
     * The contained subjects
     */
    private final Map<String, PersistedSubject> subjects = LoadingMap.of(s -> new PersistedSubject(getService(), this, s));

    public PersistedCollection(LuckPermsService service, String identifier) {
        this.service = service;
        this.identifier = identifier;
        this.isDefaultsCollection = identifier.equals("defaults");
        this.spongeProxy = ProxyFactory.toSponge(this);
    }

    public void loadAll() {
        Map<String, SubjectDataContainer> holders = this.service.getStorage().loadAllFromFile(this.identifier);
        for (Map.Entry<String, SubjectDataContainer> e : holders.entrySet()) {
            PersistedSubject subject = this.subjects.get(e.getKey().toLowerCase(Locale.ROOT));
            if (subject != null) {
                subject.loadData(e.getValue());
            }
        }
    }

    @Override
    public SubjectCollection sponge() {
        return this.spongeProxy;
    }

    @Override
    public LuckPermsService getService() {
        return this.service;
    }

    @Override
    public String getIdentifier() {
        return this.identifier;
    }

    @Override
    public Predicate<String> getIdentifierValidityPredicate() {
        return Predicates.alwaysTrue();
    }

    @Override
    public boolean isDefaultsCollection() {
        return this.isDefaultsCollection;
    }

    public LPSubject obtainSubject(String identifier) {
        return this.subjects.get(identifier.toLowerCase(Locale.ROOT));
    }

    @Override
    @Deprecated // not necessary to wrap with a completablefuture
    public CompletableFuture<LPSubject> loadSubject(String identifier) {
        return CompletableFuture.completedFuture(obtainSubject(identifier));
    }

    @Override
    @Deprecated // not necessary to wrap with an optional
    public Optional<LPSubject> getSubject(String identifier) {
        return Optional.of(Objects.requireNonNull(obtainSubject(identifier)));
    }

    @Override
    public CompletableFuture<Boolean> hasRegistered(String identifier) {
        return CompletableFuture.completedFuture(this.subjects.containsKey(identifier.toLowerCase(Locale.ROOT)));
    }

    @Override
    public CompletableFuture<ImmutableCollection<LPSubject>> loadSubjects(Iterable<String> identifiers) {
        ImmutableSet.Builder<LPSubject> subjects = ImmutableSet.builder();
        for (String id : identifiers) {
            subjects.add(Objects.requireNonNull(this.subjects.get(id.toLowerCase(Locale.ROOT))));
        }
        return CompletableFuture.completedFuture(subjects.build());
    }

    @Override
    public ImmutableCollection<LPSubject> getLoadedSubjects() {
        return ImmutableList.copyOf(this.subjects.values());
    }

    @Override
    public CompletableFuture<ImmutableSet<String>> getAllIdentifiers() {
        return CompletableFuture.completedFuture(ImmutableSet.copyOf(this.subjects.keySet()));
    }

    @Override
    public CompletableFuture<ImmutableMap<LPSubjectReference, Boolean>> getAllWithPermission(String permission) {
        return CompletableFuture.completedFuture(getLoadedWithPermission(permission).entrySet().stream()
                .collect(ImmutableCollectors.toMap(e -> e.getKey().toReference(), Map.Entry::getValue)));
    }

    @Override
    public CompletableFuture<ImmutableMap<LPSubjectReference, Boolean>> getAllWithPermission(ImmutableContextSet contexts, String permission) {
        return CompletableFuture.completedFuture(getLoadedWithPermission(contexts, permission).entrySet().stream()
                .collect(ImmutableCollectors.toMap(e -> e.getKey().toReference(), Map.Entry::getValue)));
    }

    @Override
    public ImmutableMap<LPSubject, Boolean> getLoadedWithPermission(String permission) {
        ImmutableMap.Builder<LPSubject, Boolean> m = ImmutableMap.builder();
        for (LPSubject subject : this.subjects.values()) {
            Tristate ts = subject.getPermissionValue(ImmutableContextSetImpl.EMPTY, permission);
            if (ts != Tristate.UNDEFINED) {
                m.put(subject, ts.asBoolean());
            }

        }
        return m.build();
    }

    @Override
    public ImmutableMap<LPSubject, Boolean> getLoadedWithPermission(ImmutableContextSet contexts, String permission) {
        ImmutableMap.Builder<LPSubject, Boolean> m = ImmutableMap.builder();
        for (LPSubject subject : this.subjects.values()) {
            Tristate ts = subject.getPermissionValue(contexts, permission);
            if (ts != Tristate.UNDEFINED) {
                m.put(subject, ts.asBoolean());
            }

        }
        return m.build();
    }

    @Override
    public LPSubject getDefaults() {
        return this.service.getDefaultSubjects().getTypeDefaults(getIdentifier());
    }

}
