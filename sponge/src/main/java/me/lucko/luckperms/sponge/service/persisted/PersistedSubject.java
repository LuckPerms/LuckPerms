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

import lombok.Getter;
import lombok.NonNull;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.buffers.BufferedRequest;
import me.lucko.luckperms.common.verbose.CheckOrigin;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.service.ProxyFactory;
import me.lucko.luckperms.sponge.service.calculated.CalculatedSubjectData;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.SubjectReference;
import me.lucko.luckperms.sponge.service.storage.SubjectStorageModel;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.permission.Subject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * A simple persistable Subject implementation
 */
@Getter
public class PersistedSubject implements LPSubject {
    private final String identifier;

    private final LuckPermsService service;
    private final PersistedCollection parentCollection;

    private final PersistedSubjectData subjectData;
    private final CalculatedSubjectData transientSubjectData;

    private final LoadingCache<PermissionLookupKey, Tristate> permissionLookupCache = Caffeine.newBuilder()
            .expireAfterAccess(20, TimeUnit.MINUTES)
            .build(lookup -> lookupPermissionValue(lookup.getContexts(), lookup.getNode()));

    private final LoadingCache<ImmutableContextSet, ImmutableList<SubjectReference>> parentLookupCache = Caffeine.newBuilder()
            .expireAfterAccess(20, TimeUnit.MINUTES)
            .build(this::lookupParents);

    private final LoadingCache<OptionLookupKey, Optional<String>> optionLookupCache = Caffeine.newBuilder()
            .expireAfterAccess(20, TimeUnit.MINUTES)
            .build(lookup -> lookupOptionValue(lookup.getContexts(), lookup.getKey()));

    private final BufferedRequest<Void> saveBuffer = new BufferedRequest<Void>(1000L, 500L, r -> PersistedSubject.this.service.getPlugin().getScheduler().doAsync(r)) {
        @Override
        protected Void perform() {
            try {
                service.getStorage().saveToFile(PersistedSubject.this);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    };

    public PersistedSubject(String identifier, LuckPermsService service, PersistedCollection parentCollection) {
        this.identifier = identifier;
        this.service = service;
        this.parentCollection = parentCollection;

        this.subjectData = new PersistedSubjectData(service, parentCollection.getIdentifier() + "/" + identifier + "/p", this);
        this.transientSubjectData = new CalculatedSubjectData(this, service, parentCollection.getIdentifier() + "/" + identifier + "/t");
    }

    @Override
    public void invalidateCaches(CacheLevel type) {
        optionLookupCache.invalidateAll();

        if (type == CacheLevel.OPTION) {
            return;
        }

        permissionLookupCache.invalidateAll();
        subjectData.invalidateLookupCache();
        transientSubjectData.invalidateLookupCache();

        if (type == CacheLevel.PERMISSION) {
            return;
        }

        parentLookupCache.invalidateAll();
    }

    @Override
    public void performCleanup() {
        this.subjectData.cleanup();
        this.transientSubjectData.cleanup();
        this.permissionLookupCache.cleanUp();
        this.parentLookupCache.cleanUp();
        this.optionLookupCache.cleanUp();
    }

    public void loadData(SubjectStorageModel dataHolder) {
        subjectData.setSave(false);
        dataHolder.applyToData(subjectData);
        subjectData.setSave(true);
    }

    public void save() {
        saveBuffer.request();
    }

    @Override
    public Subject sponge() {
        return ProxyFactory.toSponge(this);
    }

    @Override
    public Optional<CommandSource> getCommandSource() {
        return Optional.empty();
    }

    private Tristate lookupPermissionValue(ImmutableContextSet contexts, String node) {
        Tristate res;

        // if transient has priority
        if (!parentCollection.getIdentifier().equals("defaults")) {
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
            res = parent.resolveLp().join().getPermissionValue(contexts, node);
            if (res != Tristate.UNDEFINED) {
                return res;
            }
        }

        if (getParentCollection().getIdentifier().equalsIgnoreCase("defaults")) {
            return Tristate.UNDEFINED;
        }

        res = getParentCollection().getDefaults().getPermissionValue(contexts, node);
        if (res != Tristate.UNDEFINED) {
            return res;
        }

        res = service.getDefaults().getPermissionValue(contexts, node);
        return res;
    }

    private ImmutableList<SubjectReference> lookupParents(ImmutableContextSet contexts) {
        List<SubjectReference> s = new ArrayList<>();
        s.addAll(subjectData.getParents(contexts));
        s.addAll(transientSubjectData.getParents(contexts));

        if (!getParentCollection().getIdentifier().equalsIgnoreCase("defaults")) {
            s.addAll(getParentCollection().getDefaults().getParents(contexts));
            s.addAll(service.getDefaults().getParents(contexts));
        }

        return service.sortSubjects(s);
    }

    private Optional<String> lookupOptionValue(ImmutableContextSet contexts, String key) {
        Optional<String> res;

        // if transient has priority
        if (!parentCollection.getIdentifier().equals("defaults")) {
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
            res = parent.resolveLp().join().getOption(contexts, key);
            if (res.isPresent()) {
                return res;
            }
        }

        if (getParentCollection().getIdentifier().equalsIgnoreCase("defaults")) {
            return Optional.empty();
        }

        res = getParentCollection().getDefaults().getOption(contexts, key);
        if (res.isPresent()) {
            return res;
        }

        return service.getDefaults().getOption(contexts, key);
    }

    @Override
    public Tristate getPermissionValue(@NonNull ImmutableContextSet contexts, @NonNull String node) {
        Tristate t = permissionLookupCache.get(PermissionLookupKey.of(node, contexts));
        service.getPlugin().getVerboseHandler().offerCheckData(CheckOrigin.INTERNAL, getParentCollection().getIdentifier() + "/" + identifier, contexts, node, t);
        return t;
    }

    @Override
    public boolean isChildOf(@NonNull ImmutableContextSet contexts, @NonNull SubjectReference subject) {
        if (getParentCollection().getIdentifier().equalsIgnoreCase("defaults")) {
            return subjectData.getParents(contexts).contains(subject) ||
                    transientSubjectData.getParents(contexts).contains(subject);
        } else {
            return subjectData.getParents(contexts).contains(subject) ||
                    transientSubjectData.getParents(contexts).contains(subject) ||
                    getParentCollection().getDefaults().getParents(contexts).contains(subject) ||
                    service.getDefaults().getParents(contexts).contains(subject);
        }
    }

    @Override
    public ImmutableList<SubjectReference> getParents(@NonNull ImmutableContextSet contexts) {
        return parentLookupCache.get(contexts);
    }

    @Override
    public Optional<String> getOption(ImmutableContextSet contexts, String key) {
        return optionLookupCache.get(OptionLookupKey.of(key, contexts));
    }

    @Override
    public ImmutableContextSet getActiveContextSet() {
        return service.getPlugin().getContextManager().getApplicableContext(sponge());
    }
}
