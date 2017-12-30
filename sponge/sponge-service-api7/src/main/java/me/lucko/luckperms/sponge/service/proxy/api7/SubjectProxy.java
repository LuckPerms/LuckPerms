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

import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.sponge.service.CompatibilityUtil;
import me.lucko.luckperms.sponge.service.model.LPPermissionService;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.ProxiedSubject;
import me.lucko.luckperms.sponge.service.model.SubjectReference;
import me.lucko.luckperms.sponge.service.model.SubjectReferenceFactory;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.util.Tristate;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unchecked")
@RequiredArgsConstructor
public final class SubjectProxy implements Subject, ProxiedSubject {
    private final LPPermissionService service;
    private final SubjectReference ref;

    private CompletableFuture<LPSubject> getHandle() {
        return ref.resolveLp();
    }

    @Override
    public SubjectReference getReference() {
        return ref;
    }

    @Override
    public Optional<CommandSource> getCommandSource() {
        return getHandle().thenApply(LPSubject::getCommandSource).join();
    }

    @Override
    public SubjectCollection getContainingCollection() {
        return service.getCollection(ref.getCollectionIdentifier()).sponge();
    }

    @Override
    public org.spongepowered.api.service.permission.SubjectReference asSubjectReference() {
        return ref;
    }

    @Override
    public boolean isSubjectDataPersisted() {
        return true;
    }

    @Override
    public SubjectData getSubjectData() {
        return new SubjectDataProxy(service, ref, true);
    }

    @Override
    public SubjectData getTransientSubjectData() {
        return new SubjectDataProxy(service, ref, false);
    }

    @Override
    public boolean hasPermission(Set<Context> contexts, String permission) {
        return getHandle().thenApply(handle -> handle.getPermissionValue(CompatibilityUtil.convertContexts(contexts), permission).asBoolean()).join();
    }

    @Override
    public boolean hasPermission(String permission) {
        return getHandle().thenApply(handle -> handle.getPermissionValue(ImmutableContextSet.empty(), permission).asBoolean()).join();
    }

    @Override
    public Tristate getPermissionValue(Set<Context> contexts, String permission) {
        return getHandle().thenApply(handle -> CompatibilityUtil.convertTristate(handle.getPermissionValue(CompatibilityUtil.convertContexts(contexts), permission))).join();
    }

    @Override
    public boolean isChildOf(org.spongepowered.api.service.permission.SubjectReference parent) {
        return getHandle().thenApply(handle -> handle.isChildOf(ImmutableContextSet.empty(), SubjectReferenceFactory.obtain(service, parent))).join();
    }

    @Override
    public boolean isChildOf(Set<Context> contexts, org.spongepowered.api.service.permission.SubjectReference parent) {
        return getHandle().thenApply(handle -> handle.isChildOf(CompatibilityUtil.convertContexts(contexts), SubjectReferenceFactory.obtain(service, parent))).join();
    }

    @Override
    public List<org.spongepowered.api.service.permission.SubjectReference> getParents() {
        return (List) getHandle().thenApply(handle -> handle.getParents(ImmutableContextSet.empty())).join();
    }

    @Override
    public List<org.spongepowered.api.service.permission.SubjectReference> getParents(Set<Context> contexts) {
        return (List) getHandle().thenApply(handle -> handle.getParents(CompatibilityUtil.convertContexts(contexts))).join();
    }

    @Override
    public Optional<String> getOption(Set<Context> contexts, String key) {
        return getHandle().thenApply(handle -> handle.getOption(CompatibilityUtil.convertContexts(contexts), key)).join();
    }

    @Override
    public Optional<String> getOption(String key) {
        return getHandle().thenApply(handle -> handle.getOption(ImmutableContextSet.empty(), key)).join();
    }

    @Override
    public String getIdentifier() {
        return ref.getSubjectIdentifier();
    }

    @Override
    public Optional<String> getFriendlyIdentifier() {
        return getHandle().thenApply(LPSubject::getFriendlyIdentifier).join();
    }

    @Override
    public Set<Context> getActiveContexts() {
        return getHandle().thenApply(handle -> CompatibilityUtil.convertContexts(handle.getActiveContextSet())).join();
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof SubjectProxy && ref.equals(((SubjectProxy) o).ref);
    }

    @Override
    public int hashCode() {
        return ref.hashCode();
    }

    @Override
    public String toString() {
        return "luckperms.api7.SubjectProxy(ref=" + this.ref + ")";
    }
}
