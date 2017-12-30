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

package me.lucko.luckperms.sponge.service.proxy.api7;

import lombok.RequiredArgsConstructor;

import me.lucko.luckperms.common.utils.ImmutableCollectors;
import me.lucko.luckperms.sponge.service.CompatibilityUtil;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectCollection;
import me.lucko.luckperms.sponge.service.model.SubjectReferenceFactory;

import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectReference;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

@SuppressWarnings("unchecked")
@RequiredArgsConstructor
public final class SubjectCollectionProxy implements SubjectCollection {
    private final LPSubjectCollection handle;

    @Override
    public String getIdentifier() {
        return handle.getIdentifier();
    }

    @Override
    public Predicate<String> getIdentifierValidityPredicate() {
        return handle.getIdentifierValidityPredicate();
    }

    @Override
    public CompletableFuture<Subject> loadSubject(String s) {
        return handle.loadSubject(s).thenApply(LPSubject::sponge);
    }

    @Override
    public Optional<Subject> getSubject(String s) {
        return handle.getSubject(s).map(LPSubject::sponge);
    }

    @Override
    public CompletableFuture<Boolean> hasSubject(String s) {
        return handle.hasRegistered(s);
    }

    @Override
    public CompletableFuture<Map<String, Subject>> loadSubjects(Set<String> set) {
        return handle.loadSubjects(set).thenApply(subs -> subs.stream().collect(ImmutableCollectors.toMap(LPSubject::getIdentifier, LPSubject::sponge)));
    }

    @Override
    public Collection<Subject> getLoadedSubjects() {
        return handle.getLoadedSubjects().stream().map(LPSubject::sponge).collect(ImmutableCollectors.toSet());
    }

    @Override
    public CompletableFuture<Set<String>> getAllIdentifiers() {
        return (CompletableFuture) handle.getAllIdentifiers();
    }

    @Override
    public SubjectReference newSubjectReference(String s) {
        return SubjectReferenceFactory.obtain(handle.getService(), getIdentifier(), s);
    }

    @Override
    public CompletableFuture<Map<SubjectReference, Boolean>> getAllWithPermission(String s) {
        return (CompletableFuture) handle.getAllWithPermission(s);
    }

    @Override
    public CompletableFuture<Map<SubjectReference, Boolean>> getAllWithPermission(Set<Context> set, String s) {
        return (CompletableFuture) handle.getAllWithPermission(CompatibilityUtil.convertContexts(set), s);
    }

    @Override
    public Map<Subject, Boolean> getLoadedWithPermission(String s) {
        return handle.getLoadedWithPermission(s).entrySet().stream()
                .collect(ImmutableCollectors.toMap(
                        sub -> sub.getKey().sponge(),
                        Map.Entry::getValue
                ));
    }

    @Override
    public Map<Subject, Boolean> getLoadedWithPermission(Set<Context> set, String s) {
        return handle.getLoadedWithPermission(CompatibilityUtil.convertContexts(set), s).entrySet().stream()
                .collect(ImmutableCollectors.toMap(
                        sub -> sub.getKey().sponge(),
                        Map.Entry::getValue
                ));
    }

    @Override
    public Subject getDefaults() {
        return handle.getDefaults().sponge();
    }

    @Override
    public void suggestUnload(String s) {
        // unused by lp
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
        return "luckperms.api7.SubjectCollectionProxy(handle=" + this.handle + ")";
    }

}
