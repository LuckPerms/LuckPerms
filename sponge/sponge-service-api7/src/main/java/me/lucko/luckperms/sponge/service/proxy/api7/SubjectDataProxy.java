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

import me.lucko.luckperms.common.utils.ImmutableCollectors;
import me.lucko.luckperms.sponge.service.CompatibilityUtil;
import me.lucko.luckperms.sponge.service.model.LPPermissionService;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectData;
import me.lucko.luckperms.sponge.service.reference.LPSubjectReference;
import me.lucko.luckperms.sponge.service.reference.SubjectReferenceFactory;

import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.util.Tristate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@SuppressWarnings("unchecked")
public final class SubjectDataProxy implements SubjectData {
    private final LPPermissionService service;
    private final LPSubjectReference ref;
    private final boolean enduring;

    public SubjectDataProxy(LPPermissionService service, LPSubjectReference ref, boolean enduring) {
        this.service = service;
        this.ref = ref;
        this.enduring = enduring;
    }

    private CompletableFuture<LPSubjectData> handle() {
        return this.enduring ?
                this.ref.resolveLp().thenApply(LPSubject::getSubjectData) :
                this.ref.resolveLp().thenApply(LPSubject::getTransientSubjectData);
    }

    @Nonnull
    @Override
    public Map<Set<Context>, Map<String, Boolean>> getAllPermissions() {
        return (Map) handle().thenApply(handle -> handle.getAllPermissions().entrySet().stream()
                .collect(ImmutableCollectors.toMap(
                        e -> CompatibilityUtil.convertContexts(e.getKey()),
                        Map.Entry::getValue
                ))).join();
    }

    @Nonnull
    @Override
    public Map<String, Boolean> getPermissions(@Nonnull Set<Context> contexts) {
        return handle().thenApply(handle -> handle.getPermissions(CompatibilityUtil.convertContexts(contexts))).join();
    }

    @Nonnull
    @Override
    public CompletableFuture<Boolean> setPermission(@Nonnull Set<Context> contexts, @Nonnull String permission, @Nonnull Tristate value) {
        return handle().thenCompose(handle -> handle.setPermission(
                CompatibilityUtil.convertContexts(contexts),
                permission,
                CompatibilityUtil.convertTristate(value)
        ));
    }

    @Nonnull
    @Override
    public CompletableFuture<Boolean> clearPermissions() {
        return handle().thenCompose(LPSubjectData::clearPermissions);
    }

    @Nonnull
    @Override
    public CompletableFuture<Boolean> clearPermissions(@Nonnull Set<Context> contexts) {
        return handle().thenCompose(handle -> handle.clearPermissions(CompatibilityUtil.convertContexts(contexts)));
    }

    @Nonnull
    @Override
    public Map<Set<Context>, List<org.spongepowered.api.service.permission.SubjectReference>> getAllParents() {
        return (Map) handle().thenApply(handle -> handle.getAllParents().entrySet().stream()
                .collect(ImmutableCollectors.toMap(
                        e -> CompatibilityUtil.convertContexts(e.getKey()),
                        Map.Entry::getValue
                ))).join();
    }

    @Nonnull
    @Override
    public List<org.spongepowered.api.service.permission.SubjectReference> getParents(@Nonnull Set<Context> contexts) {
        return (List) handle().thenApply(handle -> handle.getParents(CompatibilityUtil.convertContexts(contexts))).join();
    }

    @Nonnull
    @Override
    public CompletableFuture<Boolean> addParent(@Nonnull Set<Context> contexts, @Nonnull org.spongepowered.api.service.permission.SubjectReference ref) {
        return handle().thenCompose(handle -> handle.addParent(CompatibilityUtil.convertContexts(contexts), SubjectReferenceFactory.obtain(this.service, ref)));
    }

    @Nonnull
    @Override
    public CompletableFuture<Boolean> removeParent(@Nonnull Set<Context> contexts, @Nonnull org.spongepowered.api.service.permission.SubjectReference ref) {
        return handle().thenCompose(handle -> handle.removeParent(CompatibilityUtil.convertContexts(contexts), SubjectReferenceFactory.obtain(this.service, ref)));
    }

    @Nonnull
    @Override
    public CompletableFuture<Boolean> clearParents() {
        return handle().thenCompose(LPSubjectData::clearParents);
    }

    @Nonnull
    @Override
    public CompletableFuture<Boolean> clearParents(@Nonnull Set<Context> contexts) {
        return handle().thenCompose(handle -> handle.clearParents(CompatibilityUtil.convertContexts(contexts)));
    }

    @Nonnull
    @Override
    public Map<Set<Context>, Map<String, String>> getAllOptions() {
        return (Map) handle().thenApply(handle -> handle.getAllOptions().entrySet().stream()
                .collect(ImmutableCollectors.toMap(
                        e -> CompatibilityUtil.convertContexts(e.getKey()),
                        Map.Entry::getValue
                ))).join();
    }

    @Nonnull
    @Override
    public Map<String, String> getOptions(@Nonnull Set<Context> contexts) {
        return handle().thenApply(handle -> handle.getOptions(CompatibilityUtil.convertContexts(contexts))).join();
    }

    @Nonnull
    @Override
    public CompletableFuture<Boolean> setOption(@Nonnull Set<Context> contexts, @Nonnull String key, @Nullable String value) {
        if (value == null) {
            return handle().thenCompose(handle -> handle.unsetOption(CompatibilityUtil.convertContexts(contexts), key));
        } else {
            return handle().thenCompose(handle -> handle.setOption(CompatibilityUtil.convertContexts(contexts), key, value));
        }
    }

    @Nonnull
    @Override
    public CompletableFuture<Boolean> clearOptions() {
        return handle().thenCompose(LPSubjectData::clearOptions);
    }

    @Nonnull
    @Override
    public CompletableFuture<Boolean> clearOptions(@Nonnull Set<Context> contexts) {
        return handle().thenCompose(handle -> handle.clearOptions(CompatibilityUtil.convertContexts(contexts)));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof SubjectDataProxy)) return false;
        final SubjectDataProxy other = (SubjectDataProxy) o;
        return this.ref.equals(other.ref) && this.enduring == other.enduring;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.ref.hashCode();
        result = result * PRIME + (this.enduring ? 79 : 97);
        return result;
    }

    @Override
    public String toString() {
        return "luckperms.api7.SubjectDataProxy(ref=" + this.ref + ", enduring=" + this.enduring + ")";
    }
}
