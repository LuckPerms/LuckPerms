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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.utils.ImmutableCollectors;
import me.lucko.luckperms.sponge.service.references.SubjectReference;

import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.util.Tristate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import static me.lucko.luckperms.sponge.service.proxy.Util.convertContexts;
import static me.lucko.luckperms.sponge.service.proxy.Util.convertTristate;

public interface LPSubjectData extends SubjectData {

    LPSubject getParentSubject();

    Map<ImmutableContextSet, Map<String, Boolean>> getPermissions();

    default Map<String, Boolean> getPermissions(ContextSet contexts) {
        return ImmutableMap.copyOf(getPermissions().getOrDefault(contexts, ImmutableMap.of()));
    }

    boolean setPermission(ContextSet contexts, String permission, me.lucko.luckperms.api.Tristate value);

    @Override
    boolean clearPermissions();

    boolean clearPermissions(ContextSet contexts);

    Map<ImmutableContextSet, Set<SubjectReference>> getParents();

    default Set<SubjectReference> getParents(ContextSet contexts) {
        return ImmutableSet.copyOf(getParents().getOrDefault(contexts, ImmutableSet.of()));
    }

    boolean addParent(ContextSet contexts, SubjectReference parent);

    boolean removeParent(ContextSet contexts, SubjectReference parent);

    @Override
    boolean clearParents();

    boolean clearParents(ContextSet contexts);

    Map<ImmutableContextSet, Map<String, String>> getOptions();

    default Map<String, String> getOptions(ContextSet contexts) {
        return ImmutableMap.copyOf(getOptions().getOrDefault(contexts, ImmutableMap.of()));
    }

    boolean setOption(ContextSet contexts, String key, String value);

    boolean unsetOption(ContextSet contexts, String key);

    boolean clearOptions(ContextSet contexts);

    @Override
    boolean clearOptions();



    /* Compat */

    @Deprecated
    @Override
    default Map<Set<Context>, Map<String, Boolean>> getAllPermissions() {
        return getPermissions().entrySet().stream()
                .collect(ImmutableCollectors.toImmutableMap(
                        e -> convertContexts(e.getKey()),
                        e -> ImmutableMap.copyOf(e.getValue()))
                );
    }

    @Deprecated
    @Override
    default Map<String, Boolean> getPermissions(Set<Context> contexts) {
        return ImmutableMap.copyOf(getPermissions(convertContexts(contexts)));
    }

    @Deprecated
    @Override
    default boolean setPermission(Set<Context> contexts, String permission, Tristate value) {
        return setPermission(convertContexts(contexts), permission, convertTristate(value));
    }

    @Deprecated
    @Override
    default boolean clearPermissions(Set<Context> contexts) {
        return clearPermissions(convertContexts(contexts));
    }

    @Deprecated
    @Override
    default Map<Set<Context>, List<Subject>> getAllParents() {
        return getParents().entrySet().stream()
                .collect(ImmutableCollectors.toImmutableMap(
                        e -> convertContexts(e.getKey()),
                        e -> e.getValue().stream()
                                .map(s -> s.resolve(getParentSubject().getService()))
                                .collect(ImmutableCollectors.toImmutableList())
                        )
                );
    }

    @Deprecated
    @Override
    default List<Subject> getParents(Set<Context> contexts) {
        return getParents(convertContexts(contexts)).stream().map(s -> s.resolve(getParentSubject().getService())).collect(ImmutableCollectors.toImmutableList());
    }

    @Deprecated
    @Override
    default boolean addParent(Set<Context> contexts, Subject parent) {
        return addParent(convertContexts(contexts), SubjectReference.of(parent));
    }

    @Deprecated
    @Override
    default boolean removeParent(Set<Context> contexts, Subject parent) {
        return removeParent(convertContexts(contexts), SubjectReference.of(parent));
    }

    @Deprecated
    @Override
    default boolean clearParents(Set<Context> contexts) {
        return clearParents(convertContexts(contexts));
    }

    @Deprecated
    @Override
    default Map<Set<Context>, Map<String, String>> getAllOptions() {
        return getOptions().entrySet().stream()
                .collect(ImmutableCollectors.toImmutableMap(
                        e -> convertContexts(e.getKey()),
                        e -> ImmutableMap.copyOf(e.getValue()))
                );
    }

    @Deprecated
    @Override
    default Map<String, String> getOptions(Set<Context> contexts) {
        return ImmutableMap.copyOf(getOptions(convertContexts(contexts)));
    }

    @Deprecated
    @Override
    default boolean setOption(Set<Context> contexts, String key, @Nullable String value) {
        return value == null ? unsetOption(convertContexts(contexts), key) : setOption(convertContexts(contexts), key, value);
    }

    @Deprecated
    @Override
    default boolean clearOptions(Set<Context> contexts) {
        return clearOptions(convertContexts(contexts));
    }

}
