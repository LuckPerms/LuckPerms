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

import com.google.common.collect.ImmutableSet;

import me.lucko.luckperms.common.utils.ImmutableCollectors;
import me.lucko.luckperms.sponge.service.model.LPPermissionDescription;
import me.lucko.luckperms.sponge.service.model.LPPermissionService;
import me.lucko.luckperms.sponge.service.model.LPSubjectCollection;
import me.lucko.luckperms.sponge.service.model.SubjectReferenceFactory;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectReference;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

@RequiredArgsConstructor
public final class PermissionServiceProxy implements PermissionService {
    private final LPPermissionService handle;

    @Override
    public SubjectCollection getUserSubjects() {
        return handle.getUserSubjects().sponge();
    }

    @Override
    public SubjectCollection getGroupSubjects() {
        return handle.getGroupSubjects().sponge();
    }

    @Override
    public Subject getDefaults() {
        return handle.getDefaults().sponge();
    }

    @Override
    public Predicate<String> getIdentifierValidityPredicate() {
        return handle.getIdentifierValidityPredicate();
    }

    @Override
    public CompletableFuture<SubjectCollection> loadCollection(String s) {
        return CompletableFuture.completedFuture(handle.getCollection(s).sponge());
    }

    @Override
    public Optional<SubjectCollection> getCollection(String s) {
        return Optional.ofNullable(handle.getLoadedCollections().get(s.toLowerCase())).map(LPSubjectCollection::sponge);
    }

    @Override
    public CompletableFuture<Boolean> hasCollection(String s) {
        return CompletableFuture.completedFuture(handle.getLoadedCollections().containsKey(s.toLowerCase()));
    }

    @Override
    public Map<String, SubjectCollection> getLoadedCollections() {
        return handle.getLoadedCollections().entrySet().stream()
                .collect(ImmutableCollectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().sponge()
                ));
    }

    @Override
    public CompletableFuture<Set<String>> getAllIdentifiers() {
        return CompletableFuture.completedFuture(ImmutableSet.copyOf(handle.getLoadedCollections().keySet()));
    }

    @Override
    public SubjectReference newSubjectReference(String s, String s1) {
        return SubjectReferenceFactory.obtain(handle, s, s1);
    }

    @Override
    public PermissionDescription.Builder newDescriptionBuilder(Object o) {
        Optional<PluginContainer> container = Sponge.getGame().getPluginManager().fromInstance(o);
        if (!container.isPresent()) {
            throw new IllegalArgumentException("Couldn't find a plugin container for " + o.getClass().getSimpleName());
        }

        return new SimpleDescriptionBuilder(handle, container.get());
    }

    @Override
    public Optional<PermissionDescription> getDescription(String s) {
        return handle.getDescription(s).map(LPPermissionDescription::sponge);
    }

    @Override
    public Collection<PermissionDescription> getDescriptions() {
        return handle.getDescriptions().stream().map(LPPermissionDescription::sponge).collect(ImmutableCollectors.toSet());
    }

    @Override
    public void registerContextCalculator(ContextCalculator<Subject> contextCalculator) {
        handle.registerContextCalculator(contextCalculator);
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof PermissionServiceProxy && handle.equals(((PermissionServiceProxy) o).handle);
    }

    @Override
    public int hashCode() {
        return handle.hashCode();
    }

    @Override
    public String toString() {
        return "luckperms.api7.PermissionServiceProxy(handle=" + this.handle + ")";
    }
}
