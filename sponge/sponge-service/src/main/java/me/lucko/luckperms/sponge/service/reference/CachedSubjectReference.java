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

package me.lucko.luckperms.sponge.service.reference;

import me.lucko.luckperms.sponge.service.model.LPPermissionService;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectReference;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.service.permission.Subject;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Represents a reference to a given Subject.
 *
 * Use of this class (or interface) should have no negative impact on
 * performance, as {@link #resolve()} calls are cached.
 */
final class CachedSubjectReference implements LPSubjectReference {

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
    private final @NonNull String collectionIdentifier;

    /**
     * The identifier of the subject
     */
    private final @NonNull String subjectIdentifier;

    // cache
    private long lastLookup = 0L;
    private WeakReference<LPSubject> cache = null;

    CachedSubjectReference(LPPermissionService service, String collectionIdentifier, String subjectIdentifier) {
        this.service = Objects.requireNonNull(service);
        this.collectionIdentifier = Objects.requireNonNull(collectionIdentifier);
        this.subjectIdentifier = Objects.requireNonNull(subjectIdentifier);
    }

    @Override
    public @NonNull String collectionIdentifier() {
        return this.collectionIdentifier;
    }

    @Override
    public @NonNull String subjectIdentifier() {
        return this.subjectIdentifier;
    }

    void fillCache(LPSubject subject) {
        LPSubject sub = tryCache();

        if (sub == null) {
            // if no value is currently cached, populate with the passed value
            this.lastLookup = System.currentTimeMillis();
            this.cache = new WeakReference<>(subject);
        } else if (sub == subject) {
            // if equal, reset the cache timeout
            this.lastLookup = System.currentTimeMillis();
        }
    }

    private LPSubject tryCache() {
        if (System.currentTimeMillis() - this.lastLookup < CACHE_TIME) {
            if (this.cache != null) {
                return this.cache.get();
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
        s = this.service.getCollection(this.collectionIdentifier).loadSubject(this.subjectIdentifier).join();

        // cache the result
        this.lastLookup = System.currentTimeMillis();
        this.cache = new WeakReference<>(s);
        return s;
    }

    @Override
    public @NonNull CompletableFuture<LPSubject> resolveLp() {
        // check if there is a cached value before loading
        LPSubject s = tryCache();
        if (s != null) {
            return CompletableFuture.completedFuture(s);
        }

        // load the subject
        return CompletableFuture.supplyAsync(this::resolveDirectly);
    }

    @Override
    public @NonNull CompletableFuture<Subject> resolve() {
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
        if (!(o instanceof LPSubjectReference)) return false;
        final LPSubjectReference other = (LPSubjectReference) o;
        return this.collectionIdentifier.equals(other.collectionIdentifier()) &&
                this.subjectIdentifier.equals(other.subjectIdentifier());
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
