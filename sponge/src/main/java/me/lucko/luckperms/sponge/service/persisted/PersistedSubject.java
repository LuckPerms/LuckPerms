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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.buffers.BufferedRequest;
import me.lucko.luckperms.common.model.NodeMapType;
import me.lucko.luckperms.common.verbose.CheckOrigin;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.service.ProxyFactory;
import me.lucko.luckperms.sponge.service.calculated.CalculatedSubjectData;
import me.lucko.luckperms.sponge.service.calculated.MonitoredSubjectData;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectData;
import me.lucko.luckperms.sponge.service.reference.LPSubjectReference;
import me.lucko.luckperms.sponge.service.storage.SubjectStorageModel;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.permission.Subject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * A simple persistable Subject implementation
 */
public class PersistedSubject implements LPSubject {
    private final String identifier;

    private final LuckPermsService service;
    private final PersistedCollection parentCollection;

    private final PersistedSubjectData subjectData;
    private final CalculatedSubjectData transientSubjectData;

    private final LoadingCache<PermissionLookupKey, Tristate> permissionLookupCache = Caffeine.newBuilder()
            .expireAfterAccess(20, TimeUnit.MINUTES)
            .build(lookup -> lookupPermissionValue(lookup.contexts, lookup.node));

    private final LoadingCache<ImmutableContextSet, ImmutableList<LPSubjectReference>> parentLookupCache = Caffeine.newBuilder()
            .expireAfterAccess(20, TimeUnit.MINUTES)
            .build(this::lookupParents);

    private final LoadingCache<OptionLookupKey, Optional<String>> optionLookupCache = Caffeine.newBuilder()
            .expireAfterAccess(20, TimeUnit.MINUTES)
            .build(lookup -> lookupOptionValue(lookup.contexts, lookup.key));

    private final BufferedRequest<Void> saveBuffer = new BufferedRequest<Void>(1000L, 500L, r -> PersistedSubject.this.service.getPlugin().getScheduler().doAsync(r)) {
        @Override
        protected Void perform() {
            try {
                PersistedSubject.this.service.getStorage().saveToFile(PersistedSubject.this);
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

        String displayName = parentCollection.getIdentifier() + "/" + identifier;
        this.subjectData = new PersistedSubjectData(this, NodeMapType.ENDURING, service, displayName + "/p") {
            @Override
            protected void onUpdate(boolean success) {
                super.onUpdate(success);
                if (success) {
                    fireUpdateEvent(this);
                }
            }
        };
        this.transientSubjectData = new MonitoredSubjectData(this, NodeMapType.TRANSIENT, service, displayName + "/t") {
            @Override
            protected void onUpdate(boolean success) {
                if (success) {
                    fireUpdateEvent(this);
                }
            }
        };
    }

    private void fireUpdateEvent(LPSubjectData subjectData) {
        this.service.getPlugin().getUpdateEventHandler().fireUpdateEvent(subjectData);
    }

    @Override
    public void invalidateCaches(CacheLevel type) {
        this.optionLookupCache.invalidateAll();

        if (type == CacheLevel.OPTION) {
            return;
        }

        this.permissionLookupCache.invalidateAll();
        this.subjectData.invalidateLookupCache();
        this.transientSubjectData.invalidateLookupCache();

        if (type == CacheLevel.PERMISSION) {
            return;
        }

        this.parentLookupCache.invalidateAll();
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
        this.subjectData.setSave(false);
        dataHolder.applyToData(this.subjectData);
        this.subjectData.setSave(true);
    }

    public void save() {
        this.saveBuffer.request();
    }

    @Override
    public Subject sponge() {
        return ProxyFactory.toSponge(this);
    }

    @Override
    public String getIdentifier() {
        return this.identifier;
    }

    @Override
    public LuckPermsService getService() {
        return this.service;
    }

    @Override
    public PersistedCollection getParentCollection() {
        return this.parentCollection;
    }

    @Override
    public PersistedSubjectData getSubjectData() {
        return this.subjectData;
    }

    @Override
    public CalculatedSubjectData getTransientSubjectData() {
        return this.transientSubjectData;
    }

    @Override
    public Optional<CommandSource> getCommandSource() {
        return Optional.empty();
    }

    private Tristate lookupPermissionValue(ImmutableContextSet contexts, String node) {
        Tristate res;

        // if transient has priority
        if (!this.parentCollection.getIdentifier().equals("defaults")) {
            res = this.transientSubjectData.getPermissionValue(contexts, node);
            if (res != Tristate.UNDEFINED) {
                return res;
            }

            res = this.subjectData.getPermissionValue(contexts, node);
            if (res != Tristate.UNDEFINED) {
                return res;
            }
        } else {
            res = this.subjectData.getPermissionValue(contexts, node);
            if (res != Tristate.UNDEFINED) {
                return res;
            }

            res = this.transientSubjectData.getPermissionValue(contexts, node);
            if (res != Tristate.UNDEFINED) {
                return res;
            }
        }

        for (LPSubjectReference parent : getParents(contexts)) {
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

        res = this.service.getDefaults().getPermissionValue(contexts, node);
        return res;
    }

    private ImmutableList<LPSubjectReference> lookupParents(ImmutableContextSet contexts) {
        List<LPSubjectReference> s = new ArrayList<>();
        s.addAll(this.subjectData.getParents(contexts));
        s.addAll(this.transientSubjectData.getParents(contexts));

        if (!getParentCollection().getIdentifier().equalsIgnoreCase("defaults")) {
            s.addAll(getParentCollection().getDefaults().getParents(contexts));
            s.addAll(this.service.getDefaults().getParents(contexts));
        }

        return this.service.sortSubjects(s);
    }

    private Optional<String> lookupOptionValue(ImmutableContextSet contexts, String key) {
        Optional<String> res;

        // if transient has priority
        if (!this.parentCollection.getIdentifier().equals("defaults")) {
            res = Optional.ofNullable(this.transientSubjectData.getOptions(contexts).get(key));
            if (res.isPresent()) {
                return res;
            }

            res = Optional.ofNullable(this.subjectData.getOptions(contexts).get(key));
            if (res.isPresent()) {
                return res;
            }
        } else {
            res = Optional.ofNullable(this.subjectData.getOptions(contexts).get(key));
            if (res.isPresent()) {
                return res;
            }

            res = Optional.ofNullable(this.transientSubjectData.getOptions(contexts).get(key));
            if (res.isPresent()) {
                return res;
            }
        }

        for (LPSubjectReference parent : getParents(contexts)) {
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

        return this.service.getDefaults().getOption(contexts, key);
    }

    @Override
    public Tristate getPermissionValue(ImmutableContextSet contexts, String node) {
        Objects.requireNonNull(contexts, "contexts");
        Objects.requireNonNull(node, "node");

        Tristate t = this.permissionLookupCache.get(new PermissionLookupKey(node, contexts));
        this.service.getPlugin().getVerboseHandler().offerCheckData(CheckOrigin.INTERNAL, getParentCollection().getIdentifier() + "/" + this.identifier, contexts, node, t);
        return t;
    }

    @Override
    public boolean isChildOf(ImmutableContextSet contexts, LPSubjectReference subject) {
        Objects.requireNonNull(contexts, "contexts");
        Objects.requireNonNull(subject, "subject");

        if (getParentCollection().getIdentifier().equalsIgnoreCase("defaults")) {
            return this.subjectData.getParents(contexts).contains(subject) ||
                    this.transientSubjectData.getParents(contexts).contains(subject);
        } else {
            return this.subjectData.getParents(contexts).contains(subject) ||
                    this.transientSubjectData.getParents(contexts).contains(subject) ||
                    getParentCollection().getDefaults().getParents(contexts).contains(subject) ||
                    this.service.getDefaults().getParents(contexts).contains(subject);
        }
    }

    @Override
    public ImmutableList<LPSubjectReference> getParents(ImmutableContextSet contexts) {
        Objects.requireNonNull(contexts, "contexts");
        return this.parentLookupCache.get(contexts);
    }

    @Override
    public Optional<String> getOption(ImmutableContextSet contexts, String key) {
        return this.optionLookupCache.get(new OptionLookupKey(key, contexts));
    }

    private static final class PermissionLookupKey {
        private final String node;
        private final ImmutableContextSet contexts;

        public PermissionLookupKey(String node, ImmutableContextSet contexts) {
            this.node = node;
            this.contexts = contexts;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof PermissionLookupKey)) return false;
            final PermissionLookupKey other = (PermissionLookupKey) o;

            return this.node.equals(other.node) && this.contexts.equals(other.contexts);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.node, this.contexts);
        }
    }

    public static final class OptionLookupKey {
        private final String key;
        private final ImmutableContextSet contexts;

        public OptionLookupKey(String key, ImmutableContextSet contexts) {
            this.key = key;
            this.contexts = contexts;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof OptionLookupKey)) return false;
            final OptionLookupKey other = (OptionLookupKey) o;

            return this.key.equals(other.key) && this.contexts.equals(other.contexts);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.key, this.contexts);
        }
    }
}
