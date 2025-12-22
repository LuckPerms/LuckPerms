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

package me.lucko.luckperms.common.context.manager;

import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.query.QueryOptions;

import java.util.UUID;

/**
 * Manages contexts for subjects.
 *
 * @param <S> the subject type
 * @param <P> the player type
 */
public interface ContextManager<S, P extends S> {

    QueryOptions getQueryOptions(S subject);

    ImmutableContextSet getContext(S subject);

    QueryOptions getStaticQueryOptions();

    ImmutableContextSet getStaticContext();

    UUID getUniqueId(P player);

    void signalContextUpdate(S subject);

    void invalidateCache(S subject);

    void registerCalculator(ContextCalculator<? super S> calculator);

    void unregisterCalculator(ContextCalculator<? super S> calculator);

    ImmutableContextSet getPotentialContexts();

    Class<S> getSubjectClass();

    Class<P> getPlayerClass();
}
