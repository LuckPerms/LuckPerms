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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.utils.ImmutableCollectors;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.service.proxy.LPSubject;
import me.lucko.luckperms.sponge.service.proxy.LPSubjectCollection;
import me.lucko.luckperms.sponge.service.references.SubjectReference;

import java.util.Collection;
import java.util.Map;

/**
 * A simple persistable subject collection
 */
@Getter
@RequiredArgsConstructor
public class PersistedCollection implements LPSubjectCollection {
    private final LuckPermsService service;
    private final String identifier;
    private final boolean transientHasPriority;

    @Getter(AccessLevel.NONE)
    private final LoadingCache<String, PersistedSubject> subjects = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, PersistedSubject>() {
                @Override
                public PersistedSubject load(String s) {
                    return new PersistedSubject(s, service, PersistedCollection.this);
                }
            });

    public void loadAll() {
        Map<String, SubjectDataHolder> holders = service.getStorage().loadAllFromFile(identifier);
        for (Map.Entry<String, SubjectDataHolder> e : holders.entrySet()) {
            PersistedSubject subject = get(e.getKey());
            subject.loadData(e.getValue());
        }
    }

    @Override
    public PersistedSubject get(@NonNull String id) {
        return subjects.getUnchecked(id.toLowerCase());
    }

    @Override
    public boolean hasRegistered(@NonNull String id) {
        return subjects.asMap().containsKey(id.toLowerCase());
    }

    @Override
    public Collection<LPSubject> getSubjects() {
        return subjects.asMap().values().stream().map(s -> (LPSubject) s).collect(ImmutableCollectors.toImmutableList());
    }

    @Override
    public Map<LPSubject, Boolean> getWithPermission(@NonNull ContextSet contexts, @NonNull String node) {
        ImmutableMap.Builder<LPSubject, Boolean> m = ImmutableMap.builder();
        for (LPSubject subject : subjects.asMap().values()) {
            Tristate ts = subject.getPermissionValue(contexts, node);
            if (ts != Tristate.UNDEFINED) {
                m.put(subject, ts.asBoolean());
            }

        }
        return m.build();
    }

    @Override
    public SubjectReference getDefaultSubject() {
        return SubjectReference.of("defaults", identifier);
    }

    @Override
    public boolean getTransientHasPriority() {
        return transientHasPriority;
    }
}
