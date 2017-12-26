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

package me.lucko.luckperms.sponge.service;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.sponge.service.context.DelegatingContextSet;
import me.lucko.luckperms.sponge.service.context.DelegatingImmutableContextSet;

import org.spongepowered.api.service.context.Context;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for converting between Sponge and LuckPerms context and tristate classes
 */
@UtilityClass
public class CompatibilityUtil {
    private static final LoadingCache<Set<Context>, ImmutableContextSet> SPONGE_TO_LP_CACHE = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(ImmutableContextSet::fromEntries);

    private static final LoadingCache<ImmutableContextSet, Set<Context>> LP_TO_SPONGE_CACHE = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(DelegatingImmutableContextSet::new);

    public static ImmutableContextSet convertContexts(@NonNull Set<Context> contexts) {
        if (contexts instanceof DelegatingContextSet) {
            return ((DelegatingContextSet) contexts).getDelegate().makeImmutable();
        }

        return SPONGE_TO_LP_CACHE.get(ImmutableSet.copyOf(contexts));
    }

    public static Set<Context> convertContexts(@NonNull ContextSet contexts) {
        return LP_TO_SPONGE_CACHE.get(contexts.makeImmutable());
    }

    public static org.spongepowered.api.util.Tristate convertTristate(Tristate tristate) {
        switch (tristate) {
            case TRUE:
                return org.spongepowered.api.util.Tristate.TRUE;
            case FALSE:
                return org.spongepowered.api.util.Tristate.FALSE;
            default:
                return org.spongepowered.api.util.Tristate.UNDEFINED;
        }
    }

    public static Tristate convertTristate(org.spongepowered.api.util.Tristate tristate) {
        switch (tristate) {
            case TRUE:
                return Tristate.TRUE;
            case FALSE:
                return Tristate.FALSE;
            default:
                return Tristate.UNDEFINED;
        }
    }

}
