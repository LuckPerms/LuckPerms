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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import com.google.common.base.Splitter;

import org.spongepowered.api.service.permission.Subject;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@ToString(of = {"collectionIdentifier", "subjectIdentifier"})
@EqualsAndHashCode(of = {"collectionIdentifier", "subjectIdentifier"})
@RequiredArgsConstructor(staticName = "of")
public final class SubjectReference implements org.spongepowered.api.service.permission.SubjectReference {

    @Deprecated
    public static SubjectReference deserialize(LPPermissionService service, String s) {
        List<String> parts = Splitter.on('/').limit(2).splitToList(s);
        return of(service, parts.get(0), parts.get(1));
    }

    public static SubjectReference of(LPPermissionService service, Subject subject) {
        return of(service, subject.getContainingCollection().getIdentifier(), subject.getIdentifier());
    }

    public static SubjectReference cast(LPPermissionService service, org.spongepowered.api.service.permission.SubjectReference reference) {
        if (reference instanceof SubjectReference) {
            return ((SubjectReference) reference);
        } else {
            return of(service, reference.getCollectionIdentifier(), reference.getSubjectIdentifier());
        }
    }

    private final LPPermissionService service;

    @Getter
    private final String collectionIdentifier;

    @Getter
    private final String subjectIdentifier;

    private long lastLookup = 0L;
    private WeakReference<LPSubject> cache = null;

    private synchronized LPSubject resolveDirectly() {
        long sinceLast = System.currentTimeMillis() - lastLookup;

        // try the cache
        if (sinceLast < TimeUnit.SECONDS.toMillis(10)) {
            if (cache != null) {
                LPSubject s = cache.get();
                if (s != null) {
                    return s;
                }
            }
        }

        LPSubject s = service.getCollection(collectionIdentifier).loadSubject(subjectIdentifier).join();
        lastLookup = System.currentTimeMillis();
        cache = new WeakReference<>(s);
        return s;
    }

    public CompletableFuture<LPSubject> resolveLp() {
        long sinceLast = System.currentTimeMillis() - lastLookup;

        // try the cache
        if (sinceLast < TimeUnit.SECONDS.toMillis(10)) {
            if (cache != null) {
                LPSubject s = cache.get();
                if (s != null) {
                    return CompletableFuture.completedFuture(s);
                }
            }
        }

        return CompletableFuture.supplyAsync(this::resolveDirectly);
    }

    @Override
    public CompletableFuture<Subject> resolve() {
        long sinceLast = System.currentTimeMillis() - lastLookup;

        // try the cache
        if (sinceLast < TimeUnit.SECONDS.toMillis(10)) {
            if (cache != null) {
                LPSubject s = cache.get();
                if (s != null) {
                    return CompletableFuture.completedFuture(s.sponge());
                }
            }
        }

        return CompletableFuture.supplyAsync(() -> resolveDirectly().sponge());
    }
}
