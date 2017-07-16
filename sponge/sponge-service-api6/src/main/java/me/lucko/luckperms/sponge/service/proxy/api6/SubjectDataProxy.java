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
import me.lucko.luckperms.sponge.service.model.CompatibilityUtil;
import me.lucko.luckperms.sponge.service.model.LPPermissionService;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectData;
import me.lucko.luckperms.sponge.service.model.SubjectReference;

import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.util.Tristate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unchecked")
@RequiredArgsConstructor
public class SubjectDataProxy implements SubjectData {
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
    public boolean setPermission(Set<Context> contexts, String permission, Tristate value) {
        getHandle().thenCompose(handle -> {
            return handle.setPermission(
                    CompatibilityUtil.convertContexts(contexts),
                    permission,
                    CompatibilityUtil.convertTristate(value)
            );
        });
        return true;
    }

    @Override
    public boolean clearPermissions() {
        getHandle().thenCompose(LPSubjectData::clearPermissions);
        return true;
    }

    @Override
    public boolean clearPermissions(Set<Context> contexts) {
        getHandle().thenCompose(handle -> handle.clearPermissions(CompatibilityUtil.convertContexts(contexts)));
        return true;
    }

    @Override
    public Map<Set<Context>, List<Subject>> getAllParents() {
        return (Map) getHandle().thenApply(handle -> {
            return handle.getAllParents().entrySet().stream()
                    .collect(ImmutableCollectors.toImmutableMap(
                            e -> CompatibilityUtil.convertContexts(e.getKey()),
                            e -> e.getValue().stream()
                                    .map(s -> new SubjectProxy(service, s))
                                    .collect(ImmutableCollectors.toImmutableList())
                            )
                    );
        }).join();
    }

    @Override
    public List<Subject> getParents(Set<Context> contexts) {
        return (List) getHandle().thenApply(handle -> {
            return handle.getParents(CompatibilityUtil.convertContexts(contexts)).stream()
                    .map(s -> new SubjectProxy(service, s))
                    .collect(ImmutableCollectors.toImmutableList());
        }).join();
    }

    @Override
    public boolean addParent(Set<Context> contexts, Subject parent) {
        getHandle().thenCompose(handle -> {
            return handle.addParent(
                    CompatibilityUtil.convertContexts(contexts),
                    service.newSubjectReference(
                            parent.getContainingCollection().getIdentifier(),
                            parent.getIdentifier()
                    )
            );
        });
        return true;
    }

    @Override
    public boolean removeParent(Set<Context> contexts, Subject parent) {
        getHandle().thenCompose(handle -> {
            return handle.removeParent(
                    CompatibilityUtil.convertContexts(contexts),
                    service.newSubjectReference(
                            parent.getContainingCollection().getIdentifier(),
                            parent.getIdentifier()
                    )
            );
        });
        return true;
    }

    @Override
    public boolean clearParents() {
        getHandle().thenCompose(LPSubjectData::clearParents);
        return true;
    }

    @Override
    public boolean clearParents(Set<Context> contexts) {
        ;
        return true;
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
    public boolean setOption(Set<Context> contexts, String key, String value) {
        if (value == null) {
            getHandle().thenCompose(handle -> handle.unsetOption(CompatibilityUtil.convertContexts(contexts), key));
            return true;
        } else {
            getHandle().thenCompose(handle -> handle.setOption(CompatibilityUtil.convertContexts(contexts), key, value));
            return true;
        }
    }

    @Override
    public boolean clearOptions(Set<Context> contexts) {
        getHandle().thenCompose(handle -> handle.clearOptions(CompatibilityUtil.convertContexts(contexts)));
        return true;
    }

    @Override
    public boolean clearOptions() {
        getHandle().thenCompose(LPSubjectData::clearOptions);
        return true;
    }
}
