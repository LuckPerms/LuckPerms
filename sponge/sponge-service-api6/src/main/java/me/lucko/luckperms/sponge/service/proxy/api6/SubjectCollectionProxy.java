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

import lombok.RequiredArgsConstructor;

import me.lucko.luckperms.common.utils.ImmutableCollectors;
import me.lucko.luckperms.sponge.service.CompatibilityUtil;
import me.lucko.luckperms.sponge.service.model.LPPermissionService;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectCollection;
import me.lucko.luckperms.sponge.service.model.SubjectReferenceFactory;

import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;

import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unchecked")
@RequiredArgsConstructor
public final class SubjectCollectionProxy implements SubjectCollection {
    private final LPPermissionService service;
    private final LPSubjectCollection handle;

    @Override
    public String getIdentifier() {
        return handle.getIdentifier();
    }

    @Override
    public Subject get(String s) {
        // force load the subject.
        // after this call, users will expect that the subject is loaded in memory.
        return handle.loadSubject(s).thenApply(LPSubject::sponge).join();
    }

    @Override
    public boolean hasRegistered(String s) {
        return handle.hasRegistered(s).join();
    }

    @Override
    public Iterable<Subject> getAllSubjects() {
        // this will lazily load all subjects. it will initially just get the identifiers of each subject, and will initialize dummy
        // providers for those identifiers. when any methods against the dummy are called, the actual data will be loaded.
        // this behaviour should be replaced when CompletableFutures are added to Sponge
        return (List) handle.getAllIdentifiers()
                .thenApply(ids -> ids.stream()
                        .map(s -> new SubjectProxy(service, SubjectReferenceFactory.obtain(service, getIdentifier(), s)))
                        .collect(ImmutableCollectors.toList())
                ).join();
    }

    @Override
    public Map<Subject, Boolean> getAllWithPermission(String s) {
        // again, these methods will lazily load subjects.
        return (Map) handle.getAllWithPermission(s)
                .thenApply(map -> map.entrySet().stream()
                        .collect(ImmutableCollectors.toMap(
                                e -> new SubjectProxy(service, e.getKey()),
                                Map.Entry::getValue
                        ))
                ).join();
    }

    @Override
    public Map<Subject, Boolean> getAllWithPermission(Set<Context> set, String s) {
        return (Map) handle.getAllWithPermission(CompatibilityUtil.convertContexts(set), s)
                .thenApply(map -> map.entrySet().stream()
                        .collect(ImmutableCollectors.toMap(
                                e -> new SubjectProxy(service, e.getKey()),
                                Map.Entry::getValue
                        ))
                ).join();
    }

    @Override
    public Subject getDefaults() {
        return handle.getDefaults().sponge();
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof SubjectCollectionProxy && handle.equals(((SubjectCollectionProxy) o).handle);
    }

    @Override
    public int hashCode() {
        return handle.hashCode();
    }

    @Override
    public String toString() {
        return "luckperms.api6.SubjectCollectionProxy(handle=" + this.handle + ")";
    }
}
