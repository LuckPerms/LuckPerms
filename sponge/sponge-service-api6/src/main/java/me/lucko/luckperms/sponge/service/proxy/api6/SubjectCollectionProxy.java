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

package me.lucko.luckperms.sponge.service.proxy.api6;

import me.lucko.luckperms.common.utils.ImmutableCollectors;
import me.lucko.luckperms.sponge.service.CompatibilityUtil;
import me.lucko.luckperms.sponge.service.model.LPPermissionService;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectCollection;

import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

@SuppressWarnings("unchecked")
public final class SubjectCollectionProxy implements SubjectCollection {
    private final LPPermissionService service;
    private final LPSubjectCollection handle;

    public SubjectCollectionProxy(LPPermissionService service, LPSubjectCollection handle) {
        this.service = service;
        this.handle = handle;
    }

    @Nonnull
    @Override
    public String getIdentifier() {
        return this.handle.getIdentifier();
    }

    @Nonnull
    @Override
    public Subject get(@Nonnull String s) {
        // force load the subject.
        // after this call, users will expect that the subject is loaded in memory.
        return this.handle.loadSubject(s).thenApply(LPSubject::sponge).join();
    }

    @Override
    public boolean hasRegistered(@Nonnull String s) {
        return this.handle.hasRegistered(s).join();
    }

    @Nonnull
    @Override
    public Iterable<Subject> getAllSubjects() {
        // this will lazily load all subjects. it will initially just get the identifiers of each subject, and will initialize dummy
        // providers for those identifiers. when any methods against the dummy are called, the actual data will be loaded.
        // this behaviour should be replaced when CompletableFutures are added to Sponge
        return (List) this.handle.getAllIdentifiers()
                .thenApply(ids -> ids.stream()
                        .map(s -> new SubjectProxy(this.service, this.service.getReferenceFactory().obtain(getIdentifier(), s)))
                        .collect(ImmutableCollectors.toList())
                ).join();
    }

    @Nonnull
    @Override
    public Map<Subject, Boolean> getAllWithPermission(@Nonnull String s) {
        // again, these methods will lazily load subjects.
        return (Map) this.handle.getAllWithPermission(s)
                .thenApply(map -> map.entrySet().stream()
                        .collect(ImmutableCollectors.toMap(
                                e -> new SubjectProxy(this.service, e.getKey()),
                                Map.Entry::getValue
                        ))
                ).join();
    }

    @Nonnull
    @Override
    public Map<Subject, Boolean> getAllWithPermission(@Nonnull Set<Context> set, @Nonnull String s) {
        return (Map) this.handle.getAllWithPermission(CompatibilityUtil.convertContexts(set), s)
                .thenApply(map -> map.entrySet().stream()
                        .collect(ImmutableCollectors.toMap(
                                e -> new SubjectProxy(this.service, e.getKey()),
                                Map.Entry::getValue
                        ))
                ).join();
    }

    @Nonnull
    @Override
    public Subject getDefaults() {
        return this.handle.getDefaults().sponge();
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof SubjectCollectionProxy && this.handle.equals(((SubjectCollectionProxy) o).handle);
    }

    @Override
    public int hashCode() {
        return this.handle.hashCode();
    }

    @Override
    public String toString() {
        return "luckperms.api6.SubjectCollectionProxy(handle=" + this.handle + ")";
    }
}
