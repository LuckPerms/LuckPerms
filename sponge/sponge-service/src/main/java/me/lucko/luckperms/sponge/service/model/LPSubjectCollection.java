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

package me.lucko.luckperms.sponge.service.model;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.query.dataorder.DataQueryOrder;
import org.spongepowered.api.service.permission.SubjectCollection;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * LuckPerms model for the Sponge {@link SubjectCollection}
 */
public interface LPSubjectCollection {

    SubjectCollection sponge();

    LPPermissionService getService();

    String getIdentifier();

    Predicate<String> getIdentifierValidityPredicate();

    // transient has priority for all collections except default
    default DataQueryOrder getResolutionOrder() {
        if (isDefaultsCollection()) {
            return DataQueryOrder.TRANSIENT_LAST;
        } else {
            return DataQueryOrder.TRANSIENT_FIRST;
        }
    }

    default boolean isDefaultsCollection() {
        return false;
    }

    CompletableFuture<LPSubject> loadSubject(String identifier);

    Optional<LPSubject> getSubject(String identifier);

    CompletableFuture<Boolean> hasRegistered(String identifier);

    CompletableFuture<ImmutableCollection<LPSubject>> loadSubjects(Iterable<String> identifiers);

    ImmutableCollection<LPSubject> getLoadedSubjects();

    CompletableFuture<ImmutableSet<String>> getAllIdentifiers();

    CompletableFuture<ImmutableMap<LPSubjectReference, Boolean>> getAllWithPermission(String permission);

    CompletableFuture<ImmutableMap<LPSubjectReference, Boolean>> getAllWithPermission(ImmutableContextSet contexts, String permission);

    ImmutableMap<LPSubject, Boolean> getLoadedWithPermission(String permission);

    ImmutableMap<LPSubject, Boolean> getLoadedWithPermission(ImmutableContextSet contexts, String permission);

    LPSubject getDefaults();

}
