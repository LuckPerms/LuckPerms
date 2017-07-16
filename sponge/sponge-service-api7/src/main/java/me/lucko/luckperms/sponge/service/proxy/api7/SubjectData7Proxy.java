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
import me.lucko.luckperms.sponge.service.model.CompatibilityUtil;
import me.lucko.luckperms.sponge.service.model.LPPermissionService;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectData;
import me.lucko.luckperms.sponge.service.model.SubjectReference;

import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.util.Tristate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

/**
 * Proxies a LuckPerms Subject to implement {@link SubjectData}.
 */
@SuppressWarnings("unchecked")
@RequiredArgsConstructor
public class SubjectData7Proxy implements SubjectData {
    private final LPPermissionService service;
    private final SubjectReference ref;
    private final boolean enduring;

    private CompletableFuture<LPSubjectData> getHandle() {
        return enduring ? ref.resolveLp().thenApply(LPSubject::getSubjectData) : ref.resolveLp().thenApply(LPSubject::getTransientSubjectData);
    }

    @Override
    public Map<Set<Context>, Map<String, Boolean>> getAllPermissions() {
        return (Map) getHandle().thenApply(handle -> {
            return handle.getAllPermissions().entrySet().stream()
                    .collect(ImmutableCollectors.toImmutableMap(
                            e -> CompatibilityUtil.convertContexts(e.getKey()),
                            Map.Entry::getValue
                    ));
        }).join();
    }

    @Override
    public Map<String, Boolean> getPermissions(Set<Context> contexts) {
        return getHandle().thenApply(handle -> handle.getPermissions(CompatibilityUtil.convertContexts(contexts))).join();
    }

    @Override
    public CompletableFuture<Boolean> setPermission(Set<Context> contexts, String permission, Tristate value) {
        return getHandle().thenCompose(handle -> {
            return handle.setPermission(
                    CompatibilityUtil.convertContexts(contexts),
                    permission,
                    CompatibilityUtil.convertTristate(value)
            );
        });
    }

    @Override
    public CompletableFuture<Boolean> clearPermissions() {
        return getHandle().thenCompose(LPSubjectData::clearPermissions);
    }

    @Override
    public CompletableFuture<Boolean> clearPermissions(Set<Context> contexts) {
        return getHandle().thenCompose(handle -> handle.clearPermissions(CompatibilityUtil.convertContexts(contexts)));
    }

    @Override
    public Map<Set<Context>, List<org.spongepowered.api.service.permission.SubjectReference>> getAllParents() {
        return (Map) getHandle().thenApply(handle -> {
            return handle.getAllParents().entrySet().stream()
                    .collect(ImmutableCollectors.toImmutableMap(
                            e -> CompatibilityUtil.convertContexts(e.getKey()),
                            Map.Entry::getValue
                    ));
        }).join();
    }

    @Override
    public List<org.spongepowered.api.service.permission.SubjectReference> getParents(Set<Context> contexts) {
        return (List) getHandle().thenApply(handle -> handle.getParents(CompatibilityUtil.convertContexts(contexts))).join();
    }

    @Override
    public CompletableFuture<Boolean> addParent(Set<Context> contexts, org.spongepowered.api.service.permission.SubjectReference ref) {
        return getHandle().thenCompose(handle -> handle.addParent(CompatibilityUtil.convertContexts(contexts), SubjectReference.cast(service, ref)));
    }

    @Override
    public CompletableFuture<Boolean> removeParent(Set<Context> contexts, org.spongepowered.api.service.permission.SubjectReference ref) {
        return getHandle().thenCompose(handle -> handle.removeParent(CompatibilityUtil.convertContexts(contexts), SubjectReference.cast(service, ref)));
    }

    @Override
    public CompletableFuture<Boolean> clearParents() {
        return getHandle().thenCompose(LPSubjectData::clearParents);
    }

    @Override
    public CompletableFuture<Boolean> clearParents(Set<Context> contexts) {
        return getHandle().thenCompose(handle -> handle.clearParents(CompatibilityUtil.convertContexts(contexts)));
    }

    @Override
    public Map<Set<Context>, Map<String, String>> getAllOptions() {
        return (Map) getHandle().thenApply(handle -> {
            return handle.getAllOptions().entrySet().stream()
                    .collect(ImmutableCollectors.toImmutableMap(
                            e -> CompatibilityUtil.convertContexts(e.getKey()),
                            Map.Entry::getValue
                    ));
        }).join();
    }

    @Override
    public Map<String, String> getOptions(Set<Context> contexts) {
        return getHandle().thenApply(handle -> handle.getOptions(CompatibilityUtil.convertContexts(contexts))).join();
    }

    @Override
    public CompletableFuture<Boolean> setOption(Set<Context> contexts, String key, @Nullable String value) {
        if (value == null) {
            return getHandle().thenCompose(handle -> handle.unsetOption(CompatibilityUtil.convertContexts(contexts), key));
        } else {
            return getHandle().thenCompose(handle -> handle.setOption(CompatibilityUtil.convertContexts(contexts), key, value));
        }
    }

    @Override
    public CompletableFuture<Boolean> clearOptions() {
        return getHandle().thenCompose(LPSubjectData::clearOptions);
    }

    @Override
    public CompletableFuture<Boolean> clearOptions(Set<Context> contexts) {
        return getHandle().thenCompose(handle -> handle.clearOptions(CompatibilityUtil.convertContexts(contexts)));
    }
}
