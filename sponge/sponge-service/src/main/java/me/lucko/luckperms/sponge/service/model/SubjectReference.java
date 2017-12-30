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

import lombok.Getter;

import com.google.common.base.Preconditions;

import org.spongepowered.api.service.permission.Subject;

import java.lang.ref.WeakReference;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * Represents a reference to a given Subject.
 *
 * Use of this class (or interface) should have no negative impact on
 * performance, as {@link #resolve()} calls are cached.
 */
public final class SubjectReference implements org.spongepowered.api.service.permission.SubjectReference {

    /**
     * The time a subject instance should be cached in this reference
     */
    private static final long CACHE_TIME = TimeUnit.MINUTES.toMillis(5);

    /**
     * Reference to the permission service
     */
    private final LPPermissionService service;

    /**
     * The identifier of the collection which holds the subject
     */
    @Getter
    @Nonnull
    private final String collectionIdentifier;

    /**
     * The identifier of the subject
     */
    @Getter
    @Nonnull
    private final String subjectIdentifier;

    // cache
    private long lastLookup = 0L;
    private WeakReference<LPSubject> cache = null;

    SubjectReference(LPPermissionService service, String collectionIdentifier, String subjectIdentifier) {
        this.service = Preconditions.checkNotNull(service);
        this.collectionIdentifier = Preconditions.checkNotNull(collectionIdentifier);
        this.subjectIdentifier = Preconditions.checkNotNull(subjectIdentifier);
    }

    void fillCache(LPSubject subject) {
        LPSubject sub = tryCache();

        if (sub == null) {
            // if no value is currently cached, populate with the passed value
            lastLookup = System.currentTimeMillis();
            cache = new WeakReference<>(subject);
        } else if (sub == subject) {
            // if equal, reset the cache timeout
            lastLookup = System.currentTimeMillis();
        }
    }

    private LPSubject tryCache() {
        if ((System.currentTimeMillis() - lastLookup) < CACHE_TIME) {
            if (cache != null) {
                return cache.get();
            }
        }

        return null;
    }

    private synchronized LPSubject resolveDirectly() {
        /* As this method is synchronized, it's possible that since this was invoked
           the subject has been cached.
           Therefore, we check the cache again, and return if there's a value present.
           This effectively means all calls to this method block, but all return the same value
           at the same time once the data is loaded :) */

        LPSubject s = tryCache();
        if (s != null) {
            return s;
        }

        // subject isn't cached, so make a call to load it
        s = service.getCollection(collectionIdentifier).loadSubject(subjectIdentifier).join();

        // cache the result
        lastLookup = System.currentTimeMillis();
        cache = new WeakReference<>(s);
        return s;
    }

    public CompletableFuture<LPSubject> resolveLp() {
        // check if there is a cached value before loading
        LPSubject s = tryCache();
        if (s != null) {
            return CompletableFuture.completedFuture(s);
        }

        // load the subject
        return CompletableFuture.supplyAsync(this::resolveDirectly);
    }

    @Override
    public CompletableFuture<Subject> resolve() {
        // check if there is a cached value before loading
        LPSubject s = tryCache();
        if (s != null) {
            return CompletableFuture.completedFuture(s.sponge());
        }

        // load the subject
        return CompletableFuture.supplyAsync(() -> resolveDirectly().sponge());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof SubjectReference)) return false;
        final SubjectReference other = (SubjectReference) o;
        return this.collectionIdentifier.equals(other.collectionIdentifier) && this.subjectIdentifier.equals(other.subjectIdentifier);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.collectionIdentifier.hashCode();
        result = result * PRIME + this.subjectIdentifier.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "luckperms.SubjectReference(" +
                "collection=" + this.collectionIdentifier + ", " +
                "subject=" + this.subjectIdentifier + ")";
    }
}
