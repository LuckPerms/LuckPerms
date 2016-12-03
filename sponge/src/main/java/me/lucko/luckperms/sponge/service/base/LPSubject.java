/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

package me.lucko.luckperms.sponge.service.base;

import lombok.NonNull;

import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.utils.ImmutableCollectors;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.service.references.SubjectCollectionReference;
import me.lucko.luckperms.sponge.service.references.SubjectReference;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.util.Tristate;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static me.lucko.luckperms.sponge.service.base.Util.convertContexts;
import static me.lucko.luckperms.sponge.service.base.Util.convertTristate;

public interface LPSubject extends Subject {

    @Override
    String getIdentifier();

    default Optional<String> getFriendlyIdentifier() {
        return Optional.empty();
    }

    @Override
    default Optional<CommandSource> getCommandSource() {
        return Optional.empty();
    }

    SubjectCollectionReference getParentCollection();

    LuckPermsService getService();

    default SubjectReference toReference() {
        return SubjectReference.of(getParentCollection().getCollection(), getIdentifier());
    }

    @Override
    LPSubjectData getSubjectData();

    @Override
    LPSubjectData getTransientSubjectData();

    me.lucko.luckperms.api.Tristate getPermissionValue(ContextSet contexts, String permission);

    boolean isChildOf(ContextSet contexts, SubjectReference parent);

    Set<SubjectReference> getParents(ContextSet contexts);

    Optional<String> getOption(ContextSet contexts, String key);

    ContextSet getActiveContextSet();


    /* Compat */

    @Override
    default LPSubjectCollection getContainingCollection() {
        return getParentCollection().resolve(getService());
    }

    @Deprecated
    @Override
    default boolean hasPermission(@NonNull Set<Context> contexts, @NonNull String permission) {
        return getPermissionValue(convertContexts(contexts), permission).asBoolean();
    }

    @Deprecated
    @Override
    default boolean hasPermission(@NonNull String permission) {
        return getPermissionValue(getActiveContextSet(), permission).asBoolean();
    }

    @Deprecated
    @Override
    default Tristate getPermissionValue(@NonNull Set<Context> contexts, @NonNull String permission) {
        return convertTristate(getPermissionValue(convertContexts(contexts), permission));
    }

    @Deprecated
    @Override
    default boolean isChildOf(@NonNull Subject parent) {
        return isChildOf(getActiveContextSet(), SubjectReference.of(parent));
    }

    @Deprecated
    @Override
    default boolean isChildOf(@NonNull Set<Context> contexts, @NonNull Subject parent) {
        return isChildOf(convertContexts(contexts), SubjectReference.of(parent));
    }

    @Deprecated
    @Override
    default List<Subject> getParents() {
        return getParents(getActiveContextSet()).stream().map(s -> s.resolve(getService())).collect(ImmutableCollectors.toImmutableList());
    }

    @Deprecated
    @Override
    default List<Subject> getParents(@NonNull Set<Context> contexts) {
        return getParents(convertContexts(contexts)).stream().map(s -> s.resolve(getService())).collect(ImmutableCollectors.toImmutableList());
    }

    @Deprecated
    @Override
    default Optional<String> getOption(@NonNull String key) {
        return getOption(getActiveContextSet(), key);
    }

    @Deprecated
    @Override
    default Optional<String> getOption(@NonNull Set<Context> contexts, @NonNull String key) {
        return getOption(getActiveContextSet(), key);
    }

    @Deprecated
    @Override
    default Set<Context> getActiveContexts() {
        return convertContexts(getActiveContextSet());
    }
}
