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
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static me.lucko.luckperms.sponge.service.base.Util.convertContexts;

public interface LPSubjectCollection extends SubjectCollection {

    @Override
    String getIdentifier();

    LuckPermsService getService();

    default SubjectCollectionReference toReference() {
        return SubjectCollectionReference.of(getIdentifier());
    }

    @Override
    LPSubject get(String identifier);

    @Override
    boolean hasRegistered(String identifier);

    Collection<LPSubject> getSubjects();

    default Map<LPSubject, Boolean> getWithPermission(String permission) {
        return getWithPermission(ContextSet.empty(), permission);
    }

    Map<LPSubject, Boolean> getWithPermission(ContextSet contexts, String permission);

    SubjectReference getDefaultSubject();

    @Deprecated
    @Override
    default Subject getDefaults() {
        return getDefaultSubject().resolve(getService());
    }

    @Deprecated
    @Override
    default Iterable<Subject> getAllSubjects() {
        return getSubjects().stream().collect(ImmutableCollectors.toImmutableList());
    }

    @Deprecated
    @Override
    default Map<Subject, Boolean> getAllWithPermission(@NonNull String permission) {
        return getWithPermission(permission).entrySet().stream().collect(ImmutableCollectors.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Deprecated
    @Override
    default Map<Subject, Boolean> getAllWithPermission(@NonNull Set<Context> contexts, @NonNull String permission) {
        return getWithPermission(convertContexts(contexts), permission).entrySet().stream().collect(ImmutableCollectors.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
