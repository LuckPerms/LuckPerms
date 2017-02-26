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

package me.lucko.luckperms.sponge.service.persisted;

import lombok.Getter;
import lombok.NonNull;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.utils.BufferedRequest;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.service.calculated.CalculatedSubjectData;
import me.lucko.luckperms.sponge.service.calculated.OptionLookup;
import me.lucko.luckperms.sponge.service.calculated.PermissionLookup;
import me.lucko.luckperms.sponge.service.proxy.LPSubject;
import me.lucko.luckperms.sponge.service.references.SubjectCollectionReference;
import me.lucko.luckperms.sponge.service.references.SubjectReference;
import me.lucko.luckperms.sponge.timings.LPTiming;

import org.spongepowered.api.command.CommandSource;

import co.aikar.timings.Timing;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A simple persistable Subject implementation
 */
@Getter
public class PersistedSubject implements LPSubject {
    private final String identifier;

    @Getter
    private final LuckPermsService service;
    private final SubjectCollectionReference parentCollection;
    private final PersistedSubjectData subjectData;
    private final CalculatedSubjectData transientSubjectData;

    private final LoadingCache<PermissionLookup, Tristate> permissionLookupCache = CacheBuilder.newBuilder()
            .expireAfterAccess(20, TimeUnit.MINUTES)
            .build(new CacheLoader<PermissionLookup, Tristate>() {
                @Override
                public Tristate load(PermissionLookup lookup) {
                    return lookupPermissionValue(lookup.getContexts(), lookup.getNode());
                }
            });
    private final LoadingCache<ImmutableContextSet, Set<SubjectReference>> parentLookupCache = CacheBuilder.newBuilder()
            .expireAfterAccess(20, TimeUnit.MINUTES)
            .build(new CacheLoader<ImmutableContextSet, Set<SubjectReference>>() {
                @Override
                public Set<SubjectReference> load(ImmutableContextSet contexts) {
                    return lookupParents(contexts);
                }
            });
    private final LoadingCache<OptionLookup, Optional<String>> optionLookupCache = CacheBuilder.newBuilder()
            .expireAfterAccess(20, TimeUnit.MINUTES)
            .build(new CacheLoader<OptionLookup, Optional<String>>() {
                @Override
                public Optional<String> load(OptionLookup lookup) {
                    return lookupOptionValue(lookup.getContexts(), lookup.getKey());
                }
            });


    private final BufferedRequest<Void> saveBuffer = new BufferedRequest<Void>(1000L, r -> PersistedSubject.this.service.getPlugin().doAsync(r)) {
        @Override
        protected Void perform() {
            service.getPlugin().doAsync(() -> {
                try {
                    service.getStorage().saveToFile(PersistedSubject.this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            return null;
        }
    };

    public PersistedSubject(String identifier, LuckPermsService service, PersistedCollection containingCollection) {
        this.identifier = identifier;
        this.service = service;
        this.parentCollection = containingCollection.toReference();

        this.subjectData = new PersistedSubjectData(service, "local:" + containingCollection.getIdentifier() + "/" + identifier + "(p)", this);
        this.transientSubjectData = new CalculatedSubjectData(this, service, "local:" + containingCollection.getIdentifier() + "/" + identifier + "(t)");

        service.getLocalDataCaches().add(subjectData);
        service.getLocalDataCaches().add(transientSubjectData);
        service.getLocalPermissionCaches().add(permissionLookupCache);
        service.getLocalParentCaches().add(parentLookupCache);
        service.getLocalOptionCaches().add(optionLookupCache);
    }

    @Override
    public void performCleanup() {
        this.subjectData.cleanup();
        this.transientSubjectData.cleanup();
        this.permissionLookupCache.cleanUp();
        this.parentLookupCache.cleanUp();
        this.optionLookupCache.cleanUp();
    }

    public void loadData(SubjectDataHolder dataHolder) {
        subjectData.setSave(false);
        dataHolder.copyTo(subjectData);
        subjectData.setSave(true);
    }

    public void save() {
        saveBuffer.request();
    }

    @Override
    public Optional<CommandSource> getCommandSource() {
        return Optional.empty();
    }

    private Tristate lookupPermissionValue(ContextSet contexts, String node) {
        Tristate res;

        if (parentCollection.resolve(service).getTransientHasPriority()) {
            res = transientSubjectData.getPermissionValue(contexts, node);
            if (res != Tristate.UNDEFINED) {
                return res;
            }

            res = subjectData.getPermissionValue(contexts, node);
            if (res != Tristate.UNDEFINED) {
                return res;
            }
        } else {
            res = subjectData.getPermissionValue(contexts, node);
            if (res != Tristate.UNDEFINED) {
                return res;
            }

            res = transientSubjectData.getPermissionValue(contexts, node);
            if (res != Tristate.UNDEFINED) {
                return res;
            }
        }

        for (SubjectReference parent : getParents(contexts)) {
            res = parent.resolve(service).getPermissionValue(contexts, node);
            if (res != Tristate.UNDEFINED) {
                return res;
            }
        }

        if (getParentCollection().resolve(service).getIdentifier().equalsIgnoreCase("defaults")) {
            return Tristate.UNDEFINED;
        }

        res = getParentCollection().resolve(service).getDefaultSubject().resolve(service).getPermissionValue(contexts, node);
        if (res != Tristate.UNDEFINED) {
            return res;
        }

        res = service.getDefaults().getPermissionValue(contexts, node);
        return res;
    }

    private Set<SubjectReference> lookupParents(ContextSet contexts) {
        Set<SubjectReference> s = new HashSet<>();
        s.addAll(subjectData.getParents(contexts));
        s.addAll(transientSubjectData.getParents(contexts));

        if (!getParentCollection().resolve(service).getIdentifier().equalsIgnoreCase("defaults")) {
            s.addAll(getParentCollection().resolve(service).getDefaultSubject().resolve(service).getParents(contexts));
            s.addAll(service.getDefaults().getParents(contexts));
        }

        return ImmutableSet.copyOf(s);
    }

    private Optional<String> lookupOptionValue(ContextSet contexts, String key) {
        Optional<String> res;

        if (getParentCollection().resolve(service).getTransientHasPriority()) {
            res = Optional.ofNullable(transientSubjectData.getOptions(contexts).get(key));
            if (res.isPresent()) {
                return res;
            }

            res = Optional.ofNullable(subjectData.getOptions(contexts).get(key));
            if (res.isPresent()) {
                return res;
            }
        } else {
            res = Optional.ofNullable(subjectData.getOptions(contexts).get(key));
            if (res.isPresent()) {
                return res;
            }

            res = Optional.ofNullable(transientSubjectData.getOptions(contexts).get(key));
            if (res.isPresent()) {
                return res;
            }
        }

        for (SubjectReference parent : getParents(contexts)) {
            res = parent.resolve(service).getOption(contexts, key);
            if (res.isPresent()) {
                return res;
            }
        }

        if (getParentCollection().resolve(service).getIdentifier().equalsIgnoreCase("defaults")) {
            return Optional.empty();
        }

        res = getParentCollection().resolve(service).getDefaultSubject().resolve(service).getOption(contexts, key);
        if (res.isPresent()) {
            return res;
        }

        return service.getDefaults().getOption(contexts, key);
    }

    @Override
    public Tristate getPermissionValue(@NonNull ContextSet contexts, @NonNull String node) {
        try (Timing ignored = service.getPlugin().getTimings().time(LPTiming.INTERNAL_SUBJECT_GET_PERMISSION_VALUE)) {
            Tristate t = permissionLookupCache.getUnchecked(PermissionLookup.of(node, contexts.makeImmutable()));
            service.getPlugin().getDebugHandler().offer("local:" + getParentCollection().getCollection() + "/" + identifier, node, t);
            return t;
        }
    }

    @Override
    public boolean isChildOf(@NonNull ContextSet contexts, @NonNull SubjectReference subject) {
        try (Timing ignored = service.getPlugin().getTimings().time(LPTiming.INTERNAL_SUBJECT_IS_CHILD_OF)) {
            if (getParentCollection().resolve(service).getIdentifier().equalsIgnoreCase("defaults")) {
                return subjectData.getParents(contexts).contains(subject) ||
                        transientSubjectData.getParents(contexts).contains(subject);
            } else {
                return subjectData.getParents(contexts).contains(subject) ||
                        transientSubjectData.getParents(contexts).contains(subject) ||
                        getParentCollection().resolve(service).getDefaultSubject().resolve(service).getParents(contexts).contains(subject) ||
                        service.getDefaults().getParents(contexts).contains(subject);
            }
        }
    }

    @Override
    public Set<SubjectReference> getParents(@NonNull ContextSet contexts) {
        try (Timing ignored = service.getPlugin().getTimings().time(LPTiming.INTERNAL_SUBJECT_GET_PARENTS)) {
            return parentLookupCache.getUnchecked(contexts.makeImmutable());
        }
    }

    @Override
    public Optional<String> getOption(ContextSet contexts, String key) {
        try (Timing ignored = service.getPlugin().getTimings().time(LPTiming.INTERNAL_SUBJECT_GET_OPTION)) {
            return optionLookupCache.getUnchecked(OptionLookup.of(key, contexts.makeImmutable()));
        }
    }

    @Override
    public ContextSet getActiveContextSet() {
        try (Timing ignored = service.getPlugin().getTimings().time(LPTiming.INTERNAL_SUBJECT_GET_ACTIVE_CONTEXTS)) {
            return service.getPlugin().getContextManager().getApplicableContext(this);
        }
    }
}
