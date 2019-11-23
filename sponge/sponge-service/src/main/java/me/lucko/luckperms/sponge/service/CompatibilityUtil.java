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

import com.google.common.collect.ImmutableSet;

import me.lucko.luckperms.common.context.contextset.ContextImpl;
import me.lucko.luckperms.common.context.contextset.ImmutableContextSetImpl;
import me.lucko.luckperms.sponge.service.context.DelegatingContextSet;
import me.lucko.luckperms.sponge.service.context.DelegatingImmutableContextSet;

import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.util.Tristate;

import org.spongepowered.api.service.context.Context;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Utility class for converting between Sponge and LuckPerms context and tristate classes
 */
public final class CompatibilityUtil {
    private CompatibilityUtil() {}

    private static final Set<Context> EMPTY = ImmutableSet.of();

    public static ImmutableContextSet convertContexts(Set<Context> contexts) {
        Objects.requireNonNull(contexts, "contexts");

        if (contexts instanceof DelegatingContextSet) {
            return ((DelegatingContextSet) contexts).getDelegate().immutableCopy();
        }

        if (contexts.isEmpty()) {
            return ImmutableContextSetImpl.EMPTY;
        }

        ImmutableContextSet.Builder builder = new ImmutableContextSetImpl.BuilderImpl();
        for (Map.Entry<String, String> entry : contexts) {
            builder.add(new ContextImpl(entry.getKey(), entry.getValue()));
        }
        return builder.build();
    }

    public static Set<Context> convertContexts(ContextSet contexts) {
        Objects.requireNonNull(contexts, "contexts");

        if (contexts.isEmpty()) {
            return EMPTY;
        }

        return new DelegatingImmutableContextSet(contexts.immutableCopy());
    }

    public static org.spongepowered.api.util.Tristate convertTristate(Tristate tristate) {
        Objects.requireNonNull(tristate, "tristate");
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
        Objects.requireNonNull(tristate, "tristate");
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
