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

package me.lucko.luckperms.sponge.service.proxy;

import me.lucko.luckperms.sponge.service.CompatibilityUtil;
import me.lucko.luckperms.sponge.service.model.LPPermissionService;
import me.lucko.luckperms.sponge.service.model.LPProxiedServiceObject;
import me.lucko.luckperms.sponge.service.model.LPProxiedSubject;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectReference;
import me.lucko.luckperms.sponge.service.model.LPSubjectUser;
import net.luckperms.api.query.QueryOptions;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.util.Tristate;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class SubjectProxy implements Subject, LPProxiedSubject, LPProxiedServiceObject {
    private final LPPermissionService service;
    private final LPSubjectReference ref;

    public SubjectProxy(LPPermissionService service, LPSubjectReference ref) {
        this.service = service;
        this.ref = ref;
    }

    private CompletableFuture<LPSubject> handle() {
        return this.ref.resolveLp();
    }

    @Override
    public @NonNull LPSubjectReference asSubjectReference() {
        return this.ref;
    }

    @Override
    public @NonNull QueryOptions getQueryOptions() {
        return this.service.getContextManager().getQueryOptions(this);
    }

    @Override
    public @NonNull SubjectCollection containingCollection() {
        return this.service.getCollection(this.ref.collectionIdentifier()).sponge();
    }

    @Override
    public boolean isSubjectDataPersisted() {
        return true;
    }

    @Override
    public SubjectData subjectData() {
        return new SubjectDataProxy(this.service, this.ref, true);
    }

    @Override
    public SubjectData transientSubjectData() {
        return new SubjectDataProxy(this.service, this.ref, false);
    }

    @Override
    public @NonNull Tristate permissionValue(@NonNull String permission, @NonNull Cause cause) {
        return handle().thenApply(handle -> CompatibilityUtil.convertTristate(handle.getPermissionValue(this.service.getContextsForCause(cause), permission))).join();
    }

    @Override
    public @NonNull Tristate permissionValue(@NonNull String permission, @NonNull Set<Context> contexts) {
        return handle().thenApply(handle -> CompatibilityUtil.convertTristate(handle.getPermissionValue(CompatibilityUtil.convertContexts(contexts), permission))).join();
    }

    @Override
    public boolean isChildOf(@NonNull SubjectReference parent, @NonNull Cause cause) {
        return handle().thenApply(handle -> handle.isChildOf(this.service.getContextsForCause(cause), this.service.getReferenceFactory().obtain(parent))).join();
    }

    @Override
    public boolean isChildOf(@NonNull SubjectReference parent, @NonNull Set<Context> contexts) {
        return handle().thenApply(handle -> handle.isChildOf(CompatibilityUtil.convertContexts(contexts), this.service.getReferenceFactory().obtain(parent))).join();
    }

    @Override
    public List<? extends SubjectReference> parents(@NonNull Cause cause) {
        return handle().thenApply(handle -> handle.getParents(this.service.getContextsForCause(cause))).join();
    }

    @Override
    public @NonNull List<? extends SubjectReference> parents(@NonNull Set<Context> contexts) {
        return handle().thenApply(handle -> handle.getParents(CompatibilityUtil.convertContexts(contexts))).join();
    }

    @Override
    public Optional<String> option(@NonNull String key, @NonNull Cause cause) {
        return handle().thenApply(handle -> handle.getOption(this.service.getContextsForCause(cause), key)).join();
    }

    @Override
    public @NonNull Optional<String> option(@NonNull String key, @NonNull Set<Context> contexts) {
        return handle().thenApply(handle -> handle.getOption(CompatibilityUtil.convertContexts(contexts), key)).join();
    }

    @Override
    public String identifier() {
        return this.ref.subjectIdentifier();
    }

    @Override
    public @NonNull Optional<String> friendlyIdentifier() {
        return handle().thenApply(LPSubject::getFriendlyIdentifier).join();
    }

    @Override
    public Optional<?> associatedObject() {
        if (this.ref.collectionIdentifier().equals(PermissionService.SUBJECTS_USER)) {
            LPSubject lpSubject = handle().join();
            if (lpSubject instanceof LPSubjectUser) {
                ServerPlayer player = ((LPSubjectUser) lpSubject).resolvePlayer().orElse(null);
                if (player != null) {
                    return Optional.of(player);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof SubjectProxy && this.ref.equals(((SubjectProxy) o).ref);
    }

    @Override
    public int hashCode() {
        return this.ref.hashCode();
    }

    @Override
    public String toString() {
        return "luckperms.SubjectProxy(ref=" + this.ref + ")";
    }
}
