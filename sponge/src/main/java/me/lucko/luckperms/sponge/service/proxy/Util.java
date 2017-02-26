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

package me.lucko.luckperms.sponge.service.proxy;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.utils.ImmutableCollectors;

import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.util.Tristate;

import java.util.Set;

@UtilityClass
public class Util {
    private static final LoadingCache<Set<Context>, ImmutableContextSet> SPONGE_TO_LP_CACHE = CacheBuilder.newBuilder()
            .build(new CacheLoader<Set<Context>, ImmutableContextSet>() {
                @Override
                public ImmutableContextSet load(Set<Context> contexts) {
                    return ContextSet.fromEntries(contexts);
                }
            });

    private static final LoadingCache<ImmutableContextSet, Set<Context>> LP_TO_SPONGE_CACHE = CacheBuilder.newBuilder()
            .build(new CacheLoader<ImmutableContextSet, Set<Context>>() {
                @Override
                public Set<Context> load(ImmutableContextSet set) {
                    return set.toSet().stream().map(e -> new Context(e.getKey(), e.getValue())).collect(ImmutableCollectors.toImmutableSet());
                }
            });

    public static ContextSet convertContexts(@NonNull Set<Context> contexts) {
        return SPONGE_TO_LP_CACHE.getUnchecked(contexts);
    }

    public static Set<Context> convertContexts(@NonNull ContextSet contexts) {
        return LP_TO_SPONGE_CACHE.getUnchecked(contexts.makeImmutable());
    }

    public static Tristate convertTristate(me.lucko.luckperms.api.Tristate tristate) {
        switch (tristate) {
            case TRUE:
                return Tristate.TRUE;
            case FALSE:
                return Tristate.FALSE;
            default:
                return Tristate.UNDEFINED;
        }
    }

    public static me.lucko.luckperms.api.Tristate convertTristate(Tristate tristate) {
        switch (tristate) {
            case TRUE:
                return me.lucko.luckperms.api.Tristate.TRUE;
            case FALSE:
                return me.lucko.luckperms.api.Tristate.FALSE;
            default:
                return me.lucko.luckperms.api.Tristate.UNDEFINED;
        }
    }

}
